package cz.tul.dic.engine;

import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLDevice.Type;
import com.jogamp.opencl.CLPlatform;
import com.jogamp.opencl.util.Filter;
import cz.tul.dic.ComputationException;
import cz.tul.dic.data.Coordinates;
import cz.tul.dic.data.Facet;
import cz.tul.dic.data.FacetUtils;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.deformation.DeformationDegree;
import cz.tul.dic.data.deformation.DeformationUtils;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.task.ComputationTask;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.data.task.splitter.TaskSplitter;
import cz.tul.dic.engine.cluster.Analyzer2D;
import cz.tul.dic.engine.opencl.Kernel;
import cz.tul.dic.engine.opencl.KernelType;
import cz.tul.dic.engine.opencl.interpolation.Interpolation;
import cz.tul.dic.generators.DeformationGenerator;
import cz.tul.dic.generators.facet.FacetGenerator;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.Set;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Jecmen
 */
public final class Engine extends Observable {

    private static final int BEST_RESULT_COUNT_MAX = 50;
    private static final double PRECISION = 0.1;
    private static final Type DEVICE_TYPE = Type.GPU;
    private final CLPlatform platform;
    private final CLContext context;
    private final CLDevice device;

    public Engine() {
        @SuppressWarnings("unchecked")
        final CLPlatform tmpP = CLPlatform.getDefault((Filter<CLPlatform>) (CLPlatform i) -> i.getMaxFlopsDevice(Type.GPU) != null && i.listCLDevices(CLDevice.Type.CPU).length == 0);
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
        context.addCLErrorHandler((String string, ByteBuffer bb, long l) -> {
            System.err.println("CLError - " + string);
        });
    }

    public void computeTask(final TaskContainer tc) throws ComputationException {
        final int roundCount = TaskContainerUtils.getRoundCount(tc);
        tc.clearResultData();
        setChanged();
        notifyObservers(new int[]{0, roundCount});
        for (int round = 0; round < roundCount; round++) {
            computeRound(tc, round);
            setChanged();
            notifyObservers(new int[]{round, roundCount});
        }
    }

    public void computeRound(final TaskContainer tc, final int round) throws ComputationException {
        final Kernel kernel = Kernel.createKernel((KernelType) tc.getParameter(TaskParameter.KERNEL));
        int facetCount, defArrayLength;
        List<double[][]> bestResults;

        final Set<ROI> currentROIs = tc.getRois(round);
        final Map<ROI, List<Facet>> facets = FacetGenerator.generateFacets(tc, round);
        final Map<ROI, double[]> deformations = DeformationGenerator.generateDeformations(tc, round);

        for (ROI roi : currentROIs) {
            defArrayLength = TaskContainerUtils.getDeformationArrayLength(tc, round, roi);
            kernel.prepareKernel(context, device, tc.getFacetSize(round, roi), DeformationUtils.getDegreeFromLimits(tc.getDeformationLimits(round, roi)), defArrayLength, (Interpolation) tc.getParameter(TaskParameter.INTERPOLATION));

            facetCount = facets.get(roi).size();
            bestResults = new ArrayList<>(facetCount);
            for (int i = 0; i < facetCount; i++) {
                bestResults.add(null);
            }

            final Iterator<ComputationTask> it = TaskSplitter.prepareSplitter(tc, round, facets.get(roi), deformations.get(roi));
            ComputationTask ct;
            while (it.hasNext()) {
                ct = it.next();
                ct.setResults(kernel.compute(ct.getImageA(), ct.getImageB(), ct.getFacets(), ct.getDeformations(), defArrayLength));
                kernel.finishRound();
                // pick best results for this computation task and discard ct data                          
                pickBestResultsForTask(ct, bestResults, tc, round, roi, facets, deformations);
            }
            // store data           
            tc.setResult(bestResults, round, roi);
        }
        buildFinalResults(tc, round, facets);
        kernel.finishComputation();
        Logger.trace("Computed round {0}.", round + 1);
    }

    private void pickBestResultsForTask(final ComputationTask task, final List<double[][]> bestResults, final TaskContainer tc, final int round, final ROI roi, final Map<ROI, List<Facet>> facetMap, final Map<ROI, double[]> deformationMap) throws ComputationException {
        final List<Facet> globalFacets = facetMap.get(roi);
        final Comparator<Integer> candidatesComparator = new DeformationResultSorter(tc, round, roi, deformationMap.get(roi));

        final List<Facet> localFacets = task.getFacets();
        final int facetCount = localFacets.size();
        final int deformationCount = TaskContainerUtils.getDeformationCount(tc, round, roi, deformationMap.get(roi));

        float val, best;
        final List<Integer> candidates = new ArrayList<>();
        int baseIndex, globalFacetIndex, l;
        float[] taskResults;
        double[][] bestResult;
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

            globalFacetIndex = globalFacets.indexOf(localFacets.get(localFacetIndex));
            if (globalFacetIndex < 0) {
                throw new IllegalArgumentException("Local facet not found in global registry.");
            }

            if (candidates.isEmpty()) {
                Logger.warn("No best value found for facet nr." + globalFacetIndex);
                bestResults.set(globalFacetIndex, new double[][]{{0, 0}});
            } else {
                if (candidates.size() > 1) {
                    Collections.sort(candidates, candidatesComparator);
                }

                l = Math.min(candidates.size(), BEST_RESULT_COUNT_MAX);
                bestResult = new double[l][];
                for (int i = 0; i < l; i++) {
                    bestResult[i] = TaskContainerUtils.extractDeformation(tc, candidates.get(i), round, roi, deformationMap.get(roi));
                }

                bestResults.set(globalFacetIndex, bestResult);
            }
        }
    }

    private void buildFinalResults(final TaskContainer tc, final int round, final Map<ROI, List<Facet>> facetMap) throws ComputationException {
        final Image img = tc.getImage(round);
        final int width = img.getWidth();
        final int height = img.getHeight();

        final double[][][] finalResults = new double[width][height][];
        @SuppressWarnings("unchecked")
        final Analyzer2D[][] counters = new Analyzer2D[width][height];
        List<Facet> facets;
        List<double[][]> results;
        Facet f;
        double[] d;
        int x, y;
        Analyzer2D counter;
        Map<int[], double[]> deformedFacet;
        DeformationDegree degree;
        for (ROI roi : tc.getRois(round)) {
            facets = facetMap.get(roi);
            results = tc.getResults(round, roi);

            degree = DeformationUtils.getDegree(results.get(0)[0]);

            for (int i = 0; i < facets.size(); i++) {
                f = facets.get(i);
                d = results.get(i)[0];

                deformedFacet = FacetUtils.deformFacet(f, d, degree);
                for (Entry<int[], double[]> e : deformedFacet.entrySet()) {
                    x = e.getKey()[Coordinates.X];
                    y = e.getKey()[Coordinates.Y];

                    counter = counters[x][y];
                    if (counter == null) {
                        counter = new Analyzer2D();
                        counter.setPrecision(PRECISION);
                        counters[x][y] = counter;
                    }
                    counter.addValue(e.getValue());
                }
            }
        }
        
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                counter = counters[i][j];
                if (counter != null) {
                    finalResults[i][j] = counter.findMajorValue();
                }
            }
        }

        tc.setPerPixelResult(finalResults, round);
    }

}
