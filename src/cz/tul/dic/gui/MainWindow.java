package cz.tul.dic.gui;

import cz.tul.dic.Computation;
import cz.tul.dic.ComputationException;
import cz.tul.dic.complextask.ComplextTaskSolver;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerChecker;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.engine.Engine;
import cz.tul.dic.generators.facet.FacetGeneratorMode;
import cz.tul.dic.gui.lang.Lang;
import cz.tul.dic.input.InputLoader;
import cz.tul.dic.output.Direction;
import cz.tul.dic.output.ExportTarget;
import cz.tul.dic.output.ExportTask;
import cz.tul.dic.output.Exporter;
import cz.tul.dic.output.target.TargetExportFile;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.prefs.Preferences;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.Dialogs;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Jecmen
 */
public class MainWindow implements Initializable {

    private static final boolean TEST_CASE = true;
    private static final String LAST_DIR = "lastDir";

    @FXML
    private TextField textFs;
    @FXML
    private Button buttonRun;
    @FXML
    private Button buttonROI;
    @FXML
    private Button buttonExpert;
    @FXML
    private Button buttonExport;
    @FXML
    private Button buttonPrev;
    @FXML
    private Button buttonPlay;
    @FXML
    private Button buttonPause;
    @FXML
    private Button buttonNext;
    @FXML
    private Button buttonResults;
    @FXML
    private Button buttonSave;
    @FXML
    private InputPresenter imagePane;
    private Timeline timeLine;

    @FXML
    private void handleButtonActionInput(ActionEvent event) throws IOException, InterruptedException, ExecutionException {
        final FileChooser fc = new FileChooser();
        final String lastDir = Preferences.userRoot().get(LAST_DIR, null);
        if (lastDir != null) {
            File last = new File(lastDir);
            if (!last.isDirectory()) {
                last = last.getParentFile();
            }
            fc.setInitialDirectory(last);
        }
        fc.getExtensionFilters().clear();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("AVI files (*.avi)", "*.avi"));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image files (*.bmp, *.jpg)", "*.bmp", "*.jpg"));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Config files (*.config)", "*.config"));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Task files (*.task)", "*.task"));
        List<File> fileList = fc.showOpenMultipleDialog(null);
        if (fileList != null && !fileList.isEmpty()) {
            loadInput(fileList);
            Preferences.userRoot().put(LAST_DIR, fileList.get(0).getAbsolutePath());
        }
    }

    private void loadInput(List<File> fileList) {
        Task<String> worker = new Task<String>() {
            @Override
            protected String call() throws Exception {
                String result = null;
                boolean error = false;

                updateProgress(0, 5);
                if (fileList.size() == 1) {
                    final File in = fileList.get(0);
                    final String name = in.getName();
                    final String ext = name.substring(name.lastIndexOf(".") + 1);
                    updateProgress(1, 5);
                    switch (ext) {
                        case "avi":
                            Context.getInstance().setTc(new TaskContainer(in));
                            break;
                        case "config":
                            Context.getInstance().setTc(TaskContainerUtils.deserializeTaskFromConfig(in));
                            break;
                        case "task":
                            try {
                                Context.getInstance().setTc(TaskContainerUtils.deserializeTaskFromBinary(in));
                                Platform.runLater(() -> {
                                    adjustResultButtons(false);
                                });
                            } catch (ClassNotFoundException | IOException ex) {
                                error = true;
                                result = Lang.getString("wrongBin");
                            }
                            break;
                        default:
                            error = true;
                            result = Lang.getString("wrongIn");
                    }
                } else {
                    updateProgress(1, 5);
                    Context.getInstance().setTc(new TaskContainer(fileList));
                }
                updateProgress(2, 5);
                if (!error) {
                    try {
                        updateProgress(3, 5);
                        final TaskContainer tc = Context.getInstance().getTc();
                        InputLoader.loadInput(tc);
                        updateProgress(4, 5);
                        Platform.runLater(() -> {
                            if (imagePane != null && imagePane.getScene() != null) {
                                adjustConfigButtons(false);
                                adjustImageButtons(false);
                                imagePane.displayImage();

                                imagePane.getScene().getWindow().setWidth(tc.getImage(0).getWidth() + 143);
                                imagePane.getScene().getWindow().setHeight(tc.getImage(0).getHeight() + 114);

                                final Object o = tc.getParameter(TaskParameter.FACET_SIZE);
                                if (o != null) {
                                    textFs.setText(o.toString());
                                }
                            }
                        });
                    } catch (IOException ex) {
                        result = Lang.getString("IO", ex.getLocalizedMessage());
                    }
                }
                updateProgress(5, 5);

                return result;
            }
        };

        Dialogs.create()
                .title(Lang.getString("Wait"))
                .message(Lang.getString("LoadingData"))
                .showWorkerProgress(worker);

        Thread th = new Thread(worker);
        th.setDaemon(true);
        th.start();

        th = new Thread(() -> {
            Platform.runLater(() -> {
                try {
                    final String err = worker.get();
                    if (err != null) {
                        Dialogs.create()
                                .title(Lang.getString("error"))
                                .masthead(null)
                                .message(err)
                                .showWarning();

                    }
                } catch (InterruptedException | ExecutionException ex) {
                    Dialogs.create()
                            .title(Lang.getString("error"))
                            .masthead(null)
                            .message(ex.getLocalizedMessage())
                            .showException(ex);
                }
            });
        });
        th.setDaemon(true);
        th.start();
    }

    @FXML
    private void handleButtonActionSave(ActionEvent event) throws IOException {
        final String c1 = Lang.getString("TypeConfig");
        final String t1 = Lang.getString("TypeConfigD");
        final String c2 = Lang.getString("TypeBinary");
        final String t2 = Lang.getString("TypeBinaryD");
        final Action a = Dialogs.create()
                .masthead(null)
                .title(Lang.getString("Save"))
                .message(Lang.getString("ChooseDataType"))
                .showCommandLinks(null, new Dialogs.CommandLink(c1, t1), new Dialogs.CommandLink(c2, t2));
        final String val = a.textProperty().get();
        if (!val.equals("@@dlg.cancel.button")) {
            // pick target file        
            final FileChooser fc = new FileChooser();
            final File in = (File) Context.getInstance().getTc().getParameter(TaskParameter.IN);
            fc.setInitialDirectory(in.getParentFile());
            fc.getExtensionFilters().clear();
            if (val.equals(c1)) {
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Config files (*".concat(TaskContainerUtils.EXT_CONFIG).concat(")"), "*".concat(TaskContainerUtils.EXT_CONFIG)));
                fc.setInitialFileName(in.getName().concat(TaskContainerUtils.EXT_CONFIG));
            } else if (val.equals(c2)) {
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Task files (*".concat(TaskContainerUtils.EXT_BINARY).concat(")"), "*".concat(TaskContainerUtils.EXT_BINARY)));
                fc.setInitialFileName(in.getName().concat(TaskContainerUtils.EXT_BINARY));
            }

            File target = fc.showSaveDialog(null);
            if (target != null) {
                if (val.equals(c1)) {
                    if (!target.getName().endsWith(TaskContainerUtils.EXT_CONFIG)) {
                        target = new File(target.getAbsolutePath().concat(TaskContainerUtils.EXT_CONFIG));
                    }
                    TaskContainerUtils.serializeTaskToConfig(Context.getInstance().getTc(), target);
                } else if (val.equals(c2)) {
                    if (!target.getName().endsWith(TaskContainerUtils.EXT_BINARY)) {
                        target = new File(target.getAbsolutePath().concat(TaskContainerUtils.EXT_BINARY));
                    }
                    TaskContainerUtils.serializeTaskToBinary(Context.getInstance().getTc(), target);
                }
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
                tc.setParameter(TaskParameter.FACET_SIZE, fs);
                TaskContainerChecker.checkTaskValidity(tc);
                ComplextTaskSolver cts = new ComplextTaskSolver();
                final Task<Exception> worker = new ComputationObserver(cts, tc);
                Dialogs.create()
                        .title(Lang.getString("Wait"))
                        .message(Lang.getString("Computing"))
                        .masthead(null)
                        .showWorkerProgress(worker);

                Thread th = new Thread(worker);
                th.setDaemon(true);
                th.start();

                th = new Thread(() -> {
                    try {
                        final Exception err = worker.get();
                        if (err != null) {
                            Dialogs.create()
                                    .title(Lang.getString("Exception"))
                                    .message(err.getLocalizedMessage())
                                    .showWarning();
                            Logger.error(err);

                        } else {
                            Platform.runLater(() -> {
                                buttonResults.setDisable(false);
                            });
                        }
                    } catch (InterruptedException | ExecutionException ex) {
                        System.out.println(ex);
                    }
                });
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
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(imagePane.getScene().getWindow());
            stage.showAndWait();
        } catch (IOException e) {
            Logger.error("Error loading ROI dialog from JAR.\n{0}", e);
        }
    }

    @FXML
    private void handleButtonActionExport(ActionEvent event) {
        try {
            final Parent root = FXMLLoader.load(getClass().getClassLoader().getResource("cz/tul/dic/gui/ExportEditor.fxml"), Lang.getBundle());
            final Stage stage = new Stage();
            stage.setTitle(Lang.getString("Export"));
            stage.setScene(new Scene(root));
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(imagePane.getScene().getWindow());
            stage.showAndWait();
        } catch (IOException e) {
            Logger.error("Error loading Export dialog from JAR.\n{0}", e);
        }
    }

    @FXML
    private void handleButtonActionExpert(ActionEvent event) throws ComputationException, IOException {
        try {
            final Parent root = FXMLLoader.load(getClass().getClassLoader().getResource("cz/tul/dic/gui/ExpertSettings.fxml"), Lang.getBundle());
            final Stage stage = new Stage();
            stage.setTitle(Lang.getString("Expert"));
            stage.setScene(new Scene(root));
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(imagePane.getScene().getWindow());
            stage.showAndWait();
        } catch (IOException e) {
            Logger.error("Error loading Expert settings dialog from JAR.\n{0}", e);
        }
    }

    @FXML
    private void handleButtonActionResults(ActionEvent event) {
        try {
            final Parent root = FXMLLoader.load(getClass().getClassLoader().getResource("cz/tul/dic/gui/ResultPresenter.fxml"), Lang.getBundle());
            final Stage stage = new Stage();
            stage.setTitle(Lang.getString("Results"));
            stage.setScene(new Scene(root));
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(imagePane.getScene().getWindow());
            stage.showAndWait();
        } catch (IOException e) {
            Logger.error("Error loading Results dialog from JAR.\n{0}", e);
        }
    }

    @FXML
    private void handleButtonActionNext(ActionEvent event) {
        stopVideo();
        imagePane.nextImage();
        event.consume();
    }

    private void stopVideo() {
        if (timeLine != null) {
            timeLine.stop();
        }
    }

    @FXML
    private void handleButtonActionPrev(ActionEvent event) {
        stopVideo();
        imagePane.previousImage();
    }

    @FXML
    private void handleButtonActionPlay(ActionEvent event) {
        stopVideo();
        timeLine = new Timeline(new KeyFrame(Duration.millis(250), (ActionEvent event1) -> {
            if (imagePane.nextImage()) {
                stopVideo();
            }
        }));
        timeLine.setCycleCount(Timeline.INDEFINITE);
        timeLine.play();

        event.consume();
    }

    @FXML
    private void handleButtonActionStop(ActionEvent event) {
        stopVideo();

        event.consume();
    }

    @FXML
    private void handleTextKeyTyped(KeyEvent keyEvent) {
        if (!"0123456789".contains(keyEvent.getCharacter())) {
            keyEvent.consume();
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        textFs.setText("7");

        imagePane.initialize(url, rb);

        Image img = new Image(getClass().getClassLoader().getResourceAsStream("cz/tul/dic/gui/resources/play_24x32.png"));
        ImageView image = new ImageView(img);
        image.setFitWidth(20);
        image.setFitHeight(20);
        image.setPreserveRatio(true);
        buttonPlay.setGraphic(image);

        img = new Image(getClass().getClassLoader().getResourceAsStream("cz/tul/dic/gui/resources/pause_24x32.png"));
        image = new ImageView(img);
        image.setFitWidth(20);
        image.setFitHeight(20);
        image.setPreserveRatio(true);
        buttonPause.setGraphic(image);

        img = new Image(getClass().getClassLoader().getResourceAsStream("cz/tul/dic/gui/resources/arrow_left_32x32.png"));
        image = new ImageView(img);
        image.setFitWidth(20);
        image.setFitHeight(20);
        image.setPreserveRatio(true);
        buttonPrev.setGraphic(image);

        img = new Image(getClass().getClassLoader().getResourceAsStream("cz/tul/dic/gui/resources/arrow_right_32x32.png"));
        image = new ImageView(img);
        image.setFitWidth(20);
        image.setFitHeight(20);
        image.setPreserveRatio(true);
        buttonNext.setGraphic(image);

        adjustImageButtons(true);
        adjustConfigButtons(true);
        adjustResultButtons(true);

        if (TEST_CASE) {
//            performComputationTest();    
            
//            try {
//                Context.getInstance().setTc(TaskContainerUtils.deserializeTaskFromBinary(new File("D:\\temp\\7202845m.avi.test.task")));
//
//                TaskContainer tc = Context.getInstance().getTc();
//                InputLoader.loadInput(tc);
//                tc.getExports().clear();
//                tc.addExport(ExportTask.generateSequenceExport(Direction.Dx, ExportTarget.FILE, new File("D:\\temp\\results\\export.avi"), ExportTask.EXPORT_SEQUENCE_AVI));
//                tc.addExport(ExportTask.generateSequenceExport(Direction.Dx, ExportTarget.FILE, new File("D:\\temp\\results\\export.avi"), ExportTask.EXPORT_SEQUENCE_BMP));
//                tc.addExport(ExportTask.generateSequenceExport(Direction.Dx, ExportTarget.FILE, new File("D:\\temp\\results\\export.avi"), ExportTask.EXPORT_SEQUENCE_CSV));
//                Exporter.export(tc);
//            } catch (IOException | ComputationException | ClassNotFoundException ex) {
//                java.util.logging.Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
//            }
        }
    }

    private void performComputationTest() {
        try {
            Context.getInstance().setTc(TaskContainerUtils.deserializeTaskFromConfig(new File("D:\\temp\\7202845m.avi.config")));
            TaskContainer tc = Context.getInstance().getTc();
            InputLoader.loadInput(tc);
            tc.setParameter(TaskParameter.FACET_SIZE, 20);
            tc.setParameter(TaskParameter.FACET_GENERATOR_MODE, FacetGeneratorMode.CLASSIC);
            Computation.commenceComputationDynamic(tc);

            TaskContainerUtils.serializeTaskToBinary(tc, new File("D:\\temp\\7202845m.avi.test.task"));

            // 7202845m
            // 9905121m
            // 9820088m
            final int val1 = 10;
            final int val2 = 30;
            for (int size = val1; size <= val2; size++) {
                for (FacetGeneratorMode fgm : FacetGeneratorMode.values()) {
//                    Computation.commenceComputationDynamic(new File("D:\\temp\\7202845m.avi"), size);

//                    Context.getInstance().setTc(TaskContainerUtils.deserializeTaskFromConfig(new File("D:\\temp\\7202845m.avi.config")));
//                    TaskContainer tc = Context.getInstance().getTc();
//                    InputLoader.loadInput(tc);
//                    tc.setParameter(TaskParameter.FACET_SIZE, size);
//                    tc.setParameter(TaskParameter.FACET_GENERATOR_MODE, fgm);
////                    Computation.commenceComputationDynamic(tc);
//                    Computation.commenceComputationDynamicStrainParamSweep(tc, 10, 30);
//                    Context.getInstance().setTc(TaskContainerUtils.deserializeTaskFromConfig(new File("D:\\temp\\9905121m.avi.config")));
//                    tc = Context.getInstance().getTc();
//                    InputLoader.loadInput(tc);
//                    tc.setParameter(TaskParameter.FACET_SIZE, size);
//                    Computation.commenceComputationDynamic(tc);
//
//                    Context.getInstance().setTc(TaskContainerUtils.deserializeTaskFromConfig(new File("D:\\temp\\9820088m.avi.config")));
//                    tc = Context.getInstance().getTc();
//                    InputLoader.loadInput(tc);
//                    tc.setParameter(TaskParameter.FACET_SIZE, size);
//                    Computation.commenceComputationDynamic(tc);
                }
            }
        } catch (IOException | ComputationException ex) {
            System.err.println(ex);
        }
    }

    private void adjustImageButtons(final boolean disabled) {
        buttonPlay.setDisable(disabled);
        buttonPause.setDisable(disabled);
        buttonPrev.setDisable(disabled);
        buttonNext.setDisable(disabled);
    }

    private void adjustConfigButtons(final boolean disabled) {
        buttonExpert.setDisable(disabled);
        buttonExport.setDisable(disabled);
        buttonROI.setDisable(disabled);
        buttonRun.setDisable(disabled);
        textFs.setDisable(disabled);
        buttonSave.setDisable(disabled);
    }

    private void adjustResultButtons(final boolean disabled) {
        buttonResults.setDisable(disabled);
        buttonSave.setDisable(disabled);
    }

    private static class ComputationObserver extends Task<Exception> implements Observer {

        private final ComplextTaskSolver cts;
        private final TaskContainer tc;

        public ComputationObserver(ComplextTaskSolver cts, final TaskContainer tc) {
            this.cts = cts;
            this.tc = tc;
        }

        @Override
        protected Exception call() throws Exception {
            Exception result = null;
            try {
                if (cts.isValidComplexTask(tc)) {
                    cts.addObserver(this);
                    cts.solveComplexTask(tc);
                } else {
                    final Engine engine = new Engine();
                    engine.addObserver(this);
                    engine.computeTask(tc);
                }
            } catch (ComputationException ex) {
                result = ex;
            }
            return result;
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
