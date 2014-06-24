package cz.tul.dic.gui;

import cz.tul.dic.data.roi.RoiType;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;

public class ROISelector implements Initializable {

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
        event.consume();
    }

    @FXML
    private void handleButtonActionPrev(ActionEvent event) {
        imagePane.previousImage();
        event.consume();
    }

    @FXML
    private void handleButtonActionDel(ActionEvent event) {
        imagePane.deleteAllRois();
        event.consume();
    }

    @FXML
    private void init(MouseEvent event) {
        if (!displayed) {
            imagePane.displayImage();
            displayed = true;
        }
        Context.getInstance().getTc().addObserver(imagePane);
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
