package cz.tul.dic.gui;

import cz.tul.dic.Computation;
import cz.tul.dic.ComputationException;
import cz.tul.dic.complextask.ComplextTaskSolver;
import cz.tul.dic.data.Config;
import cz.tul.dic.data.ConfigType;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerChecker;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.data.task.TaskParameter;
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
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(DEFAULT_DIR);
        fileChooser.getExtensionFilters().clear();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("AVI files (*.avi)", "*.avi"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image files (*.bmp, *.jpg)", "*.bmp", "*.jpg"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Config files (*.config)", "*..TASK.config", "*..EXPORT.config"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Task files (*.task)", "*.task"));
        List<File> fileList = fileChooser.showOpenMultipleDialog(null);
        if (fileList != null && !fileList.isEmpty()) {
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
                                try {
                                    Context.getInstance().setTc(TaskContainerUtils.deserializeTaskFromBinary(in));
                                    Platform.runLater(() -> {
                                        buttonResults.setDisable(false);
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
                        updateProgress(2, 5);
                    } else {
                        updateProgress(1, 5);
                        Context.getInstance().setTc(new TaskContainer(fileList));
                        updateProgress(2, 5);
                    }
                    if (!error) {
                        try {
                            updateProgress(3, 5);
                            final TaskContainer tc = Context.getInstance().getTc();
                            InputLoader.loadInput(tc);
                            updateProgress(4, 5);
                            Platform.runLater(() -> {
                                adjustConfigButtons(false);
                                adjustImageButtons(false);
                                imagePane.displayImage();

                                imagePane.getScene().getWindow().setWidth(tc.getImage(0).getWidth() + 143);
                                imagePane.getScene().getWindow().setHeight(tc.getImage(0).getHeight() + 114);

                                final Object o = tc.getParameter(TaskParameter.FACET_SIZE);
                                if (o != null) {
                                    textFs.setText(o.toString());
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
                try {
                    final String err = worker.get();
                    if (err != null) {
                        Dialogs.create()
                                .title(Lang.getString("error"))
                                .message(err)
                                .showWarning();

                    }
                } catch (InterruptedException | ExecutionException ex) {
                    System.out.println(ex);
                }
            });
            th.setDaemon(true);
            th.start();
        }
    }

    @FXML
    private void handleButtonActionSave(ActionEvent event) throws IOException {
        final String c1 = Lang.getString("TypeConfig");
        final String t1 = Lang.getString("TypeConfigD");
        final String c2 = Lang.getString("TypeBinary");
        final String t2 = Lang.getString("TypeBinaryD");
        final Action a = Dialogs.create()
                .title(Lang.getString("Save"))
                .message(Lang.getString("ChooseDataType"))
                .showCommandLinks(null, new Dialogs.CommandLink(c1, t1), new Dialogs.CommandLink(c2, t2));
        final String val = a.textProperty().get();
        if (val.equals(c1)) {
            TaskContainerUtils.serializeTaskToConfig(Context.getInstance().getTc());
        } else if (val.equals(c2)) {
            TaskContainerUtils.serializeTaskToBinary(Context.getInstance().getTc());
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
    private void handleButtonActionExpert(ActionEvent event) throws ComputationException, IOException {
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
//            tc.addExport(ExportTask.generateMapExport(Direction.X, ExportTarget.FILE, new File(target.concat(String.format("%02d", round)).concat("-X-").concat(ext)), round));
//            tc.addExport(ExportTask.generateMapExport(Direction.Y, ExportTarget.FILE, new File(target.concat(String.format("%02d", round)).concat("-Y-").concat(ext)), round));
//            tc.addExport(ExportTask.generateMapExport(Direction.ABS, ExportTarget.FILE, new File(target.concat(String.format("%02d", round)).concat("-ABS-").concat(ext)), round));
//        }
//        tc.addExport(ExportTask.generateSequenceExport(Direction.X, ExportTarget.FILE, new File(target.concat("-X-").concat(ext).replace("bmp", "avi"))));
//        tc.addExport(ExportTask.generateSequenceExport(Direction.Y, ExportTarget.FILE, new File(target.concat("-Y-").concat(ext).replace("bmp", "avi"))));
//
//        TaskContainerUtils.serializeTaskToConfig(tc);
//
//        // compute dynamic task
//        Computation.computeDynamicTask(tc);
//        
//        // export results
//        TaskContainerUtils.exportTask(tc);
//
//        // serialize task container to binary file
//        TaskContainerUtils.serializeTaskToBinary(tc);

        try {
            final Parent root = FXMLLoader.load(getClass().getClassLoader().getResource("cz/tul/dic/gui/ExpertSettings.fxml"), Lang.getBundle());
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
        try {
            Computation.commenceComputationDynamic(new File("D:\\temp\\7202845m.avi"), 10);
        } catch (IOException | ComputationException ex) {
            System.err.println(ex);
        }
        
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
        buttonResults.setDisable(true);
    }

    private void adjustImageButtons(final boolean disabled) {
        buttonPlay.setDisable(disabled);
        buttonPause.setDisable(disabled);
        buttonPrev.setDisable(disabled);
        buttonNext.setDisable(disabled);
    }

    private void adjustConfigButtons(final boolean disabled) {
        buttonExpert.setDisable(disabled);
        buttonROI.setDisable(disabled);
        buttonRun.setDisable(disabled);
        textFs.setDisable(disabled);
        buttonSave.setDisable(disabled);
    }

    private static class ComputationObserver extends Task implements Observer {

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
                cts.addObserver(this);
                cts.solveComplexTask(tc);
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
