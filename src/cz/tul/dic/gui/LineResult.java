/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.gui;

import cz.tul.dic.ComputationException;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.output.ExportTask;
import cz.tul.dic.output.Exporter;
import cz.tul.dic.output.NameGenerator;
import cz.tul.dic.output.target.ExportTarget;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;

/**
 * FXML Controller class
 *
 * @author Petr Jecmen
 */
public class LineResult implements Initializable {

    @FXML
    private LineChart<Number, Number> chart;    

    /**
     * Initializes the controller class.
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(final URL url, final ResourceBundle rb) {
        // no init required
    }

    public void handleButtonActionSave(final ActionEvent event) throws IOException, ComputationException {
        final Object[] data = (Object[]) chart.getUserData();
        if (data.length == 3) {
            final TaskContainer tc = (TaskContainer) data[0];
            final int x = (int) data[1];
            final int y = (int) data[2];

            Exporter.export(tc, ExportTask.generatePointExport(ExportTarget.CSV, new File(NameGenerator.generateCsvPoint(tc, x, y)), x, y));
        } else {
            final TaskContainer tc = (TaskContainer) data[0];
            final int x1 = (int) data[1];
            final int y1 = (int) data[2];
            final int x2 = (int) data[3];
            final int y2 = (int) data[4];
            
            Exporter.export(tc, ExportTask.generateDoublePointExport(ExportTarget.CSV, new File(NameGenerator.generateCsvDoublePoint(tc, x1, y1, x2, y2)), x1, y1, x2, y2));
        }
    }
}
