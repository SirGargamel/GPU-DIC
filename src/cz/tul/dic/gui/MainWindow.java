package cz.tul.dic.gui;

import cz.tul.dic.ComputationException;
import cz.tul.dic.complextask.ComplextTaskSolver;
import cz.tul.dic.data.Config;
import cz.tul.dic.data.ConfigType;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.gui.lang.Lang;
import cz.tul.dic.input.InputLoader;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import javafx.application.Platform;
import javafx.concurrent.Task;
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

    @FXML
    private TextField textFs;
    @FXML
    private Button buttonRun;
    @FXML
    private Button buttonROI;
    @FXML
    private Button buttonExpert;

    @FXML
    private void handleButtonActionInput(ActionEvent event) throws IOException, InterruptedException, ExecutionException {
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(DEFAULT_DIR);
        fileChooser.getExtensionFilters().clear();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("AVI files (*.avi)", "*.avi"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image files (*.bmp, *.jpg)", "*.bmp", "*.jpg"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Config files (*.config)", "*..TASK.config", "*..EXPORT.config"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Task files (*.task)", "*.task"));
        List<File> fileList = fileChooser.showOpenMultipleDialog(null);
        if (fileList != null && !fileList.isEmpty()) {
            Task<Object> worker = new Task<Object>() {
                @Override
                protected Object call() throws Exception {
                    updateProgress(0, 5);
                    if (fileList.size() == 1) {
                        final File in = fileList.get(0);
                        final String name = in.getName();
                        final String ext = name.substring(name.lastIndexOf(".") + 1);
                        switch (ext) {
                            case "avi":
                                updateProgress(1, 5);
                                Context.getInstance().setTc(new TaskContainer(in));
                                break;
                            case "config":
                                updateProgress(1, 5);
                                final ConfigType ct = Config.determineType(in);
                                switch (ct) {
                                    case TASK:
                                        Context.getInstance().setTc(TaskContainerUtils.deserializeTaskFromConfig(in));
                                        break;
                                    case SEQUENCE:
                                        // find avi and load it                                                                 
                                        Context.getInstance().setTc(new TaskContainer(Config.determineProjectFile(in)));
                                }
                                break;
                            case "task":
                                updateProgress(1, 5);
                                try {
                                    Context.getInstance().setTc(TaskContainerUtils.deserializeTaskFromBinary(in));
                                } catch (ClassNotFoundException | IOException ex) {
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
                        updateProgress(2, 5);
                    } else {
                        updateProgress(1, 5);
                        Context.getInstance().setTc(new TaskContainer(fileList));
                        updateProgress(2, 5);
                    }
                    try {
                        InputLoader.loadInput(Context.getInstance().getTc());
                        updateProgress(4, 5);
                        Platform.runLater(new Runnable() {

                            @Override
                            public void run() {
                                buttonRun.setDisable(false);
                                buttonExpert.setDisable(false);
                                textFs.setDisable(false);
                                buttonROI.setDisable(false);
                            }
                        });
                    } catch (IOException ex) {
                        Dialogs.create()
                                .title(Lang.getString("error"))
                                .message(Lang.getString("IO", ex.getLocalizedMessage()))
                                .showWarning();
                    }
                    updateProgress(5, 5);
                    return null;
                }
            };

            Dialogs.create()
                    .title(Lang.getString("Wait"))
                    .message(Lang.getString("LoadingData"))
                    .masthead(null)
                    .showWorkerProgress(worker);

            Thread th = new Thread(worker);
            th.setDaemon(true);
            th.start();
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
                ComplextTaskSolver cts = new ComplextTaskSolver();
                final Task<Object> worker = new ComputationObserver(cts, tc);
                Dialogs.create()
                        .title(Lang.getString("Wait"))
                        .message(Lang.getString("Computing"))
                        .masthead(null)
                        .showWorkerProgress(worker);

                Thread th = new Thread(worker);
                th.setDaemon(true);
                th.start();
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
        Task<Object> worker = new Task<Object>() {
            @Override
            protected Object call() throws Exception {
                for (int i = 0; i < 100; i++) {
                    updateProgress(i, 99);
                    updateMessage("progress: " + i);
                    System.out.println("progress: " + i);
                    Thread.sleep(100);
                }
                return null;
            }
        };

        Dialogs.create()
                .title("Progress")
                .message("Now Loading...")
                .showWorkerProgress(worker);

        Thread th = new Thread(worker);
        th.setDaemon(true);
        th.start();

//        TaskContainer tc = new TaskContainer(new File("D:\\temp\\7202845m.avi"));
//        InputLoader.loadInput(tc);
//
//        final int roiRadius = 26;
//        tc.addRoi(new CircularROI(108, 12, roiRadius), 0);
//        tc.addRoi(new CircularROI(201, 7, roiRadius), 0);
//        tc.addRoi(new CircularROI(108, 86, roiRadius), 0);
//        tc.addRoi(new CircularROI(202, 84, roiRadius), 0);
//
//        for (ROI roi : tc.getRois(0)) {
//            tc.setDeformationLimits(new double[]{-1, 1, 1.0, -5, 5, 0.25}, 0, roi);
//        }
//
//        TaskContainerUtils.setUniformFacetSize(tc, 0, roiRadius / 2);
//        TaskContainerChecker.checkTaskValidity(tc);
//
//        final String target = new File("D:\\temp\\results").getAbsolutePath().concat(File.separator).concat("dyn").concat(File.separator).concat(tc.getParameter(TaskParameter.KERNEL).toString()).concat("-");
//        final String ext = String.format("%02d", 19).concat(".bmp");
//        for (int round = 0; round < TaskContainerUtils.getRoundCount(tc); round++) {
//            exports.add(new ExportTask(ExportMode.MAP, ExportTarget.FILE, Direction.X, new File(target.concat(String.format("%02d", round)).concat("-X-").concat(ext)), new int[]{round}));
//            exports.add(new ExportTask(ExportMode.MAP, ExportTarget.FILE, Direction.Y, new File(target.concat(String.format("%02d", round)).concat("-Y-").concat(ext)), new int[]{round}));
//            exports.add(new ExportTask(ExportMode.MAP, ExportTarget.FILE, Direction.ABS, new File(target.concat(String.format("%02d", round)).concat("-ABS-").concat(ext)), new int[]{round}));
//        }
//        exports.add(new ExportTask(ExportMode.SEQUENCE, ExportTarget.FILE, Direction.X, new File(target.concat("-X-").concat(ext).replace("bmp", "avi")), null));
//        exports.add(new ExportTask(ExportMode.SEQUENCE, ExportTarget.FILE, Direction.Y, new File(target.concat("-Y-").concat(ext).replace("bmp", "avi")), null));
//
//        TaskContainerUtils.serializeTaskToConfig(tc);
//        OutputUtils.serializeExports(exports, tc);
//
//        // compute dynamic task
//        Computation.computeDynamicTask(tc);
//
//        // serialize task container to binary file
//        TaskContainerUtils.serializeTaskToBinary(new File("D:\\temp\\task.task"), tc);
    }

    @FXML
    private void handleButtonActionResults(ActionEvent event) {
        try {
            final Parent root = FXMLLoader.load(getClass().getClassLoader().getResource("cz/tul/dic/gui/ResultPresenter.fxml"), Lang.getBundle());
            final Stage stage = new Stage();
            stage.setTitle(Lang.getString("Results"));
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (IOException e) {
            Logger.error("Error loading Results dialog from JAR.\n{0}", e);
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        buttonExpert.setDisable(false);
        buttonROI.setDisable(true);
        buttonRun.setDisable(true);

        textFs.setText("7");
        textFs.setDisable(true);
    }

    private static class ComputationObserver extends Task implements Observer {

        private final ComplextTaskSolver cts;
        private final TaskContainer tc;

        public ComputationObserver(ComplextTaskSolver cts, final TaskContainer tc) {
            this.cts = cts;
            this.tc = tc;
        }

        @Override
        protected Object call() throws Exception {
            cts.addObserver(this);
            cts.solveComplexTask(tc);
            return null;
        }

        @Override
        public void update(Observable o, Object arg) {
            if (arg instanceof int[]) {
                final int[] data = (int[]) arg;
                updateProgress(data[0], data[1]);
            }
        }
    }
}
