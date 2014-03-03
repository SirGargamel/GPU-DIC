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
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.engine.opencl.Kernel;
import cz.tul.dic.engine.opencl.KernelType;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
        kernel.prepareKernel(context, device, tc);

        float[] roundResult;
        List<double[]> bestResults;
        final int roundCount = TaskContainerUtils.getRoundCount(tc);
        for (int round = 0; round < roundCount; round++) {
            roundResult = kernel.compute(tc, round);
            analyze(roundResult);
            // pick best values            
            bestResults = pickBestResults(roundResult, tc, tc.getFacets(round).size());
            // store data           
            tc.storeResult(bestResults, round);
            buildFinalResults(tc, round);
        }

        kernel.finish();
    }

    private List<double[]> pickBestResults(final float[] completeResults, final TaskContainer tc, final int facetCount) {
        final List<double[]> result = new ArrayList<>(facetCount);
        final Comparator<Integer> candidatesComparator = new DeformationResultSorter(tc);

        final int deformationCount = TaskContainerUtils.getDeformationCount(tc);

        float val, best;
        List<Integer> candidates = new LinkedList<>();
        int baseIndex, bestIndex;
        for (int facet = 0; facet < facetCount; facet++) {
            best = -Float.MAX_VALUE;
            baseIndex = facet * deformationCount;
            for (int def = 0; def < deformationCount; def++) {
                val = completeResults[baseIndex + def];
                if (val > best) {
                    best = val;

                    candidates.clear();
                    candidates.add(def);
                } else if (val == best) {
                    candidates.add(def);
                }
            }

            if (candidates.isEmpty()) {
//                System.err.println("No best value found for facet nr." + facet);
                result.add(new double[]{0, 0});
            } else {
                if (candidates.size() > 1) {
                    Collections.sort(candidates, candidatesComparator);
                }
                bestIndex = candidates.get(0);

                result.add(TaskContainerUtils.extractDeformation(tc, bestIndex));
            }
        }

        return result;
    }

    private void analyze(final float[] completeResults) {
        int counter = 0;
        int groupCount = 0;
        boolean lastNotNan = false;
        for (float f : completeResults) {
            if (Float.isNaN(f)) {
                counter++;
                lastNotNan = false;
            } else if (!lastNotNan) {
                lastNotNan = true;
                groupCount++;
            }
        }
        if (counter > 0) {
            System.out.println("Found " + counter + " NaN values in " + groupCount + " groups out of " + completeResults.length);
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
