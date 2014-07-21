package cz.tul.dic.complextask;

import cz.tul.dic.ComputationException;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.engine.EngineUtils;
import cz.tul.dic.output.ExportMode;
import cz.tul.dic.output.ExportTask;
import cz.tul.dic.output.Exporter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Observable;

/**
 *
 * @author Petr Jecmen
 */
public class ComplexTaskSolver extends Observable {

    public void solveComplexTask(final TaskContainer tc) throws ComputationException, IOException {
        final int roundCount = TaskContainerUtils.getRounds(tc).size();
        tc.clearResultData();

        final int[] rounds = (int[]) tc.getParameter(TaskParameter.ROUND_LIMITS);
        int currentRound = 0;
        final int baseRound = rounds[0];

        setChanged();
        notifyObservers(new int[]{currentRound, roundCount});

        final CircleROIManager crm = CircleROIManager.prepareManager(tc, baseRound);
        final RectROIManager rrm = RectROIManager.prepareManager(tc, crm, baseRound);
        final TaskContainer tcR = rrm.getTc();

        int r, nextR;
        for (Entry<Integer, Integer> e : TaskContainerUtils.getRounds(tc).entrySet()) {
            r = e.getKey();
            nextR = e.getValue();

            computeRound(r, nextR, crm);
            computeRound(r, nextR, rrm);
            currentRound++;
            setChanged();
            notifyObservers(new int[]{currentRound, roundCount});
            exportRound(tcR, r);

            tc.setResults(r, tcR.getResults(r));
            tc.setDisplacement(r, tcR.getDisplacement(r));
            tc.setStrain(r, tcR.getStrain(r));
        }
    }

    private void computeRound(final int r, final int nextR, final ROIManager rm) throws ComputationException {
        EngineUtils.getInstance().computeRound(rm.getTc(), r, nextR);
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
