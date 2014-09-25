package cz.tul.dic.gui;

import cz.tul.dic.ComputationException;
import cz.tul.dic.complextask.ComplexTaskSolver;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.data.task.TaskDefaultValues;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.engine.Engine;
import cz.tul.dic.gui.lang.Lang;
import cz.tul.dic.input.InputLoader;
import cz.tul.dic.output.NameGenerator;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import java.util.prefs.Preferences;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.fxml.JavaFXBuilderFactory;
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
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.Dialogs;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Jecmen
 */
public class MainWindow implements Initializable {

    private static final String LAST_DIR = "lastDir";

    @FXML
    private TextField textFs;
    @FXML
    private TextField textIndex;
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
    private Button buttonRealSize;
    @FXML
    private Button buttonResults;
    @FXML
    private Button buttonSave;
    @FXML
    private InputPresenter imagePane;
    private Timeline timeLine;

    @FXML
    private void handleButtonActionInput(ActionEvent event) throws IOException, InterruptedException, ExecutionException, ComputationException {
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
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Project files", "*.avi", "*.bmp", "*.jpg", "*.config", "*.task", "*.scr"));
        List<File> fileList = fc.showOpenMultipleDialog(buttonRun.getScene().getWindow());
        if (fileList != null && !fileList.isEmpty()) {
            if (fileList.get(0).getName().endsWith(".scr")) {
                // load batch batch file
                List<String> fileNames;
                List<File> input = new ArrayList<>(1);
                for (File f : fileList) {
                    fileNames = Files.readAllLines(f.toPath());
                    for (String s : fileNames) {
                        // load each config and run it
                        input.clear();
                        input.add(new File(s.trim()));
                        loadInput(input).get();
                        handleButtonActionRun(null);
                    }
                }
            } else {
                loadInput(fileList);
            }
            Preferences.userRoot().put(LAST_DIR, fileList.get(0).getAbsolutePath());
        }
    }

    private Task loadInput(List<File> fileList) {
        Task<String> worker = new Task<String>() {
            @Override
            protected String call() throws Exception {
                String result = null;
                boolean error = false;

                updateProgress(0, 5);
                final File in = fileList.get(0);
                if (fileList.size() == 1) {
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
                        tc.setParameter(TaskParameter.IN, in);
                        tc.addObserver(imagePane);
                        InputLoader.loadInput(tc);
                        updateProgress(4, 5);
                        Platform.runLater(() -> {
                            final Stage stage = (Stage) MainWindow.this.buttonROI.getScene().getWindow();
                            stage.setTitle("DIC - " + in.getName());
                            if (imagePane != null && imagePane.getScene() != null) {
                                adjustConfigButtons(false);
                                adjustImageButtons(false);
                                textIndex.textProperty().setValue("0");
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
                .masthead(null)
                .showWorkerProgress(worker);

        Thread th = new Thread(worker);
        th.setDaemon(true);
        th.start();

        th = new Thread(() -> {
            try {
                final String err = worker.get();
                if (err != null) {
                    Platform.runLater(() -> {
                        Dialogs.create()
                                .title(Lang.getString("error"))
                                .masthead(null)
                                .message(err)
                                .showWarning();
                    });

                }
            } catch (InterruptedException | ExecutionException ex) {
                Platform.runLater(() -> {
                    Dialogs.create()
                            .title(Lang.getString("error"))
                            .masthead(null)
                            .message(ex.getLocalizedMessage())
                            .showException(ex);
                });
            }
        });
        th.setDaemon(true);
        th.start();

        return worker;
    }

    @FXML
    private void handleButtonActionSave(ActionEvent event) throws IOException {
        saveFacetSize();

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
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Config files (*".concat(NameGenerator.EXT_CONFIG).concat(")"), "*".concat(NameGenerator.EXT_CONFIG)));
                fc.setInitialFileName(in.getName().concat(NameGenerator.EXT_CONFIG));
            } else if (val.equals(c2)) {
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Task files (*".concat(NameGenerator.EXT_BINARY).concat(")"), "*".concat(NameGenerator.EXT_BINARY)));
                fc.setInitialFileName(in.getName().concat(NameGenerator.EXT_BINARY));
            }

            File target = fc.showSaveDialog(buttonRun.getScene().getWindow());
            if (target != null) {
                if (val.equals(c1)) {
                    if (!target.getName().endsWith(NameGenerator.EXT_CONFIG)) {
                        target = new File(target.getAbsolutePath().concat(NameGenerator.EXT_CONFIG));
                    }
                    TaskContainerUtils.serializeTaskToConfig(Context.getInstance().getTc(), target);
                } else if (val.equals(c2)) {
                    if (!target.getName().endsWith(NameGenerator.EXT_BINARY)) {
                        target = new File(target.getAbsolutePath().concat(NameGenerator.EXT_BINARY));
                    }
                    TaskContainerUtils.serializeTaskToBinary(Context.getInstance().getTc(), target);
                }
            }
        }
    }

    @FXML
    private void handleButtonActionRun(ActionEvent event) throws IOException, ComputationException {
        try {
            final TaskContainer tc = Context.getInstance().getTc();
            if (tc != null) {
                saveFacetSize();
                TaskContainerUtils.checkTaskValidity(tc);
                ComplexTaskSolver cts = new ComplexTaskSolver();
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
                            Platform.runLater(() -> {
                                Dialogs.create()
                                        .title(Lang.getString("Exception"))
                                        .message(err.getLocalizedMessage())
                                        .masthead(null)
                                        .showWarning();
                            });
                            Logger.error(err);

                        } else {
                            Platform.runLater(() -> {
                                buttonResults.setDisable(false);
                            });
                        }
                    } catch (InterruptedException | ExecutionException ex) {
                        Logger.error(ex);
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
            stage.setTitle(Lang.getString("selectROIs"));
            stage.setScene(new Scene(root));
            stage.getIcons().add(new javafx.scene.image.Image(MainWindow.class.getResourceAsStream("logo.png")));
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
            saveFacetSize();
            final FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getClassLoader().getResource("cz/tul/dic/gui/ExportEditor.fxml"));
            loader.setResources(Lang.getBundle());
            loader.setBuilderFactory(new JavaFXBuilderFactory());
            final Parent root = loader.load();
            final ExportEditor controller = loader.getController();
            final Stage stage = new Stage();
            stage.setTitle(Lang.getString("Exports"));
            stage.setScene(new Scene(root));
            stage.getIcons().add(new javafx.scene.image.Image(MainWindow.class.getResourceAsStream("logo.png")));
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(imagePane.getScene().getWindow());
            stage.addEventHandler(WindowEvent.WINDOW_SHOWN, new EventHandler<WindowEvent>() {

                @Override
                public void handle(WindowEvent event) {
                    controller.actualizeExports();
                }
            });
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
            stage.getIcons().add(new javafx.scene.image.Image(MainWindow.class.getResourceAsStream("logo.png")));
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
            final FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("cz/tul/dic/gui/ResultPresenter.fxml"), Lang.getBundle());
            final Parent root = loader.load();
            final Stage stage = new Stage();
            stage.setTitle(Lang.getString("Results"));
            stage.setScene(new Scene(root));
            stage.getIcons().add(new javafx.scene.image.Image(MainWindow.class.getResourceAsStream("logo.png")));
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(imagePane.getScene().getWindow());
            stage.setResizable(false);
            stage.setOnShown((WindowEvent event1) -> {
                ResultPresenter controller = loader.getController();
                controller.init();
            });
            stage.showAndWait();
        } catch (IOException e) {
            Logger.error("Error loading Results dialog from JAR.\n{0}", e);
        }
    }

    @FXML
    private void handleButtonActionRealSize(ActionEvent event) {
        try {
            final Parent root = FXMLLoader.load(getClass().getClassLoader().getResource("cz/tul/dic/gui/PxToMmMapper.fxml"), Lang.getBundle());
            final Stage stage = new Stage();
            stage.setTitle(Lang.getString("RealSizeW"));
            stage.setScene(new Scene(root));
            stage.getIcons().add(new javafx.scene.image.Image(MainWindow.class.getResourceAsStream("logo.png")));
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(imagePane.getScene().getWindow());
            stage.setResizable(false);
            final Scene s = stage.getScene();
            if (s != null) {
                final BufferedImage image = Context.getInstance().getTc().getImage(0);
                double width = Math.max(PxToMmMapperController.MIN_WIDTH, image.getWidth() + PxToMmMapperController.EXTRA_WIDTH);
                s.getWindow().setWidth(width);
                s.getWindow().setHeight(image.getHeight() + PxToMmMapperController.EXTRA_HEIGHT);
            }
            stage.showAndWait();
        } catch (IOException e) {
            Logger.error("Error loading PxToMmMapper dialog from JAR.\n{0}", e);
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
        } else {
            saveFacetSize();
        }
    }

    private void saveFacetSize() {
        final String text = textFs.getText();
        if (!text.isEmpty()) {
            final int fs = Integer.parseInt(text);
            final TaskContainer tc = Context.getInstance().getTc();
            if (tc != null) {
                tc.setParameter(TaskParameter.FACET_SIZE, fs);
            }
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        textFs.setText(Integer.toString(TaskDefaultValues.DEFAULT_FACET_SIZE));

        imagePane.initialize(url, rb);
        imagePane.assignImageIndexTextField(textIndex.textProperty());

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
        buttonRealSize.setDisable(disabled);
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

        private final ComplexTaskSolver cts;
        private final TaskContainer tc;
        private final int roundCount;
        private int round;

        public ComputationObserver(ComplexTaskSolver cts, final TaskContainer tc) {
            this.cts = cts;
            this.tc = tc;
            roundCount = TaskContainerUtils.getRounds(tc).keySet().size();
        }

        @Override
        protected Exception call() throws Exception {
            Exception result = null;
            try {
                if (cts.isValidComplexTask(tc)) {
                    cts.addObserver(this);
                    cts.solveComplexTask(tc);
                } else {
                    Engine.getInstance().addObserver(this);
                    Engine.getInstance().computeTask(tc);
                }
            } catch (ComputationException ex) {
                result = ex;
            }
            Engine.getInstance().deleteObserver(this);
            cts.deleteObserver(this);

            return result;
        }

        @Override
        public void update(Observable o, Object arg) {
            if (arg instanceof Integer) {
                round = (int) arg;
                updateProgress(round, roundCount);
            } else if (arg instanceof Class) {
                final Class cls = (Class) arg;
                final StringBuilder sb = new StringBuilder();
                sb.append("[");
                sb.append(Lang.getString("Round"));
                sb.append(" ");
                sb.append(round);
                sb.append("] ");
                sb.append(Lang.getString(cls.getSimpleName()));
                updateMessage(sb.toString());
            }
        }
    }
}
