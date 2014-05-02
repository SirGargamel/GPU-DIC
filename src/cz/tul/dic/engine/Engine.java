package cz.tul.dic.engine;

import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice;
import cz.tul.dic.ComputationException;
import cz.tul.dic.data.Facet;
import cz.tul.dic.data.deformation.DeformationUtils;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.task.ComputationTask;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.data.task.splitter.TaskSplitter;
import cz.tul.dic.engine.displacement.DisplacementCalculator;
import cz.tul.dic.engine.opencl.DeviceManager;
import cz.tul.dic.engine.opencl.Kernel;
import cz.tul.dic.engine.opencl.KernelType;
import cz.tul.dic.engine.opencl.interpolation.Interpolation;
import cz.tul.dic.engine.strain.StrainEstimator;
import cz.tul.dic.generators.DeformationGenerator;
import cz.tul.dic.generators.facet.FacetGenerator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Set;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Jecmen
 */
public final class Engine extends Observable {

    private static final int BEST_RESULT_COUNT_MAX = 50;    
    private final CLContext context;
    private final CLDevice device;

    public Engine() {
        device = DeviceManager.getDevice();
        context = DeviceManager.getContext();
    }

    public void computeTask(final TaskContainer tc) throws ComputationException {
        final int roundCount = TaskContainerUtils.getRoundCount(tc);
        tc.clearResultData();
        setChanged();
        notifyObservers(new int[]{0, roundCount});

        final int[] rounds = (int[]) tc.getParameter(TaskParameter.ROUND_LIMITS);
        int currentRound = 0;
        for (int round = 0; round < rounds.length; round += 2) {
            if (round > 0) {
                computeRound(tc, rounds[round - 1], rounds[round]);
                currentRound++;
                setChanged();
                notifyObservers(new int[]{currentRound, roundCount});
            }

            for (int r = rounds[round]; r < rounds[round + 1]; r++) {
                computeRound(tc, r, r + 1);
                currentRound++;
                setChanged();
                notifyObservers(new int[]{currentRound, roundCount});
            }
        }
    }

    public void computeRound(final TaskContainer tc, final int index1, final int index2) throws ComputationException {
        Logger.trace("Preparing round {0}.", index1 + 1);
        final Kernel kernel = Kernel.createKernel((KernelType) tc.getParameter(TaskParameter.KERNEL));
        int facetCount, defArrayLength;
        List<double[][]> bestResults;

        final Set<ROI> currentROIs = tc.getRois(index1);
        final Map<ROI, List<Facet>> facets;
        final Map<ROI, double[]> deformations = new HashMap<>();

        facets = FacetGenerator.generateFacets(tc, index1);

        for (ROI roi : currentROIs) {
            deformations.put(roi, DeformationGenerator.generateDeformations(tc.getDeformationLimits(index1, roi)));
        }

        Logger.trace("Computing round {0}.", index1 + 1);
        for (ROI roi : currentROIs) {
            defArrayLength = TaskContainerUtils.getDeformationArrayLength(tc, index1, roi);
            kernel.prepareKernel(context, device, tc.getFacetSize(index1, roi), DeformationUtils.getDegreeFromLimits(tc.getDeformationLimits(index1, roi)), defArrayLength, (Interpolation) tc.getParameter(TaskParameter.INTERPOLATION));

            facetCount = facets.get(roi).size();
            bestResults = new ArrayList<>(facetCount);
            for (int i = 0; i < facetCount; i++) {
                bestResults.add(null);
            }

            final Iterator<ComputationTask> it = TaskSplitter.prepareSplitter(tc, index1, index2, facets.get(roi), deformations.get(roi), roi);
            ComputationTask ct;
            while (it.hasNext()) {
                ct = it.next();
                ct.setResults(kernel.compute(ct.getImageA(), ct.getImageB(), ct.getFacets(), ct.getDeformations(), defArrayLength));
                kernel.finishRound();
                // pick best results for this computation task and discard ct data                          
                pickBestResultsForTask(ct, bestResults, tc, index1, roi, facets, deformations);
            }
            // store data           
            tc.setResult(index1, roi, bestResults);
        }
        Logger.trace("Building results for round {0}.", index1 + 1);
        DisplacementCalculator.computeDisplacement(tc, index1, facets);
        Logger.trace("Estimating strain for round {0}.", index1 + 1);
        StrainEstimator.computeStrain(tc, index1);
        kernel.finishComputation();
        Logger.debug("Computed round {0}.", index1 + 1);
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

}
