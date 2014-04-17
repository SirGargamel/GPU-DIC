package cz.tul.dic.complextask;

import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.roi.CircularROI;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.roi.RectangleROI;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.engine.Engine;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Observable;
import java.util.Set;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Jecmen
 */
public class ComplextTaskSolver extends Observable {

    private static final int ROI_COUNT = 4;
    private static final int MAX_SHIFT_DIFFERENCE = 3;
    private static final int ROI_CIRCLE_FS_DENOM = 3;
    private static final double[] DEFAULT_DEF_CIRCLE = new double[]{-1, 1, 0.5, -5, 5, 0.5};
    private static final double[] DEFAULT_DEF_RECT = new double[]{-5, 5, 0.5, -5, 5, 0.5, -0.5, 0.5, 0.5, -0.5, 0.5, 0.5, -0.5, 0.5, 0.5, -0.5, 0.5, 0.5};
    private double[] deformationCircle, deformationRect;
    private final Engine engine;

    public ComplextTaskSolver() {
        deformationCircle = DEFAULT_DEF_CIRCLE;
        deformationRect = DEFAULT_DEF_RECT;

        engine = new Engine();
    }

    public void setDeformationCircle(double[] deformationCircle) {
        this.deformationCircle = deformationCircle;
    }

    public void setDeformationRect(double[] deformationRect) {
        this.deformationRect = deformationRect;
    }

    public void solveComplexTask(final TaskContainer tc) throws ComputationException {
        final int roundCount = TaskContainerUtils.getRoundCount(tc);
        tc.clearResultData();

        final int[] rounds = (int[]) tc.getParameter(TaskParameter.ROUND_LIMITS);
        int currentRound = 0;
        final int baseRound = rounds[0];

        setChanged();
        notifyObservers(new int[]{currentRound, roundCount});

        // check task
        //// 4x CircleROI is required
        Set<ROI> rois = tc.getRois(baseRound);
        int counter = 0;
        CircularROI cr;
        for (ROI roi : rois) {
            if (roi instanceof CircularROI) {
                counter++;
                cr = (CircularROI) roi;
                tc.addFacetSize(baseRound, roi, (int) (cr.getRadius() / ROI_CIRCLE_FS_DENOM));
            } else {
                throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Only circular ROIs needed.");
            }
        }
        if (counter != ROI_COUNT) {
            throw new ComputationException(ComputationExceptionCause.NOT_ENOUGH_ROIS, Integer.toString(counter));
        }
        // generate rectangle ROI for base round       
        Image img = tc.getImage(baseRound);
        List<ROI> sortedROIs = new ArrayList<>(rois);
        Collections.sort(sortedROIs, new RoiSorter());
        ROI rectangleRoi = generateRectangleROI(sortedROIs, img.getWidth(), img.getHeight());
        tc.addRoi(baseRound, rectangleRoi);
        rois.add(rectangleRoi);
        // generate possible deformation for ROIs
        for (ROI roi : rois) {
            if (roi instanceof CircularROI) {
                tc.setDeformationLimits(baseRound, roi, deformationCircle);
            } else {
                tc.setDeformationLimits(baseRound, roi, deformationRect);
            }
        }
        // compute first round     
        engine.computeRound(tc, baseRound, baseRound + 1);

        currentRound++;
        setChanged();
        notifyObservers(new int[]{currentRound, roundCount});

        boolean skip = true;
        int prevR = baseRound;
        for (int round = 0; round < rounds.length; round += 2) {
            if (round > 0) {
                computeRound(tc, rounds[round - 1], rounds[round], prevR, sortedROIs);
                currentRound++;
                setChanged();
                notifyObservers(new int[]{currentRound, roundCount});
            }

            for (int r = rounds[round]; r < rounds[round + 1]; r++) {
                if (skip) {
                    skip = false;
                    continue;
                }

                computeRound(tc, r, r + 1, prevR, sortedROIs);
                currentRound++;
                setChanged();
                notifyObservers(new int[]{currentRound, roundCount});
                prevR = r;
            }
        }
    }

    private void computeRound(final TaskContainer tc, final int r, final int nextR, final int prevR, final List<ROI> sortedROIs) throws ComputationException {
        // find new position of Circle ROIs
        //// determine shifts of circle ROIs from previous round
        final double shift0 = FacetDeformationAnalyzator.determineROIShift(tc, prevR, sortedROIs.get(0));
        final double shift1 = FacetDeformationAnalyzator.determineROIShift(tc, prevR, sortedROIs.get(1));
        final double shift2 = FacetDeformationAnalyzator.determineROIShift(tc, prevR, sortedROIs.get(2));
        final double shift3 = FacetDeformationAnalyzator.determineROIShift(tc, prevR, sortedROIs.get(3));
        Logger.debug(shift2 + ", " + shift3);
        //// check if left equals right
        if (Math.abs(shift2 - shift3) > MAX_SHIFT_DIFFERENCE) {
            Logger.warn(ComputationExceptionCause.FIXTURES_SHIFT_MISMATCH.toString().concat("-LOWER- ").concat(Double.toString(shift2)).concat(" vs ".concat(Double.toString(shift3))));
        }
        if (Math.abs(shift0 - shift0) > MAX_SHIFT_DIFFERENCE) {
            Logger.warn(ComputationExceptionCause.FIXTURES_SHIFT_MISMATCH.toString().concat("-UPPER- ").concat(Double.toString(shift0)).concat(" vs ".concat(Double.toString(shift1))));
        }
        //// generate new Circle ROIs
        final Set<ROI> rois = new HashSet<>(5);
        CircularROI cRoi = (CircularROI) sortedROIs.get(0);
        CircularROI newRoi = new CircularROI(cRoi.getCenterX(), cRoi.getCenterY() + shift0, cRoi.getRadius());
        sortedROIs.set(0, newRoi);
        rois.add(newRoi);

        cRoi = (CircularROI) sortedROIs.get(1);
        newRoi = new CircularROI(cRoi.getCenterX(), cRoi.getCenterY() + shift1, cRoi.getRadius());
        sortedROIs.set(1, newRoi);
        rois.add(newRoi);

        cRoi = (CircularROI) sortedROIs.get(2);
        newRoi = new CircularROI(cRoi.getCenterX(), cRoi.getCenterY() + shift2, cRoi.getRadius());
        sortedROIs.set(2, newRoi);
        rois.add(newRoi);

        cRoi = (CircularROI) sortedROIs.get(3);
        newRoi = new CircularROI(cRoi.getCenterX(), cRoi.getCenterY() + shift3, cRoi.getRadius());
        sortedROIs.set(3, newRoi);
        rois.add(newRoi);
        // generate RectangleROI for  this round from new CircleROIs
        sortedROIs.clear();
        sortedROIs.addAll(rois);
        Collections.sort(sortedROIs, new RoiSorter());
        tc.setROIs(r, rois);
        Image img = tc.getImage(r);
        final ROI rectangleRoi = generateRectangleROI(sortedROIs, img.getWidth(), img.getHeight());
        rois.add(rectangleRoi);
        // generate limits
        //// TODO generate limits dynamically according to previous results
        CircularROI cr;
        for (ROI roi : rois) {
            if (roi instanceof CircularROI) {
                cr = (CircularROI) roi;
                tc.addFacetSize(r, roi, (int) (cr.getRadius() / ROI_CIRCLE_FS_DENOM));
                tc.setDeformationLimits(r, roi, deformationCircle);
            } else {
                tc.setDeformationLimits(r, roi, deformationRect);
            }
        }
        // compute round
        engine.computeRound(tc, r, nextR);
    }

    private static ROI generateRectangleROI(final List<ROI> sortedROIs, final int imageWidth, final int imageHeight) {
        int xLeft = Math.min(sortedROIs.get(0).getX2(), sortedROIs.get(2).getX2());
        xLeft = Math.max(xLeft, 0);
        int yTop = Math.min(sortedROIs.get(0).getY1(), sortedROIs.get(1).getY1());
        yTop = Math.max(yTop, 0);
        int xRight = Math.min(sortedROIs.get(1).getX1(), sortedROIs.get(3).getX1());
        xRight = Math.min(xRight, imageWidth);
        int yBottom = Math.min(sortedROIs.get(2).getY2(), sortedROIs.get(3).getY2());
        yBottom = Math.min(yBottom, imageHeight);

        return new RectangleROI(
                xLeft,
                yTop,
                xRight,
                yBottom);
    }

}
