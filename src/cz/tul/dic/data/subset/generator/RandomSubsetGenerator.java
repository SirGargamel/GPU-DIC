/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.data.subset.generator;

import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.data.subset.AbstractSubset;
import cz.tul.dic.data.subset.SquareSubset2D;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.data.roi.AbstractROI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.pmw.tinylog.Logger;

public class RandomSubsetGenerator extends AbstractSubsetGenerator {

    @Override
    public HashMap<AbstractROI, List<AbstractSubset>> generateSubsets(TaskContainer tc, int round) throws ComputationException {
        final Object o = tc.getParameter(TaskParameter.SUBSET_GENERATOR_PARAM);
        if (o == null) {
            throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "No subset generator spacing.");
        }

        final int maxSubsetCount = (int) o;
        if (maxSubsetCount <= 0) {
            throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Illegal subset count " + maxSubsetCount);
        }

        final int width = tc.getImage(round).getWidth();
        final int height = tc.getImage(round).getHeight();

        // generate centers
        final Set<AbstractROI> rois = tc.getRois(round);
        final HashMap<AbstractROI, List<AbstractSubset>> result = new LinkedHashMap<>(rois.size());
        final Random rnd = new Random();

        List<AbstractSubset> subsets;
        int subsetSize, roiW, roiH, subsetCount;
        double centerX, centerY;
        for (AbstractROI roi : rois) {
            subsets = new ArrayList<>();

            subsetSize = tc.getSubsetSize(round, roi);

            roiW = (int) roi.getWidth();
            roiH = (int) roi.getHeight();

            subsetCount = Math.min(
                    maxSubsetCount,
                    (roiH - 2 * subsetSize) * (roiW - 2 * subsetSize));
            if (subsetCount < 1) {
                Logger.warn("Negative count of subsets - {}", subsetCount);
            }

            while (subsets.size() < subsetCount) {
                centerX = roi.getX1() + rnd.nextInt(roiW);
                centerY = roi.getY1() + rnd.nextInt(roiH);

                if (checkAreaValidity(
                        centerX - subsetSize, centerY - subsetSize,
                        centerX + subsetSize, centerY + subsetSize,
                        width, height)
                        && roi.isAreaInside(
                                centerX - subsetSize, centerY - subsetSize,
                                centerX + subsetSize, centerY + subsetSize)) {
                    subsets.add(new SquareSubset2D(subsetSize, centerX, centerY));
                }
            }

            result.put(roi, subsets);
        }

        return result;
    }

}
