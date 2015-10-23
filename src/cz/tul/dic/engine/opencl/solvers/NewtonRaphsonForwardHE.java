/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine.opencl.solvers;

import cz.tul.dic.data.deformation.DeformationDegree;
import cz.tul.dic.data.deformation.DeformationUtils;
import cz.tul.dic.data.subset.AbstractSubset;
import cz.tul.dic.data.subset.SubsetDeformator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.math3.analysis.BivariateFunction;
import org.apache.commons.math3.analysis.interpolation.PiecewiseBicubicSplineInterpolatingFunction;
import org.apache.commons.math3.analysis.interpolation.PiecewiseBicubicSplineInterpolator;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;

/**
 *
 * @author Petr Ječmen
 */
public class NewtonRaphsonForwardHE extends NewtonRaphsonForward {

    private static final int COUNT_STEP = 3;
    private static final double DX = 0.5;
    private static final double DY = DX;    

    @Override
    protected RealMatrix generateHessianMatrix(final AbstractSubset subset) {
        final double[] deformationLimits = limits.get(subset);
        final DeformationDegree defDegree = DeformationUtils.getDegreeFromLimits(deformationLimits);
        final int coeffCount = DeformationUtils.getDeformationCoeffCount(defDegree);
        final double[][] data = new double[coeffCount][coeffCount];

        final double[] deformation = extractSolutionFromLimits(deformationLimits);
        final SubsetDeformator deformator = new SubsetDeformator();

        final byte[][] image = fullTask.getImageB().to2DBWArray();
        final Map<int[], double[]> deformedSubset = deformator.deformSubset(subset, deformation);
        final PiecewiseBicubicSplineInterpolatingFunction interpolationFunction = prepareInterpolator(deformedSubset, image);
        final double firstSum = computeFirstSum(deformedSubset, interpolationFunction);

        for (int i = 0; i < coeffCount; i++) {
            for (int j = i; j < coeffCount; j++) {
                data[i][j] = firstSum * computeSecondSum(i, j, subset, deformedSubset, interpolationFunction);
                data[j][i] = data[i][j];
            }
        }

        return new Array2DRowRealMatrix(data, false);
    }

    private double computeFirstSum(final Map<int[], double[]> deformedSubset, final PiecewiseBicubicSplineInterpolatingFunction interpolation) {
        final int imageWidth = fullTask.getImageB().getWidth();
        final int imageHeight = fullTask.getImageB().getHeight();

        double sum = 0;
        double val;
        for (double[] coords : deformedSubset.values()) {
            if (!coordsValid(coords, imageWidth, imageHeight)) {
                continue;
            }

            val = interpolation.value(coords[0], coords[1]);
            sum += val * val;
        }
        return -2 / sum;
    }

    private static boolean coordsValid(final double[] coords, final int width, final int height) {
        return coords[0] >= 0 && coords[1] >= 0 && coords[0] < width && coords[1] < height;
    }

    private PiecewiseBicubicSplineInterpolatingFunction prepareInterpolator(
            final Map<int[], double[]> deformedSubset, final byte[][] image) {
        int leftX = Integer.MAX_VALUE;
        int topY = Integer.MAX_VALUE;
        int rightX = 0;
        int bottomY = 0;
        for (double[] val : deformedSubset.values()) {
            leftX = (int) Math.min(leftX, Math.floor(val[0]));
            rightX = (int) Math.max(rightX, Math.ceil(val[0]));
            topY = (int) Math.min(topY, Math.floor(val[1]));
            bottomY = (int) Math.max(bottomY, Math.ceil(val[1]));
        }
        // DX and DY compensation
        if (leftX > 0) {
            leftX--;
        } else {
            leftX = 0;
        }
        if (rightX < image.length - 1) {
            rightX++;
        } else {
            rightX = image.length - 1;
        }
        if (topY > 0) {
            topY--;
        } else {
            topY = 0;
        }
        if (bottomY < image[0].length - 1) {
            bottomY++;
        } else {
            bottomY = image[0].length - 1;
        }

        final int pointCountX = rightX - leftX + 1;
        final int pointCountY = bottomY - topY + 1;
        double[] xval = new double[pointCountX];
        double[] yval = new double[pointCountY];
        double[][] fval = new double[pointCountX][pointCountY];
        for (int x = leftX; x <= rightX; x++) {
            xval[x - leftX] = x;

            for (int y = topY; y <= bottomY; y++) {
                yval[y - topY] = y;
                fval[x - leftX][y - topY] = image[x][y];
            }
        }

        return new PiecewiseBicubicSplineInterpolator().interpolate(xval, yval, fval);
    }

    private double computeSecondSum(
            final int i, final int j,
            final AbstractSubset subset,
            final Map<int[], double[]> deformedSubset,
            final PiecewiseBicubicSplineInterpolatingFunction interpolation) {
        final Approximation approximationI = new Approximation(subset, interpolation, i);
        final Approximation approximationJ = new Approximation(subset, interpolation, j);
        
        final int imageWidth = fullTask.getImageB().getWidth();
        final int imageHeight = fullTask.getImageB().getHeight();

        double sum = 0;
        double valueI, valueJ;
        for (double[] coords : deformedSubset.values()) {
            if (!coordsValid(coords, imageWidth, imageHeight)) {
                continue;
            }

            valueI = approximationI.calculateValue(coords[0], coords[1]);
            valueJ = approximationJ.calculateValue(coords[0], coords[1]);
            sum += valueI * valueJ;
        }

        return sum;
    }

    @Override
    protected int getSetpCountForOneDimension() {
        return COUNT_STEP;
    }

    private static class Approximation {

        private static final List<HessianApproximationFunction> functions;
        private final AbstractSubset subset;
        private final BivariateFunction interpolation;
        private final HessianApproximationFunction matrixApproxFunction;

        static {
            functions = new ArrayList<>(6);
            functions.add(new HessianApproximationFunction() {
                @Override
                public double calculateValue(double x, double y, AbstractSubset subset, final BivariateFunction interpolation) {
                    return (interpolation.value(x + DX, y) - interpolation.value(x, y)) / DX;
                }
            });
            functions.add(new HessianApproximationFunction() {
                @Override
                public double calculateValue(double x, double y, AbstractSubset subset, final BivariateFunction interpolation) {
                    return (interpolation.value(x, y + DY) - interpolation.value(x, y)) / DY;
                }
            });
            functions.add(new HessianApproximationFunction() {
                @Override
                public double calculateValue(double x, double y, AbstractSubset subset, final BivariateFunction interpolation) {
                    final double dif = (interpolation.value(x + DX, y) - interpolation.value(x, y)) / DX;
                    return (x - subset.getCenter()[0]) * dif;
                }
            });
            functions.add(new HessianApproximationFunction() {
                @Override
                public double calculateValue(double x, double y, AbstractSubset subset, final BivariateFunction interpolation) {
                    final double dif = (interpolation.value(x, y + DY) - interpolation.value(x, y)) / DY;
                    return (y - subset.getCenter()[1]) * dif;
                }
            });
            functions.add(new HessianApproximationFunction() {
                @Override
                public double calculateValue(double x, double y, AbstractSubset subset, final BivariateFunction interpolation) {
                    final double dif = (interpolation.value(x + DX, y) - interpolation.value(x, y)) / DX;
                    return (y - subset.getCenter()[1]) * dif;
                }
            });
            functions.add(new HessianApproximationFunction() {
                @Override
                public double calculateValue(double x, double y, AbstractSubset subset, final BivariateFunction interpolation) {
                    final double dif = (interpolation.value(x, y + DY) - interpolation.value(x, y)) / DY;
                    return (x - subset.getCenter()[0]) * dif;
                }
            });
        }

        public Approximation(final AbstractSubset subset, final BivariateFunction interpolation, final int i) {
            this.subset = subset;
            this.interpolation = interpolation;
            matrixApproxFunction = functions.get(i);
        }

        public double calculateValue(final double x, final double y) {
            return matrixApproxFunction.calculateValue(x, y, subset, interpolation);
        }

        private interface HessianApproximationFunction {

            double calculateValue(final double x, final double y, final AbstractSubset subset, final BivariateFunction interpolation);
        }

    }
}
