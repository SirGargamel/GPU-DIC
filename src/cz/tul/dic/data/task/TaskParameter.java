package cz.tul.dic.data.task;

import cz.tul.dic.data.deformation.DeformationDegree;
import cz.tul.dic.data.task.splitter.TaskSplit;
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
    FACET_GENERATOR_SPACING(Integer.class),
    DEFORMATION_DEGREE(DeformationDegree.class),
    DEFORMATION_BOUNDS(double[].class),
    KERNEL(KernelType.class),
    TASK_SPLIT_VARIANT(TaskSplit.class),
    TASK_SPLIT_VALUE(Object.class),
    ;
    
    private final Class type;
    
    TaskParameter(final Class cls) {
        this.type = cls;
    }
            
    public Class getType() {
        return type;
    }
}
