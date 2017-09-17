/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.gui;

import cz.tul.dic.ComputationException;
import cz.tul.dic.complextask.ComplexTaskSolver;
import cz.tul.dic.data.deformation.DeformationOrder;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.data.task.TaskDefaultValues;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.engine.Engine;
import cz.tul.dic.engine.platform.PlatformManager;
import cz.tul.dic.gui.lang.Lang;
import cz.tul.dic.output.NameGenerator;
import cz.tul.pj.journal.Journal;
import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.ResourceBundle;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.prefs.Preferences;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Jecmen
 */
public class MainWindow implements Initializable {

    private static final String LAST_DIR = "lastDir";
    private static final String LOGO = "logo.png";
    private static final int EXTRA_WIDTH = 19;
    private static final int EXTRA_HEIGHT = 10;

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
    private Button buttonPrev;
    @FXML
    private Button buttonPlay;
    @FXML
    private Button buttonNext;
    @FXML
    private Button buttonRealSize;
    @FXML
    private Button buttonResults;
    @FXML
    private MenuItem buttonSave;
    @FXML
    private InputPresenter imagePane;
    @FXML
    private ComboBox<Scenario> comboScenario;
    @FXML
    private ComboBox<DeformationOrder> comboOrder;
    @FXML
    private VBox boxRight;
    @FXML
    private HBox boxImage;
    private Timeline timeLine;

    // Main menu    

    @FXML
    private void handleButtonActionExit(ActionEvent event) {
        Platform.exit();
    }

    @FXML
    private void handleButtonActionAbout(ActionEvent event) {
        final Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle(Lang.getString("About"));
        dialog.setHeaderText(null);

        final Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
        stage.getIcons().add(new javafx.scene.image.Image(MainWindow.class.getResourceAsStream("logo.png")));

        final GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        final String webLink = "https://github.com/SirGargamel/GPU-DIC";
        final Hyperlink web = new Hyperlink(webLink);
        web.setOnAction((ActionEvent event1) -> {
            try {
                Desktop.getDesktop().browse(new URI(webLink));
            } catch (IOException | URISyntaxException ex) {
                Journal.getInstance().addDataEntry(ex, "Error opening GitHub link.");
            }
        });

        grid.add(new Label(Lang.getString("Author") + ":"), 0, 0);
        grid.add(new Label("Petr Ječmen, TUL"), 1, 0);
        grid.add(new Label("WWW:"), 0, 1);
        grid.add(web, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.show();
    }

    @FXML
    private void handleButtonActionInput(ActionEvent event) throws IOException, InterruptedException, ExecutionException, ComputationException {
        final FileChooser fc = new FileChooser();
        final String lastDir = Preferences.userNodeForPackage(this.getClass()).get(LAST_DIR, null);
        if (lastDir != null) {
            File last = new File(lastDir);
            if (!last.isDirectory()) {
                last = last.getParentFile();
            }
            if (last.exists()) {
                fc.setInitialDirectory(last);
            }
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
            Preferences.userNodeForPackage(this.getClass()).put(LAST_DIR, fileList.get(0).getAbsolutePath());
        }
    }

    private Task loadInput(List<File> fileList) {
        final Task<String> worker = new InputLoader(fileList);

        Dialogs.showProgress(worker, Lang.getString("LoadingData"));

        Thread th = new Thread(worker);
        th.setDaemon(true);
        th.start();

        th = new Thread(() -> {
            try {
                final String err = worker.get();
                if (err != null) {
                    Platform.runLater(()
                            -> Dialogs.showWarning(
                                    Lang.getString("error"),
                                    err)
                    );

                }
            } catch (InterruptedException | ExecutionException ex) {
                Platform.runLater(()
                        -> Dialogs.showException(ex)
                );
            } catch (CancellationException ex) {
                // don nothing when user cancels
            }
        });
        th.setDaemon(true);
        th.start();

        return worker;
    }

    @FXML
    private void handleButtonActionSave(ActionEvent event) {
        saveSubsetSize();

        final ButtonType config = new ButtonType(Lang.getString("TypeConfig"));
        final ButtonType binary = new ButtonType(Lang.getString("TypeBinary"));
        final ButtonType cancel = new ButtonType(Lang.getString("Cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        final Alert dlg = new Alert(Alert.AlertType.CONFIRMATION, null, config, binary, cancel);
        dlg.setTitle(Lang.getString("Save"));
        dlg.setHeaderText(Lang.getString("ChooseDataType"));
        dlg.showAndWait().ifPresent((ButtonType t) -> {
            try {
                // pick target file        
                final FileChooser fc = new FileChooser();
                final File in = (File) Context.getInstance().getTc().getParameter(TaskParameter.IN);
                fc.setInitialDirectory(in.getParentFile());
                fc.getExtensionFilters().clear();
                if (t == config) {
                    fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Config files (*".concat(NameGenerator.EXT_CONFIG).concat(")"), "*".concat(NameGenerator.EXT_CONFIG)));
                    fc.setInitialFileName(in.getName().concat(NameGenerator.EXT_CONFIG));
                } else if (t == binary) {
                    fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Task files (*".concat(NameGenerator.EXT_BINARY).concat(")"), "*".concat(NameGenerator.EXT_BINARY)));
                    fc.setInitialFileName(in.getName().concat(NameGenerator.EXT_BINARY));
                }

                File target = fc.showSaveDialog(buttonRun.getScene().getWindow());
                if (target != null) {
                    if (t == config) {
                        if (!target.getName().endsWith(NameGenerator.EXT_CONFIG)) {
                            target = new File(target.getAbsolutePath().concat(NameGenerator.EXT_CONFIG));
                        }
                        TaskContainerUtils.serializeTaskToConfig(Context.getInstance().getTc(), target);
                    } else if (t == binary) {
                        if (!target.getName().endsWith(NameGenerator.EXT_BINARY)) {
                            target = new File(target.getAbsolutePath().concat(NameGenerator.EXT_BINARY));
                        }
                        TaskContainerUtils.serializeTaskToBinary(Context.getInstance().getTc(), target);
                    }
                }
            } catch (IOException ex) {
                Platform.runLater(()
                        -> Dialogs.showWarning(
                                Lang.getString("error"),
                                ex.getLocalizedMessage()));
                Journal.getInstance().addDataEntry(ex, "Error saving task container to file.");
            }
        });
    }

    @FXML
    private void handleButtonActionSettings(ActionEvent event) {
        try {
            final Parent root = FXMLLoader.load(getClass().getClassLoader().getResource("cz/tul/dic/gui/Settings.fxml"), Lang.getBundle());
            final Stage stage = new Stage();
            stage.setTitle(Lang.getString("Settings"));
            stage.setScene(new Scene(root));
            stage.getIcons().add(new javafx.scene.image.Image(MainWindow.class.getResourceAsStream(LOGO)));
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(imagePane.getScene().getWindow());
            stage.setResizable(false);
            stage.showAndWait();
        } catch (IOException e) {
            Logger.error(e, "Error loading Settings dialog from JAR.");
        }
    }

    // Right column
    @FXML
    private void handleButtonActionRun(ActionEvent event) throws ComputationException {
        try {
            final TaskContainer tc = Context.getInstance().getTc();
            if (tc != null) {
                saveSubsetSize();                
                ComplexTaskSolver cts = new ComplexTaskSolver(tc);
                final Task<Exception> worker = new ComputationObserver(cts, tc);
                Dialogs.showProgress(worker, Lang.getString("Computing"));

                Thread th = new Thread(worker);
                th.setDaemon(true);
                th.start();

                th = new Thread(() -> {
                    try {
                        final Exception err = worker.get();
                        if (err != null) {
                            Platform.runLater(()
                                    -> Dialogs.showWarning(
                                            Lang.getString("Exception"),
                                            err.getLocalizedMessage())
                            );
                            Journal.getInstance().addDataEntry(err, "Exception occured during computation.");
                        } else {
                            Platform.runLater(()
                                    -> buttonResults.setDisable(false)
                            );
                        }
                    } catch (InterruptedException | ExecutionException ex) {
                        Journal.getInstance().addDataEntry(ex, "Exception occured during computation.");
                    } catch (CancellationException ex) {
                        Platform.runLater(()
                                -> buttonResults.setDisable(true)
                        );
                    }
                });
                th.setDaemon(true);
                th.start();
            } else {
                Dialogs.showError(Lang.getString("noTC"));
            }
        } catch (NumberFormatException ex) {
            Dialogs.showError(Lang.getString("wrongFS"));
        }
    }

    @FXML
    private void handleButtonActionROI(ActionEvent event) {
        try {
            final Parent root = FXMLLoader.load(getClass().getClassLoader().getResource("cz/tul/dic/gui/ROISelector.fxml"), Lang.getBundle());
            final Stage stage = new Stage();
            stage.setTitle(Lang.getString("selectROIs"));
            stage.setScene(new Scene(root));
            stage.getIcons().add(new javafx.scene.image.Image(MainWindow.class.getResourceAsStream(LOGO)));
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(imagePane.getScene().getWindow());
            stage.showAndWait();
        } catch (IOException e) {
            Logger.error(e, "Error loading ROI dialog from JAR.");
        }
    }

    @FXML
    private void handleButtonActionExpert(ActionEvent event) {
        try {
            final Parent root = FXMLLoader.load(getClass().getClassLoader().getResource("cz/tul/dic/gui/ExpertSettings.fxml"), Lang.getBundle());
            final Stage stage = new Stage();
            stage.setTitle(Lang.getString("Expert"));
            stage.setScene(new Scene(root));
            stage.getIcons().add(new javafx.scene.image.Image(MainWindow.class.getResourceAsStream(LOGO)));
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(imagePane.getScene().getWindow());
            stage.showAndWait();
        } catch (IOException e) {
            Logger.error(e, "Error loading Expert settings dialog from JAR.");
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
            stage.getIcons().add(new javafx.scene.image.Image(MainWindow.class.getResourceAsStream(LOGO)));
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(imagePane.getScene().getWindow());
            stage.setResizable(false);
            stage.setOnShown((WindowEvent event1) -> {
                ResultPresenter controller = loader.getController();
                controller.init();
            });
            stage.showAndWait();
        } catch (IOException e) {
            Logger.error(e, "Error loading Results dialog from JAR.");
        }
    }

    @FXML
    private void handleButtonActionRealSize(ActionEvent event) {
        try {
            final Parent root = FXMLLoader.load(getClass().getClassLoader().getResource("cz/tul/dic/gui/PxToMmMapper.fxml"), Lang.getBundle());
            final Stage stage = new Stage();
            stage.setTitle(Lang.getString("RealSizeW"));
            stage.setScene(new Scene(root));
            stage.getIcons().add(new javafx.scene.image.Image(MainWindow.class.getResourceAsStream(LOGO)));
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
            Logger.error(e, "Error loading PxToMmMapper dialog from JAR.");
        }
    }

    @FXML
    private void handleButtonActionNext(ActionEvent event) {
        stopVideo();
        imagePane.nextImage();
        event.consume();
    }

    @FXML
    private void handleButtonActionPrev(ActionEvent event) {
        stopVideo();
        imagePane.previousImage();
    }

    @FXML
    private void handleButtonActionPlay(ActionEvent event) {
        if (timeLine == null) {
            playVideo();
        } else {
            stopVideo();
        }

        event.consume();
    }

    private void stopVideo() {
        if (timeLine != null) {
            timeLine.stop();
            timeLine = null;
        }

        final Image img = new Image(getClass().getClassLoader().getResourceAsStream("cz/tul/dic/gui/resources/play_24x32.png"));
        final ImageView image = new ImageView(img);
        image.setFitWidth(20);
        image.setFitHeight(20);
        image.setPreserveRatio(true);
        buttonPlay.setGraphic(image);
    }

    private void playVideo() {
        timeLine = new Timeline(new KeyFrame(Duration.millis(250), (ActionEvent event1) -> {
            if (imagePane.nextImage()) {
                stopVideo();
            }
        }));
        timeLine.setCycleCount(Timeline.INDEFINITE);
        timeLine.play();

        final Image img = new Image(getClass().getClassLoader().getResourceAsStream("cz/tul/dic/gui/resources/pause_24x32.png"));
        final ImageView image = new ImageView(img);
        image.setFitWidth(20);
        image.setFitHeight(20);
        image.setPreserveRatio(true);
        buttonPlay.setGraphic(image);
    }

    @FXML
    private void handleTextKeyTyped(KeyEvent keyEvent) {
        if (!"0123456789".contains(keyEvent.getCharacter())) {
            keyEvent.consume();
        } else {
            saveSubsetSize();
        }
    }

    @FXML
    private void handleRoundAction(ActionEvent event) {
        final String rndS = textIndex.getText();
        if (rndS != null && !rndS.isEmpty()) {
            try {
                final int rnd = Integer.valueOf(rndS);
                imagePane.setImageIndex(rnd);
            } catch (NumberFormatException ex) {
            }
        }
    }

    private void saveSubsetSize() {
        final String text = textFs.getText();
        if (!text.isEmpty()) {
            final int fs = Integer.parseInt(text);
            final TaskContainer tc = Context.getInstance().getTc();
            if (tc != null) {
                tc.setParameter(TaskParameter.SUBSET_SIZE, fs);
            }
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        final ObservableList<Scenario> comboScenarioData = FXCollections.observableArrayList();
        comboScenarioData.addAll(Scenario.values());
        comboScenario.setItems(comboScenarioData);
        comboScenario.valueProperty().addListener((ObservableValue<? extends Scenario> observable, Scenario oldValue, Scenario newValue) -> {
            final TaskContainer tc = Context.getInstance().getTc();
            tc.setParameter(TaskParameter.SUBSET_GENERATOR_PARAM, newValue.getSubsetSpacing());
        });
        comboScenario.setConverter(new StringConverter<Scenario>() {

            private final Map<String, Scenario> data = new HashMap<>(Scenario.values().length);

            @Override
            public String toString(Scenario object) {
                final String result = Lang.getString(object.toString());
                data.put(result, object);
                return result;
            }

            @Override
            public Scenario fromString(String string) {
                return data.get(string);
            }
        });

        final ObservableList<DeformationOrder> comboOrderData = FXCollections.observableArrayList();
        comboOrderData.addAll(DeformationOrder.values());
        comboOrder.setItems(comboOrderData);
        comboOrder.valueProperty().addListener((ObservableValue<? extends DeformationOrder> observable, DeformationOrder oldValue, DeformationOrder newValue) -> {
            final TaskContainer tc = Context.getInstance().getTc();
            tc.setParameter(TaskParameter.DEFORMATION_ORDER, newValue);
        });
        comboOrder.setConverter(new StringConverter<DeformationOrder>() {

            private final Map<String, DeformationOrder> data = new HashMap<>(DeformationOrder.values().length);

            @Override
            public String toString(DeformationOrder object) {
                final String result = Lang.getString(object.toString());
                data.put(result, object);
                return result;
            }

            @Override
            public DeformationOrder fromString(String string) {
                return data.get(string);
            }
        });

        textFs.setText(Integer.toString(TaskDefaultValues.DEFAULT_SUBSET_SIZE));

        imagePane.initialize(url, rb);
        imagePane.assignImageIndexTextField(textIndex.textProperty());

        Image img = new Image(getClass().getClassLoader().getResourceAsStream("cz/tul/dic/gui/resources/play_24x32.png"));
        ImageView image = new ImageView(img);
        image.setFitWidth(20);
        image.setFitHeight(20);
        image.setPreserveRatio(true);
        buttonPlay.setGraphic(image);

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
        buttonPrev.setDisable(disabled);
        buttonNext.setDisable(disabled);
    }

    private void adjustConfigButtons(final boolean disabled) {
        buttonExpert.setDisable(disabled);
        buttonRealSize.setDisable(disabled);
        buttonROI.setDisable(disabled);
        buttonRun.setDisable(disabled);
        textFs.setDisable(disabled);
        buttonSave.setDisable(disabled);
        comboScenario.setDisable(disabled);
        comboOrder.setDisable(disabled);
    }

    private void adjustResultButtons(final boolean disabled) {
        buttonResults.setDisable(disabled);
        buttonSave.setDisable(disabled);
    }

    private void loadCurrentTaskContainerToGUI() {
        final TaskContainer tc = Context.getInstance().getTc();

        Object o = tc.getParameter(TaskParameter.SUBSET_SIZE);
        if (o != null) {
            textFs.setText(o.toString());
        } else {
            textFs.setText(Integer.toString(TaskDefaultValues.DEFAULT_SUBSET_SIZE));
        }

        o = tc.getParameter(TaskParameter.DEFORMATION_ORDER);
        if (o != null) {
            comboOrder.setValue(DeformationOrder.valueOf(o.toString()));
        } else {
            comboOrder.setValue(TaskDefaultValues.DEFAULT_DEFORMATION_ORDER);
        }
    }

    private static class ComputationObserver extends Task<Exception> implements Observer {

        private final ComplexTaskSolver cts;
        private final TaskContainer tc;
        private final int roundCount, roundOne;
        private int round;
        private final StringBuilder action, time;

        public ComputationObserver(ComplexTaskSolver cts, final TaskContainer tc) {
            this.cts = cts;
            this.tc = tc;
            roundCount = TaskContainerUtils.getRounds(tc).keySet().size();
            roundOne = TaskContainerUtils.getFirstRound(tc);

            time = new StringBuilder();
            action = new StringBuilder();
        }

        @Override
        protected Exception call() throws Exception {
            Exception result = null;
            try {
                update(null, PlatformManager.class);
                TaskContainerUtils.checkTaskValidity(tc);
                if (cts.isValidComplexTask()) {
                    cts.addObserver(this);
                    cts.solveComplexTask();
                } else {
                    Engine.getInstance().addObserver(this);
                    Engine.getInstance().computeTask(tc);
                }
            } catch (Exception ex) {
                result = ex;
            }
            Engine.getInstance().deleteObserver(this);
            cts.deleteObserver(this);

            return result;
        }

        @Override
        protected void cancelled() {
            super.cancelled();

            Engine.getInstance().stop();
            cts.stop();

            Engine.getInstance().deleteObserver(this);
            cts.deleteObserver(this);
        }

        @Override
        public void update(Observable o, Object arg) {
            if (arg instanceof Integer) {
                round = (int) arg;
                updateProgress(round - roundOne, roundCount);
            } else if (arg instanceof Double) {
                updateProgress(round - roundOne + (double) arg, (double) roundCount);
            } else if (arg instanceof Long) {
                final long roundTime = (long) arg;
                final int roundsLeft = roundCount - (round - roundOne) - 1;
                final String timeLeft = new SimpleDateFormat("hh:mm:ss").format(new Date(System.currentTimeMillis() + roundTime * roundsLeft));
                time.setLength(0);
                time.append(Lang.getString("FinishTime"))
                        .append(" : ")
                        .append(timeLeft);
            } else if (arg instanceof Class) {
                final Class cls = (Class) arg;
                action.setLength(0);
                action.append("[")
                        .append(Lang.getString("Round"))
                        .append(" ")
                        .append(round)
                        .append("] ")
                        .append(Lang.getString(cls.getSimpleName()));
            }
            updateMessage(action.toString().concat("\n").concat(time.toString()));
        }
    }

    private static enum Scenario {

        DEFAULT(5),
        PRECISE(1),        
        COARSE(10);

        private final int subsetSpacing;

        private Scenario(int subsetSpacing) {
            this.subsetSpacing = subsetSpacing;
        }

        public int getSubsetSpacing() {
            return subsetSpacing;
        }

    }

    private class InputLoader extends Task<String> {

        private final List<File> fileList;
        private boolean cancel = false;

        public InputLoader(List<File> fileList) {
            this.fileList = fileList;
        }

        @Override
        protected String call() throws Exception {
            String result = null;
            boolean error = false;

            updateProgress(0, 4);
            final File in = fileList.get(0);
            if (fileList.size() == 1) {
                final String name = in.getName();
                final String ext = name.substring(name.lastIndexOf(".") + 1);
                updateProgress(1, 4);
                switch (ext) {
                    case "avi":
                    case "config":
                        Context.getInstance().setTc(TaskContainer.initTaskContainer(in));
                        break;
                    case "task":
                        try {
                            Context.getInstance().setTc(TaskContainer.initTaskContainer(in));
                            Platform.runLater(() -> {
                                adjustResultButtons(false);
                            });
                        } catch (ComputationException ex) {
                            error = true;
                            result = Lang.getString("wrongBin");
                            Journal.getInstance().addDataEntry(ex, "Error loading binary task.");
                        }
                        break;
                    default:
                        error = true;
                        result = Lang.getString("wrongIn");
                }
            } else {
                updateProgress(1, 4);
                Context.getInstance().setTc(TaskContainer.initTaskContainer(fileList));
            }
            updateProgress(2, 4);
            if (cancel) {
                handleCancel();
                return result;
            }

            if (!error) {
                final TaskContainer tc = Context.getInstance().getTc();
                tc.setParameter(TaskParameter.IN, in);
                tc.addObserver(imagePane);
                updateProgress(3, 4);
                if (cancel) {
                    handleCancel();
                    return result;
                }

                Platform.runLater(() -> {
                    final Stage stage = (Stage) MainWindow.this.buttonROI.getScene().getWindow();
                    stage.setTitle(Lang.getString("Title") + " - " + in.getName());
                    if (imagePane != null && imagePane.getScene() != null) {
                        adjustConfigButtons(false);
                        adjustImageButtons(false);
                        textIndex.textProperty().setValue("0");
                        imagePane.displayImage();

                        double size;
                        size = Math.max(
                                tc.getImage(0).getWidth() + boxRight.getWidth() + EXTRA_WIDTH,
                                boxImage.getMinWidth() + boxRight.getWidth() + EXTRA_WIDTH);
                        imagePane.getScene().getWindow().setWidth(size);
                        size = Math.max(
                                tc.getImage(0).getHeight() + boxImage.getHeight() + EXTRA_HEIGHT,
                                boxRight.getHeight() + boxImage.getHeight() + EXTRA_HEIGHT);
                        imagePane.getScene().getWindow().setHeight(size);

                        loadCurrentTaskContainerToGUI();
                    }
                });
            }
            updateProgress(4, 4);

            return result;
        }

        @Override
        protected void cancelled() {
            super.cancelled();

            cancel = true;
        }

        private void handleCancel() {
            adjustResultButtons(true);
            adjustConfigButtons(true);
            adjustImageButtons(true);

            Context.getInstance().setTc(null);

            final Stage stage = (Stage) MainWindow.this.buttonROI.getScene().getWindow();
            stage.setTitle(Lang.getString("Title"));
        }
    }
}
