package cz.tul.dic.gui;

import cz.tul.dic.Computation;
import cz.tul.dic.ComputationException;
import cz.tul.dic.data.Config;
import cz.tul.dic.data.ConfigType;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.gui.lang.Lang;
import cz.tul.dic.input.InputLoader;
import cz.tul.dic.output.ExportTask;
import cz.tul.dic.output.OutputUtils;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.controlsfx.dialog.Dialogs;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Jecmen
 */
public class MainWindow implements Initializable {

    private static final File DEFAULT_DIR = new File("D:\\temp");
    private TaskContainer tc;
    private Set<ExportTask> exports;

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
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(DEFAULT_DIR);
        fileChooser.getExtensionFilters().clear();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("AVI files (*.avi)", "*.avi"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image files (*.bmp, *.jpg)", "*.bmp", "*.jpg"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Config files (*.config)", "*.config"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Task files (*.task)", "*.task"));
        List<File> fileList = fileChooser.showOpenMultipleDialog(null);
        if (fileList != null && !fileList.isEmpty()) {
            if (fileList.size() == 1) {
                final File in = fileList.get(0);
                final String name = in.getName();
                final String ext = name.substring(name.lastIndexOf(".") + 1);
                switch (ext) {
                    case "avi":
                        tc = new TaskContainer(in);
                        break;
                    case "config":
                        final ConfigType ct = Config.determineType(in);
                        switch (ct) {
                            case TASK:
                                tc = TaskContainerUtils.deserializeTaskContainer(Config.loadConfig(in.getAbsoluteFile(), in.getName(), ConfigType.TASK));
                                break;
                            case EXPORT:
                                exports = OutputUtils.deserializeExports(Config.loadConfig(in.getAbsoluteFile(), in.getName(), ConfigType.EXPORT));
                                break;
                            case SEQUENCE:
                                // find avi and load it
                                tc = new TaskContainer(Config.determineProjectFile(in));
                        }

                        break;
                    case "task":
                        try {
                            tc = TaskContainerUtils.readTaskFromFile(in);
                        } catch (ClassNotFoundException | IOException ex) {
                            // TODO show error during loading
                            Dialogs.create()
                                    .title(Lang.getString("error"))
                                    .message(Lang.getString("wrongBin"))
                                    .showWarning();
                        }
                        break;
                    default:
                        Dialogs.create()
                                .title(Lang.getString("error"))
                                .message(Lang.getString("wrongIn"))
                                .showWarning();
                }

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
    private void handleButtonActionRun(ActionEvent event) throws IOException, ComputationException {
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
        Parent root;
        try {
            root = FXMLLoader.load(getClass().getClassLoader().getResource("cz/tul/dic/gui/ROISelector.fxml"), Lang.getBundle());
            Stage stage = new Stage();
            stage.setTitle("Select ROIs");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (IOException e) {
            Logger.error("Error loading ROI dialog from JAR.\n{0}", e);
        }

//        if (tc != null) {
//            // TODO show dialog for ROI marking            
//        } else {
//            Dialogs.create()
//                    .title(Lang.getString("error"))
//                    .message(Lang.getString("noTC"))
//                    .showError();
//        }
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
//        buttonROI.setDisable(true);
        buttonRun.setDisable(true);

        textFs.setText("7");
        textFs.setDisable(true);
    }

}
