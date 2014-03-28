package cz.tul.dic.gui;

import cz.tul.dic.Computation;
import cz.tul.dic.ComputationException;
import cz.tul.dic.data.Config;
import cz.tul.dic.data.ConfigType;
import cz.tul.dic.data.roi.CircularROI;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerChecker;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.gui.lang.Lang;
import cz.tul.dic.input.InputLoader;
import cz.tul.dic.output.Direction;
import cz.tul.dic.output.ExportMode;
import cz.tul.dic.output.ExportTarget;
import cz.tul.dic.output.ExportTask;
import cz.tul.dic.output.OutputUtils;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
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
                                Context.getInstance().setTc(TaskContainerUtils.deserializeTaskContainerFromConfig(in));
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
    private void handleButtonActionExpert(ActionEvent event) throws ComputationException, IOException {
        TaskContainer tc = new TaskContainer(new File("D:\\temp\\7202845m.avi"));
        InputLoader.loadInput(tc);

        final int roiRadius = 26;
        tc.addRoi(new CircularROI(108, 12, roiRadius), 0);
        tc.addRoi(new CircularROI(201, 7, roiRadius), 0);
        tc.addRoi(new CircularROI(108, 86, roiRadius), 0);
        tc.addRoi(new CircularROI(202, 84, roiRadius), 0);

        for (ROI roi : tc.getRois(0)) {
            tc.setDeformationLimits(new double[]{-1, 1, 1.0, -5, 5, 0.25}, 0, roi);
        }

        TaskContainerUtils.setUniformFacetSize(tc, 0, roiRadius / 2);
        TaskContainerChecker.checkTaskValidity(tc);

        final String target = new File("D:\\temp\\results").getAbsolutePath().concat(File.separator).concat("dyn").concat(File.separator).concat(tc.getParameter(TaskParameter.KERNEL).toString()).concat("-");
        final String ext = String.format("%02d", 19).concat(".bmp");
        for (int round = 0; round < TaskContainerUtils.getRoundCount(tc); round++) {
            exports.add(new ExportTask(ExportMode.MAP, ExportTarget.FILE, Direction.X, new File(target.concat(String.format("%02d", round)).concat("-X-").concat(ext)), new int[]{round}));
            exports.add(new ExportTask(ExportMode.MAP, ExportTarget.FILE, Direction.Y, new File(target.concat(String.format("%02d", round)).concat("-Y-").concat(ext)), new int[]{round}));
            exports.add(new ExportTask(ExportMode.MAP, ExportTarget.FILE, Direction.ABS, new File(target.concat(String.format("%02d", round)).concat("-ABS-").concat(ext)), new int[]{round}));
        }
        exports.add(new ExportTask(ExportMode.SEQUENCE, ExportTarget.FILE, Direction.X, new File(target.concat("-X-").concat(ext).replace("bmp", "avi")), null));
        exports.add(new ExportTask(ExportMode.SEQUENCE, ExportTarget.FILE, Direction.Y, new File(target.concat("-Y-").concat(ext).replace("bmp", "avi")), null));

        TaskContainerUtils.serializeTaskContainerToConfig(tc);
        OutputUtils.serializeExports(exports, tc);
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        exports = new HashSet<>();

//        buttonExpert.setDisable(true);
        buttonROI.setDisable(true);
        buttonRun.setDisable(true);

        textFs.setText("7");
        textFs.setDisable(true);
    }

}
