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
public class NewtonRaphsonCentral extends NewtonRaphson {

    private static final int COUNT_STEP = 3;

    public NewtonRaphsonCentral() {
        super(COUNT_STEP);
    }

    // central difference
    // dF / dx = (F(x + h) - F(x - h)) / 2h
    @Override
    protected RealVector generateNegativeGradient(final AbstractSubset subset) {
        final double[] subsetLimits = prepareArrayForSolver(subset);                
        final int coeffCount = getCoeffCount(subset);        
        final double[] data = new double[coeffCount];

        final int resultsBase = (subsetsToCompute.indexOf(subset) * computeDeformationCount(defDegree));
        final int midPoint = COUNT_STEP/ 2;
        final int[] indices = new int[coeffCount];
        Arrays.fill(indices, midPoint);
        final long[] counts = DeformationUtils.generateDeformationCounts(subsetLimits);

        for (int i = 0; i < coeffCount; i++) {
            // right index
            indices[i]++;
            data[i] = gpuData[resultsBase + generateIndex(counts, indices)];
            // left index
            indices[i] -= 2;
            data[i] -= gpuData[resultsBase + generateIndex(counts, indices)];
            indices[i] = midPoint;

            data[i] /= 2 * subsetLimits[i * 3 + 2];
            data[i] *= -1;
        }

        return new ArrayRealVector(data);
    }

    @Override
    // d^2 F / dx1 dx2 = (F(x1 + h, x2 + h) - F(x1 + h, x2 - h) - F(x1 - h, x2 + h) + F(x1 - h, x2 - h)) / (2h)^2
    protected RealMatrix generateHessianMatrix(final AbstractSubset subset) {
        final double[] subsetLimits = prepareArrayForSolver(subset);                
        final int coeffCount = getCoeffCount(subset);
        final double[][] data = new double[coeffCount][coeffCount];

        final int resultsBase = (fullTask.getSubsets().indexOf(subset) * computeDeformationCount(defDegree));
        final int[] indices = new int[coeffCount];
        final int midPoint = COUNT_STEP / 2;
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

        for (int i = 0; i < coeffCount; i++) {
            for (int j = i + 1; j < coeffCount; j++) {
                indices[i]++;
                indices[j]++;
                data[i][j] = gpuData[resultsBase + generateIndex(counts, indices)];
                indices[i] -= 2;
                indices[j] -= 2;
                data[i][j] += gpuData[resultsBase + generateIndex(counts, indices)];
                indices[j] += 2;
                data[i][j] -= gpuData[resultsBase + generateIndex(counts, indices)];
                indices[i] += 2;
                indices[j] -= 2;
                data[i][j] -= gpuData[resultsBase + generateIndex(counts, indices)];
                indices[i] = midPoint;
                indices[j] = midPoint;

                data[i][j] /= (subsetLimits[i * 3 + 2] + subsetLimits[j * 3 + 2]) * (subsetLimits[i * 3 + 2] + subsetLimits[j * 3 + 2]);
                data[j][i] = data[i][j];
            }
        }

        return new Array2DRowRealMatrix(data, false);
    }
}
