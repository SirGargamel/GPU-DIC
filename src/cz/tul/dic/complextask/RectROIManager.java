package cz.tul.dic.complextask;

import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.data.deformation.DeformationLimit;
import cz.tul.dic.data.roi.CircularROI;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.roi.RectangleROI;
import cz.tul.dic.data.task.TaskDefaultValues;
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

    private static final int GAP = 5;
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
                    xLeft + GAP,
                    yTop,
                    xRight - GAP,
                    yBottom);
        }

        final StringBuilder sb = new StringBuilder();
        sb.append("Main ROI - [");
        sb.append(rect.getX1());
        sb.append("; ");
        sb.append(rect.getY1());
        sb.append("], [");
        sb.append(rect.getX2());
        sb.append("; ");
        sb.append(rect.getY2());
        sb.append("]");
        Logger.trace(sb);

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

        defLimits = TaskDefaultValues.DEFAULT_DEFORMATION_LIMITS_FIRST;

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
        defLimits[DeformationLimit.VMAX] = shiftBottom;
        setRois(round);
        
        rect = new RectangleROI(rect.getX1(), rect.getY1() + shiftTop, rect.getX2(), rect.getY2() + shiftBottom);
        setRois(nextRound);
    }

}
