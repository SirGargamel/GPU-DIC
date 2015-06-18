/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.gui;

import cz.tul.dic.ComputationException;
import cz.tul.dic.FpsManager;
import cz.tul.dic.Utils;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.gui.lang.Lang;
import cz.tul.dic.output.Direction;
import cz.tul.dic.output.target.ExportTarget;
import cz.tul.dic.output.ExportTask;
import cz.tul.dic.output.Exporter;
import cz.tul.dic.output.NameGenerator;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.pmw.tinylog.Logger;

public class ResultPresenter implements Initializable {

    private static final String SEPARATOR = " [";
    private static final int PREF_SIZE_W_BASE = 30;
    private static final int PREF_SIZE_W_M = 5;
    private static final int PREF_SIZE_H = 30;
    private static final int EXTRA_WIDTH = 30;
    private static final int EXTRA_HEIGHT = 100;
    private static final int MIN_WIDTH = 380;

    @FXML
    private ComboBox<Direction> choiceDir;
    @FXML
    private TextField textIndex, textMin, textMax;
    @FXML
    private Label labelTime;
    @FXML
    private ImageView image;
    @FXML
    private Button buttonPrev;
    @FXML
    private Button buttonPlay;
    @FXML
    private Button buttonPause;
    @FXML
    private Button buttonNext;
    private int index;
    private Timeline timeLine;
    private final Map<Stage, ChartHandler> charts;

    public ResultPresenter() {
        charts = new LinkedHashMap<>();
    }

    @FXML
    private void handleButtonActionNext(ActionEvent event) {
        stopVideo();
        changeIndex(1);
        displayImage();
        event.consume();
    }

    private boolean changeIndex(int change) {
        boolean result = false;
        index += change;

        final TaskContainer tc = Context.getInstance().getTc();
        final int maxCount = tc.getImages().size();
        if (index < 0) {
            index = maxCount - 1;
            result = true;
        } else if (index >= maxCount) {
            index = 0;
            result = true;
        }

        textIndex.setText(Integer.toString(index));

        final FpsManager fpsM = new FpsManager(tc);
        labelTime.setText(Utils.format(fpsM.getTime(index)).concat(fpsM.getTickUnit()));

        return result;
    }

    private void displayImage() {
        try {
            final BufferedImage i = Context.getInstance().getMapResult(index, choiceDir.getValue());
            if (i != null) {
                final Image img = SwingFXUtils.toFXImage(i, null);
                image.setImage(img);

                final Scene s = image.getParent().getScene();
                if (s != null) {
                    double width = Math.max(MIN_WIDTH, image.getParent().getBoundsInLocal().getWidth() + EXTRA_WIDTH);
                    s.getWindow().setWidth(width);
                    s.getWindow().setHeight(image.getParent().getBoundsInLocal().getHeight() + EXTRA_HEIGHT);
                }
            } else {
                image.setImage(null);
            }

        } catch (ComputationException ex) {
            Logger.warn(ex);
        }
    }

    @FXML
    private void handleButtonActionPrev(ActionEvent event) {
        stopVideo();
        changeIndex(-1);
        displayImage();
    }

    @FXML
    private void handleButtonActionPlay(ActionEvent event) {
        stopVideo();
        timeLine = new Timeline(new KeyFrame(Duration.millis(250), (ActionEvent event1) -> {
            boolean stop = changeIndex(1);
            displayImage();
            if (stop) {
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
    private void handleChoiceChange(ActionEvent event) {
        displayImage();
        actualizeCharts(choiceDir.getValue());
    }

    @FXML
    private void handleTextKeyTyped(KeyEvent keyEvent) {
        if (!"0123456789".contains(keyEvent.getCharacter())) {
            keyEvent.consume();
        }
    }

    @FXML
    private void handleButtonActionSave(ActionEvent event) throws IOException, ComputationException {
        final TaskContainer tc = Context.getInstance().getTc();
        final ButtonType map = new ButtonType(Lang.getString("TypeMap"));
        final ButtonType sequence = new ButtonType(Lang.getString("TypeSequence"));
        final ButtonType cancel = new ButtonType(Lang.getString("Cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        final Alert dlg = new Alert(AlertType.CONFIRMATION, null, map, sequence, cancel);
        dlg.setTitle(Lang.getString("Save"));
        dlg.setHeaderText(Lang.getString("ChooseDataType"));
        final Optional<ButtonType> result = dlg.showAndWait();
        final ButtonType button = result.get();
        try {
            if (button == map) {
                final ExportTarget eTarget = determineTarget();
                if (eTarget != null) {
                    switch (eTarget) {
                        case FILE: {
                            Exporter.export(tc, ExportTask.generateMapExport(choiceDir.getValue(), eTarget, new File(NameGenerator.generateMap(tc, index, choiceDir.getValue())), index, getLimits()));
                        }
                        break;
                        case CSV:
                            Exporter.export(tc, ExportTask.generateMapExport(choiceDir.getValue(), eTarget, new File(NameGenerator.generateCsvMap(tc, index, choiceDir.getValue())), index, getLimits()));
                            break;
                        default:
                            Logger.warn("Illegal target - {0}", eTarget);
                            break;
                    }
                }
            } else if (button == sequence) {
                final ExportTask eTask = determineType(tc);
                if (eTask != null) {
                    Exporter.export(tc, determineType(tc));
                }
            }
        } catch (IOException | ComputationException ex) {
            Logger.warn(ex);
        }
    }

    @FXML
    private void handleLimitsAction(ActionEvent event) {
        Context.getInstance().setLimits(getLimits());

        displayImage();
    }

    private double[] getLimits() {
        final double[] result = new double[]{Double.NaN, Double.NaN};                
        try {
            final String min = textMin.getText().replaceAll(",", ".");
            result[0] = Double.valueOf(min);
        } catch (NumberFormatException ex) {
        }
        try {
            final String max = textMax.getText().replaceAll(",", ".");
            result[1] = Double.valueOf(max);
        } catch (NumberFormatException ex) {
        }

        return result;
    }

    @FXML
    private void handleRoundAction(ActionEvent event) {
        final String rndS = textIndex.getText();
        if (rndS != null && !rndS.isEmpty()) {
            try {
                final int rnd = Integer.valueOf(rndS);

                final TaskContainer tc = Context.getInstance().getTc();
                if (rnd < 0) {
                    index = 0;
                } else if (rnd >= tc.getImages().size()) {
                    index = tc.getImages().size() - 1;
                } else {
                    index = rnd;
                }

                final FpsManager fpsM = new FpsManager(tc);
                labelTime.setText(Utils.format(fpsM.getTime(index)).concat(fpsM.getTickUnit()));
                textIndex.setText(String.valueOf(index));

                displayImage();
            } catch (NumberFormatException ex) {
            }
        }
    }

    private ExportTarget determineTarget() {
        final ButtonType img = new ButtonType(Lang.getString("TypeImage"));
        final ButtonType csv = new ButtonType(Lang.getString("TypeCsv"));
        final ButtonType cancel = new ButtonType(Lang.getString("Cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        final Alert dlg = new Alert(AlertType.CONFIRMATION, null, img, csv, cancel);
        dlg.setTitle(Lang.getString("Save"));
        dlg.setHeaderText(Lang.getString("ChooseDataType"));
        final ObjectProperty<ExportTarget> result = new SimpleObjectProperty<>(null);
        dlg.showAndWait().ifPresent((ButtonType t) -> {
            if (t == img) {
                result.setValue(ExportTarget.FILE);
            } else if (t == csv) {
                result.setValue(ExportTarget.CSV);
            }
        });
        return result.getValue();
    }

    private ExportTask determineType(final TaskContainer tc) {
        final ButtonType avi = new ButtonType(Lang.getString("TypeAvi"));
        final ButtonType img = new ButtonType(Lang.getString("TypeImage"));
        final ButtonType csv = new ButtonType(Lang.getString("TypeCsv"));
        final ButtonType cancel = new ButtonType(Lang.getString("Cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        final Alert dlg = new Alert(AlertType.CONFIRMATION, null, avi, img, csv, cancel);
        dlg.setTitle(Lang.getString("Save"));
        dlg.setHeaderText(Lang.getString("ChooseDataType"));
        final ObjectProperty<ExportTask> result = new SimpleObjectProperty<>(null);
        dlg.showAndWait().ifPresent((ButtonType t) -> {
            if (t == img) {
                result.setValue(ExportTask.generateSequenceExport(choiceDir.getValue(), ExportTarget.FILE, new File(NameGenerator.generateSequence(tc, choiceDir.getValue())), getLimits()));
            } else if (t == csv) {
                result.setValue(ExportTask.generateSequenceExport(choiceDir.getValue(), ExportTarget.CSV, new File(NameGenerator.generateSequence(tc, choiceDir.getValue())), getLimits()));
            } else if (t == avi) {
                result.setValue(ExportTask.generateVideoExport(choiceDir.getValue(), new File(NameGenerator.generateSequence(tc, choiceDir.getValue())), getLimits()));
            }
        });
        return result.getValue();
    }

    private void stopVideo() {
        if (timeLine != null) {
            timeLine.stop();
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        Context.getInstance().setLimits(getLimits());
        index = 0;

        final ObservableList<Direction> comboBoxData = FXCollections.observableArrayList();
        comboBoxData.addAll(Direction.values());
        choiceDir.setItems(comboBoxData);
        choiceDir.setConverter(new StringConverter<Direction>() {

            @Override
            public String toString(Direction object) {
                return object.getDescription();
            }

            @Override
            public Direction fromString(String string) {
                return Direction.valueOf(string.substring(0, string.indexOf(SEPARATOR)));
            }
        });

        choiceDir.getSelectionModel().selectFirst();

        image.setPreserveRatio(true);

        image.setOnMouseClicked((MouseEvent t) -> {
            try {
                final Context context = Context.getInstance();

                final int lastX = (int) Math.round(t.getX());
                final int lastY = (int) Math.round(t.getY());

                final int correctedY;
                if (choiceDir.getValue().isStretch()) {
                    final double stretchFactor = TaskContainerUtils.getStretchFactor(context.getTc(), index);
                    correctedY = (int) Math.round(lastY / stretchFactor);
                } else {
                    correctedY = lastY;
                }

                final Map<Direction, double[]> data = context.getPointResult(lastX, correctedY);
                final double[] line = data.get(choiceDir.getValue());
                if (line != null) {
                    final FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("cz/tul/dic/gui/LineResult.fxml"), Lang.getBundle());
                    final Parent root = loader.load();
                    final Stage stage = new Stage();

                    @SuppressWarnings("unchecked")
                    final BorderPane pane = (BorderPane) root.getChildrenUnmodifiable().get(0);
                    @SuppressWarnings("unchecked")
                    final LineChart<Number, Number> chart = (LineChart<Number, Number>) pane.getCenter();
                    chart.setUserData(new Object[]{Context.getInstance().getTc(), lastX, correctedY});

                    final ChartHandler ch = new SinglePointChartHandler(lastX, correctedY, chart, new FpsManager(Context.getInstance().getTc()));
                    charts.put(stage, ch);
                    ch.displayData(choiceDir.getValue());

                    stage.setOnShown((WindowEvent t2) -> {
                        stage.setX(stage.getX() + lastX + 35);
                        stage.setY(stage.getY() + lastY + 10);
                    });
                    stage.setTitle(ch.buildTitle());
                    stage.setScene(new Scene(root));
                    stage
                            .getIcons().add(new javafx.scene.image.Image(MainWindow.class
                                            .getResourceAsStream("logo.png")));
                    stage.show();

                    openTwoPointStrainAnalysis(context);
                }
            } catch (IOException e) {
                Logger.error("Error loading Results dialog from JAR.\n{0}", e);

            } catch (ComputationException ex) {
                java.util.logging.Logger.getLogger(ResultPresenter.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        });

        Image img = new Image(getClass().getClassLoader().getResourceAsStream("cz/tul/dic/gui/resources/play_24x32.png"));
        ImageView imgV = new ImageView(img);
        imgV.setFitWidth(20);
        imgV.setFitHeight(20);
        imgV.setPreserveRatio(true);
        buttonPlay.setGraphic(imgV);

        img = new Image(getClass().getClassLoader().getResourceAsStream("cz/tul/dic/gui/resources/pause_24x32.png"));
        imgV = new ImageView(img);
        imgV.setFitWidth(20);
        imgV.setFitHeight(20);
        imgV.setPreserveRatio(true);
        buttonPause.setGraphic(imgV);

        img = new Image(getClass().getClassLoader().getResourceAsStream("cz/tul/dic/gui/resources/arrow_left_32x32.png"));
        imgV = new ImageView(img);
        imgV.setFitWidth(20);
        imgV.setFitHeight(20);
        imgV.setPreserveRatio(true);
        buttonPrev.setGraphic(imgV);

        img = new Image(getClass().getClassLoader().getResourceAsStream("cz/tul/dic/gui/resources/arrow_right_32x32.png"));
        imgV = new ImageView(img);
        imgV.setFitWidth(20);
        imgV.setFitHeight(20);
        imgV.setPreserveRatio(true);
        buttonNext.setGraphic(imgV);
    }

    private void openTwoPointStrainAnalysis(final Context context) throws ComputationException, IOException {
        SinglePointChartHandler chart1 = null, chart2 = null;

        final Iterator<Stage> it = this.charts.keySet().iterator();
        Stage s;
        while (it.hasNext()) {
            s = it.next();
            if (s.isShowing()) {
                if (charts.get(s) instanceof SinglePointChartHandler) {
                    if (chart1 == null) {
                        chart1 = (SinglePointChartHandler) charts.get(s);
                    } else {
                        chart2 = (SinglePointChartHandler) charts.get(s);
                    }
                } else {
                    it.remove();
                }
            }
        }

        if (chart1 != null && chart2 != null) {
            final int x1 = chart1.x;
            final int y1 = chart1.y;
            final int x2 = chart2.x;
            final int y2 = chart2.y;

            final Map<Direction, double[]> data = context.getComparativeStrain(x1, y1, x2, y2);
            if (data != null) {
                final FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("cz/tul/dic/gui/LineResult.fxml"), Lang.getBundle());
                final Parent root = loader.load();
                final Stage stage = new Stage();

                @SuppressWarnings("unchecked")
                final BorderPane pane = (BorderPane) root.getChildrenUnmodifiable().get(0);
                @SuppressWarnings("unchecked")
                final LineChart<Number, Number> chart = (LineChart<Number, Number>) pane.getCenter();
                chart.setUserData(new Object[]{Context.getInstance().getTc(), x1, y1, x2, y2});

                final ChartHandler ch = new ComparativePointChartHandler(x1, y1, x2, y2, chart, new FpsManager(Context.getInstance().getTc()));
                charts.put(stage, ch);
                ch.displayData(choiceDir.getValue());

                stage.setOnShown((WindowEvent t2) -> {
                    stage.setX(stage.getX() + x1 + 35 + stage.getWidth());
                    stage.setY(stage.getY() + y1 + 10);
                });
                stage.setTitle(ch.buildTitle());
                stage.setScene(new Scene(root));
                stage
                        .getIcons().add(new javafx.scene.image.Image(MainWindow.class
                                        .getResourceAsStream("logo.png")));
                stage.show();
            }
        }
    }

    public void init() {
        final Scene scene = image.getParent().getScene();
        final Stage mainStage = (Stage) scene.getWindow();
        mainStage.setOnCloseRequest((WindowEvent event) -> {
            charts.keySet().stream().forEach((s) -> {
                s.close();
            });
            charts.clear();
        });
        displayImage();
    }

    private void actualizeCharts(final Direction dir) {
        final Iterator<Entry<Stage, ChartHandler>> it = charts.entrySet().iterator();
        Entry<Stage, ChartHandler> e;
        Stage s;
        ChartHandler ch;
        while (it.hasNext()) {
            e = it.next();
            s = e.getKey();
            if (s.isShowing()) {
                try {
                    ch = e.getValue();
                    ch.displayData(dir);
                    s.setTitle(ch.buildTitle());
                } catch (ComputationException ex) {
                    Logger.warn(ex, "Error obtaining line data for chart");
                }
            } else {
                it.remove();

            }
        }
    }

    private static class SinglePointChartHandler implements ChartHandler {

        private final int x, y;
        private final FpsManager fpsM;
        private final LineChart<Number, Number> chart;
        private Direction dir;

        public SinglePointChartHandler(int x, int y, LineChart<Number, Number> chart, FpsManager fpsM) {
            this.x = x;
            this.y = y;
            this.chart = chart;
            this.fpsM = fpsM;

            chart.setLegendVisible(false);
        }

        @Override
        public void displayData(final Direction dir) throws ComputationException {
            final double tickUnit = fpsM.getTickLength();
            final double[] line = Context.getInstance().getPointResult(x, y).get(dir);
            this.dir = dir;

            final double width = PREF_SIZE_W_BASE + line.length * PREF_SIZE_W_M;
            chart.setPrefSize(width, PREF_SIZE_H);

            chart.getData().clear();

            final XYChart.Series<Number, Number> series = new XYChart.Series<>();
            double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
            for (int i = 0; i < line.length; i++) {
                series.getData().add(new XYChart.Data<>((i + 1) * tickUnit, line[i]));

                if (line[i] < min) {
                    min = line[i];
                }
                if (line[i] > max) {
                    max = line[i];
                }

            }
            chart.getData().add(series);

            NumberAxis axis = (NumberAxis) chart.getXAxis();
            axis.setTickUnit(tickUnit);
            axis.setLowerBound(0);
            axis.setUpperBound(line.length * tickUnit);
            axis.setLabel(fpsM.buildTimeDescription());

            axis = (NumberAxis) chart.getYAxis();
            axis.setLabel(dir.getDescription());
            if (Double.compare(min, max) == 0) {
                axis.setAutoRanging(false);
                axis.setTickUnit(1);
                axis.setLowerBound(min - 1);
                axis.setUpperBound(max + 1);
            } else {
                axis.setAutoRanging(true);
            }
        }

        @Override
        public String buildTitle() {
            return dir.toString().concat(" : ")
                    .concat(Integer.toString(x)).concat(";").concat(Integer.toString(y));
        }

    }

    private static class ComparativePointChartHandler implements ChartHandler {

        private final int x1, y1, x2, y2;
        private final FpsManager fpsM;
        private final LineChart<Number, Number> chart;
        private Direction dir;

        public ComparativePointChartHandler(int x1, int y1, int x2, int y2, LineChart<Number, Number> chart, FpsManager fpsM) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.chart = chart;
            this.fpsM = fpsM;

            chart.setLegendVisible(false);
        }

        @Override
        public void displayData(final Direction dir) throws ComputationException {
            final double tickUnit = fpsM.getTickLength();

            // TODO
            switch (dir) {
                case DABS:
                case R_DABS:
                    this.dir = Direction.EABS;
                    break;
                case D_DABS:
                    this.dir = Direction.D_EABS;
                    break;
                case DX:
                case R_DX:
                    this.dir = Direction.EXX;
                    break;
                case D_DX:
                    this.dir = Direction.D_EXX;
                    break;
                case DY:
                case R_DY:
                    this.dir = Direction.EYY;
                    break;
                case D_DY:
                    this.dir = Direction.D_EYY;
                    break;
                default:
                    this.dir = dir;
            }

            final double[] line = Context.getInstance().getComparativeStrain(x1, y1, x2, y2).get(this.dir);

            final double width = PREF_SIZE_W_BASE + line.length * PREF_SIZE_W_M;
            chart.setPrefSize(width, PREF_SIZE_H);

            chart.getData().clear();

            final XYChart.Series<Number, Number> series = new XYChart.Series<>();
            double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
            for (int i = 0; i < line.length; i++) {
                series.getData().add(new XYChart.Data<>((i + 1) * tickUnit, line[i]));

                if (line[i] < min) {
                    min = line[i];
                }
                if (line[i] > max) {
                    max = line[i];
                }

            }
            chart.getData().add(series);

            NumberAxis axis = (NumberAxis) chart.getXAxis();
            axis.setTickUnit(tickUnit);
            axis.setLowerBound(0);
            axis.setUpperBound(line.length * tickUnit);
            axis.setLabel(fpsM.buildTimeDescription());

            axis = (NumberAxis) chart.getYAxis();
            axis.setLabel(this.dir.getDescription());
            if (Double.compare(min, max) == 0) {
                axis.setAutoRanging(false);
                axis.setTickUnit(1);
                axis.setLowerBound(min - 1);
                axis.setUpperBound(max + 1);
            } else {
                axis.setAutoRanging(true);
            }
        }

        @Override
        public String buildTitle() {
            return dir.toString().concat(" : ")
                    .concat(Integer.toString(x1)).concat(";").concat(Integer.toString(y1))
                    .concat(" vs. ")
                    .concat(Integer.toString(x2)).concat(";").concat(Integer.toString(y2));
        }

    }

    private static interface ChartHandler {

        public void displayData(final Direction dir) throws ComputationException;

        public String buildTitle();
    }

}
