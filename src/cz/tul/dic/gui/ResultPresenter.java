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
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

public class ResultPresenter implements Initializable {
    
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
            index = Integer.valueOf(textIndex.getText());
        } catch (NumberFormatException ex) {
            textIndex.setText(Integer.toString(index));
        }
        event.consume();
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
    }

}
