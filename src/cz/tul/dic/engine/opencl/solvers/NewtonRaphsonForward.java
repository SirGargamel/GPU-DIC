/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine.opencl.solvers;

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
public class NewtonRaphsonForward extends NewtonRaphson {

    private static final int COUNT_STEP = 3;

    @Override
    protected RealVector generateNegativeGradient(final AbstractSubset subset) {
        final double[] deformationLimits = limits.get(subset);
        final DeformationDegree defDegree = DeformationUtils.getDegreeFromLimits(deformationLimits);
        final int deformationCount = computeDeformationCount(defDegree);
        final int coeffCount = DeformationUtils.getDeformationCoeffCount(defDegree);
        final double[] data = new double[coeffCount];

        final int resultsBase = (subsetsToCompute.indexOf(subset) * deformationCount);
        final int midPoint = getSetpCountForOneDimension() / 2;
        final int[] indices = new int[coeffCount];
        Arrays.fill(indices, midPoint);
        final long[] counts = DeformationUtils.generateDeformationCounts(deformationLimits);

        for (int i = 0; i < coeffCount; i++) {
            // right index
            indices[i]++;
            data[i] = gpuData[resultsBase + generateIndex(counts, indices)];
            // left index
            indices[i]--;
            data[i] -= gpuData[resultsBase + generateIndex(counts, indices)];
            
            data[i] /= deformationLimits[i * 3 + 2];
            data[i] *= -1;
        }        
        return new ArrayRealVector(data);
    }

    @Override
    protected RealMatrix generateHessianMatrix(final AbstractSubset subset) {
        final double[] deformationLimits = limits.get(subset);
        final DeformationDegree defDegree = DeformationUtils.getDegreeFromLimits(deformationLimits);
        final int deformationCount = computeDeformationCount(defDegree);
        final int coeffCount = DeformationUtils.getDeformationCoeffCount(defDegree);
        final double[][] data = new double[coeffCount][coeffCount];

        final int resultsBase = (fullTask.getSubsets().indexOf(subset) * deformationCount);
        final int midPoint = getSetpCountForOneDimension() / 2;
        final int[] indices = new int[coeffCount];
        Arrays.fill(indices, midPoint);
        final long[] counts = DeformationUtils.generateDeformationCounts(deformationLimits);

        double step;
        for (int i = 0; i < coeffCount; i++) {            
            indices[i]++;
            data[i][i] = gpuData[resultsBase + generateIndex(counts, indices)];
            indices[i] -= 2;
            data[i][i] += gpuData[resultsBase + generateIndex(counts, indices)];
            indices[i] = midPoint;
            data[i][i] -= 2 * gpuData[resultsBase + generateIndex(counts, indices)];
            
            step = deformationLimits[i * 3 + 2];
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
                subResultA /= deformationLimits[j * 3 + 2];

                indices[i]--;
                indices[j]++;
                subResultB = gpuData[resultsBase + generateIndex(counts, indices)];
                indices[j]--;
                subResultB -= gpuData[resultsBase + generateIndex(counts, indices)];
                subResultB /= deformationLimits[j * 3 + 2];

                data[i][j] = (subResultA - subResultB) / deformationLimits[i * 3 + 2];
                data[j][i] = data[i][j];
            }
        }

        return new Array2DRowRealMatrix(data);
    }

    @Override
    protected int getSetpCountForOneDimension() {
        return COUNT_STEP;
    }

}
