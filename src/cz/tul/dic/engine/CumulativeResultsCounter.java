package cz.tul.dic.engine;

import cz.tul.dic.data.task.TaskContainer;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Petr Ječmen
 */
public class CumulativeResultsCounter {

    public static List<double[][][]> calculate(final TaskContainer tc, final List<double[][][]> data) {
        final List<double[][][]> result = new ArrayList<>(data.size());

        for (int i = 0; i < data.size(); i++) {
            result.add(calculateCumulativeResult(data, 0, i));
        }

        return result;
    }

    public static double[][][] calculateCumulativeResult(final List<double[][][]> data, final int srcPos, final int destPos) {
        double[][][] result = null;
        double[][][] roundData;
        int l;
        for (int i = srcPos; i <= destPos; i++) {
            roundData = data.get(i);

            if (roundData != null) {
                if (result == null) {
                    result = new double[roundData.length][roundData[0].length][];
                }

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
        }

        return result;
    }

}
