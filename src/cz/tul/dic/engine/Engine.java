package cz.tul.dic.engine;

import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLDevice.Type;
import com.jogamp.opencl.CLErrorHandler;
import com.jogamp.opencl.CLPlatform;
import com.jogamp.opencl.util.Filter;
import cz.tul.dic.data.Coordinates;
import cz.tul.dic.data.Facet;
import cz.tul.dic.data.FacetUtils;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.deformation.DeformationDegree;
import cz.tul.dic.data.task.ComputationTask;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.data.task.splitter.TaskSplitter;
import cz.tul.dic.engine.opencl.Kernel;
import cz.tul.dic.engine.opencl.KernelType;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Jecmen
 */
public final class Engine {

    private static final Type DEVICE_TYPE = Type.GPU;
    private final CLPlatform platform;
    private final CLContext context;
    private final CLDevice device;

    public Engine() {
        final CLPlatform tmpP = CLPlatform.getDefault(new Filter<CLPlatform>() {

            @Override
            public boolean accept(CLPlatform i) {
                return i.getMaxFlopsDevice(Type.GPU) != null && i.listCLDevices(CLDevice.Type.CPU).length == 0;
            }
        });
        if (tmpP == null) {
            platform = CLPlatform.getDefault();
        } else {
            platform = tmpP;
        }

        final CLDevice tmpD = platform.getMaxFlopsDevice(DEVICE_TYPE);
        if (tmpD == null) {
            device = platform.getMaxFlopsDevice();
        } else {
            device = tmpD;
        }
        System.out.println("Using " + device);

        context = CLContext.create(device);
        context.addCLErrorHandler(new CLErrorHandler() {

            @Override
            public void onError(String string, ByteBuffer bb, long l) {
                System.err.println("CLError - " + string);
            }
        });
    }

    public void computeTask(final TaskContainer tc) throws IOException {
        final Kernel kernel = Kernel.createKernel((KernelType) tc.getParameter(TaskParameter.KERNEL));
        final int defArrayLength = TaskContainerUtils.getDeformationArrayLength(tc);
        kernel.prepareKernel(context, device, tc.getFacetSize(), (DeformationDegree) tc.getParameter(TaskParameter.DEFORMATION_DEGREE), defArrayLength);

        List<double[]> bestResults;
        final int roundCount = TaskContainerUtils.getRoundCount(tc);
        int facetCount;
        for (int round = 0; round < roundCount; round++) {
            facetCount = tc.getFacets(round).size();
            bestResults = new ArrayList<>(facetCount);
            for (int i = 0; i < facetCount; i++) {
                bestResults.add(null);
            }

            final Iterator<ComputationTask> it = TaskSplitter.prepareSplitter(tc, round);
            ComputationTask ct;
            while (it.hasNext()) {
                ct = it.next();
                ct.setResults(kernel.compute(ct.getImageA(), ct.getImageB(), ct.getFacets(), ct.getDeformations(), defArrayLength));
                kernel.finishRound();
                // pick best results for this computation task and discard ct data                          
                pickBestResultsForTask(ct, bestResults, tc, round);
            }
            // store data           
            tc.storeResult(bestResults, round);
            buildFinalResults(tc, round);
            Logger.trace("Finished round {0} out of {1}.", round + 1, roundCount);
        }

        kernel.finishComputation();
    }

    private void pickBestResultsForTask(final ComputationTask task, final List<double[]> bestResults, final TaskContainer tc, final int round) {
        final List<Facet> facets = tc.getFacets(round);
        final Comparator<Integer> candidatesComparator = new DeformationResultSorter(tc, round);

        final int facetCount = task.getFacets().size();
        final int deformationCount = TaskContainerUtils.getDeformationCount(tc);

        float val, best;
        final List<Integer> candidates = new ArrayList<>();
        int baseIndex, bestIndex, globalFacetIndex;
        float[] taskResults;
        for (int localFacetIndex = 0; localFacetIndex < facetCount; localFacetIndex++) {
            best = -Float.MAX_VALUE;

            taskResults = task.getResults();
            baseIndex = localFacetIndex * deformationCount;

            for (int def = 0; def < deformationCount; def++) {
                val = taskResults[baseIndex + def];
                if (val > best) {
                    best = val;

                    candidates.clear();
                    candidates.add(def);
                } else if (val == best) {
                    candidates.add(def);
                }
            }

            globalFacetIndex = facets.indexOf(task.getFacets().get(localFacetIndex));
            if (candidates.isEmpty()) {
                Logger.warn("No best value found for facet nr." + globalFacetIndex);
                bestResults.set(globalFacetIndex, new double[]{0, 0});
            } else {
                if (candidates.size() > 1) {
                    Collections.sort(candidates, candidatesComparator);
                }
                bestIndex = candidates.get(0);

                bestResults.set(globalFacetIndex, TaskContainerUtils.extractDeformation(tc, bestIndex));
            }
        }
    }

    private void buildFinalResults(final TaskContainer tc, final int round) {
        final Image img = tc.getImage(round);
        final List<Facet> facets = tc.getFacets(round);
        final List<double[]> results = tc.getResults(round);

        final int width = img.getWidth();
        final int height = img.getHeight();

        final double[][][] finalResults = new double[width][height][];
        final int[][] counter = new int[width][height];

        Facet f;
        double[] d = new double[Coordinates.DIMENSION];
        int x, y;
        Map<int[], double[]> deformedFacet;
        for (int i = 0; i < facets.size(); i++) {
            f = facets.get(i);
            d = results.get(i);

            deformedFacet = FacetUtils.deformFacet(f, d, (DeformationDegree) tc.getParameter(TaskParameter.DEFORMATION_DEGREE));
            for (Entry<int[], double[]> e : deformedFacet.entrySet()) {
                x = e.getKey()[Coordinates.X];
                y = e.getKey()[Coordinates.Y];
                if (finalResults[x][y] == null) {
                    finalResults[x][y] = new double[d.length];
                    System.arraycopy(d, 0, finalResults[x][y], 0, d.length);
                    counter[x][y] = 1;
                } else {
                    for (int k = 0; k < d.length; k++) {
                        finalResults[x][y][k] += d[k];
                    }
                    counter[x][y]++;
                }
            }
        }

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                for (int k = 0; k < d.length; k++) {
                    if (counter[i][j] > 1) {
                        finalResults[i][j][k] /= (double) counter[i][j];
                    } else if (counter[i][j] == 0) {
                        finalResults[i][j] = new double[d.length];
                    }
                }
            }
        }

        tc.storeFinalResults(finalResults, round);
    }

}
