/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.data.task;

import cz.tul.dic.data.subset.AbstractSubset;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.deformation.DeformationUtils;
import java.util.List;

/**
 *
 * @author Lenam s.r.o.
 */
public class FullTask {

    private final Image imageA;
    private final Image imageB;
    private final List<AbstractSubset> subsets;
    private final List<Integer> subsetWeights;
    private final List<double[]> deformationLimits;    

    public FullTask(final Image imageA, final Image imageB, final List<AbstractSubset> subsets, final List<Integer> subsetWeights, final List<double[]> deformationLimits) {
        this.imageA = imageA;
        this.imageB = imageB;
        this.subsets = subsets;
        this.subsetWeights = subsetWeights;
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

    public List<Integer> getSubsetWeights() {
        return subsetWeights;
    }

    public List<double[]> getDeformationLimits() {
        return deformationLimits;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append(subsets.size());
        sb.append(" subsets; ");

        final List<long[]> counts = DeformationUtils.generateDeformationCounts(deformationLimits);
        long[] arr = counts.get(0);
        long count = arr[arr.length - 1];
        boolean equal = true;
        for (int i = 1; i < counts.size(); i++) {
            arr = counts.get(i);
            if (arr[arr.length - 1] != count) {
                equal = false;
                break;
            }
        }
        if (equal) {
            sb.append(count);
            sb.append(" deformations each");
        } else {
            sb.append("[");
            for (long[] l : counts) {
                sb.append(l[l.length - 1]);
                sb.append(",");
            }
            sb.setLength(sb.length() - ",".length());
            sb.append("] deformations");
        }

        return sb.toString();
    }

}
