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
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.util.Duration;

public class ResultPresenter implements Initializable {

    @FXML
    private AnchorPane imagePane;
    @FXML
    private ComboBox<Direction> choiceDir;
    @FXML
    private TextField textIndex;
    private int index;
    private Timeline timeLine;
    private Direction dir;

    @FXML
    private void handleButtonActionNext(ActionEvent event) {
        stopVideo();
        changeIndex(1);
        displayImage();
        event.consume();
    }

    private void changeIndex(int change) {
        index += change;

        final int roundCount = TaskContainerUtils.getRoundCount(Context.getInstance().getTc());
        if (index < 0) {
            index = roundCount - 1;
        } else if (index >= roundCount) {
            index = 0;
        }

        textIndex.setText(Integer.toString(index));
    }

    private void displayImage() {
        final BufferedImage i = Context.getInstance().getMapResult(index, dir);
        final Image img = SwingFXUtils.toFXImage(i, null);

        final Background b = new Background(new BackgroundImage(img, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER, BackgroundSize.DEFAULT));
        imagePane.setBackground(b);
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
        timeLine = new Timeline(new KeyFrame(Duration.millis(250), new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {
                changeIndex(1);
                displayImage();
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
        dir = choiceDir.getSelectionModel().getSelectedItem();        
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

        ObservableList<Direction> comboBoxData = FXCollections.observableArrayList();
        comboBoxData.addAll(Direction.values());
        choiceDir.setItems(comboBoxData);

        dir = Direction.ABS;
        choiceDir.getSelectionModel().selectFirst();
    }

}
