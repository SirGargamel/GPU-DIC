/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine.opencl.solvers;

import cz.tul.dic.ComputationException;
import cz.tul.dic.data.deformation.DeformationDegree;
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

    private static final int COUNT_STEP = 5;

    // central difference
    @Override
    protected RealVector generateNegativeGradient(final AbstractSubset subset) throws ComputationException {
        final double[] deformationLimits = limits.get(subset);
        final DeformationDegree defDegree = DeformationUtils.getDegreeFromLimits(deformationLimits);
        final int deformationCount = computeDeformationCount(defDegree);
        final int coeffCount = DeformationUtils.getDeformationCoeffCount(defDegree);
        final double[] data = new double[coeffCount];

        final int resultsBase = (int) (fullTask.getSubsets().indexOf(subset) * deformationCount);
        final int[] indices = new int[coeffCount];
        Arrays.fill(indices, getSetpCountForOneDimension() / 2);
        final long[] counts = DeformationUtils.generateDeformationCounts(deformationLimits);

        for (int i = 0; i < coeffCount; i++) {
            // right index
            indices[i]++;
            data[i] = gpuData[resultsBase + generateIndex(counts, indices)];
            // left index
            indices[i] -= 2;
            data[i] -= gpuData[resultsBase + generateIndex(counts, indices)];
            data[i] /= 2 * deformationLimits[i * 3 + 2];
            data[i] *= -1;
            indices[i]++;
        }
        return new ArrayRealVector(data);
    }

    @Override
    protected RealMatrix generateHessianMatrix(final AbstractSubset subset) throws ComputationException {
        final double[] deformationLimits = limits.get(subset);
        final DeformationDegree defDegree = DeformationUtils.getDegreeFromLimits(deformationLimits);
        final int deformationCount = computeDeformationCount(defDegree);
        final int coeffCount = DeformationUtils.getDeformationCoeffCount(defDegree);
        final double[][] data = new double[coeffCount][coeffCount];

        final int resultsBase = (int) (fullTask.getSubsets().indexOf(subset) * deformationCount);
        final int[] indices = new int[coeffCount];
        Arrays.fill(indices, getSetpCountForOneDimension() / 2);
        final long[] counts = DeformationUtils.generateDeformationCounts(deformationLimits);

        // upper triangle approach
        double step;
        for (int i = 0; i < coeffCount; i++) {
            step = deformationLimits[i * 3 + 2];

            indices[i]++;
            data[i][i] = gpuData[resultsBase + generateIndex(counts, indices)];
            indices[i] -= 2;
            data[i][i] += gpuData[resultsBase + generateIndex(counts, indices)];
            indices[i]++;
            data[i][i] -= 2 * gpuData[resultsBase + generateIndex(counts, indices)];
            data[i][i] /= step * step * 4;
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
                indices[i]--;
                indices[j]++;

                data[i][j] /= (deformationLimits[i * 3 + 2] + deformationLimits[j * 3 + 2]) * (deformationLimits[i * 3 + 2] + deformationLimits[j * 3 + 2]);
                data[i][j] /= 4.0;
                data[j][i] = data[i][j];
            }
        }

        return new Array2DRowRealMatrix(data, false);
    }

    @Override
    protected int getSetpCountForOneDimension() {
        return COUNT_STEP;
    }
}
