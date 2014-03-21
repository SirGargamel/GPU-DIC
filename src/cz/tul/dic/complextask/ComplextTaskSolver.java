package cz.tul.dic.complextask;

import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.roi.CircularROI;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.roi.RectangleROI;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.engine.Engine;
import cz.tul.dic.generators.DeformationGenerator;
import cz.tul.dic.generators.facet.FacetGenerator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Petr Jecmen
 */
public class ComplextTaskSolver {

    private static final int ROI_COUNT = 4;

    public static void solveComplexTask(final TaskContainer tc) throws IOException, ComputationException {
        final Engine engine = new Engine();

        // check task
        //// 4x CircleROI is required
        Set<ROI> rois = tc.getRois(0);
        int counter = 0;
        for (ROI roi : rois) {
            if (roi instanceof CircularROI) {
                counter++;
            }
        }
        if (counter != ROI_COUNT) {
            throw new ComputationException(ComputationExceptionCause.NOT_ENOUGH_ROIS, Integer.toString(counter));
        }

        Image img = tc.getImage(0);
        ROI rectangleRoi = generateRectangleROI(rois, img.getWidth(), img.getHeight());
        tc.addRoi(rectangleRoi, 0);

        for (ROI roi : rois) {
            if (roi instanceof CircularROI) {
                tc.setDeformationLimits(new double[]{-1, 1, 1.0, -10, 1, 0.25}, 0, roi);
            } else {
                tc.setDeformationLimits(new double[]{-1, 1, 0.5, -5, 5, 0.5, -0.5, 0.5, 0.5, -0.5, 0.5, 0.5, -0.5, 0.5, 0.5, -0.5, 0.5, 0.5}, 0, roi);
            }
        }

        FacetGenerator.generateFacets(tc);
        DeformationGenerator.generateDeformations(tc);

        // generate rectangle ROI for round 0
        // generate possible deformation for ROIs
        // compute round 0
        engine.computeRound(tc, 0);

        // find new position of Circle ROIs
        //// check if left equals right
        // generate ReactengleROI for round 1 from new CircleROIs
        // compute round 1
        // REPEAT
    }

    private static ROI generateRectangleROI(final Set<ROI> circleRois, final int imageWidth, final int imageHeight) {
        final List<ROI> sortedROIs = new ArrayList<>(ROI_COUNT);
        sortedROIs.addAll(circleRois);
        Collections.sort(sortedROIs, new RoiSorter());

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
