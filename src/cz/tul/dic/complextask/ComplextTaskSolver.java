package cz.tul.dic.complextask;

import cz.tul.dic.ComputationException;
import cz.tul.dic.data.roi.CircularROI;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.engine.Engine;
import cz.tul.dic.output.ExportMode;
import cz.tul.dic.output.ExportTask;
import cz.tul.dic.output.Exporter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Observable;
import java.util.Set;
import org.pmw.tinylog.Logger;

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

    public void solveComplexTask(final TaskContainer tc) throws ComputationException, IOException {
        final int roundCount = TaskContainerUtils.getRoundCount(tc);
        tc.clearResultData();

        final int[] rounds = (int[]) tc.getParameter(TaskParameter.ROUND_LIMITS);
        int currentRound = 0;
        final int baseRound = rounds[0];

        setChanged();
        notifyObservers(new int[]{currentRound, roundCount});

        final RoiManager rm = new RoiManager(tc, baseRound);

//        final Set<ROI> rois = rm.getROIs();
//        tc.setROIs(baseRound, rois);
//        for (ROI roi : rois) {
//            if (roi instanceof CircularROI) {
//                tc.addFacetSize(baseRound, roi, (int) (((CircularROI) roi).getRadius() / ROI_CIRCLE_FS_DENOM));
//                tc.setDeformationLimits(baseRound, roi, rm.getDefLimitsCircle());
//            } else {
//                tc.setDeformationLimits(baseRound, roi, rm.getDefLimitsRect());
//            }
//        }
//        // compute first round     
//        engine.computeRound(tc, baseRound, baseRound + 1);
//
//        currentRound++;
//        setChanged();
//        notifyObservers(new int[]{currentRound, roundCount});        
//        exportRound(tc, baseRound);
        int prevR = baseRound;
        for (int round = 0; round < rounds.length; round += 2) {
            if (round > 0) {
                computeRound(tc, rounds[round - 1], rounds[round], prevR, rm);
                currentRound++;
                setChanged();
                notifyObservers(new int[]{currentRound, roundCount});
                exportRound(tc, rounds[round - 1]);
            }

            for (int r = rounds[round]; r < rounds[round + 1]; r++) {
                computeRound(tc, r, r + 1, prevR, rm);
                currentRound++;
                setChanged();
                notifyObservers(new int[]{currentRound, roundCount});
                prevR = r;
                exportRound(tc, r);
            }
        }
    }

    private void computeRound(final TaskContainer tc, final int r, final int nextR, final int prevR, final RoiManager rm) throws ComputationException {
        engine.computeRound(tc, r, nextR);

        if (rm.areLimitsReached(tc, r)) {
            Logger.debug("Computing round nr." + (r + 1) + " again.");
            engine.computeRound(tc, r, nextR);
        }

        rm.generateNextRound(tc, prevR, r);
    }

    private void exportRound(final TaskContainer tc, final int round) throws IOException, ComputationException {
        Iterator<ExportTask> it = tc.getExports().iterator();
        ExportTask et;
        while (it.hasNext()) {
            et = it.next();
            if (et.getMode().equals(ExportMode.MAP) && et.getDataParams()[0] == round) {
                Exporter.export(et, tc);
                it.remove();
            }
        }
    }

}
