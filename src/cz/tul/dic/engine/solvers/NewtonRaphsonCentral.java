/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine.solvers;

import cz.tul.dic.data.deformation.DeformationUtils;
import cz.tul.dic.data.subset.AbstractSubset;
import cz.tul.dic.engine.AbstractDeviceManager;
import cz.tul.dic.engine.platform.Platform;
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
public class NewtonRaphsonCentral extends NewtonRaphson {

    public NewtonRaphsonCentral(Platform platform) {
        super(platform);
    }

    // central difference
    // dF / dx = (F(x + h) - F(x - h)) / 2h
    @Override
    protected RealVector generateNegativeGradient(final AbstractSubset subset, final double step) {
        final int coeffCount = getCoeffCount();
        final double[] data = new double[coeffCount];

        final int deformationCount = (int) getDeformationCount();
        final int resultsBase = (subsetsToCompute.indexOf(subset) * deformationCount);

        try {            
            for (int i = 0; i < coeffCount; i++) {
                // f(x+h)            
                data[i] = gpuData[resultsBase + 1 + i];
                // f(x-h)
                data[i] -= gpuData[resultsBase + 1 + coeffCount + i];

                data[i] /= 2 * step;
                data[i] *= -1;
            }
        } catch (Exception ex) {
            System.err.println("ERROR");
        }
        return new ArrayRealVector(data);
    }

    // d^2 F / dx1 dx2 = (F(x1 + h, x2 + h) - F(x1 + h, x2 - h) - F(x1 - h, x2 + h) + F(x1 - h, x2 - h)) / (2h)^2
    @Override
    protected RealMatrix generateHessianMatrix(final AbstractSubset subset, final double step) {
        final int coeffCount = getCoeffCount();
        final double[][] data = new double[coeffCount][coeffCount];

        final int deformationCount = (int) getDeformationCount();
        final int resultsBase = (subsetsToCompute.indexOf(subset) * deformationCount);

        final double step212 = 12 * step * step;
        for (int i = 0; i < coeffCount; i++) {
            data[i][i] = -gpuData[resultsBase + generatePositiveDoubleStepIndex(i, i, coeffCount)];
            data[i][i] += 16 * gpuData[resultsBase + 1 + i];
            data[i][i] -= 30 * gpuData[resultsBase];
            data[i][i] += 16 * gpuData[resultsBase + 1 + coeffCount + i];
            data[i][i] -= gpuData[resultsBase + generateNegativeDoubleStepIndex(i, i, coeffCount)];

            data[i][i] /= step212;
        }

        final double step4 = 4 * step * step;
        for (int i = 0; i < coeffCount; i++) {
            for (int j = i + 1; j < coeffCount; j++) {
                data[i][j] = gpuData[resultsBase + generatePositiveDoubleStepIndex(i, j, coeffCount)];
                data[i][j] -= gpuData[resultsBase + generatePositiveNegativeDoubleStepIndex(i, j, coeffCount)];
                data[i][j] -= gpuData[resultsBase + generateNegativePositiveDoubleStepIndex(i, j, coeffCount)];
                data[i][j] += gpuData[resultsBase + generateNegativeDoubleStepIndex(i, j, coeffCount)];

                data[i][j] /= step4;
                data[j][i] = data[i][j];
            }
        }

        return new Array2DRowRealMatrix(data, false);
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
        // f(x - h)
        for (int i = 0; i < coeffCount; i++) {
            deformation = Arrays.copyOf(solution, coeffCount);
            deformation[i] -= step;
            resultA.add(deformation);
        }
        // f(x + h + k)
        for (int i = 0; i < coeffCount; i++) {
            for (int j = 0; j < coeffCount; j++) {
                deformation = Arrays.copyOf(solution, coeffCount);
                deformation[i] += step;
                deformation[j] += step;
                resultA.add(deformation);
            }
        }
        // f(x - h - k)
        for (int i = 0; i < coeffCount; i++) {
            for (int j = 0; j < coeffCount; j++) {
                deformation = Arrays.copyOf(solution, coeffCount);
                deformation[i] -= step;
                deformation[j] -= step;
                resultA.add(deformation);
            }
        }
        // f(x + h - k)
        for (int i = 0; i < coeffCount; i++) {
            for (int j = 0; j < coeffCount; j++) {
                deformation = Arrays.copyOf(solution, coeffCount);
                deformation[i] += step;
                deformation[j] -= step;
                resultA.add(deformation);
            }
        }
        // f(x - h + k)
        for (int i = 0; i < coeffCount; i++) {
            for (int j = 0; j < coeffCount; j++) {
                deformation = Arrays.copyOf(solution, coeffCount);
                deformation[i] -= step;
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

    private int generatePositiveDoubleStepIndex(final int i, final int j, final int coeffCount) {
        return 1 + (2 * coeffCount) + (i * coeffCount) + j;
    }

    private int generateNegativeDoubleStepIndex(final int i, final int j, final int coeffCount) {
        return 1 + (2 * coeffCount) + (coeffCount * coeffCount) + (i * coeffCount) + j;
    }

    private int generatePositiveNegativeDoubleStepIndex(final int i, final int j, final int coeffCount) {
        return 1 + (2 * coeffCount) + (2 * coeffCount * coeffCount) + (i * coeffCount) + j;
    }

    private int generateNegativePositiveDoubleStepIndex(final int i, final int j, final int coeffCount) {
        return 1 + (2 * coeffCount) + (3 * coeffCount * coeffCount) + (i * coeffCount) + j;
    }

    @Override
    public long getDeformationCount() {
        deformationOrder = DeformationUtils.getOrderFromLimits(fullTask.getDeformationLimits().get(0));
        final int coeffCount = DeformationUtils.getDeformationCoeffCount(deformationOrder);
        return 1 + 2 * coeffCount + 4 * coeffCount * coeffCount;
    }
}
