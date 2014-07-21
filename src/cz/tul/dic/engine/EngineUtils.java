/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.tul.dic.engine;

import cz.tul.dic.ComputationException;
import cz.tul.dic.data.Facet;
import cz.tul.dic.data.deformation.DeformationUtils;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerChecker;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.data.task.splitter.TaskSplit;
import cz.tul.dic.engine.displacement.DisplacementCalculator;
import cz.tul.dic.engine.opencl.KernelType;
import cz.tul.dic.engine.opencl.interpolation.Interpolation;
import cz.tul.dic.engine.strain.StrainEstimator;
import cz.tul.dic.generators.DeformationGenerator;
import cz.tul.dic.generators.facet.FacetGenerator;
import cz.tul.dic.output.ExportMode;
import cz.tul.dic.output.ExportTask;
import cz.tul.dic.output.Exporter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Ječmen
 */
public class EngineUtils extends Observable {
    
    private static final EngineUtils instance;
    
    static {
        instance = new EngineUtils();
    }

    public static EngineUtils getInstance() {
        return instance;
    }

    public void computeTask(final TaskContainer tc) throws ComputationException, IOException {
        final int roundCount = TaskContainerUtils.getRounds(tc).size();
        tc.clearResultData();
        TaskContainerChecker.checkTaskValidity(tc);

        int r, nextR, currentRound = 0;
        for (Map.Entry<Integer, Integer> e : TaskContainerUtils.getRounds(tc).entrySet()) {
            r = e.getKey();
            nextR = e.getValue();

            computeRound(tc, r, nextR);
            currentRound++;
            setChanged();
            notifyObservers(new int[]{currentRound, roundCount});
            exportRound(tc, r);
        }
    }

    private void exportRound(final TaskContainer tc, final int round) throws IOException, ComputationException {
        Iterator<ExportTask> it = tc.getExports().iterator();
        ExportTask et;
        while (it.hasNext()) {
            et = it.next();
            if (et.getMode().equals(ExportMode.MAP) && et.getDataParams()[0] == round) {
                Exporter.export(tc, et);
                it.remove();
            }
        }
    }

    public void computeRound(final TaskContainer tc, final int index1, final int index2) throws ComputationException {
        Logger.trace("Computing round {0}.", index1 + 1);                

        final Engine engine = new Engine();
        // prepare parameters
        engine.setKernel((KernelType) tc.getParameter(TaskParameter.KERNEL));
        engine.setInterpolation((Interpolation) tc.getParameter(TaskParameter.INTERPOLATION));
        final TaskSplit taskSplit = (TaskSplit) tc.getParameter(TaskParameter.TASK_SPLIT_VARIANT);
        engine.setTaskSplitVariant(taskSplit);

        Object taskSplitValue = tc.getParameter(TaskParameter.TASK_SPLIT_VALUE);

        // prepare data
        final Map<ROI, List<Facet>> facets = FacetGenerator.generateFacets(tc, index1);
        Logger.trace("Facets generated.");

        final Map<ROI, double[]> deformations = new HashMap<>();
        for (ROI roi : tc.getRois(index1)) {
            deformations.put(roi, DeformationGenerator.generateDeformations(tc.getDeformationLimits(index1, roi)));
        }
        Logger.trace("Deformations generated.");

        // compute round        
        for (ROI roi : tc.getRois(index1)) {
            // OpenCL splitter needs dynamic data
            if (taskSplit != null && taskSplit.equals(TaskSplit.DYNAMIC)) {
                taskSplitValue = new Object[]{
                    TaskContainerUtils.getDeformationArrayLength(tc, index1, roi),
                    tc.getFacetSize(index1, roi),
                    tc.getDeformationLimits(index1, roi)};
            }

            // store result
            tc.setResult(index1, roi, engine.computeCorrelations(
                    tc.getImage(index1), tc.getImage(index2),
                    roi, facets.get(roi),
                    deformations.get(roi),
                    DeformationUtils.getDegreeFromLimits(tc.getDeformationLimits(index1, roi)),
                    TaskContainerUtils.getDeformationArrayLength(tc, index1, roi),
                    tc.getFacetSize(index1, roi), taskSplitValue));
        }

        DisplacementCalculator.computeDisplacement(tc, index1, facets);

        StrainEstimator.computeStrain(tc, index1);
                       
        Logger.debug("Computed round {0}.", index1);
    }

}
