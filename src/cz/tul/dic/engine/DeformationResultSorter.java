package cz.tul.dic.engine;

import cz.tul.dic.data.deformation.DeformationUtils;
import java.io.Serializable;
import java.util.Comparator;

/**
 *
 * @author Petr Jecmen
 */
public class DeformationResultSorter implements Comparator<Integer>, Serializable {

    private final double[] deformationLimits;
    private final int[] deformationCounts;

    public DeformationResultSorter(final double[] deformationLimits) {
        this.deformationLimits = deformationLimits;
        deformationCounts = DeformationUtils.generateDeformationCounts(deformationLimits);
    }

    @Override
    public int compare(Integer o1, Integer o2) {
        return Double.compare(
                DeformationUtils.getAbs(o1, deformationLimits, deformationCounts),
                DeformationUtils.getAbs(o2, deformationLimits, deformationCounts));
    }

}
