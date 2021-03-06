/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.data.task;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import cz.tul.dic.data.deformation.DeformationOrder;
import cz.tul.dic.data.task.splitter.TaskSplitMethod;
import cz.tul.dic.engine.displacement.DisplacementCalculation;
import cz.tul.dic.data.Interpolation;
import cz.tul.dic.engine.solvers.SolverType;
import cz.tul.dic.engine.strain.StrainEstimationMethod;
import cz.tul.dic.data.subset.generator.SubsetGenerator;
import cz.tul.dic.engine.KernelInfo;
import java.io.File;

/**
 *
 * @author Petr Jecmen
 */
@XStreamAlias("TaskParameter")
public enum TaskParameter {

    IN(File.class),
    SUBSET_GENERATOR_METHOD(SubsetGenerator.class),
    SUBSET_GENERATOR_PARAM(Integer.class),
    SUBSET_SIZE(Integer.class),
    FILTER_KERNEL_SIZE(Integer.class),
    FPS(Integer.class),
    KERNEL(KernelInfo.class),
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
    DEFORMATION_ORDER(DeformationOrder.class),
    SOLVER(SolverType.class),
    CORRELATION_WEIGHT(Double.class);

    private final Class type;

    TaskParameter(final Class cls) {
        this.type = cls;
    }

    public Class getType() {
        return type;
    }
}
