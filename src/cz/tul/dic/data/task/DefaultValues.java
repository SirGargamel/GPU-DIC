package cz.tul.dic.data.task;

import cz.tul.dic.data.task.splitter.TaskSplit;
import cz.tul.dic.engine.ResultCompilation;
import cz.tul.dic.engine.displacement.DisplacementCalculation;
import cz.tul.dic.engine.opencl.KernelType;
import cz.tul.dic.engine.opencl.interpolation.Interpolation;
import cz.tul.dic.engine.strain.StrainEstimationMethod;
import cz.tul.dic.generators.facet.FacetGeneratorMode;

/**
 *
 * @author Petr Ječmen
 */
public interface DefaultValues {

    DisplacementCalculation DEFAULT_DISPLACEMENT_CALCULATION = DisplacementCalculation.FIND_MAX_AND_AVERAGE;
    int DEFAULT_FPS = 5000;
    double DEFAULT_PRECISION_ZERO = 0.5;
    double DEFAULT_PRECISION_FIRST = 0.25;
    double[] DEFAULT_DEFORMATION_LIMITS_ZERO = new double[]{-1, 1, DEFAULT_PRECISION_ZERO, -5, 5, DEFAULT_PRECISION_ZERO};
    double[] DEFAULT_DEFORMATION_LIMITS_FIRST = new double[]{
        -5, 5, DEFAULT_PRECISION_ZERO, -5, 5, DEFAULT_PRECISION_ZERO,
        -0.5, 0.5, DEFAULT_PRECISION_FIRST, -0.5, 0.5, DEFAULT_PRECISION_FIRST, -0.5, 0.5, DEFAULT_PRECISION_FIRST, -0.5, 0.5, DEFAULT_PRECISION_FIRST};
    FacetGeneratorMode DEFAULT_FACET_GENERATOR = FacetGeneratorMode.TIGHT;
    int DEFAULT_FACET_SPACING = 1;
    int DEFAULT_FACET_SIZE = 7;
    Interpolation DEFAULT_INTERPOLATION = Interpolation.BICUBIC;
    KernelType DEFAULT_KERNEL = KernelType.CL_1D_I_V_LL_MC_D;
    ResultCompilation DEFAULT_RESULT_COMPILATION = ResultCompilation.MAJOR_AVERAGING;
    StrainEstimationMethod DEFAULT_STRAIN_ESTIMATION = StrainEstimationMethod.LOCAL_LEAST_SQUARES;
    TaskSplit DEFAULT_TASK_SPLIT = TaskSplit.DYNAMIC;
    int DEFAULT_TASK_SPLIT_VALUE = 1000;
    int DEFAULT_WINDOW_SIZE = 0;
}
