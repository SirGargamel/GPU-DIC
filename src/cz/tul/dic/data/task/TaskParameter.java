package cz.tul.dic.data.task;

import cz.tul.dic.data.deformation.DeformationDegree;
import cz.tul.dic.engine.opencl.KernelType;
import cz.tul.dic.generators.facet.FacetGeneratorMode;
import java.io.File;

/**
 *
 * @author Petr Jecmen
 */
public enum TaskParameter {
    
    DIR(File.class),
    FACET_GENERATOR_MODE(FacetGeneratorMode.class),
    FACET_GENERATOR_SPACING(int.class),
    DEFORMATION_DEGREE(DeformationDegree.class),
    DEFORMATION_BOUNDS(double[].class),
    KERNEL(KernelType.class),
    ;
    
    private final Class type;
    
    TaskParameter(final Class cls) {
        this.type = cls;
    }
            
    public Class getType() {
        return type;
    }
}
