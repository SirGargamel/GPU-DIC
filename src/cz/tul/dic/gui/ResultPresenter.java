package cz.tul.dic.gui;

import cz.tul.dic.ComputationException;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.gui.lang.Lang;
import cz.tul.dic.output.Direction;
import cz.tul.dic.output.ExportTarget;
import cz.tul.dic.output.ExportTask;
import cz.tul.dic.output.Exporter;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.logging.Level;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
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
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.Dialogs;
import org.pmw.tinylog.Logger;

public class ResultPresenter implements Initializable {

    private static final int PREF_SIZE_W_BASE = 30;
    private static final int PREF_SIZE_W_M = 5;
    private static final int PREF_SIZE_H = 30;
    private static final String EXT_SEQUENCE = ".avi";
    private static final String EXT_IMAGE = ".bmp";
    private static final String EXT_CSV = ".avi";
    private static final String DELIMITER = "-";

    @FXML
    private ComboBox<Direction> choiceDir;
    @FXML
    private TextField textIndex;
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
    private int lastX, lastY;
    private final Map<Stage, ChartHandler> charts;

    public ResultPresenter() {
        charts = new HashMap<>();
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

        final int roundCount = TaskContainerUtils.getMaxRoundCount(Context.getInstance().getTc());
        if (index < 0) {
            index = roundCount - 1;
            result = true;
        } else if (index >= roundCount) {
            index = 0;
            result = true;
        }

        textIndex.setText(Integer.toString(index));
        return result;
    }

    private void displayImage() {
        try {
            final BufferedImage i = Context.getInstance().getMapResult(index, choiceDir.getValue());
            if (i != null) {
                final Image img = SwingFXUtils.toFXImage(i, null);
                image.setImage(img);
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
        event.consume();
    }

    @FXML
    private void handleTextActionIndex(ActionEvent event) {
        try {
            int newIndex = Integer.valueOf(textIndex.getText());
            changeIndex(newIndex - index);
        } catch (NumberFormatException ex) {
            textIndex.setText(Integer.toString(index));
        }
        event.consume();
    }

    @FXML
    private void handleTextKeyTyped(KeyEvent keyEvent) {
        if (!"0123456789".contains(keyEvent.getCharacter())) {
            keyEvent.consume();
        }
    }

    @FXML
    private void handleButtonActionSave(ActionEvent event) throws IOException, ComputationException {
        final String c1 = Lang.getString("TypeMap");
        final String t1 = Lang.getString("TypeMapD");
        final String c2 = Lang.getString("TypeLine");
        final String t2 = Lang.getString("TypeLineD");
        final String c3 = Lang.getString("TypeSequence");
        final String t3 = Lang.getString("TypeSequenceD");
        final Action a = Dialogs.create()
                .masthead(null)
                .title(Lang.getString("Save"))
                .message(Lang.getString("ChooseDataType"))
                .showCommandLinks(null, new Dialogs.CommandLink(c1, t1), new Dialogs.CommandLink(c2, t2), new Dialogs.CommandLink(c3, t3));
        final String val = a.textProperty().get();
        final TaskContainer tc = Context.getInstance().getTc();
        final String baseName = tc.getParameter(TaskParameter.IN).toString();
        if (val.equals(c1)) {
            final ExportTarget et = determineTarget();
            if (et != null) {
                final String fileName = baseName.concat(DELIMITER).concat(choiceDir.getValue().toString()).concat(DELIMITER).concat(Integer.toString(index)).concat(EXT_IMAGE);
                Exporter.export(tc, ExportTask.generateMapExport(choiceDir.getValue(), ExportTarget.FILE, new File(fileName), index));
            }
        } else if (val.equals(c2)) {
            final ExportTarget et = determineTarget();
            if (et != null) {
                final String fileName = baseName.concat(DELIMITER).concat(choiceDir.getValue().toString()).concat(DELIMITER).concat(Integer.toString(index)).concat(EXT_CSV);
                Exporter.export(tc, ExportTask.generateLineExport(choiceDir.getValue(), ExportTarget.CSV, new File(fileName), lastX, lastY));
            }
        } else if (val.equals(c3)) {
            final String fileName = baseName.concat(DELIMITER).concat(choiceDir.getValue().toString()).concat(EXT_SEQUENCE);
            Exporter.export(tc, ExportTask.generateSequenceExport(choiceDir.getValue(), ExportTarget.FILE, new File(fileName)));
        }
    }

    private ExportTarget determineTarget() {
        final String c1 = Lang.getString("TypeImage");
        final String c2 = Lang.getString("TypeCsv");
        final Action a = Dialogs.create()
                .title(Lang.getString("Save"))
                .message(Lang.getString("ChooseDataType"))
                .showCommandLinks(null, new Dialogs.CommandLink(c1, null), new Dialogs.CommandLink(c2, null));
        final String val = a.textProperty().get();
        final ExportTarget result;
        if (val.equals(c1)) {
            result = ExportTarget.FILE;
        } else if (val.equals(c2)) {
            result = ExportTarget.CSV;
        } else {
            result = null;
        }
        return result;
    }

    private void stopVideo() {
        if (timeLine != null) {
            timeLine.stop();
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        index = 0;

        final ObservableList<Direction> comboBoxData = FXCollections.observableArrayList();
        comboBoxData.addAll(Direction.values());
        choiceDir.setItems(comboBoxData);

        choiceDir.getSelectionModel().selectFirst();

        image.setPreserveRatio(true);
        image.setFitHeight(0);
        image.setFitWidth(0);

        image.setOnMouseClicked((MouseEvent t) -> {
            try {
                lastX = (int) Math.round(t.getX());
                lastY = (int) Math.round(t.getY());
                final double[] line = Context.getInstance().getLineResult(lastX, lastY, choiceDir.getValue());
                if (line != null) {
                    final Parent root = FXMLLoader.load(getClass().getClassLoader().getResource("cz/tul/dic/gui/LineResult.fxml"), Lang.getBundle());
                    final Stage stage = new Stage();

                    @SuppressWarnings("unchecked")
                    final LineChart<Number, Number> chart = (LineChart<Number, Number>) root.getChildrenUnmodifiable().get(0);

                    final ChartHandler ch = new ChartHandler(lastX, lastY, chart);
                    charts.put(stage, ch);
                    ch.displayData(choiceDir.getValue());

                    stage.setOnShown((WindowEvent t2) -> {
                        stage.setX(stage.getX() + lastX + 35);
                        stage.setY(stage.getY() + lastY + 10);
                    });

                    stage.setTitle(choiceDir.getValue().toString().concat(" : ").concat(Integer.toString(lastX).concat(";").concat(Integer.toString(lastY))));
                    stage.setScene(new Scene(root));
                    stage.setIconified(false);
                    stage.show();
                }
            } catch (IOException e) {
                Logger.error("Error loading Results dialog from JAR.\n{0}", e);
            } catch (ComputationException ex) {
                java.util.logging.Logger.getLogger(ResultPresenter.class.getName()).log(Level.SEVERE, null, ex);
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

    private void actualizeCharts(final Direction dir) {
        final Iterator<Entry<Stage, ChartHandler>> it = charts.entrySet().iterator();
        Entry<Stage, ChartHandler> e;
        while (it.hasNext()) {
            e = it.next();
            if (e.getKey().isShowing()) {
                try {
                    e.getValue().displayData(dir);
                } catch (ComputationException ex) {
                    Logger.warn(ex, "Error obtaining line data for chart");
                }
            } else {
                it.remove();
            }
        }
    }

    private static class ChartHandler {

        private final int x, y;
        private final LineChart<Number, Number> chart;

        public ChartHandler(int x, int y, LineChart<Number, Number> chart) {
            this.x = x;
            this.y = y;
            this.chart = chart;

            chart.setLegendVisible(false);
        }

        public void displayData(final Direction dir) throws ComputationException {
            final double[] line = Context.getInstance().getLineResult(x, y, dir);

            final double width = PREF_SIZE_W_BASE + line.length * PREF_SIZE_W_M;
            chart.setPrefSize(width, PREF_SIZE_H);

            chart.getData().clear();

            final XYChart.Series<Number, Number> series = new XYChart.Series<>();
            double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
            for (int i = 0; i < line.length; i++) {
                series.getData().add(new XYChart.Data<>(i + 1, line[i]));

                if (line[i] < min) {
                    min = line[i];
                }
                if (line[i] > max) {
                    max = line[i];
                }

            }
            chart.getData().add(series);

            NumberAxis axis = (NumberAxis) chart.getXAxis();
            axis.setTickUnit(1);
            axis.setLowerBound(0);
            axis.setUpperBound(line.length + 1);

            axis = (NumberAxis) chart.getYAxis();
            if (Double.compare(min, max) == 0) {
                axis.setAutoRanging(false);
                axis.setTickUnit(1);
                axis.setLowerBound(min - 1);
                axis.setUpperBound(min + 1);
            } else {
                axis.setAutoRanging(true);
            }
        }
    }

}
