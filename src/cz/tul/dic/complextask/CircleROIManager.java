package cz.tul.dic.complextask;

import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.data.Coordinates;
import cz.tul.dic.data.deformation.DeformationUtils;
import cz.tul.dic.data.roi.CircularROI;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.roi.RectangleROI;
import cz.tul.dic.data.task.Hint;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.engine.CorrelationResult;
import cz.tul.dic.engine.cluster.Analyzer1D;
import cz.tul.dic.generators.facet.FacetGeneratorMethod;
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
public class CircleROIManager extends ROIManager {

    public static final float LIMIT_RESULT_QUALITY = 0.5f;
    private static final double[] DEFAULT_DEFORMATION_LIMITS = new double[]{-1, 1, 0.5, -20, 20, 0.5};
    private static final int MAX_SHIFT_DIFFERENCE = 3;
    private static final int ROI_CIRCLE_FS_DENOM = 2;
    private CircularROI topLeft, topRight, bottomLeft, bottomRight;
    private double shiftTop, shiftBottom;

    public static CircleROIManager prepareManager(final TaskContainer tc, final int initialRound) throws ComputationException {
        final TaskContainer tcC = tc.cloneInputTask();

        tcC.setROIs(initialRound, tc.getRois(initialRound));

        return new CircleROIManager(tcC, initialRound);
    }

    private CircleROIManager(TaskContainer tc, final int initialRound) throws ComputationException {
        super(tc);

        tc.setParameter(TaskParameter.FACET_GENERATOR_METHOD, FacetGeneratorMethod.CLASSIC);
        tc.setParameter(TaskParameter.FACET_GENERATOR_PARAM, 0);
        tc.addHint(Hint.NO_STRAIN);
        tc.addHint(Hint.NO_CUMULATIVE);
        tc.addHint(Hint.NO_FINE_SEARCH);
        tc.clearResultData();

        final List<CircularROI> cRois = new ArrayList<>(4);
        if (tc.getRois(initialRound) != null) {
            for (ROI r : tc.getRois(initialRound)) {
                if (r instanceof CircularROI) {
                    cRois.add((CircularROI) r);
                } else if (!(r instanceof RectangleROI)) {
                    throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Unsupported type of ROI - " + r.getClass());
                }
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

        final StringBuilder sb = new StringBuilder();
        sb.append("Top Left - ");
        sb.append(topLeft);
        sb.append(" ; Top Right - ");
        sb.append(topRight);
        sb.append(" ; Bottom Left - ");
        sb.append(bottomLeft);
        sb.append(" ; Bottom Right - ");
        sb.append(bottomRight);
        Logger.trace(sb);

        defLimits = DEFAULT_DEFORMATION_LIMITS;
        setROIs(initialRound);
    }

    @Override
    public void generateNextRound(int round, int nextRound) {
        // find new position of Circle ROIs
        //// determine shifts of circle ROIs from previous round
        final double shift0 = determineROIShift(round, topLeft);
        final double shift1 = determineROIShift(round, topRight);
        final double shift2 = determineROIShift(round, bottomLeft);
        final double shift3 = determineROIShift(round, bottomRight);
        Logger.trace(shift2 + ", " + shift3);
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

        if (shift0 < 0 || shift1 < 0) {
            shiftTop = Math.min(shift0, shift1);
        } else {
            shiftTop = Math.max(shift0, shift1);
        }
        if (shift2 < 0 || shift3 < 0) {
            shiftBottom = Math.min(shift2, shift3);
        } else {
            shiftBottom = Math.max(shift2, shift3);
        }

        setROIs(nextRound);
    }

    private double determineROIShift(final int round, final ROI roi) {        
        final Analyzer1D analyzer = new Analyzer1D();
        analyzer.setPrecision(PRECISION);

        for (CorrelationResult cr : tc.getResult(round, roi)) {
            if (cr != null && cr.getValue() >= LIMIT_RESULT_QUALITY) {
                analyzer.addValue(cr.getDeformation()[Coordinates.Y]);
            }
        }

        return analyzer.findMajorValue();
    }

    private void setROIs(final int round) {
        final Set<ROI> rois = new HashSet<>(4);
        rois.add(topLeft);
        rois.add(topRight);
        rois.add(bottomLeft);
        rois.add(bottomRight);

        tc.setROIs(round, rois);

        CircularROI cr;
        for (ROI roi : rois) {
            cr = (CircularROI) roi;
            tc.addFacetSize(round, roi, Math.max(1, (int) (cr.getRadius() / ROI_CIRCLE_FS_DENOM)));
            tc.setDeformationLimits(round, roi, defLimits);
        }
    }

    public double getShiftTop() {
        return shiftTop;
    }

    public double getShiftBottom() {
        return shiftBottom;
    }

    public Set<ROI> getBottomRois() {
        final Set<ROI> result = new HashSet<>(2);
        result.add(bottomLeft);
        result.add(bottomRight);
        return result;
    }

    public boolean hasMoved() {
        return haveMoved(shiftBottom, shiftBottom);
    }

    public void increaseLimits(final int round) {
        final double[] oldLimits = defLimits;
        defLimits = new double[oldLimits.length];
        System.arraycopy(oldLimits, 0, defLimits, 0, oldLimits.length);

        final int[] stepCounts = DeformationUtils.generateDeformationCounts(defLimits);
        double mod = stepCounts[0] / 4 * defLimits[2];
        defLimits[0] -= mod;
        defLimits[1] += mod;
        mod = stepCounts[1] / 4 * defLimits[5];
        defLimits[3] -= mod;
        defLimits[4] += mod;
        
        setROIs(round);
    }

}
