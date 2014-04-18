package cz.tul.dic.complextask;

import cz.tul.dic.ComputationException;
import cz.tul.dic.data.roi.CircularROI;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.engine.Engine;
import cz.tul.dic.output.ExportMode;
import cz.tul.dic.output.ExportTask;
import cz.tul.dic.output.Exporter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Observable;
import java.util.Set;

/**
 *
 * @author Petr Jecmen
 */
public class ComplextTaskSolver extends Observable {

//    private static final int ROI_COUNT = 4;
//    private static final int MAX_SHIFT_DIFFERENCE = 3;
    private static final int ROI_CIRCLE_FS_DENOM = 3;
//    private static final double[] DEFAULT_DEF_CIRCLE = new double[]{-1, 1, 0.5, -5, 5, 0.5};
//    private static final double[] DEFAULT_DEF_RECT = new double[]{-5, 5, 0.5, -5, 5, 0.5, -0.5, 0.5, 0.5, -0.5, 0.5, 0.5, -0.5, 0.5, 0.5, -0.5, 0.5, 0.5};
//    private static final int DEFAULT_DEF_RECT_PRECISION_SH = 10;
//    private static final int DEFAULT_DEF_RECT_PRECISION_EL = 5;
//    private static final double MINIMAL_SHIFT = 0.25;
//    private double[] deformationLimitsCircle, deformationLimitsRect;
    private final Engine engine;

    public ComplextTaskSolver() {
//        deformationLimitsCircle = DEFAULT_DEF_CIRCLE;
//        deformationLimitsRect = DEFAULT_DEF_RECT;

        engine = new Engine();
    }

//    public void setDeformationCircle(double[] deformationCircle) {
//        this.deformationLimitsCircle = deformationCircle;
//    }
//
//    public void setDeformationRect(double[] deformationRect) {
//        this.deformationLimitsRect = deformationRect;
//    }
    public void solveComplexTask(final TaskContainer tc) throws ComputationException, IOException {
        final int roundCount = TaskContainerUtils.getRoundCount(tc);
        tc.clearResultData();

        final int[] rounds = (int[]) tc.getParameter(TaskParameter.ROUND_LIMITS);
        int currentRound = 0;
        final int baseRound = rounds[0];

        setChanged();
        notifyObservers(new int[]{currentRound, roundCount});

        final RoiManager rm = new RoiManager(tc.getRois(baseRound));

        final Set<ROI> rois = rm.getROIs();
        tc.setROIs(baseRound, rois);
        for (ROI roi : rois) {
            if (roi instanceof CircularROI) {
                tc.addFacetSize(baseRound, roi, (int) (((CircularROI) roi).getRadius() / ROI_CIRCLE_FS_DENOM));
                tc.setDeformationLimits(baseRound, roi, rm.getDefLimitsCircle());
            } else {
                tc.setDeformationLimits(baseRound, roi, rm.getDefLimitsRect());
            }
        }
        // compute first round     
        engine.computeRound(tc, baseRound, baseRound + 1);

        currentRound++;
        setChanged();
        notifyObservers(new int[]{currentRound, roundCount});
        exportRound(tc, baseRound);

        boolean skip = true;
        int prevR = baseRound;
        for (int round = 0; round < rounds.length; round += 2) {
            if (round > 0) {
                computeRound(tc, rounds[round - 1], rounds[round], prevR, rm);
                currentRound++;
                setChanged();
                notifyObservers(new int[]{currentRound, roundCount});
                exportRound(tc, rounds[round - 1]);
            }

            for (int r = rounds[round]; r < rounds[round + 1]; r++) {
                if (skip) {
                    skip = false;
                    continue;
                }

                computeRound(tc, r, r + 1, prevR, rm);
                currentRound++;
                setChanged();
                notifyObservers(new int[]{currentRound, roundCount});
                prevR = r;
                exportRound(tc, r);
            }
        }
    }

    private void computeRound(final TaskContainer tc, final int r, final int nextR, final int prevR, final RoiManager rm) throws ComputationException {
        rm.detectROIShifts(tc, prevR, r);
        tc.setROIs(r, rm.getROIs());

        CircularROI cr;
        for (ROI roi : rm.getROIs()) {
            if (roi instanceof CircularROI) {
                cr = (CircularROI) roi;
                tc.addFacetSize(r, roi, (int) (cr.getRadius() / ROI_CIRCLE_FS_DENOM));
                tc.setDeformationLimits(r, roi, rm.getDefLimitsCircle());
            } else {
                tc.setDeformationLimits(r, roi, rm.getDefLimitsRect());
            }
        }
        // compute round
        engine.computeRound(tc, r, nextR);
    }

    private void exportRound(final TaskContainer tc, final int round) throws IOException, ComputationException {
        Iterator<ExportTask> it = tc.getExports().iterator();
        ExportTask et;
        while (it.hasNext()) {
            et = it.next();
            if (et.getMode().equals(ExportMode.MAP) && et.getDataParams()[0] == round) {
                Exporter.export(et, tc);
                it.remove();
            }
        }
    }

}
