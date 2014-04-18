package cz.tul.dic.complextask;

import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.Utils;
import cz.tul.dic.data.roi.CircularROI;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.roi.RectangleROI;
import cz.tul.dic.data.task.TaskContainer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Jecmen
 */
public class RoiManager {

    private static final int MAX_SHIFT_DIFFERENCE = 3;
    private static final double MINIMAL_SHIFT = 0.25;
    private static final double[] DEFAULT_DEF_CIRCLE = new double[]{-1, 1, 0.5, -5, 5, 0.5};
    private static final double[] DEFAULT_DEF_RECT = new double[]{-5, 5, 0.5, -5, 5, 0.5, -0.5, 0.5, 0.5, -0.5, 0.5, 0.5, -0.5, 0.5, 0.5, -0.5, 0.5, 0.5};
    private static final int DEFAULT_DEF_RECT_PRECISION_SH = 15;
    private static final int DEFAULT_DEF_RECT_PRECISION_EL = 5;
    private CircularROI topLeft, topRight, bottomLeft, bottomRight;
    private RectangleROI rect;
    private double[] defLimitsCircle, defLimitsRect;

    public RoiManager(final Set<ROI> rois) throws ComputationException {
        final List<CircularROI> cRois = new ArrayList<>(4);
        for (ROI r : rois) {
            if (r instanceof CircularROI) {
                cRois.add((CircularROI) r);
            } else if (r instanceof RectangleROI) {
                if (rect == null) {
                    rect = (RectangleROI) r;
                } else {
                    throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Only one rectangle ROI allowed.");
                }
            } else {
                throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Unsupported type of ROI - " + r.getClass());
            }
        }

        if (cRois.size() != 4) {
            throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "4 circular ROIs needed.");
        }

        Collections.sort(cRois, new RoiSorter());
        topLeft = cRois.get(0);
        topRight = cRois.get(1);
        bottomLeft = cRois.get(2);
        bottomRight = cRois.get(3);

        if (rect == null) {
            int xLeft = Math.min(topLeft.getX2(), bottomLeft.getX2());
            int yTop = Math.min(topLeft.getY1(), topRight.getY1());
            int xRight = Math.min(topRight.getX1(), bottomRight.getX1());
            int yBottom = Math.min(bottomLeft.getY2(), bottomRight.getY2());

            rect = new RectangleROI(
                    xLeft,
                    yTop,
                    xRight,
                    yBottom);
        }

        defLimitsCircle = DEFAULT_DEF_CIRCLE;
        defLimitsRect = DEFAULT_DEF_RECT;
    }

    public Set<ROI> getROIs() {
        final Set<ROI> result = new HashSet<>();
        result.add(topLeft);
        result.add(topRight);
        result.add(bottomLeft);
        result.add(bottomRight);
        result.add(rect);

        return result;
    }

    public void detectROIShifts(final TaskContainer tc, final int prevRound, final int round) {
        // find new position of Circle ROIs
        //// determine shifts of circle ROIs from previous round
        final double shift0 = FacetDeformationAnalyzator.determineROIShift(tc, prevRound, topLeft);
        final double shift1 = FacetDeformationAnalyzator.determineROIShift(tc, prevRound, topRight);
        final double shift2 = FacetDeformationAnalyzator.determineROIShift(tc, prevRound, bottomLeft);
        final double shift3 = FacetDeformationAnalyzator.determineROIShift(tc, prevRound, bottomRight);
        Logger.debug(shift2 + ", " + shift3);
        //// check if left equals right
        if (Math.abs(shift2 - shift3) > MAX_SHIFT_DIFFERENCE) {
            Logger.warn(ComputationExceptionCause.FIXTURES_SHIFT_MISMATCH.toString().concat("-LOWER- ").concat(Double.toString(shift2)).concat(" vs ".concat(Double.toString(shift3))));
        }
        if (Math.abs(shift1 - shift0) > MAX_SHIFT_DIFFERENCE) {
            Logger.warn(ComputationExceptionCause.FIXTURES_SHIFT_MISMATCH.toString().concat("-UPPER- ").concat(Double.toString(shift0)).concat(" vs ".concat(Double.toString(shift1))));
        }
        // generate new Circle ROIs
        topLeft = new CircularROI(topLeft.getCenterX(), topLeft.getCenterY() + shift0, topLeft.getRadius());
        topRight = new CircularROI(topRight.getCenterX(), topRight.getCenterY() + shift1, topRight.getRadius());
        bottomLeft = new CircularROI(bottomLeft.getCenterX(), bottomLeft.getCenterY() + shift2, bottomLeft.getRadius());
        bottomRight = new CircularROI(bottomRight.getCenterX(), bottomRight.getCenterY() + shift3, bottomRight.getRadius());
        // shift rectangle ROI according to circle ROIs shifts
        rect = new RectangleROI(rect.getX1(), rect.getY1() + Math.min(shift0, shift1), rect.getX2(), rect.getY2() + Math.min(shift2, shift3));
        // calculate new deformation limits
        final double[] newLimits = estimateNewCircleDeformationLimits(shift2, shift3);
        if (newLimits != defLimitsCircle) {
            defLimitsCircle = newLimits;
            System.out.println("New circle limits - " + Utils.toString(defLimitsCircle));
            defLimitsRect = estimateNewRectangleDeformationLimits(defLimitsCircle, tc.getFacetSize(round, rect));
            System.out.println("New rect limits - " + Utils.toString(defLimitsRect));
        }
    }

    private double[] estimateNewCircleDeformationLimits(final double shift0, final double shift1) {
        final double[] result;
        if (haveMoved(shift0, shift1)) {
            result = new double[defLimitsCircle.length];
            System.arraycopy(defLimitsCircle, 0, result, 0, defLimitsCircle.length);

            double val = Math.max(shift0, shift1);
            if (val < 0) {
                result[3] = adjustValue(val, defLimitsCircle[3]);
                result[4] = -result[0] / 2.0;
            } else {
                result[4] = adjustValue(val, defLimitsCircle[4]);
                result[3] = -result[4] / 2.0;
            }
        } else {
            result = defLimitsCircle;
        }
        return result;
    }

    private static boolean haveMoved(final double shift0, final double shift1) {
        return Math.abs(shift0) > MINIMAL_SHIFT || Math.abs(shift1) > MINIMAL_SHIFT;
    }

    private static double adjustValue(final double value, final double limit) {
        if (Math.signum(limit) != Math.signum(value)) {
            throw new IllegalArgumentException("Values must have same sign.");
        }
        final double val = Math.abs(value);
        final double lim = Math.abs(limit);

        final double result;
        if (val <= (lim / 3.0)) {
            result = lim / 2.0;
        } else if (val < (lim * 2 / 3.0)) {
            result = lim;
        } else {
            result = lim * 2;
        }
        return result;
    }

    private static double[] estimateNewRectangleDeformationLimits(final double[] circleLimits, final int facetSize) {
        final double[] result = new double[18];
        // U
        result[0] = circleLimits[0];
        result[1] = circleLimits[1];
        result[2] = (result[1] - result[0]) / (double) DEFAULT_DEF_RECT_PRECISION_SH;
        // V
        result[3] = circleLimits[3];
        result[4] = circleLimits[4];
        result[5] = (result[4] - result[3]) / (double) DEFAULT_DEF_RECT_PRECISION_SH;
        // UX
        result[6] = 2 * calculateElongation(result[0], facetSize);
        result[7] = -result[6];
        result[8] = (result[7] - result[6]) / (double) DEFAULT_DEF_RECT_PRECISION_EL;
        // UY
        result[9] = result[6];
        result[10] = result[7];
        result[11] = result[8];
        // VX
        result[12] = 2 * calculateElongation(result[3], facetSize);
        result[13] = -result[12];
        result[14] = (result[13] - result[12]) / (double) DEFAULT_DEF_RECT_PRECISION_EL;
        // VY
        result[15] = result[12];
        result[16] = result[13];
        result[17] = result[14];

        return result;
    }

    private static double calculateElongation(final double max, final int facetSize) {
        return max / (double) facetSize;
    }

    public double[] getDefLimitsCircle() {
        return defLimitsCircle;
    }

    public double[] getDefLimitsRect() {
        return defLimitsRect;
    }

}
