package cz.tul.dic.data.task;

import cz.tul.dic.data.task.splitter.TaskSplitMethod;
import cz.tul.dic.engine.ResultCompilation;
import cz.tul.dic.engine.displacement.DisplacementCalculation;
import cz.tul.dic.engine.opencl.KernelType;
import cz.tul.dic.engine.opencl.interpolation.Interpolation;
import cz.tul.dic.engine.strain.StrainEstimationMethod;
import cz.tul.dic.generators.facet.FacetGeneratorMethod;

/**
 *
 * @author Petr Ječmen
 */
public interface TaskDefaultValues {

    DisplacementCalculation DEFAULT_DISPLACEMENT_CALCULATION_METHOD = DisplacementCalculation.FIND_MAX_AND_AVERAGE;
    int DEFAULT_DISPLACEMENT_CALCULATION_PARAM = 2000;
    int DEFAULT_FPS = 5000;    
    double[] DEFAULT_DEFORMATION_LIMITS_ZERO = new double[]{-2.0, 2.0, 0.25, -1, 5, 0.25};
    double[] DEFAULT_DEFORMATION_LIMITS_FIRST = new double[]{
        -1.5, 1.5, 0.5, -1.0, 2.0, 0.5,
        -0.25, 0.0, 0.025, -0.4, 0.2, 0.05, -0.2, 0.4, 0.05, -0.2, 0.5, 0.1};
    FacetGeneratorMethod DEFAULT_FACET_GENERATOR = FacetGeneratorMethod.TIGHT;
    int DEFAULT_FACET_SPACING = 1;
    int DEFAULT_FACET_SIZE = 20;
    Interpolation DEFAULT_INTERPOLATION = Interpolation.BICUBIC;
    KernelType DEFAULT_KERNEL = KernelType.CL_1D_I_V_LL_MC_D;
    double DEFAULT_MM_TO_PX_RATIO = 1;
    ResultCompilation DEFAULT_RESULT_COMPILATION = ResultCompilation.MAJOR_AVERAGING;
    double DEFAULT_RESULT_QUALITY = 0.5;
    StrainEstimationMethod DEFAULT_STRAIN_ESTIMATION_METHOD = StrainEstimationMethod.LOCAL_LEAST_SQUARES;
    double DEFAULT_STRAIN_ESTIMATION_PARAMETER = 20;
    TaskSplitMethod DEFAULT_TASK_SPLIT_METHOD = TaskSplitMethod.DYNAMIC;
    int DEFAULT_TASK_SPLIT_PARAMETER = 1000;
    int DEFAULT_WINDOW_SIZE = 0;
}
