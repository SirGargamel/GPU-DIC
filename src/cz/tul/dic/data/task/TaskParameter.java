package cz.tul.dic.data.task;

import cz.tul.dic.data.task.splitter.TaskSplitMethod;
import cz.tul.dic.engine.ResultCompilation;
import cz.tul.dic.engine.displacement.DisplacementCalculation;
import cz.tul.dic.engine.opencl.interpolation.Interpolation;
import cz.tul.dic.engine.opencl.KernelType;
import cz.tul.dic.engine.strain.StrainEstimationMethod;
import cz.tul.dic.generators.facet.FacetGeneratorMethod;
import java.io.File;

/**
 *
 * @author Petr Jecmen
 */
public enum TaskParameter {

    IN(File.class),
    FACET_GENERATOR_METHOD(FacetGeneratorMethod.class),
    FACET_GENERATOR_PARAM(Integer.class),
    FACET_SIZE(Integer.class),
    FPS(Integer.class),
    KERNEL(KernelType.class),    
    TASK_SPLIT_METHOD(TaskSplitMethod.class),
    TASK_SPLIT_PARAM(Integer.class),
    INTERPOLATION(Interpolation.class),
    MM_TO_PX_RATIO(Double.class),
    RESULT_COMPILATION(ResultCompilation.class),
    ROUND_LIMITS(int[].class),
    DISPLACEMENT_CALCULATION(DisplacementCalculation.class),
    STRAIN_ESTIMATION_METHOD(StrainEstimationMethod.class),
    STRAIN_ESTIMATION_PARAM(Integer.class),
    LOCAL_SEARCH_PARAM(Integer.class),
    ;

    private final Class type;

    TaskParameter(final Class cls) {
        this.type = cls;
    }

    public Class getType() {
        return type;
    }
}
