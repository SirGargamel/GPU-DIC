/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.output;

import cz.tul.dic.output.target.ExportTarget;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.output.target.AbstractExportTarget;
import cz.tul.dic.output.target.ExportTargetCsv;
import cz.tul.dic.output.target.ExportTargetFile;
import cz.tul.dic.output.target.ExportTargetGUI;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

/**
 *
 * @author Petr Jecmen
 */
public final class Exporter {

    private static final Map<ExportTarget, AbstractExportTarget> targetExporters;

    static {
        targetExporters = new EnumMap<>(ExportTarget.class);
        targetExporters.put(ExportTarget.FILE, new ExportTargetFile());
        targetExporters.put(ExportTarget.CSV, new ExportTargetCsv());
        targetExporters.put(ExportTarget.GUI, new ExportTargetGUI());
    }

    private Exporter() {
    }

    public static void export(final TaskContainer tc, final ExportTask et) throws IOException {
        final ExportTarget target = et.getTarget();
        if (targetExporters.containsKey(target)) {
            targetExporters.get(target).exportData(et, tc);
        } else {
            throw new IllegalArgumentException("Unsupported export target - " + et.toString());
        }
    }
}
