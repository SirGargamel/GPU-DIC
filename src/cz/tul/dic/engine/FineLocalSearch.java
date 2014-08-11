package cz.tul.dic.engine;

import cz.tul.dic.data.Coordinates;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Ječmen
 */
public class FineLocalSearch {

    public static void searchForBestPosition(final TaskContainer tc, final int r, final int nextR) {
        final double[][][] results = tc.getDisplacement(r);
        final Image in = tc.getImage(r);
        final int width = in.getWidth();
        final int height = in.getHeight();
        final Image out = tc.getImage(nextR);
        final int windowSize = (int) tc.getParameter(TaskParameter.LOCAL_SEARCH_PARAM);

        double[] d;
        int colorIn, colorOut, dif, best, newX, newY;
        List<double[]> candidates = new ArrayList<>();
        double bestDx = 0, bestDy = 0;
        for (int x = 0; x < results.length; x++) {
            for (int y = 0; y < results[x].length; y++) {
                d = results[x][y];
                if (d != null) {
                    best = Integer.MAX_VALUE;
                    candidates.clear();
                    colorIn = in.getRGB(x, y);

                    for (int dx = -windowSize; dx <= windowSize; dx++) {
                        for (int dy = -windowSize; dy <= windowSize; dy++) {
                            newX = (int) Math.round(x + d[Coordinates.X] + dx);
                            newY = (int) Math.round(y + d[Coordinates.Y] + dy);

                            if (newX >= 0 && newY >= 0 && newX < width && newY < height) {
                                colorOut = out.getRGB(newX, newY);
                                dif = Math.abs(colorIn - colorOut);

                                if (dif < best) {
                                    best = dif;
                                    candidates.clear();
                                    candidates.add(new double[]{d[Coordinates.X] + dx, d[Coordinates.Y] + dy});
                                } else if (dif == best) {
                                    candidates.add(new double[]{d[Coordinates.X] + dx, d[Coordinates.Y] + dy});
                                }
                            }
                        }
                    }

                    if (!candidates.isEmpty()) {
                        if (candidates.size() > 1) {
                            Collections.sort(candidates, new ResultSorter(d));
                        }

                        bestDx = candidates.get(0)[Coordinates.X];
                        bestDy = candidates.get(0)[Coordinates.Y];

                        if (bestDx != d[Coordinates.X] || bestDy != d[Coordinates.Y]) {
                            Logger.trace("Found better location - {0}; {1} vs. {2}; {3}.", d[Coordinates.X], d[Coordinates.Y], bestDx, bestDy);
                        }

                        d[Coordinates.X] = bestDx;
                        d[Coordinates.Y] = bestDy;
                    }
                }
            }
        }
    }

    private static class ResultSorter implements Comparator<double[]> {

        private final double[] orig;

        public ResultSorter(double[] orig) {
            this.orig = orig;
        }

        @Override
        public int compare(double[] o1, double[] o2) {
            double da = o1[0] - orig[0];
            double db = o1[1] - orig[1];
            final double d1 = Math.sqrt(da * da + db * db);
            da = o2[0] - orig[0];
            db = o2[1] - orig[1];
            final double d2 = Math.sqrt(da * da + db * db);
            return Double.compare(d1, d2);
        }

    }

}
