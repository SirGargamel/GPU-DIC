/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine.opencl.solvers;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

/**
 *
 * @author Petr Ječmen
 */
public class NewtonRaphsonForward extends NewtonRaphson {

    @Override
    protected RealVector generateGradient(final double[] resultData, final int subsetIndex, final int subsetCount, final int[] counts, final double[] deformationLimits) {
        final int coeffCount = counts.length - 1;
        final double[] data = new double[coeffCount];

        final int deformationCount = resultData.length / subsetCount;
        final int resultsBase = subsetIndex * deformationCount;
        final int[] indices = prepareIndices(counts);
        for (int i = 0; i < coeffCount; i++) {
            // right index
            indices[i]++;
            data[i] = resultData[resultsBase + generateIndex(counts, indices)];
            // left index
            indices[i]--;
            data[i] -= resultData[resultsBase + generateIndex(counts, indices)];
            data[i] /= deformationLimits[i * 3 + 2];
        }
        return new ArrayRealVector(data);
    }

    @Override
    protected RealMatrix generateHessianMatrix(final double[] resultData, final int subsetIndex, final int subsetCount, final int[] counts, final double[] deformationLimits) {
        final int coeffCount = counts.length - 1;
        final double[][] data = new double[coeffCount][coeffCount];

        final int deformationCount = resultData.length / subsetCount;
        final int resultsBase = subsetIndex * deformationCount;
        final int[] indices = prepareIndices(counts);

        // direct approach with forward difference
        double subResultA, subResultB;
        for (int i = 0; i < coeffCount; i++) {
            for (int j = i; j < coeffCount; j++) {
                indices[i]++;
                indices[j]++;
                subResultA = resultData[resultsBase + generateIndex(counts, indices)];
                indices[j]--;
                subResultA -= resultData[resultsBase + generateIndex(counts, indices)];
                subResultA /= deformationLimits[j * 3 + 2];

                indices[i]--;
                indices[j]++;
                subResultB = resultData[resultsBase + generateIndex(counts, indices)];
                indices[j]--;
                subResultB -= resultData[resultsBase + generateIndex(counts, indices)];
                subResultB /= deformationLimits[j * 3 + 2];

                data[i][j] = (subResultA - subResultB) / deformationLimits[i * 3 + 2];
                data[j][i] = data[i][j];
            }
        }

        return new Array2DRowRealMatrix(data);
    }

}
