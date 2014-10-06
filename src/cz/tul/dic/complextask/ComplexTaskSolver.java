package cz.tul.dic.complextask;

import cz.tul.dic.ComputationException;
import cz.tul.dic.FpsManager;
import cz.tul.dic.Utils;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.task.DisplacementResult;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.debug.Stats;
import cz.tul.dic.engine.CorrelationResult;
import cz.tul.dic.engine.Engine;
import cz.tul.dic.engine.strain.StrainEstimation;
import cz.tul.dic.output.CsvWriter;
import cz.tul.dic.output.Direction;
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

    private final double LIMIT_COUNT_RATIO = 0.5;
    private static final int LIMIT_REPETITION = 10;
    private final List<Double> bottomShifts;

    public ComplexTaskSolver() {
        bottomShifts = new LinkedList<>();
    }

    public void solveComplexTask(final TaskContainer tc) throws ComputationException, IOException {
        TaskContainerUtils.checkTaskValidity(tc);
        tc.clearResultData();
        bottomShifts.clear();

        final int[] rounds = (int[]) tc.getParameter(TaskParameter.ROUND_LIMITS);
        int currentRound = 0;
        final int baseRound = rounds[0];

        setChanged();
        notifyObservers(currentRound);

        final CircleROIManager crm = CircleROIManager.prepareManager(tc, baseRound);
        final RectROIManager rrm = RectROIManager.prepareManager(tc, crm, baseRound);
        final TaskContainer tcR = rrm.getTc();

        int r, nextR, repeat;
        boolean good;
        for (Entry<Integer, Integer> e : TaskContainerUtils.getRounds(tc).entrySet()) {
            r = e.getKey();
            nextR = e.getValue();

            setChanged();
            notifyObservers(CircleROIManager.class);

            repeat = 0;
            good = true;
            do {
                if (!good) {
                    crm.increaseLimits(r);
                }
                Engine.getInstance().computeRound(crm.getTc(), r, nextR);
                good = checkResultsQuality(crm, r);
                repeat++;
            } while (!good && repeat < LIMIT_REPETITION);
            crm.generateNextRound(r, nextR);

            setChanged();
            notifyObservers(RectROIManager.class);
            if (crm.hasMoved()) {
                rrm.generateNextRound(r, nextR);
                Engine.getInstance().computeRound(rrm.getTc(), r, nextR);
            } else {
                Logger.info("Skipping round " + r + ", no shift detected.");
                final Image img = rrm.getTc().getImage(r);
                final double[][][] data = new double[img.getWidth()][img.getHeight()][];
                if (!rrm.getTc().getRois(r).isEmpty()) {
                    ROI roi = rrm.getTc().getRois(r).iterator().next();
                    for (int x = roi.getX1(); x <= roi.getX2(); x++) {
                        if (x < 0 || x >= data.length) {
                            continue;
                        }
                        for (int y = roi.getY1(); y <= roi.getY2(); y++) {
                            if (y < 0 || y >= data[x].length) {
                                continue;
                            }
                            data[x][y] = new double[2];
                        }
                    }
                }
                rrm.getTc().setDisplacement(r, nextR, new DisplacementResult(data, null));
            }

            tc.setResults(r, tcR.getResults(r));
            tc.setDisplacement(r, nextR, tcR.getDisplacement(r, nextR));

            exportRound(tc, r);
            bottomShifts.add(crm.getShiftBottom());

            currentRound++;
            setChanged();
            notifyObservers(currentRound);
        }

        Stats.dumpResultStatistics(tc);

        StrainEstimation strain = new StrainEstimation();
        strain.computeStrain(tc);
        Exporter.export(tc);
        TaskContainerUtils.serializeTaskToBinary(tc, new File(NameGenerator.generateBinary(tc)));

        final FpsManager fpsM = new FpsManager(tc);
        final String[][] out = new String[bottomShifts.size() + 1][2];
        out[0][0] = fpsM.buildTimeDescription();
        out[0][1] = "dY";
        for (int i = 0; i < bottomShifts.size(); i++) {
            out[i + 1][0] = Utils.format(fpsM.getTime(i + 1));
            out[i + 1][1] = Utils.format(bottomShifts.get(i));
        }
        CsvWriter.writeDataToCsv(new File(NameGenerator.generateCsvShifts(tc)), out);
    }

    private boolean checkResultsQuality(final CircleROIManager crm, int round) {
        int countNotGood = 0, count = 0;
        for (ROI roi : crm.getBottomRois()) {
            for (CorrelationResult cr : crm.getTc().getResult(round, roi)) {
                if (cr == null || cr.getValue() < CircleROIManager.LIMIT_RESULT_QUALITY) {
                    countNotGood++;
                }
                count++;
            }
        }
        final double ratio = countNotGood / (double) count;
        return ratio < LIMIT_COUNT_RATIO;
    }

    private void exportRound(final TaskContainer tc, final int round) throws IOException, ComputationException {
        Iterator<ExportTask> it = tc.getExports().iterator();
        ExportTask et;
        while (it.hasNext()) {
            et = it.next();
            if (et.getMode().equals(ExportMode.MAP) && et.getDataParams()[0] == round && !isStrainExport(et)) {
                Exporter.export(tc, et);
            }
        }
    }

    private boolean isStrainExport(ExportTask et) {
        final Direction dir = et.getDirection();
        return dir == Direction.Eabs || dir == Direction.Exy || dir == Direction.Exx || dir == Direction.Eyy;
    }

    public boolean isValidComplexTask(final TaskContainer tc) {
        boolean result = true;
        try {
            final int[] rounds = (int[]) tc.getParameter(TaskParameter.ROUND_LIMITS);
            final int baseRound = rounds[0];
            CircleROIManager.prepareManager(tc, baseRound);
        } catch (ComputationException ex) {
            result = false;
        }
        return result;
    }

    public List<Double> getBottomShifts() {
        return bottomShifts;
    }

}
