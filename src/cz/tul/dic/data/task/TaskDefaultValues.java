/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.data.task;

import cz.tul.dic.data.deformation.DeformationDegree;
import cz.tul.dic.data.task.splitter.TaskSplitMethod;
import cz.tul.dic.engine.displacement.ResultCompilation;
import cz.tul.dic.engine.displacement.DisplacementCalculation;
import cz.tul.dic.engine.opencl.interpolation.Interpolation;
import cz.tul.dic.engine.opencl.solvers.Solver;
import cz.tul.dic.engine.strain.StrainEstimationMethod;
import cz.tul.dic.generators.facet.FacetGeneratorMethod;

/**
 *
 * @author Petr Ječmen
 */
public final class TaskDefaultValues {

    public static final Solver DEFAULT_SOLVER = Solver.NewtonRaphson;
    public static final DisplacementCalculation DEFAULT_DISPLACEMENT_CALCULATION_METHOD = DisplacementCalculation.FIND_MAX_AND_AVERAGE;
    public static final int DEFAULT_DISPLACEMENT_CALCULATION_PARAM = 2000;
    public static final int DEFAULT_FPS = 5000;
    public static final DeformationDegree DEFAULT_DEFORMATION_DEGREE = DeformationDegree.FIRST;
    public static final double[] DEFAULT_DEFORMATION_LIMITS_ZERO = new double[]{-10.0, 10.0, 0.25, -10, 10, 0.25};
    public static final double[] DEFAULT_DEFORMATION_LIMITS_FIRST = new double[]{
        -10.0, 10.0, 0.25, -10, 10, 0.25,
        -0.25, 0.25, 0.05, -0.25, 0.25, 0.05, -0.25, 0.25, 0.05, -0.25, 0.25, 0.05};
    public static final FacetGeneratorMethod DEFAULT_FACET_GENERATOR = FacetGeneratorMethod.TIGHT;
    public static final int DEFAULT_FACET_SPACING = 1;
    public static final int DEFAULT_FACET_SIZE = 20;
    public static final Interpolation DEFAULT_INTERPOLATION = Interpolation.BICUBIC;
    public static final double DEFAULT_MM_TO_PX_RATIO = 1;
    public static final ResultCompilation DEFAULT_RESULT_COMPILATION = ResultCompilation.MAJOR_AVERAGING;
    public static final double DEFAULT_RESULT_QUALITY = 0.5;
    public static final StrainEstimationMethod DEFAULT_STRAIN_ESTIMATION_METHOD = StrainEstimationMethod.LOCAL_LEAST_SQUARES;
    public static final double DEFAULT_STRAIN_ESTIMATION_PARAMETER = 20;
    public static final TaskSplitMethod DEFAULT_TASK_SPLIT_METHOD = TaskSplitMethod.DYNAMIC;
    public static final int DEFAULT_TASK_SPLIT_PARAMETER = 1000;
    public static final int DEFAULT_WINDOW_SIZE = 0;

    private TaskDefaultValues() {
    }
}
