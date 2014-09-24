package cz.tul.dic.gui;

import cz.tul.dic.ComputationException;
import cz.tul.dic.FpsManager;
import cz.tul.dic.Utils;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;
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
import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.Dialogs;
import org.pmw.tinylog.Logger;

public class ResultPresenter implements Initializable {

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
    private int lastX, lastY;
    private final Map<Stage, ChartHandler> charts;
    private boolean inited;

    public ResultPresenter() {
        charts = new HashMap<>();
        inited = false;
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

        final FpsManager fpsM = new FpsManager((int) tc.getParameter(TaskParameter.FPS));
        labelTime.setText(Utils.format(fpsM.getTime(index)).concat(fpsM.getTickUnit()));

        return result;
    }

    private void displayImage() {
        init();
        try {
            final BufferedImage i = Context.getInstance().getMapResult(index, choiceDir.getValue());
            if (i != null) {
                final Scene s = image.getParent().getScene();
                if (s != null) {
                    double width = Math.max(MIN_WIDTH, image.getParent().getBoundsInLocal().getWidth() + EXTRA_WIDTH);
                    s.getWindow().setWidth(width);
                    s.getWindow().setHeight(image.getParent().getBoundsInLocal().getHeight() + EXTRA_HEIGHT);
                }

                final Image img = SwingFXUtils.toFXImage(i, null);
                image.setImage(img);
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
        event.consume();
    }

    @FXML
    private void handleTextActionIndex(ActionEvent event) {
        try {
            int newIndex = Integer.parseInt(textIndex.getText());
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
        final String c2 = Lang.getString("TypeSequence");
        final String t2 = Lang.getString("TypeSequenceD");
        final Action a = Dialogs.create()
                .masthead(null)
                .title(Lang.getString("Save"))
                .message(Lang.getString("ChooseDataType"))
                .showCommandLinks(null, new Dialogs.CommandLink(c1, t1), new Dialogs.CommandLink(c2, t2));
        final String val = a.textProperty().get();
        final TaskContainer tc = Context.getInstance().getTc();
        if (val.equals(c1)) {
            final ExportTarget et = determineTarget();
            if (et != null) {
                switch (et) {
                    case FILE:
                        Exporter.export(tc, ExportTask.generateMapExport(choiceDir.getValue(), et, new File(NameGenerator.generateMap(tc, index, choiceDir.getValue())), index));
                        break;
                    case CSV:
                        Exporter.export(tc, ExportTask.generateMapExport(choiceDir.getValue(), et, new File(NameGenerator.generateCsvMap(tc, index, choiceDir.getValue())), index));
                        break;
                    default:
                        Logger.warn("Illegal target - {0}", et);
                        break;
                }
            }
        } else if (val.equals(c2)) {
            Exporter.export(tc, ExportTask.generateSequenceExport(choiceDir.getValue(), ExportTarget.FILE, new File(NameGenerator.generateSequence(tc, choiceDir.getValue())), determineType()));
        }
    }

    @FXML
    private void handleLimitsAction(ActionEvent event) {
        final String minS = textMin.getText();
        final String maxS = textMax.getText();
        if (minS != null && !minS.isEmpty() && maxS != null && !maxS.isEmpty()) {
            try {
                final double min = Double.valueOf(minS);
                final double max = Double.valueOf(maxS);
                Context.getInstance().setLimits(min, max);                
            } catch (NumberFormatException ex) {
                Context.getInstance().setLimits(Double.NaN, Double.NaN);
            }
        } else {
            Context.getInstance().setLimits(Double.NaN, Double.NaN);
        }

        displayImage();
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

    private int determineType() {
        final String c1 = Lang.getString("TypeAvi");
        final String c2 = Lang.getString("TypeImage");
        final String c3 = Lang.getString("TypeCsv");
        final Action a = Dialogs.create()
                .title(Lang.getString("Save"))
                .message(Lang.getString("ChooseSequenceType"))
                .showCommandLinks(null, new Dialogs.CommandLink(c1, null), new Dialogs.CommandLink(c2, null), new Dialogs.CommandLink(c3, null));
        final String val = a.textProperty().get();
        final int result;
        if (val.equals(c1)) {
            result = ExportTask.EXPORT_SEQUENCE_AVI;
        } else if (val.equals(c2)) {
            result = ExportTask.EXPORT_SEQUENCE_BMP;
        } else {
            result = ExportTask.EXPORT_SEQUENCE_CSV;
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
        lastX = -1;

        final ObservableList<Direction> comboBoxData = FXCollections.observableArrayList();
        comboBoxData.addAll(Direction.values());
        choiceDir.setItems(comboBoxData);

        choiceDir.getSelectionModel().selectFirst();

        image.setPreserveRatio(true);

        image.setOnMouseClicked((MouseEvent t) -> {
            try {
                final Context context = Context.getInstance();
                final int newX = (int) Math.round(t.getX());
                final int newY = (int) Math.round(t.getY());

                if (lastX != -1) {
                    final Map<Direction, double[]> data = context.getComparativeStrain(lastX, lastY, newX, newY);
                    if (data != null) {
                        final FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("cz/tul/dic/gui/LineResult.fxml"), Lang.getBundle());
                        final Parent root = loader.load();
                        final Stage stage = new Stage();

                        @SuppressWarnings("unchecked")
                        final BorderPane pane = (BorderPane) root.getChildrenUnmodifiable().get(0);
                        @SuppressWarnings("unchecked")
                        final LineChart<Number, Number> chart = (LineChart<Number, Number>) pane.getCenter();
                        chart.setUserData(new Object[]{Context.getInstance().getTc(), lastX, lastY, newX, newY});

                        final ChartHandler ch = new ComparativePointChartHandler(lastX, lastY, newX, newY, chart, (int) Context.getInstance().getTc().getParameter(TaskParameter.FPS));
                        charts.put(stage, ch);
                        ch.displayData(choiceDir.getValue());

                        final LineResult controller = loader.getController();
                        controller.init();

                        stage.setOnShown((WindowEvent t2) -> {
                            stage.setX(stage.getX() + lastX + 35);
                            stage.setY(stage.getY() + lastY + 50);
                        });
                        stage.setTitle(ch.buildTitle());
                        stage.setScene(new Scene(root));
                        stage.setIconified(false);
                        stage.show();
                    }
                }

                lastX = newX;
                lastY = newY;
                final Map<Direction, double[]> data = context.getPointResult(lastX, lastY);
                final double[] line = data.get(choiceDir.getValue());
                if (line != null) {
                    final FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("cz/tul/dic/gui/LineResult.fxml"), Lang.getBundle());
                    final Parent root = loader.load();
                    final Stage stage = new Stage();

                    @SuppressWarnings("unchecked")
                    final BorderPane pane = (BorderPane) root.getChildrenUnmodifiable().get(0);
                    @SuppressWarnings("unchecked")
                    final LineChart<Number, Number> chart = (LineChart<Number, Number>) pane.getCenter();
                    chart.setUserData(new Object[]{Context.getInstance().getTc(), lastX, lastY});

                    final LineResult controller = loader.getController();
                    controller.init();

                    final ChartHandler ch = new SinglePointChartHandler(lastX, lastY, chart, (int) Context.getInstance().getTc().getParameter(TaskParameter.FPS));
                    charts.put(stage, ch);
                    ch.displayData(choiceDir.getValue());

                    stage.setOnShown((WindowEvent t2) -> {
                        stage.setX(stage.getX() + lastX + 35);
                        stage.setY(stage.getY() + lastY + 10);
                    });
                    stage.setTitle(ch.buildTitle());
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

    private void init() {
        if (!inited) {
            final Scene scene = image.getParent().getScene();
            if (scene != null) {
                final Stage mainStage = (Stage) scene.getWindow();
                mainStage.setResizable(false);
                mainStage.setOnCloseRequest((WindowEvent event) -> {
                    charts.keySet().stream().forEach((s) -> {
                        s.close();
                    });
                    charts.clear();
                });

                inited = true;
            }
        }

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

        private final int x, y, fps;
        private final LineChart<Number, Number> chart;
        private Direction dir;

        public SinglePointChartHandler(int x, int y, LineChart<Number, Number> chart, int fps) {
            this.x = x;
            this.y = y;
            this.chart = chart;
            this.fps = fps;

            chart.setLegendVisible(false);
        }

        @Override
        public void displayData(final Direction dir) throws ComputationException {
            final FpsManager fpsM = new FpsManager(fps);
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

        private final int x1, y1, x2, y2, fps;
        private final LineChart<Number, Number> chart;
        private Direction dir;

        public ComparativePointChartHandler(int x1, int y1, int x2, int y2, LineChart<Number, Number> chart, int fps) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.chart = chart;
            this.fps = fps;

            chart.setLegendVisible(false);
        }

        @Override
        public void displayData(final Direction dir) throws ComputationException {
            final FpsManager fpsM = new FpsManager(fps);
            final double tickUnit = fpsM.getTickLength();

            // TODO
            switch (dir) {
                case Dabs:
                case rDabs:
                    this.dir = Direction.Eabs;
                    break;
                case dDabs:
                    this.dir = Direction.dEabs;
                    break;
                case Dx:
                case rDx:
                    this.dir = Direction.Exx;
                    break;
                case dDx:
                    this.dir = Direction.dExx;
                    break;
                case Dy:
                case rDy:
                    this.dir = Direction.Eyy;
                    break;
                case dDy:
                    this.dir = Direction.dEyy;
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
