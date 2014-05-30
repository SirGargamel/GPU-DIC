package cz.tul.dic.output.data;

import cz.tul.dic.ComputationException;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.output.Direction;
import java.util.ArrayList;
import java.util.List;

public class DataExportSequence implements IDataExport<List<double[][]>> {        

    @Override
    public List<double[][]> exportData(TaskContainer tc, Direction direction, int[] dataParams, ROI[] rois) throws ComputationException {
        final List<double[][]> result = new ArrayList<>();
        final DataExportMap mapExporter = new DataExportMap();

        for (int r = 0; r < TaskContainerUtils.getMaxRoundCount(tc); r++) {
            result.add(mapExporter.exportData(tc, direction, new int[]{r}, rois));
        }

        return result;
    }

}
