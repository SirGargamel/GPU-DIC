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
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EqualSpacingSubsetGenerator extends AbstractSubsetGenerator {

    @Override
    public Map<AbstractROI, List<AbstractSubset>> generateFacets(TaskContainer tc, int round) throws ComputationException {
        final Object o = tc.getParameter(TaskParameter.FACET_GENERATOR_PARAM);
        if (o == null) {
            throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "No subset generator spacing.");
        }

        final int spacing = (int) o;
        if (spacing <= 0) {
            throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Illegal subset generator spacing " + spacing);
        }

        final int width = tc.getImage(round).getWidth();
        final int height = tc.getImage(round).getHeight();

        // generate centers
        final Set<AbstractROI> rois = tc.getRois(round);
        final Map<AbstractROI, List<AbstractSubset>> result = new HashMap<>(rois.size());

        List<AbstractSubset> subsets;
        int wCount, hCount, subsetSize;
        double centerX, centerY, roiW, roiH;
        for (AbstractROI roi : rois) {
            subsets = new ArrayList<>();

            subsetSize = tc.getSubsetSize(round, roi);

            roiW = roi.getWidth();
            roiH = roi.getHeight();

            if ((roiW - 2 * subsetSize) < spacing) {
                if ((roiW - 2 * subsetSize) > 0) {
                    wCount = 1;
                } else {
                    wCount = 0;
                }
            } else {
                wCount = (int) ((roiW - 2 * subsetSize) / spacing);
            }
            if ((roiH - 2 * subsetSize) < spacing) {
                if ((roiH - 2 * subsetSize) > 0) {
                    hCount = 1;
                } else {
                    hCount = 0;
                }
            } else {
                hCount = (int) ((roiH - 2 * subsetSize) / (spacing));
            }

            for (int y = 0; y < hCount; y++) {
                centerY = roi.getY1() + subsetSize + (y * spacing);

                for (int x = 0; x < wCount; x++) {
                    centerX = roi.getX1() + subsetSize + (x * spacing);

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
            }

            result.put(roi, subsets);
        }

        return result;
    }

    @Override
    public SubsetGeneratorMethod getMode() {
        return SubsetGeneratorMethod.EQUAL;
    }

}
