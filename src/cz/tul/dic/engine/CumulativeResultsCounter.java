package cz.tul.dic.engine;

import cz.tul.dic.data.Coordinates;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Petr Ječmen
 */
public class CumulativeResultsCounter {

    public static List<double[][][]> calculate(final TaskContainer tc, final List<double[][][]> data) {
        final int roundCount = TaskContainerUtils.getMaxRoundCount(tc);
        final int srcPos = TaskContainerUtils.getRounds(tc).keySet().iterator().next();
        int destPos = srcPos;
        for (Integer i : TaskContainerUtils.getRounds(tc).keySet()) {
            if (i > destPos) {
                destPos = i;
            }
        }
        return calculateCumulativeResults(data, roundCount, srcPos, destPos);
    }

    private static List<double[][][]> calculateCumulativeResults(final List<double[][][]> data, final int count, final int srcPos, final int destPos) {
        final List<double[][][]> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            result.add(null);
        }

        for (int i = srcPos; i <= destPos; i++) {
            result.set(i, calculateCumulativeResult(data, srcPos, i));
        }

        return result;
    }

    private static double[][][] calculateCumulativeResult(final List<double[][][]> data, final int srcPos, final int destPos) {
        double[][][] roundData = data.get(srcPos);        
        final double[][][] result = new double[roundData.length][roundData[0].length][];

        int l;
        for (int i = srcPos; i <= destPos; i++) {
            roundData = data.get(i);

            for (int x = 0; x < result.length; x++) {
                for (int y = 0; y < result[x].length; y++) {
                    if (roundData[x][y] != null) {
                        l = roundData[x][y].length;
                        result[x][y] = new double[l];
                        for (int j = 0; j < l; j++) {
                            result[x][y][j] += roundData[x][y][j];
                        }
                    }
                }
            }
        }

        return result;
    }

}
