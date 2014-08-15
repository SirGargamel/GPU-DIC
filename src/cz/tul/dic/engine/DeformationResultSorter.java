package cz.tul.dic.engine;

import cz.tul.dic.data.deformation.DeformationUtils;
import java.io.Serializable;
import java.util.Comparator;

/**
 *
 * @author Petr Jecmen
 */
public class DeformationResultSorter implements Comparator<Integer>, Serializable {

    private final int defArrayLength;    
    private final double[] deformations;

    public DeformationResultSorter(int defArrayLength, final double[] deformations) {
        this.defArrayLength = defArrayLength;        
        this.deformations = deformations;
    }

    @Override
    public int compare(Integer o1, Integer o2) {
        return Double.compare(
                DeformationUtils.getAbs(deformations, o1, defArrayLength),
                DeformationUtils.getAbs(deformations, o2, defArrayLength));
    }

}
