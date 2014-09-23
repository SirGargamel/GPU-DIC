/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.chart.LineChart;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/**
 * FXML Controller class
 *
 * @author Petr Jecmen
 */
public class LineResult implements Initializable {

    @FXML
    private LineChart<Number, Number> chart;
    @FXML
    private Button buttonSave;

    /**
     * Initializes the controller class.
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
    }

    public void init() {
        final Object[] data = (Object[]) chart.getUserData();
        if (data.length != 3) {
            buttonSave.setVisible(false);
        }
    }

    public void handleButtonActionSave(ActionEvent event) throws IOException, ComputationException {
        final Object[] data = (Object[]) chart.getUserData();
        if (data.length == 3) {
            final TaskContainer tc = (TaskContainer) data[0];
            final int x = (int) data[1];
            final int y = (int) data[2];

            Exporter.export(tc, ExportTask.generatePointExport(ExportTarget.CSV, new File(NameGenerator.generateCsvPoint(tc, x, y)), x, y));
        }
    }
}
