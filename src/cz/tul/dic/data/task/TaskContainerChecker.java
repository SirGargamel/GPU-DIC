package cz.tul.dic.data.task;

import cz.tul.dic.data.Image;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.output.ExportTask;
import cz.tul.dic.output.Exporter;

/**
 *
 * @author Petr Jecmen
 */
public class TaskContainerChecker {

    public static void checkTaskValidity(final TaskContainer tc) {
        // null data
        final int roundCount = TaskContainerUtils.getRoundCount(tc);
        if (roundCount < 1) {
            throw new IllegalArgumentException("Not enough enabled input images.");
        }

        Image img;
        ROI roi;
        for (int round = 0; round < roundCount; round++) {
            img = tc.getImage(round);
            if (img == null) {
                throw new IllegalArgumentException("NULL image found.");
            }

            roi = tc.getRoi(round);
            if (roi == null) {
                System.err.println("Adding default ROI.");
                roi = new ROI(0, 0, img.getWidth() - 1, img.getHeight() - 1);
                tc.addRoi(roi, round);
            } else {
                if (roi.getX1() < 0 || roi.getY1() < 0) {
                    throw new IllegalArgumentException("ROI coords must be positive.");
                }

                if (roi.getX2() >= img.getWidth() || roi.getY2() > img.getHeight()) {
                    throw new IllegalArgumentException("ROI cannot be larger than image.");
                }
            }
        }

        for (ExportTask et : tc.getExportTasks()) {
            if (!Exporter.isExportSupported(et)) {
                throw new IllegalArgumentException("Unsupported exeport task - " + et);
            }
        }
    }

}
