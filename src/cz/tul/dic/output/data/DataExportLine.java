package cz.tul.dic.output.data;

import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.output.Direction;
import cz.tul.dic.output.ExportUtils;

public class DataExportLine implements IDataExport<double[]> {

    @Override
    public double[] exportData(TaskContainer tc, Direction direction, int[] dataParams, ROI... rois) throws ComputationException {
        if (dataParams == null || dataParams.length < 3) {
            throw new IllegalArgumentException("Not enough input parameters (position, x, y required).");
        }

        final int roundCount = TaskContainerUtils.getRoundCount(tc);
        final double[] result = new double[roundCount];

        final int x = dataParams[0];
        final int y = dataParams[1];

        // check if position is inside ROI        
        double[][][] results;
        for (int r = 0; r < roundCount; r++) {
            if (ExportUtils.isPointInsideROIs(x, y, rois, tc, r)) {
                switch (direction) {
                    case Dx:
                    case Dy:
                    case Dabs:
                        results = tc.getDisplacement(r);
                        break;
                    case Exx:
                    case Eyy:
                    case Exy:
                    case Eabs:
                        results = tc.getStrain(r);
                    default:
                        throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Unsupported direction.");
                }
                
                if (results == null || results.length < x || results[0].length < y || results[x][y] == null) {
                    result[r] = 0;
                } else {
                    switch (direction) {
                        case Dx:
                        case Dy:
                        case Dabs:
                            result[r] = ExportUtils.calculateDisplacement(results[x][y], direction);
                            break;
                        case Exx:
                        case Eyy:
                        case Exy:
                        case Eabs:
                            result[r] = ExportUtils.calculateStrain(results[x][y], direction);
                        default:
                            throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Unsupported direction.");
                    }
                }
            }
        }

        return result;
    }
}
