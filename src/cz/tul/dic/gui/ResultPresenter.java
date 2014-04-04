package cz.tul.dic.gui;

import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.output.Direction;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.Axis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import org.controlsfx.dialog.Dialog;

public class ResultPresenter implements Initializable {

    private static final int PREF_SIZE_W_BASE = 30;
    private static final int PREF_SIZE_W_M = 5;
    private static final int PREF_SIZE_H = 30;

    @FXML
    private ComboBox<Direction> choiceDir;
    @FXML
    private TextField textIndex;
    @FXML
    private ImageView image;
    private int index;
    private Timeline timeLine;

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

        final int roundCount = TaskContainerUtils.getRoundCount(Context.getInstance().getTc());
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
        final BufferedImage i = Context.getInstance().getMapResult(index, choiceDir.getValue());
        final Image img = SwingFXUtils.toFXImage(i, null);

        image.setImage(img);
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
            final int x = (int) Math.round(t.getX());
            final int y = (int) Math.round(t.getY());
            final double[] line = Context.getInstance().getLineResult(x, y, choiceDir.getValue());

            final Axis xAxis = new NumberAxis(0, line.length, 1);
            final Axis yAxis = new NumberAxis();
            final LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
            chart.setLegendVisible(false);
            final double width = PREF_SIZE_W_BASE + line.length * PREF_SIZE_W_M;
            chart.setPrefSize(width, PREF_SIZE_H);

            XYChart.Series series = new XYChart.Series();
            double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
            for (int i = 0; i < line.length; i++) {
                series.getData().add(new XYChart.Data(i, line[i]));
                if (line[i] < min) {
                    min = line[i];
                }
                if (line[i] > max) {
                    max = line[i];
                }
            }
            chart.getData().add(series);

            series = new XYChart.Series();
            series.getData().add(new XYChart.Data(index, min));
            series.getData().add(new XYChart.Data(index, max));
            chart.getData().add(series);

            final Dialog d = new Dialog(image, String.format("%1d : %2d", x, y));
            d.contentProperty().setValue(chart);
            d.mastheadProperty().setValue(null);
            d.setResizable(true);

            final Window w = d.getWindow();
            w.setOnShown((WindowEvent t2) -> {
                w.setX(w.getX() + x + width / 2 + 10);
                w.setY(w.getY() + y + PREF_SIZE_H / 2 + 10);
            });            

            d.show();            
        });
    }

}
