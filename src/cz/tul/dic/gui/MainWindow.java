/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.tul.dic.gui;

import cz.tul.dic.Computation;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.gui.lang.Lang;
import cz.tul.dic.input.InputLoader;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import org.controlsfx.dialog.Dialog;
import org.controlsfx.dialog.Dialogs;

/**
 *
 * @author Petr Jecmen
 */
public class MainWindow implements Initializable {

    private static final File DEFAULT_DIR = new File("D:\\temp");
    private TaskContainer tc;

    @FXML
    private TextField textFs;
    @FXML
    private Button buttonRun;
    @FXML
    private Button buttonROI;
    @FXML
    private Button buttonExpert;

    @FXML
    private void handleButtonActionInput(ActionEvent event) throws IOException {
        Dialogs.create()
                        .title(Lang.getString("error"))
                        .message(Lang.getString("IO", "TEST"))
                        .showError();
        
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(DEFAULT_DIR);
        fileChooser.getExtensionFilters().clear();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("AVI files (*.avi)", "*.avi"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image files (*.bmp, *.jpg)", "*.bmp", "*.jpg"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Config files (*.config)", "*.config"));
        List<File> fileList = fileChooser.showOpenMultipleDialog(null);
        if (fileList != null && !fileList.isEmpty()) {
            if (fileList.size() == 1) {
                tc = new TaskContainer(fileList.get(0));
            } else {
                tc = new TaskContainer(fileList);
            }
            try {
                InputLoader.loadInput(tc);
                buttonExpert.setDisable(false);
                buttonROI.setDisable(false);
                buttonRun.setDisable(false);
                textFs.setDisable(false);
            } catch (IOException ex) {
                Dialogs.create()
                        .title(Lang.getString("error"))
                        .message(Lang.getString("IO", ex.getLocalizedMessage()))
                        .showWarning();
            }
        }
    }

    @FXML
    private void handleButtonActionRun(ActionEvent event) throws IOException {
        final String fsText = textFs.getText();
        try {
            final int fs = Integer.valueOf(fsText);
            if (tc != null) {
                TaskContainerUtils.setUniformFacetSize(tc, 0, fs);
                Computation.commenceComputationDynamic(tc);
            } else {
                Dialogs.create()
                        .title(Lang.getString("error"))
                        .message(Lang.getString("noTC"))
                        .showError();
            }
        } catch (NumberFormatException ex) {
            Dialogs.create()
                    .title(Lang.getString("error"))
                    .message(Lang.getString("wrongFS"))
                    .showError();
        }
    }

    @FXML
    private void handleButtonActionROI(ActionEvent event) {
        if (tc != null) {
            // TODO show dialog for ROI marking
        } else {
            Dialogs.create()
                    .title(Lang.getString("error"))
                    .message(Lang.getString("noTC"))
                    .showError();
        }
    }

    @FXML
    private void handleButtonActionExpert(ActionEvent event) {
        if (tc != null) {
            // TODO show dialog for expert settings
        } else {
            Dialogs.create()
                    .title(Lang.getString("error"))
                    .message(Lang.getString("noTC"))
                    .showError();
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        buttonExpert.setDisable(true);
        buttonROI.setDisable(true);
        buttonRun.setDisable(true);

        textFs.setText("7");
        textFs.setDisable(true);
    }

}
