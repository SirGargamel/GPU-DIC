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
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Config files (*.config)", "*..TASK.config", "*..EXPORT.config"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Task files (*.task)", "*.task"));
        List<File> fileList = fileChooser.showOpenMultipleDialog(null);
        if (fileList != null && !fileList.isEmpty()) {
            if (fileList.size() == 1) {
                final File in = fileList.get(0);
                final String name = in.getName();
                final String ext = name.substring(name.lastIndexOf(".") + 1);
                switch (ext) {
                    case "avi":
                        Context.getInstance().setTc(new TaskContainer(in));
                        break;
                    case "config":
                        final ConfigType ct = Config.determineType(in);
                        switch (ct) {
                            case TASK:
                                Context.getInstance().setTc(TaskContainerUtils.deserializeTaskContainer(in));
                                break;
                            case EXPORT:
                                exports = OutputUtils.deserializeExports(in);
                                break;
                            case SEQUENCE:
                                // find avi and load it                                
                                Context.getInstance().setTc(new TaskContainer(Config.determineProjectFile(in)));
                        }

                        break;
                    case "task":
                        try {
                            Context.getInstance().setTc(TaskContainerUtils.readTaskFromFile(in));
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
                Context.getInstance().setTc(new TaskContainer(fileList));
            }
            try {
                InputLoader.loadInput(Context.getInstance().getTc());
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
            final TaskContainer tc = Context.getInstance().getTc();
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
        try {
            final Parent root = FXMLLoader.load(getClass().getClassLoader().getResource("cz/tul/dic/gui/ROISelector.fxml"), Lang.getBundle());
            final Stage stage = new Stage();
            stage.setTitle("Select ROIs");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (IOException e) {
            Logger.error("Error loading ROI dialog from JAR.\n{0}", e);
        }
    }

    @FXML
    private void handleButtonActionExpert(ActionEvent event) {

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
