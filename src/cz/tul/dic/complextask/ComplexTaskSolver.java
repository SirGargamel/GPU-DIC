/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.complextask;

import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.FpsManager;
import cz.tul.dic.Utils;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.roi.AbstractROI;
import cz.tul.dic.data.result.DisplacementResult;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.debug.Stats;
import cz.tul.dic.data.result.CorrelationResult;
import cz.tul.dic.engine.Engine;
import cz.tul.dic.data.result.Result;
import cz.tul.dic.engine.OverlapComputation;
import cz.tul.dic.engine.opencl.memory.AbstractOpenCLMemoryManager;
import cz.tul.dic.engine.strain.StrainEstimationMethod;
import cz.tul.dic.engine.strain.StrainEstimator;
import cz.tul.dic.output.CsvWriter;
import cz.tul.dic.output.NameGenerator;
import cz.tul.pj.journal.Journal;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 *
 * @author Petr Jecmen
 */
public class ComplexTaskSolver extends Observable implements Observer {

    private static final double LIMIT_COUNT_RATIO = 0.5;
    private static final int LIMIT_REPETITION = 10;
    private final List<Double> bottomShifts;
    private final TaskContainer task;
    private StrainEstimator strain;
    private boolean stop;

    public ComplexTaskSolver(final TaskContainer task) {
        this.task = task;
        bottomShifts = new LinkedList<>();
    }

    public void solveComplexTask() throws ComputationException {
        Journal.addDataEntry(task, "Computing complex task");
        Journal.createSubEntry();

        stop = false;
        Engine.getInstance().addObserver(this);

        TaskContainerUtils.checkTaskValidity(task);
        task.clearResultData();
        bottomShifts.clear();

        AbstractOpenCLMemoryManager.getInstance().assignTask(task);

        strain = StrainEstimator.initStrainEstimator((StrainEstimationMethod) task.getParameter(TaskParameter.STRAIN_ESTIMATION_METHOD));

        final int[] rounds = (int[]) task.getParameter(TaskParameter.ROUND_LIMITS);
        final int baseRound = rounds[0];

        setChanged();
        notifyObservers(baseRound);
        final CircleROIManager crm = CircleROIManager.prepareManager(task, baseRound);
        final RectROIManager rrm = RectROIManager.prepareManager(task, crm, baseRound);
        final TaskContainer tcR = rrm.getTc();
        final Set<Future<Void>> futures = new HashSet<>();

        int r, nextR, baseR = -1, repeat;
        boolean good;
        for (Entry<Integer, Integer> e : TaskContainerUtils.getRounds(task).entrySet()) {
            if (stop) {
                return;
            }

            r = e.getKey();
            nextR = e.getValue();

            setChanged();
            notifyObservers(r);

            setChanged();
            notifyObservers(CircleROIManager.class);

            repeat = 0;
            good = true;
            do {
                if (stop) {
                    Engine.getInstance().endTask();
                    return;
                }
                if (!good) {
                    crm.increaseLimits(r);
                }
                Engine.getInstance().computeRound(crm.getTc(), r, nextR);
                good = checkResultsQuality(crm, r);
                repeat++;
            } while (!good && repeat < LIMIT_REPETITION);
            crm.generateNextRound(r, nextR);

            if (stop) {
                Engine.getInstance().endTask();
                return;
            }
            setChanged();
            notifyObservers(RectROIManager.class);
            if (crm.hasMoved()) {
                rrm.generateNextRound(r, nextR);
                Engine.getInstance().computeRound(rrm.getTc(), r, nextR);
            } else {
                Journal.addEntry("Skipping round", "No shift detected in round {0}.", r);
                final Image img = rrm.getTc().getImage(r);
                final double[][][] data;
                if (!tcR.getRois(r).isEmpty()) {
                    AbstractROI roi = tcR.getRois(r).iterator().next();
                    data = generateZeroResults(img, roi);
                } else {
                    data = new double[img.getWidth()][img.getHeight()][];
                }
                tcR.setResult(r, nextR, new Result(new DisplacementResult(data, null)));
            }

            task.setResult(r, nextR, tcR.getResult(r, nextR));
            if (baseR == -1) {
                baseR = r;
            } else {
                futures.add(Engine.getInstance().getExecutorService().submit(new OverlapComputation(task, baseR, nextR, strain)));
            }

            bottomShifts.add(crm.getShiftBottom());
        }

        Stats.getInstance().dumpDeformationsStatisticsUsage();
        Stats.getInstance().dumpDeformationsStatisticsPerQuality();

        Engine.getInstance().endTask();

        if (stop) {
            return;
        }

        // wait for overlapping computations
        try {
            setChanged();
            notifyObservers(StrainEstimator.class);
            for (Future f : futures) {
                f.get();
            }
        } catch (InterruptedException | ExecutionException | NullPointerException ex) {
            Journal.addDataEntry(ex, "Exception occured while waiting for strain computation finish.");
        }

        try {
            TaskContainerUtils.serializeTaskToBinary(task, new File(NameGenerator.generateBinary(task)));
        } catch (IOException ex) {
            throw new ComputationException(ComputationExceptionCause.IO, ex);
        }

        final FpsManager fpsM = new FpsManager(task);
        final double pxToMm = 1 / (double) task.getParameter(TaskParameter.MM_TO_PX_RATIO);
        final String[][] out = new String[bottomShifts.size() + 1][2];
        out[0][0] = fpsM.buildTimeDescription();
        out[0][1] = "dY [mm]";
        for (int i = 0; i < bottomShifts.size(); i++) {
            out[i + 1][0] = Utils.format(fpsM.getTime(i + 1));
            out[i + 1][1] = Utils.format(bottomShifts.get(i) * pxToMm);
        }
        try {
            CsvWriter.writeDataToCsv(new File(NameGenerator.generateCsvShifts(task)), out);
        } catch (IOException ex) {
            throw new ComputationException(ComputationExceptionCause.IO, ex);
        }

        Engine.getInstance().deleteObserver(this);

        Journal.closeSubEntry();
    }

    private static boolean checkResultsQuality(final CircleROIManager crm, int round) {
        int countNotGood = 0, count = 0;
        for (AbstractROI roi : crm.getBottomRois()) {
            for (CorrelationResult cr : crm.getTc().getResult(round, round + 1).getCorrelations().get(roi)) {
                if (cr == null || cr.getQuality() < CircleROIManager.LIMIT_RESULT_QUALITY) {
                    countNotGood++;
                }
                count++;
            }
        }
        final double ratio = countNotGood / (double) count;
        return ratio < LIMIT_COUNT_RATIO;
    }

    private static double[][][] generateZeroResults(final Image img, final AbstractROI roi) {
        final double[][][] data = new double[img.getWidth()][img.getHeight()][];
        for (int x = (int) roi.getX1(); x <= roi.getX2(); x++) {
            if (x < 0 || x >= data.length) {
                continue;
            }
            for (int y = (int) roi.getY1(); y <= roi.getY2(); y++) {
                if (y < 0 || y >= data[x].length) {
                    continue;
                }
                data[x][y] = new double[2];
            }
        }
        return data;
    }

    public boolean isValidComplexTask() {
        try {
            final int[] rounds = (int[]) task.getParameter(TaskParameter.ROUND_LIMITS);
            final int baseRound = rounds[0];
            return CircleROIManager.prepareManager(task, baseRound) != null;
        } catch (ComputationException ex) {
            return false;
        }
    }

    public List<Double> getBottomShifts() {
        return bottomShifts;
    }

    public void stop() {
        stop = true;
        Engine.getInstance().stop();
        strain.stop();
    }

    @Override
    public void update(final Observable o, final Object arg) {
        if (o instanceof Engine && arg instanceof Double) {
            setChanged();
            notifyObservers(arg);
        }
    }

}
