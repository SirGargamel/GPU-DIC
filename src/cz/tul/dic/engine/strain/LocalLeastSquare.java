package cz.tul.dic.engine.strain;

import cz.tul.dic.data.task.DefaultValues;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.engine.strain.StrainEstimation.StrainEstimator;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.math3.exception.MathIllegalArgumentException;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;
import org.pmw.tinylog.Logger;

public class LocalLeastSquare extends StrainEstimator {

    private static final int INDEX_A0 = 0;
    private static final int INDEX_A1 = 1;
    private static final int INDEX_A2 = 2;
    private static final int INDEX_B0 = 3;
    private static final int INDEX_B1 = 4;
    private static final int INDEX_B2 = 5;
    private static final double COEFF_ADJUST = 100;

    @Override
    void estimateStrain(TaskContainer tc, int round) {
        final double[][][] displacement = tc.getDisplacement(round);
        if (displacement != null) {
            final int width = displacement.length;
            final int height = displacement[0].length;
            final double[][][] result = new double[width][height][];
            
            final double mm = (double) tc.getParameter(TaskParameter.STRAIN_ESTIMATION_PARAM);
            final double mmToPx = (double) tc.getParameter(TaskParameter.MM_TO_PX_RATIO);
            final int windowSize = (int) Math.ceil(mm * mmToPx / 2.0);

            double[] coeffs;
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    if (displacement[x][y] != null) {
                        coeffs = computeCoeffs(displacement, x, y, windowSize / 2);
                        if (coeffs != null) {
                            result[x][y] = computeStrains(coeffs);
                        }
                    }
                }
            }

            tc.setStrain(round, result);
        }
    }

    private double[] computeCoeffs(final double[][][] data, final int x, final int y, final int ws) {
        final List<double[]> Xu = new LinkedList<>();
        final List<Double> Yu = new LinkedList<>();
        final List<double[]> Xv = new LinkedList<>();
        final List<Double> Yv = new LinkedList<>();

        final int width = data.length;
        final int height = data[x].length;
        for (int i = x - ws; i <= x + ws; i++) {
            for (int j = y - ws; j <= y + ws; j++) {
                if (i >= 0 && j >= 0 && i < width && j < height && data[i][j] != null) {
                    Xu.add(new double[]{1, i - x, j - x});
                    Yu.add(data[i][j][0]);
                    Xv.add(new double[]{1, i - x, j - x});
                    Yv.add(data[i][j][1]);
                }
            }
        }

        final double[] result;
        if (Xu.size() > 3) {
            result = new double[6];
            try {
                final OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();
                regression.setNoIntercept(true);

                double[] dataY = new double[Yu.size()];
                for (int i = 0; i < dataY.length; i++) {
                    dataY[i] = Yu.get(i);
                }
                double[][] dataX = Xu.toArray(new double[][]{});

                regression.newSampleData(dataY, dataX);
                double[] beta = regression.estimateRegressionParameters();
                System.arraycopy(beta, 0, result, 0, 3);

                dataY = new double[Yv.size()];
                for (int i = 0; i < dataY.length; i++) {
                    dataY[i] = Yv.get(i);
                }
                dataX = Xv.toArray(new double[][]{});

                regression.newSampleData(dataY, dataX);
                beta = regression.estimateRegressionParameters();
                System.arraycopy(beta, 0, result, 3, 3);
            } catch (MathIllegalArgumentException ex) {
                Logger.trace(ex.getLocalizedMessage());
                // singular matrix, let solution be zeroes
            }
        } else {
            result = null;
        }

        return result;
    }

    private double[] computeStrains(final double[] coeffs) {
        final double[] result = new double[3];

        result[StrainResult.Exx] = coeffs[INDEX_A1] * COEFF_ADJUST;
        result[StrainResult.Eyy] = coeffs[INDEX_B2] * COEFF_ADJUST;
        result[StrainResult.Exy] = 0.5 * (coeffs[INDEX_B1] + coeffs[INDEX_A2]) * COEFF_ADJUST;

        return result;
    }

}
