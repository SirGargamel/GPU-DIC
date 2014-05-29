/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.tul.dic.gui;

import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.output.ExportTask;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

/**
 * FXML Controller class
 *
 * @author Petr Jecmen
 */
public class ExportEditor implements Initializable {

    @FXML
    private VBox vBox;

    /**
     * Initializes the controller class.
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
        final TaskContainer tc = Context.getInstance().getTc();
        for (ExportTask et : tc.getExports()) {
            vBox.getChildren().add(new Text(et.toString()));
        }
    }

    @FXML
    private void handleButtonActionCancel(ActionEvent event) {
        closeWindow();
    }

    private void closeWindow() {
        final Stage stage = (Stage) vBox.getScene().getWindow();
        stage.close();
    }

}
