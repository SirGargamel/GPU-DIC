package cz.tul.dic.data.task;

import cz.tul.dic.data.task.splitter.TaskSplit;
import cz.tul.dic.engine.ResultCompilation;
import cz.tul.dic.engine.displacement.DisplacementCalculation;
import cz.tul.dic.engine.opencl.interpolation.Interpolation;
import cz.tul.dic.engine.opencl.KernelType;
import cz.tul.dic.engine.strain.StrainEstimationMethod;
import cz.tul.dic.generators.facet.FacetGeneratorMode;
import java.io.File;

/**
 *
 * @author Petr Jecmen
 */
public enum TaskParameter {

    IN(File.class),
    FACET_GENERATOR_MODE(FacetGeneratorMode.class),
    FACET_GENERATOR_SPACING(Integer.class),
    FACET_SIZE(Integer.class),
    FPS(Integer.class),
    KERNEL(KernelType.class),    
    TASK_SPLIT_VARIANT(TaskSplit.class),
    TASK_SPLIT_VALUE(Integer.class),
    INTERPOLATION(Interpolation.class),
    RESULT_COMPILATION(ResultCompilation.class),
    ROUND_LIMITS(int[].class),
    DISPLACEMENT_CALCULATION_TYPE(DisplacementCalculation.class),
    STRAIN_ESTIMATION_METHOD(StrainEstimationMethod.class),
    STRAIN_ESTIMATION_PARAMETER(Integer.class),
    WINDOW_SIZE(Integer.class),
    ;

    private final Class type;

    TaskParameter(final Class cls) {
        this.type = cls;
    }

    public Class getType() {
        return type;
    }
}
