package cz.tul.dic.complextask;

import cz.tul.dic.ComputationException;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.engine.EngineUtils;
import cz.tul.dic.output.CsvWriter;
import cz.tul.dic.output.data.ExportMode;
import cz.tul.dic.output.ExportTask;
import cz.tul.dic.output.Exporter;
import cz.tul.dic.output.NameGenerator;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Observable;
import org.pmw.tinylog.Logger;

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
        
        final List<Double> shifts = new LinkedList<>();

        int r, nextR;
        for (Entry<Integer, Integer> e : TaskContainerUtils.getRounds(tc).entrySet()) {
            r = e.getKey();
            nextR = e.getValue();

            computeRound(r, nextR, crm);
            if (crm.hasMoved()) {
                computeRound(r, nextR, rrm);
            } else {
                Logger.info("Skipping round " + r + ", no shift detected.");
                final Image img = rrm.getTc().getImage(r);
                rrm.getTc().setDisplacement(r, new double[img.getWidth()][img.getHeight()][2]);
            }
            currentRound++;
            setChanged();
            notifyObservers(new int[]{currentRound, roundCount});
            exportRound(tcR, r);

            tc.setResults(r, tcR.getResults(r));
            tc.setDisplacement(r, tcR.getDisplacement(r));
            tc.setStrain(r, tcR.getStrain(r));
            
            shifts.add(crm.getShiftBottom());
        }
        
        TaskContainerUtils.serializeTaskToBinary(tc, new File(NameGenerator.generateBinary(tc)));
        
        final String[][] shiftsS = new String[1][shifts.size()];
        for (int i = 0; i < shifts.size(); i++) {
            shiftsS[0][i] = Double.toString(shifts.get(i));
        }
        CsvWriter.writeDataToCsv(new File(NameGenerator.generateCsvShifts(tc)), shiftsS);
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
