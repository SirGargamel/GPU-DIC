package cz.tul.dic.complextask;

import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.roi.CircularROI;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.roi.RectangleROI;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.engine.Engine;
import java.io.IOException;
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
public class ComplextTaskSolver {

    private static final int ROI_COUNT = 4;
    private static final int MAX_SHIFT_DIFFERENCE = 3;
    private static final int DEFAULT_FS_CIRCLE = 15;
    private static final int DEFAULT_FS_RECT = 5;
    private static final double[] DEFAULT_DEF_CIRCLE = new double[]{-1, 1, 1.0, -5, 5, 0.25};
    private static final double[] DEFAULT_DEF_RECT = new double[]{-4, 4, 0.5, -5, 5, 0.5, -0.5, 0.5, 0.5, -0.5, 0.5, 0.5, -0.5, 0.5, 0.5, -0.5, 0.5, 0.5};

    public static void solveComplexTask(final TaskContainer tc) throws IOException, ComputationException {
        final Engine engine = new Engine();        
        
        // check task
        //// 4x CircleROI is required
        Set<ROI> rois = tc.getRois(0);
        int counter = 0;
        int facetSizeC = -1, facetSizeR = -1;
        for (ROI roi : rois) {
            if (roi instanceof CircularROI) {
                counter++;
                facetSizeC = tc.getFacetSize(0, roi);
            } else {
                facetSizeR = tc.getFacetSize(0, roi);
            }
        }
        if (counter != ROI_COUNT) {
            throw new ComputationException(ComputationExceptionCause.NOT_ENOUGH_ROIS, Integer.toString(counter));
        }
        if (facetSizeC == -1) {
            facetSizeC = DEFAULT_FS_CIRCLE;
            TaskContainerUtils.setUniformFacetSize(tc, 0, facetSizeC);
            Logger.warn("No facetSize set for circular ROIs, using default.");
        }
        if (facetSizeR == -1) {
            facetSizeR = DEFAULT_FS_RECT;
        }
        // generate rectangle ROI for round 0        
        Image img = tc.getImage(0);
        List<ROI> sortedROIs = new ArrayList<>(rois);
        Collections.sort(sortedROIs, new RoiSorter());
        ROI rectangleRoi = generateRectangleROI(sortedROIs, img.getWidth(), img.getHeight());
        tc.addRoi(rectangleRoi, 0);
        tc.addFacetSize(0, rectangleRoi, facetSizeR);
        // generate possible deformation for ROIs
        for (ROI roi : rois) {
            if (roi instanceof CircularROI) {
                tc.setDeformationLimits(DEFAULT_DEF_CIRCLE, 0, roi);
            } else {
                tc.setDeformationLimits(DEFAULT_DEF_RECT, 0, roi);
            }
        }
        // compute round 0
        engine.computeRound(tc, 0);

        final int roundCount = TaskContainerUtils.getRoundCount(tc);
        double shift1, shift2, shift;
        for (int round = 1; round < roundCount; round++) {
            // find new position of Circle ROIs
            //// determine shifts of circle ROIs from previous round
            shift1 = FacetDeformationAnalyzator.determineROIShift(tc, round - 1, sortedROIs.get(2));
            shift2 = FacetDeformationAnalyzator.determineROIShift(tc, round - 1, sortedROIs.get(3));
            shift = (shift1 + shift2) / 2.0;
            Logger.debug(shift1 + ", " + shift2);
            //// check if left equals right
            if (Math.abs(shift1 - shift2) > MAX_SHIFT_DIFFERENCE) {
                throw new ComputationException(ComputationExceptionCause.FIXTURES_SHIFT_MISMATCH, Double.toString(shift1).concat(" vs ".concat(Double.toString(shift2))));
            }
            //// generate new Circle ROIs
            rois = new HashSet<>(5);
            rois.add(sortedROIs.get(0));
            rois.add(sortedROIs.get(1));
            CircularROI cRoi = (CircularROI) sortedROIs.get(2);
            rois.add(new CircularROI(cRoi.getCenterX(), cRoi.getCenterY() + shift, cRoi.getRadius()));
            cRoi = (CircularROI) sortedROIs.get(3);
            rois.add(new CircularROI(cRoi.getCenterX(), cRoi.getCenterY() + shift, cRoi.getRadius()));
            // generate ReactengleROI for  this round from new CircleROIs
            sortedROIs.clear();
            sortedROIs.addAll(rois);
            Collections.sort(sortedROIs, new RoiSorter());
            rectangleRoi = generateRectangleROI(sortedROIs, img.getWidth(), img.getHeight());
            rois.add(rectangleRoi);
            tc.setROIs(rois, round);
            TaskContainerUtils.setUniformFacetSize(tc, round, facetSizeC);
            tc.addFacetSize(round, rectangleRoi, facetSizeR);
            // generate limits
            //// TODO generate limits dynamically according to previous results
            for (ROI roi : rois) {
            if (roi instanceof CircularROI) {
                tc.setDeformationLimits(DEFAULT_DEF_CIRCLE, round, roi);
            } else {
                tc.setDeformationLimits(DEFAULT_DEF_RECT, round, roi);
            }
        }
            // compute round
            engine.computeRound(tc, round);
        }
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