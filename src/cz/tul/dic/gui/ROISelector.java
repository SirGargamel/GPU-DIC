package cz.tul.dic.gui;

import cz.tul.dic.data.roi.RoiType;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class ROISelector implements Initializable {

    private static final int EXTRA_WIDTH = 115;
    private static final int EXTRA_HEIGHT = 66;    
    @FXML
    private EditableInputPresenter imagePane;
    @FXML
    private ChoiceBox<RoiType> choiceRoi;
    @FXML
    private Button buttonPrev;
    @FXML
    private Button buttonNext;
    boolean displayed;

    @FXML
    private void handleButtonActionNext(ActionEvent event) {
        imagePane.nextImage();
        resize();
        event.consume();
    }

    @FXML
    private void handleButtonActionPrev(ActionEvent event) {
        imagePane.previousImage();
        resize();
        event.consume();
    }

    @FXML
    private void handleButtonActionDel(ActionEvent event) {
        imagePane.deleteAllRois();
        event.consume();
    }

    private void resize() {
        final Scene s = imagePane.getParent().getScene();
        if (s != null) {            
            s.getWindow().setWidth(imagePane.getBoundsInLocal().getWidth() + EXTRA_WIDTH);
            s.getWindow().setHeight(imagePane.getBoundsInLocal().getHeight() + EXTRA_HEIGHT);
        }
    }

    @FXML
    private void init(MouseEvent event) {
        if (!displayed) {
            Stage s = (Stage) imagePane.getScene().getWindow();
            s.setResizable(false);
            imagePane.displayImage();
            displayed = true;            
        }
        Context.getInstance().getTc().addObserver(imagePane);
        imagePane.getScene().getWindow().setOnCloseRequest(new EventHandler<WindowEvent>() {

            @Override
            public void handle(WindowEvent event) {
                imagePane.saveRois();
            }
        });

        event.consume();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        ObservableList<RoiType> comboBoxData = FXCollections.observableArrayList();
        comboBoxData.addAll(RoiType.values());
        choiceRoi.setItems(comboBoxData);
        choiceRoi.getSelectionModel().selectFirst();

        imagePane.initialize(url, rb);
        imagePane.setRoiTypeProperty(choiceRoi.valueProperty());

        displayed = false;

        Image img = new Image(getClass().getClassLoader().getResourceAsStream("cz/tul/dic/gui/resources/arrow_left_32x32.png"));
        ImageView image = new ImageView(img);
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
    }

}
