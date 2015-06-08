/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.data.task;

import cz.tul.dic.data.deformation.DeformationDegree;
import cz.tul.dic.data.task.splitter.TaskSplitMethod;
import cz.tul.dic.engine.displacement.DisplacementCalculation;
import cz.tul.dic.engine.opencl.interpolation.Interpolation;
import cz.tul.dic.engine.opencl.kernels.KernelType;
import cz.tul.dic.engine.opencl.solvers.Solver;
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
    ROUND_LIMITS(int[].class),
    DISPLACEMENT_CALCULATION_METHOD(DisplacementCalculation.class),
    DISPLACEMENT_CALCULATION_PARAM(Integer.class),
    RESULT_QUALITY(Double.class),
    STRAIN_ESTIMATION_METHOD(StrainEstimationMethod.class),
    STRAIN_ESTIMATION_PARAM(Double.class),    
    DEFORMATION_LIMITS(double[].class),
    DEFORMATION_ORDER(DeformationDegree.class),
    SOLVER(Solver.class);    

    private final Class type;

    TaskParameter(final Class cls) {
        this.type = cls;
    }

    public Class getType() {
        return type;
    }
}
