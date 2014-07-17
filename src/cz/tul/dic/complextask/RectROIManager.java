package cz.tul.dic.complextask;

import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.Constants;
import cz.tul.dic.Utils;
import cz.tul.dic.data.roi.CircularROI;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.roi.RectangleROI;
import cz.tul.dic.data.task.TaskContainer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Jecmen
 */
public class RectROIManager extends ROIManager {
    
    private static final double ADJUST_COEFF_UP = 2.0;
    private static final double ADJUST_COEFF_DOWN = 0.75;
    private final CircleROIManager crm;
    private RectangleROI rect;

    public static RectROIManager prepareManager(TaskContainer tc, final CircleROIManager crm, final int initialRound) throws ComputationException, IOException {
        final TaskContainer tcR = tc.cloneInputTask();

        RectangleROI rect = null;
        final List<CircularROI> cRois = new ArrayList<>(4);
        for (ROI r : tc.getRois(initialRound)) {
            if (r instanceof CircularROI) {
                cRois.add((CircularROI) r);
            } else if (r instanceof RectangleROI) {
                if (rect == null) {
                    rect = (RectangleROI) r;
                } else {
                    throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Only one rectangle ROI allowed.");
                }
            }
        }
        Collections.sort(cRois, new RoiSorter());

        if (rect == null) {
            int xLeft = Math.min(cRois.get(0).getX2(), cRois.get(2).getX2());
            int yTop = Math.min(cRois.get(0).getY1(), cRois.get(1).getY1());
            int xRight = Math.min(cRois.get(1).getX1(), cRois.get(3).getX1());
            int yBottom = Math.min(cRois.get(2).getY2(), cRois.get(3).getY2());

            rect = new RectangleROI(
                    xLeft,
                    yTop,
                    xRight,
                    yBottom);
        }

        final HashSet<ROI> rois = new HashSet<>(1);
        rois.add(rect);
        tcR.setROIs(initialRound, rois);
        
        tc.getRois(initialRound).remove(rect);
        tcR.clearResultData();

        return new RectROIManager(tcR, crm, initialRound);
    }

    private RectROIManager(TaskContainer tc, final CircleROIManager crm, final int initialRound) throws ComputationException {
        super(tc);
        this.crm = crm;

        for (ROI r : tc.getRois(initialRound)) {
            if (r instanceof RectangleROI) {
                if (rect == null) {
                    rect = (RectangleROI) r;
                } else {
                    throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Only one rectangle ROI allowed.");
                }
            }
        }

        if (rect == null) {
            throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "No ReactangleROI specified.");
        }

        defLimits = Constants.DEFORMATION_LIMITS_FIRST;
        
        setRois(initialRound);
    }

    private void setRois(final int round) {
        final HashSet<ROI> rois = new HashSet<>(1);
        rois.add(rect);
        tc.setROIs(round, rois);
        tc.setDeformationLimits(round, rect, defLimits);
    }

    @Override
    public void generateNextRound(int round, int nextRound) {
        final double shiftTop = crm.getShiftTop();
        final double shiftBottom = crm.getShiftBottom();
        if (haveMoved(shiftTop, shiftBottom)) {
            estimateNewRectangleDeformationLimits(shiftTop, shiftBottom);
        }
        rect = new RectangleROI(rect.getX1(), rect.getY1() + crm.getShiftTop(), rect.getX2(), rect.getY2() + crm.getShiftBottom());

        setRois(nextRound);
    }

    private void estimateNewRectangleDeformationLimits(final double shiftTop, final double shiftBottom) {
        final double[] result = new double[defLimits.length];
        System.arraycopy(defLimits, 0, result, 0, defLimits.length);

        final double min = Math.min(shiftTop, shiftBottom);
        final double max = Math.max(shiftTop, shiftBottom);

        if (min < 0) {
            result[3] = adjustValue(min, defLimits[3]);
            result[12] = adjustElongation(min, defLimits[3], defLimits[12]);
            result[15] = adjustElongation(min, defLimits[3], defLimits[15]);
        }
        if (max > 0) {
            result[4] = adjustValue(max, defLimits[4]);
            result[13] = adjustElongation(max, defLimits[4], defLimits[13]);
            result[16] = adjustElongation(max, defLimits[4], defLimits[16]);
        }

        for (int i = 0; i < result.length; i++) {
            if (result[i] != defLimits[i]) {
                Logger.debug("New rect limits - " + Arrays.toString(result));
                break;
            }
        }

        defLimits = result;
    }

    private static double adjustElongation(final double value, final double limit, final double elong) {
        if (value != 0 && Math.signum(limit) != Math.signum(value)) {
            Logger.trace("Signum mismatch - {0} vs {1}", new Object[]{value, limit});
            return value;
        }
        final double val = Math.abs(value);
        final double lim = Math.abs(limit);

        final double result;
        if (val <= (lim / 3.0)) {
            result = elong * ADJUST_COEFF_DOWN;
        } else if (val <= (lim * 2 / 3.0)) {
            result = elong;
        } else {
            result = elong * ADJUST_COEFF_UP;
        }
        return result;
    }

    private static double adjustValue(final double value, final double limit) {
        return adjustElongation(value, limit, limit);
    }

}
