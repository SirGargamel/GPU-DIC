package cz.tul.dic.complextask;

import cz.tul.dic.ComputationException;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.engine.Engine;
import cz.tul.dic.generators.facet.FacetGeneratorMode;
import cz.tul.dic.output.ExportMode;
import cz.tul.dic.output.ExportTask;
import cz.tul.dic.output.Exporter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Observable;

/**
 *
 * @author Petr Jecmen
 */
public class ComplextTaskSolver extends Observable {

    private final Engine engine;

    public ComplextTaskSolver() {
//        deformationLimitsCircle = DEFAULT_DEF_CIRCLE;
//        deformationLimitsRect = DEFAULT_DEF_RECT;

        engine = new Engine();
    }

    public TaskContainer solveComplexTask(final TaskContainer tc) throws ComputationException, IOException {
        final int roundCount = TaskContainerUtils.getRoundCount(tc);
        tc.clearResultData();

        final int[] rounds = (int[]) tc.getParameter(TaskParameter.ROUND_LIMITS);
        int currentRound = 0;
        final int baseRound = rounds[0];

        setChanged();
        notifyObservers(new int[]{currentRound, roundCount});

        final CircleROIManager crm = CircleROIManager.prepareManager(tc, baseRound);
        final RectROIManager rrm = RectROIManager.prepareManager(tc, crm, baseRound);

        final TaskContainer tcR = rrm.getTc();

        for (int round = 0; round < rounds.length; round += 2) {
            if (round > 0) {
                computeRound(rounds[round - 1], rounds[round], crm);
                computeRound(rounds[round - 1], rounds[round], rrm);
                currentRound++;
                setChanged();
                notifyObservers(new int[]{currentRound, roundCount});
                exportRound(tcR, rounds[round - 1]);
            }

            for (int r = rounds[round]; r < rounds[round + 1]; r++) {
                computeRound(r, r + 1, crm);
                computeRound(r, r + 1, rrm);
                currentRound++;
                setChanged();
                notifyObservers(new int[]{currentRound, roundCount});
                exportRound(tcR, r);
            }
        }

        return tcR;
    }

    private void computeRound(final int r, final int nextR, final ROIManager rm) throws ComputationException {
        engine.computeRound(rm.getTc(), r, nextR);
        rm.generateNextRound(r, nextR);
    }

    private void exportRound(final TaskContainer tc, final int round) throws IOException, ComputationException {
        Iterator<ExportTask> it = tc.getExports().iterator();
        ExportTask et;
        while (it.hasNext()) {
            et = it.next();
            if (et.getMode().equals(ExportMode.MAP) && et.getDataParams()[0] == round) {
                Exporter.export(tc, et);
                it.remove();
            }
        }
    }

    public boolean isValidComplexTask(final TaskContainer tc) {
        boolean result = true;
        try {
            final int[] rounds = (int[]) tc.getParameter(TaskParameter.ROUND_LIMITS);
            final int baseRound = rounds[0];
            final CircleROIManager crm = CircleROIManager.prepareManager(tc, baseRound);
        } catch (ComputationException ex) {
            result = false;
        }
        return result;
    }

}
