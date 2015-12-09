/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine.opencl.solvers;

import cz.tul.dic.data.deformation.DeformationUtils;
import cz.tul.dic.data.subset.AbstractSubset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

/**
 *
 * @author Petr Ječmen
 */
public class NewtonRaphsonForward extends NewtonRaphson {

    // forward difference
    // dF / dx = (F(x + h) - F(x)) / h
    @Override
    protected RealVector generateNegativeGradient(final AbstractSubset subset, final double step) {
        final int coeffCount = getCoeffCount();
        final double[] data = new double[coeffCount];

        final int deformationCount = getDeformationCount();
        final int resultsBase = (subsetsToCompute.indexOf(subset) * deformationCount);

        for (int i = 0; i < coeffCount; i++) {
            // right
            data[i] = gpuData[resultsBase + 1 + i];
            // origin         
            data[i] -= gpuData[resultsBase];

            data[i] /= step;
            data[i] *= -1;
        }
        return new ArrayRealVector(data);
    }

    // d^2 F / dx1 dx2 = (F(x1 + h, x2 + h) - F(x1 + h) - F(x2 + h) + F(x)) / (2h)
    @Override
    protected RealMatrix generateHessianMatrix(final AbstractSubset subset, final double step) {
        final int coeffCount = getCoeffCount();
        final double[][] data = new double[coeffCount][coeffCount];

        final int deformationCount = getDeformationCount();
        final int resultsBase = (subsetsToCompute.indexOf(subset) * deformationCount);

        final double step2 = step * step;
        // direct approach with forward difference        
        for (int i = 0; i < coeffCount; i++) {
            for (int j = i; j < coeffCount; j++) {
                data[i][j] = gpuData[resultsBase + generateDoubleStepIndex(i, j, coeffCount)];
                data[i][j] -= gpuData[resultsBase + 1 + i];
                data[i][j] -= gpuData[resultsBase + 1 + j];
                data[i][j] += gpuData[resultsBase];
                data[i][j] /= step2;
                
                data[j][i] = data[i][j];
            }
        }

        return new Array2DRowRealMatrix(data);
    }
    
    private int generateDoubleStepIndex(final int i, final int j, final int coeffCount) {
        int result = 1 + coeffCount;
        for (int k = 0; k < coeffCount; k++) {
            if (k >= i) {
                break;
            }
            result += coeffCount - k;
        }
        result += j - i;
        return result;
    }

    @Override
    protected double[] generateDeformations(double[] solution, double step) {
        final int coeffCount = solution.length;
        final List<double[]> resultA = new ArrayList<>();
        // f(x)
        resultA.add(Arrays.copyOf(solution, coeffCount));
        // f(x + h)
        double[] deformation;
        for (int i = 0; i < coeffCount; i++) {
            deformation = Arrays.copyOf(solution, coeffCount);
            deformation[i] += step;
            resultA.add(deformation);
        }
        // f(x + h + k)
        for (int i = 0; i < coeffCount; i++) {
            for (int j = i; j < coeffCount; j++) {
                deformation = Arrays.copyOf(solution, coeffCount);
                deformation[i] += step;
                deformation[j] += step;
                resultA.add(deformation);
            }
        }
        // create resulting array
        final double[] result = new double[coeffCount * resultA.size()];
        for (int i = 0; i < resultA.size(); i++) {
            System.arraycopy(resultA.get(i), 0, result, i * coeffCount, coeffCount);
        }
        return result;
    }       

    @Override
    protected int getDeformationCount() {
        final int coeffCount = DeformationUtils.getDeformationCoeffCount(order);
        return 1 + coeffCount + ((coeffCount * (coeffCount + 1)) / 2);
    }

}
