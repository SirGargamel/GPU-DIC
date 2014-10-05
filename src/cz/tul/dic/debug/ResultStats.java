package cz.tul.dic.debug;

import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.engine.CorrelationResult;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Ječmen
 */
public class ResultStats {

    public static void dumpResultStatistics(final TaskContainer tc, final int round) {
        final ValueCounter counterGood = ValueCounter.createCounter();
        final ValueCounter counterNotGood = ValueCounter.createCounter();
        final ValueCounter quality = ValueCounter.createCounter();
        final Map<ROI, List<CorrelationResult>> results = tc.getResults(round);
        final double resultQuality = (double) tc.getParameter(TaskParameter.RESULT_QUALITY);

        int val;
        for (ROI roi : results.keySet()) {
            for (CorrelationResult cr : results.get(roi)) {
                if (cr != null) {
                    val = (int) (cr.getValue() * 10);
                    quality.inc(val / (double) 10);
                    if (cr.getValue() < resultQuality) {
                        counterNotGood.inc(cr.getDeformation());
                    } else {
                        counterGood.inc(cr.getDeformation());
                    }
                } else {
                    counterNotGood.inc();
                    quality.inc();
                }
            }
        }

        final StringBuilder sb = new StringBuilder();
        sb.append("--- Resulting deformations statistics --- ROUND\n");
        sb.append("-- GOOD --");
        sb.append(counterGood.toString());
        sb.append("\n-- NOT GOOD --");
        sb.append(counterNotGood.toString());
        sb.append("\n-- QUALITY STATS --");
        sb.append(quality.toString());
        Logger.trace(sb.toString());
    }

    public static void dumpResultStatistics(final TaskContainer tc) {
        final ValueCounter counterGood = ValueCounter.createCounter();
        final ValueCounter counterNotGood = ValueCounter.createCounter();
        final ValueCounter quality = ValueCounter.createCounter();
        final Set<Integer> rounds = TaskContainerUtils.getRounds(tc).keySet();
        final double resultQuality = (double) tc.getParameter(TaskParameter.RESULT_QUALITY);

        int val;
        Map<ROI, List<CorrelationResult>> results;
        for (Integer round : rounds) {
            results = tc.getResults(round);
            if (results != null) {
                for (ROI roi : results.keySet()) {
                    for (CorrelationResult cr : results.get(roi)) {
                        if (cr != null) {
                            val = (int) (cr.getValue() * 10);
                            quality.inc(val / (double) 10);
                            if (cr.getValue() < resultQuality) {
                                counterNotGood.inc(cr.getDeformation());
                            } else {
                                counterGood.inc(cr.getDeformation());
                            }
                        } else {
                            counterNotGood.inc();
                            quality.inc();
                        }
                    }
                }
            }
        }

        final StringBuilder sb = new StringBuilder();
        sb.append("--- Resulting deformations statistics --- TASK\n");
        sb.append("-- GOOD --");
        sb.append(counterGood.toString());
        sb.append("\n-- NOT GOOD --");
        sb.append(counterNotGood.toString());
        sb.append("\n-- QUALITY STATS --");
        sb.append(quality.toString());
        Logger.trace(sb.toString());
    }

}
