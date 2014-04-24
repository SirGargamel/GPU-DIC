package cz.tul.dic.complextask;

import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.Utils;
import cz.tul.dic.data.Coordinates;
import cz.tul.dic.data.roi.CircularROI;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.roi.RectangleROI;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.engine.cluster.Analyzer1D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Jecmen
 */
public class RoiManagerOld {

    private static final double PRECISION = 0.1;
    private static final int ROI_CIRCLE_FS_DENOM = 3;
    private static final int MAX_SHIFT_DIFFERENCE = 3;
    private static final double MINIMAL_SHIFT = 0.25;
    private static final double PRECISION_RECT_ZERO = 0.5;
    private static final double PRECISION_RECT_FIRST = 0.25;
    private static final double PRECISION_CIRC_ZERO = 0.5;
    private static final double[] DEFAULT_DEF_LIM_CIRCLE = new double[]{-1, 1, PRECISION_CIRC_ZERO, -5, 5, PRECISION_CIRC_ZERO};
    private static final double[] DEFAULT_DEF_LIM_RECT = new double[]{
        -5, 5, PRECISION_RECT_ZERO, -5, 5, PRECISION_RECT_ZERO,
        -0.5, 0.5, PRECISION_RECT_FIRST, -0.5, 0.5, PRECISION_RECT_FIRST, -0.5, 0.5, PRECISION_RECT_FIRST, -0.5, 0.5, PRECISION_RECT_FIRST};
    private static final int MAX_SHIFT_X = 5;
    private static final double ADJUST_COEFF_UP = 1.5;
    private static final double ADJUST_COEFF_DOWN = 2;
    private final TaskContainer tc;
    private CircularROI topLeft, topRight, bottomLeft, bottomRight;
    private RectangleROI rect;
    private double[] defLimitsCircle, defLimitsRect;

    public RoiManagerOld(final TaskContainer tc, final int initialRound) throws ComputationException {
        this.tc = tc;

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

        // generate rectangle ROI if not present
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

        defLimitsCircle = DEFAULT_DEF_LIM_CIRCLE;
        defLimitsRect = DEFAULT_DEF_LIM_RECT;

        setROIs(initialRound);
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

    public boolean areLimitsReached(final TaskContainer tc, final int round) {
        boolean[] reachedRect = new boolean[Coordinates.DIMENSION * 2];
        boolean[] reachedCircle = new boolean[Coordinates.DIMENSION * 2];
        Arrays.fill(reachedRect, false);
        Arrays.fill(reachedCircle, false);

        final double[][][] results = tc.getDisplacement(round);
        double[] limits, values;
        if (results != null) {
            for (ROI roi : tc.getRois(round)) {
                limits = tc.getDeformationLimits(round, roi);
                if (limits == null) {
                    continue;
                }

                values = findLimits(round, roi);
                for (int i = 0; i < Coordinates.DIMENSION * 2; i++) {
                    if (Math.abs(values[i]) >= Math.abs(limits[i])) {
                        if (roi instanceof CircularROI) {
                            reachedCircle[i] = true;
                        } else {
                            reachedRect[i] = true;
                        }
                    }
                }
            }
        }

        boolean result = false;
        for (boolean b : reachedRect) {
            if (b) {
                increaseLimitsRect(reachedRect);
                checkLimits(defLimitsRect);
                result = true;
                break;
            }
        }

        for (boolean b : reachedCircle) {
            if (b) {
                increaseLimitsCircle(reachedCircle);
                checkLimits(defLimitsCircle);
                result = true;
                break;
            }
        }

        return result;
    }

    private double[] findLimits(final int round, final ROI roi) {
        final double[] result = new double[Coordinates.DIMENSION * 2];

        final int width = tc.getImage(round).getWidth();
        final int height = tc.getImage(round).getHeight();
        final double[][][] results = tc.getDisplacement(round);

        if (results != null) {
            for (int x = roi.getX1(); x <= roi.getX2(); x++) {
                for (int y = roi.getY1(); y <= roi.getY2(); y++) {
                    if (x < 0 || y < 0 || x >= width || y >= height || results[x][y] == null || !roi.isPointInside(x, y)) {
                        continue;
                    }

                    if (results[x][y][0] < result[Coordinates.X * 2]) {
                        result[Coordinates.X * 2] = results[x][y][0];
                    }
                    if (results[x][y][0] > result[Coordinates.X * 2 + 1]) {
                        result[Coordinates.X * 2 + 1] = results[x][y][0];
                    }
                    if (results[x][y][1] < result[Coordinates.Y * 2]) {
                        result[Coordinates.Y * 2] = results[x][y][1];
                    }
                    if (results[x][y][1] > result[Coordinates.Y * 2 + 1]) {
                        result[Coordinates.Y * 2 + 1] = results[x][y][1];
                    }

                }
            }
        }

        return result;
    }

    private void increaseLimitsRect(final boolean[] reached) {
        if (reached[Coordinates.X * 2]) {
            defLimitsRect[0] = ADJUST_COEFF_UP * defLimitsRect[0];
            defLimitsRect[6] = ADJUST_COEFF_UP * defLimitsRect[6];
            defLimitsRect[12] = ADJUST_COEFF_UP * defLimitsRect[12];
        }
        if (reached[Coordinates.X * 2 + 1]) {
            defLimitsRect[1] = ADJUST_COEFF_UP * defLimitsRect[1];
            defLimitsRect[7] = ADJUST_COEFF_UP * defLimitsRect[7];
            defLimitsRect[13] = ADJUST_COEFF_UP * defLimitsRect[13];
        }
        if (reached[Coordinates.Y * 2]) {
            defLimitsRect[3] = ADJUST_COEFF_UP * defLimitsRect[3];
            defLimitsRect[9] = ADJUST_COEFF_UP * defLimitsRect[9];
            defLimitsRect[15] = ADJUST_COEFF_UP * defLimitsRect[15];
        }
        if (reached[Coordinates.Y * 2 + 1]) {
            defLimitsRect[4] = ADJUST_COEFF_UP * defLimitsRect[4];
            defLimitsRect[10] = ADJUST_COEFF_UP * defLimitsRect[10];
            defLimitsRect[16] = ADJUST_COEFF_UP * defLimitsRect[16];
        }
        Logger.debug("Expanded rect limits - " + Utils.toString(defLimitsRect));
    }

    private void increaseLimitsCircle(final boolean[] reached) {
        if (reached[Coordinates.X * 2]) {
            defLimitsCircle[0] = ADJUST_COEFF_UP * defLimitsCircle[0];
        }
        if (reached[Coordinates.X * 2 + 1]) {
            defLimitsCircle[1] = ADJUST_COEFF_UP * defLimitsCircle[1];
        }
        if (reached[Coordinates.Y * 2]) {
            defLimitsCircle[3] = ADJUST_COEFF_UP * defLimitsCircle[3];
        }
        if (reached[Coordinates.Y * 2 + 1]) {
            defLimitsCircle[4] = ADJUST_COEFF_UP * defLimitsCircle[4];
        }
        Logger.debug("Expanded circle limits - " + Utils.toString(defLimitsCircle));
    }

    public void generateNextRound(final TaskContainer tc, final int round, final int nextRound) {
        // find new position of Circle ROIs
        //// determine shifts of circle ROIs from previous round
        final double shift0 = determineROIShift(round, topLeft);
        final double shift1 = determineROIShift(round, topRight);
        final double shift2 = determineROIShift(round, bottomLeft);
        final double shift3 = determineROIShift(round, bottomRight);
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
        if (haveMoved(shift2, shift3)) {
            estimateNewCircleDeformationLimits(shift2, shift3);
            estimateNewRectangleDeformationLimits(round);
        }

        setROIs(nextRound);
    }

    private double determineROIShift(final int round, final ROI roi) {
        final double[][][] results = tc.getDisplacement(round);
        final Analyzer1D analyzer = new Analyzer1D();
        analyzer.setPrecision(PRECISION);

        for (int x = roi.getX1(); x <= roi.getX2(); x++) {
            for (int y = roi.getY1(); y <= roi.getY2(); y++) {
                if (x > 0 && y > 0 && roi.isPointInside(x, y) && results[x][y] != null) {
                    analyzer.addValue(results[x][y][Coordinates.Y]);
                }
            }
        }

        return analyzer.findMajorValue();
    }

    private void estimateNewCircleDeformationLimits(final double shift0, final double shift1) {
        final double[] result;
        result = new double[defLimitsCircle.length];
        System.arraycopy(defLimitsCircle, 0, result, 0, defLimitsCircle.length);

        double val = Math.max(shift0, shift1);
        if (val < 0) {
            result[3] = adjustValue(val, defLimitsCircle[3]);
            result[4] = -result[3] / ADJUST_COEFF_UP;
        } else {
            result[4] = adjustValue(val, defLimitsCircle[4]);
            result[3] = -result[4] / ADJUST_COEFF_UP;
        }

        defLimitsCircle = result;
        checkLimits(defLimitsCircle);
        Logger.debug("New circle limits - " + Utils.toString(defLimitsCircle));
    }

    private static boolean haveMoved(final double shift0, final double shift1) {
        return Math.abs(shift0) > MINIMAL_SHIFT || Math.abs(shift1) > MINIMAL_SHIFT;
    }

    private void estimateNewRectangleDeformationLimits(final int round) {
        final double values[] = findLimits(round, rect);

        final double[] result = new double[18];        
        // U
        result[0] = adjustValue(values[0], defLimitsRect[0]);
        result[1] = adjustValue(values[1], defLimitsRect[1]);                
        // V
        result[3] = adjustValue(values[2], defLimitsRect[3]);
        result[4] = adjustValue(values[3], defLimitsRect[4]);
        // UX
        result[6] = adjustElongation(values[0], defLimitsRect[0], defLimitsRect[6]);
        result[7] = adjustElongation(values[1], defLimitsRect[1], defLimitsRect[7]);
        // UY
        result[9] = adjustElongation(values[0], defLimitsRect[0], defLimitsRect[9]);
        result[10] = adjustElongation(values[1], defLimitsRect[1], defLimitsRect[10]);
        // VX
        result[12] = adjustElongation(values[2], defLimitsRect[3], defLimitsRect[12]);
        result[13] = adjustElongation(values[3], defLimitsRect[4], defLimitsRect[13]);
        // VY
        result[15] = adjustElongation(values[2], defLimitsRect[3], defLimitsRect[15]);
        result[16] = adjustElongation(values[3], defLimitsRect[4], defLimitsRect[16]);
        // Steps
        result[2] = defLimitsRect[2];
        result[5] = defLimitsRect[5];
        result[8] = defLimitsRect[8];
        result[11] = defLimitsRect[11];
        result[14] = defLimitsRect[14];
        result[17] = defLimitsRect[17];

        for (int i = 0; i < result.length; i++) {
            if (result[i] != defLimitsRect[i]) {
                Logger.debug("New rect limits - " + Utils.toString(result));
                checkLimits(defLimitsRect);
                break;
            }
        }

        defLimitsRect = result;
    }

    private static double adjustElongation(final double value, final double limit, final double elong) {
        if (value != 0 && Math.signum(limit) != Math.signum(value)) {
            Logger.debug("Signum mismatch - {0} vs {1}", new Object[]{value, limit});
            return value;
        }
        final double val = Math.abs(value);
        final double lim = Math.abs(limit);

        final double result;
        if (val <= (lim / 3.0)) {
            result = elong / ADJUST_COEFF_DOWN;
        } else if (val <= (lim * 2 / 3.0)) {
            result = elong;
        } else {
            result = elong * ADJUST_COEFF_UP;
        }
        return result;
    }

    private static double adjustValue(final double value, final double limit) {
        return adjustElongation(value, limit, value);
    }

    private void setROIs(final int round) {
        tc.setROIs(round, getROIs());

        CircularROI cr;
        for (ROI roi : getROIs()) {
            if (roi instanceof CircularROI) {
                cr = (CircularROI) roi;
                tc.addFacetSize(round, roi, (int) (cr.getRadius() / ROI_CIRCLE_FS_DENOM));
                tc.setDeformationLimits(round, roi, defLimitsCircle);
            } else {
                tc.setDeformationLimits(round, roi, defLimitsRect);
            }
        }
    }

    private static void checkLimits(final double[] limits) {
        if (limits[0] < -MAX_SHIFT_X) {
            limits[0] = -MAX_SHIFT_X;
        }
        if (limits[1] > MAX_SHIFT_X) {
            limits[1] = MAX_SHIFT_X;
        }
    }
}
