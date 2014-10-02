package cz.tul.dic.engine.strain;

import cz.tul.dic.ComputationException;
import cz.tul.dic.Utils;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.debug.DebugControl;
import cz.tul.dic.engine.strain.StrainEstimation.StrainEstimator;
import cz.tul.dic.output.Direction;
import cz.tul.dic.output.ExportUtils;
import cz.tul.dic.output.NameGenerator;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.imageio.ImageIO;
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
    private static final int INDEX_ERRA = 6;
    private static final int INDEX_ERRB = 7;
    private static final double COEFF_ADJUST = 100;

    @Override
    void estimateStrain(TaskContainer tc, int roundFrom, int roundTo) throws ComputationException {
        final double[][][] displacement = TaskContainerUtils.getDisplacement(tc, roundFrom, roundTo);
        if (displacement != null) {
            final int width = displacement.length;
            final int height = displacement[0].length;            

            final double mm = (double) tc.getParameter(TaskParameter.STRAIN_ESTIMATION_PARAM);
            final double mmToPx = (double) tc.getParameter(TaskParameter.MM_TO_PX_RATIO);
            final int windowSize = (int) Math.ceil(mm * mmToPx);

            final List<ExecutionUnit> l = new ArrayList<>(width * height);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    if (displacement[x][y] != null) {
                        l.add(new ExecutionUnit(x, y, displacement, windowSize));
                    }
                }
            }

            final double[][][] result = new double[width][height][];
            final double[][][] resultQuality = new double[][][] {Utils.generateNaNarray(width, height), Utils.generateNaNarray(width, height)};
            try {
                final int threadCount = Runtime.getRuntime().availableProcessors();
                final ExecutorService es = Executors.newWorkStealingPool(threadCount);
                final List<Future<ExecutionUnit>> results = es.invokeAll(l);

                ExecutionUnit eu;
                for (Future<ExecutionUnit> f : results) {
                    eu = f.get();
                    result[eu.getX()][eu.getY()] = eu.getResult();
                    resultQuality[0][eu.getX()][eu.getY()] = eu.getErrors()[0];
                    resultQuality[1][eu.getX()][eu.getY()] = eu.getErrors()[1];
                }
            } catch (InterruptedException | ExecutionException ex) {
                Logger.error(ex);
            }
            
            if (DebugControl.isDebugMode()) {
                try {
                    final Image img = tc.getImage(roundTo);
                    ImageIO.write(ExportUtils.overlayImage(img, ExportUtils.createImageFromMap(resultQuality[0], Direction.Exx)), "BMP", new File(NameGenerator.generateRegressionQualityMap(tc, roundTo, Direction.Exx)));
                    ImageIO.write(ExportUtils.overlayImage(img, ExportUtils.createImageFromMap(resultQuality[1], Direction.Eyy)), "BMP", new File(NameGenerator.generateRegressionQualityMap(tc, roundTo, Direction.Eyy)));
                } catch (IOException ex) {
                    Logger.warn(ex);
                }
            }

            tc.setStrain(roundFrom, roundTo, result);
        }
    }

    private static double[] computeCoeffs(final double[][][] data, final int x, final int y, final int radius) {
        final List<double[]> Xu = new LinkedList<>();
        final List<Double> Yu = new LinkedList<>();
        final List<double[]> Xv = new LinkedList<>();
        final List<Double> Yv = new LinkedList<>();

        final int width = data.length;
        final int height = data[x].length;
        for (int i = x - radius; i <= x + radius; i++) {
            for (int j = y - radius; j <= y + radius; j++) {
                if (i >= 0 && j >= 0 && i < width && j < height && data[i][j] != null) {
                    Xu.add(new double[]{1, i - x, j - y});
                    Yu.add(data[i][j][0]);
                    Xv.add(new double[]{1, i - x, j - y});
                    Yv.add(data[i][j][1]);
                }
            }
        }

        final double[] result;
        if (Xu.size() > 3) {
            result = new double[8];
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
                result[INDEX_ERRA] = regression.estimateRegressionStandardError();

                dataY = new double[Yv.size()];
                for (int i = 0; i < dataY.length; i++) {
                    dataY[i] = Yv.get(i);
                }
                dataX = Xv.toArray(new double[][]{});

                regression.newSampleData(dataY, dataX);
                beta = regression.estimateRegressionParameters();
                System.arraycopy(beta, 0, result, 3, 3);
                result[INDEX_ERRB] = regression.estimateRegressionStandardError();
            } catch (MathIllegalArgumentException ex) {
                Logger.trace(ex.getLocalizedMessage());
                // singular matrix, let solution be zeroes
            }
        } else {
            result = null;
        }

        return result;
    }

    private static double[] computeStrains(final double[] coeffs) {
        final double[] result = new double[3];

        result[StrainResult.Exx] = coeffs[INDEX_A1] * COEFF_ADJUST;
        result[StrainResult.Eyy] = coeffs[INDEX_B2] * COEFF_ADJUST;
        result[StrainResult.Exy] = 0.5 * (coeffs[INDEX_B1] + coeffs[INDEX_A2]) * COEFF_ADJUST;

        return result;
    }

    private static class ExecutionUnit implements Callable<ExecutionUnit> {

        private final int x, y, ws;
        private final double[][][] data;
        private double[] result, errors;

        public ExecutionUnit(int x, int y, double[][][] data, final int ws) {
            this.x = x;
            this.y = y;
            this.ws = ws;
            this.data = data;
        }

        @Override
        public ExecutionUnit call() throws Exception {
            final double[] coeffs = computeCoeffs(data, x, y, (int) Math.ceil(ws / 2.0));
            if (coeffs != null) {
                result = computeStrains(coeffs);
                errors = new double[] {coeffs[INDEX_ERRA], coeffs[INDEX_ERRB]};
            }
            return this;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public double[] getResult() {
            return result;
        }

        public double[] getErrors() {
            return errors;
        }
    }

}
