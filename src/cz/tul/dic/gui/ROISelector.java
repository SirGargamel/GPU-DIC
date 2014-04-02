package cz.tul.dic.gui;

import cz.tul.dic.data.roi.RoiType;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ChoiceBox;
import javafx.scene.input.MouseEvent;

public class ROISelector implements Initializable {

    @FXML
    private EditableInputPresenter imagePane;
    @FXML
    private ChoiceBox<RoiType> choiceRoi;
    boolean displayed;

    @FXML
    private void handleButtonActionNext(ActionEvent event) {
        imagePane.nextImage();
        event.consume();
    }

    @FXML
    private void handleButtonActionPrev(ActionEvent event) {
//        saveRois();
//        changeIndex(-1);
//        displayImage();
//        actualShape = null;
        imagePane.previousImage();
        event.consume();
    }

    @FXML
    private void handleButtonActionDel(ActionEvent event) {
//        rois.clear();
//        imagePane.getChildren().clear();
//        actualShape = null;
        imagePane.deleteAllRois();
        event.consume();
    }

    @FXML
    private void init(MouseEvent event) {
        if (!displayed) {
            imagePane.displayImage();
            displayed = true;
        }
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
    }

}
