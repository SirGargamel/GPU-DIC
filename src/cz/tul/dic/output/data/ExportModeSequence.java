package cz.tul.dic.output.data;

import cz.tul.dic.ComputationException;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.output.Direction;
import java.util.ArrayList;
import java.util.List;

public class ExportModeSequence implements IExportMode<List<double[][]>> {

    @Override
    public List<double[][]> exportData(TaskContainer tc, Direction direction, int... dataParams) throws ComputationException {
        final List<double[][]> result = new ArrayList<>();
        final ExportModeMap mapExporter = new ExportModeMap();

        for (int r = 0; r < TaskContainerUtils.getMaxRoundCount(tc); r++) {
            result.add(mapExporter.exportData(tc, direction, new int[]{r}));
        }

        return result;
    }

}
