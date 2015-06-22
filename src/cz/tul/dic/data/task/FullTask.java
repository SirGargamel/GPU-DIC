/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.data.task;

import cz.tul.dic.data.subset.AbstractSubset;
import cz.tul.dic.data.Image;
import java.util.List;

/**
 *
 * @author Lenam s.r.o.
 */
public class FullTask {

    private final Image imageA;
    private final Image imageB;
    private final List<AbstractSubset> subsets;
    private final List<double[]> deformationLimits;

    public FullTask(final Image imageA, final Image imageB, final List<AbstractSubset> subsets, final List<double[]> deformationLimits) {
        this.imageA = imageA;
        this.imageB = imageB;
        this.subsets = subsets;
        this.deformationLimits = deformationLimits;
    }

    public Image getImageA() {
        return imageA;
    }

    public Image getImageB() {
        return imageB;
    }

    public List<AbstractSubset> getSubsets() {
        return subsets;
    }

    public List<double[]> getDeformationLimits() {
        return deformationLimits;
    }

}
