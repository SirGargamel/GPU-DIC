package cz.tul.dic.engine.strain;

import cz.tul.dic.data.task.TaskContainer;

public class Differentiator extends StrainEstimator {

    @Override
    void estimateStrain(TaskContainer tc, int round) {
        final double[][][] displacement = tc.getDisplacement(round);
        if (displacement != null) {
            final int width = displacement.length;
            final int height = displacement[0].length;
            final double[][][] result = new double[width][height][];

            int prevX, prevY;
            double ux, uy, vx, vy;
            for (int x = 1; x < width; x++) {
                prevX = x - 1;

                for (int y = 1; y < height; y++) {
                    prevY = y - 1;
                    if (displacement[x][y] == null || displacement[prevX][y] == null || displacement[x][prevY] == null) {
                        continue;
                    }

                    ux = displacement[x][y][0] - displacement[prevX][y][0];
                    uy = displacement[x][y][0] - displacement[x][prevY][0];
                    vx = displacement[x][y][1] - displacement[prevX][y][1];
                    vy = displacement[x][y][1] - displacement[x][prevY][1];

                    result[x][y] = new double[]{
                        ux + (ux * ux + vx * vx) / 2.0,
                        vy + (uy * uy + vy * vy) / 2.0,
                        (uy + vx) / 2.0 + (ux * vy + vx * uy) / 2.0
                    };
                }
            }

            tc.setStrain(round, result);
        }
    }

    private static double[][][] smooth(final double[][][] data) {
        final double[][][] result = new double[data.length][data[0].length][data[0][0].length];

        for (int x = 0; x < result.length; x++) {
            for (int y = 0; y < result[x].length; y++) {
                // TODO smoothing
                result[x][y] = data[x][y];
            }
        }

        return result;
    }
}
