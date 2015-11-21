/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine.opencl.solvers;

import cz.tul.dic.data.deformation.DeformationUtils;
import cz.tul.dic.data.subset.AbstractSubset;
import java.util.Arrays;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

/**
 *
 * @author Petr Ječmen
 */
public class NewtonRaphsonForward extends NewtonRaphson {

    private static final int COUNT_STEP = 3;

    public NewtonRaphsonForward() {
        super(COUNT_STEP);
    }

    protected NewtonRaphsonForward(final int stepCount) {
        super(stepCount);
    }

    @Override
    protected RealVector generateNegativeGradient(final AbstractSubset subset) {
        final double[] subsetLimits = prepareArrayForSolver(subset);                
        final int coeffCount = getCoeffCount(subset);        
        final double[] data = new double[coeffCount];

        final int resultsBase = (subsetsToCompute.indexOf(subset) * computeDeformationCount(defDegree));
        final int midPoint = COUNT_STEP / 2;
        final int[] indices = new int[coeffCount];
        Arrays.fill(indices, midPoint);
        final long[] counts = DeformationUtils.generateDeformationCounts(subsetLimits);

        for (int i = 0; i < coeffCount; i++) {
            // right index
            indices[i]++;
            data[i] = gpuData[resultsBase + generateIndex(counts, indices)];
            // left index
            indices[i]--;
            data[i] -= gpuData[resultsBase + generateIndex(counts, indices)];

            data[i] /= subsetLimits[i * 3 + 2];
            data[i] *= -1;
        }
        return new ArrayRealVector(data);
    }

    @Override
    protected RealMatrix generateHessianMatrix(final AbstractSubset subset) {
        final double[] subsetLimits = prepareArrayForSolver(subset);                
        final int coeffCount = getCoeffCount(subset);
        final double[][] data = new double[coeffCount][coeffCount];

        final int resultsBase = (fullTask.getSubsets().indexOf(subset) * computeDeformationCount(defDegree));
        final int midPoint = COUNT_STEP / 2;
        final int[] indices = new int[coeffCount];
        Arrays.fill(indices, midPoint);
        final long[] counts = DeformationUtils.generateDeformationCounts(subsetLimits);

        double step;
        for (int i = 0; i < coeffCount; i++) {
            indices[i]++;
            data[i][i] = gpuData[resultsBase + generateIndex(counts, indices)];
            indices[i] -= 2;
            data[i][i] += gpuData[resultsBase + generateIndex(counts, indices)];
            indices[i] = midPoint;
            data[i][i] -= 2 * gpuData[resultsBase + generateIndex(counts, indices)];

            step = subsetLimits[i * 3 + 2];
            data[i][i] /= step * step;
        }

        // direct approach with forward difference
        double subResultA, subResultB;
        for (int i = 0; i < coeffCount; i++) {
            for (int j = i + 1; j < coeffCount; j++) {
                indices[i]++;
                indices[j]++;
                subResultA = gpuData[resultsBase + generateIndex(counts, indices)];
                indices[j]--;
                subResultA -= gpuData[resultsBase + generateIndex(counts, indices)];
                subResultA /= subsetLimits[j * 3 + 2];

                indices[i]--;
                indices[j]++;
                subResultB = gpuData[resultsBase + generateIndex(counts, indices)];
                indices[j]--;
                subResultB -= gpuData[resultsBase + generateIndex(counts, indices)];
                subResultB /= subsetLimits[j * 3 + 2];

                data[i][j] = (subResultA - subResultB) / subsetLimits[i * 3 + 2];
                data[j][i] = data[i][j];
            }
        }

        return new Array2DRowRealMatrix(data);
    }

}
