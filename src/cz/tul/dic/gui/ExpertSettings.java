package cz.tul.dic.gui;

import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.data.task.splitter.TaskSplit;
import cz.tul.dic.engine.opencl.KernelType;
import cz.tul.dic.engine.opencl.interpolation.Interpolation;
import cz.tul.dic.generators.facet.FacetGeneratorMode;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

/**
 * FXML Controller class
 *
 * @author Petr Jecmen
 */
public class ExpertSettings implements Initializable {

    @FXML
    private ComboBox<FacetGeneratorMode> comboFGMode;
    @FXML
    private ComboBox<TaskSplit> comboTSVariant;
    @FXML
    private ComboBox<KernelType> comboKernel;
    @FXML
    private ComboBox<Interpolation> comboInterpolation;
    @FXML
    private TextField textFGSpacing;
    @FXML
    private TextField textTSValue;

    @FXML
    private void handleButtonActionOk(ActionEvent event) {
        final TaskContainer tc = Context.getInstance().getTc();
        if (tc != null) {
            tc.setParameter(TaskParameter.FACET_GENERATOR_MODE, comboFGMode.getValue());
            tc.setParameter(TaskParameter.TASK_SPLIT_VARIANT, comboTSVariant.getValue());
            tc.setParameter(TaskParameter.KERNEL, comboKernel.getValue());
            tc.setParameter(TaskParameter.INTERPOLATION, comboInterpolation.getValue());
            tc.setParameter(TaskParameter.FACET_GENERATOR_SPACING, Integer.valueOf(textFGSpacing.getText()));
            tc.setParameter(TaskParameter.TASK_SPLIT_VALUE, Integer.valueOf(textTSValue.getText()));
        }
        closeWindow();
    }

    private void closeWindow() {
        final Stage stage = (Stage) comboFGMode.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleButtonActionCancel(ActionEvent event) {
        closeWindow();
    }

    @FXML
    private void handleTextKeyTyped(KeyEvent keyEvent) {
        if (!"0123456789".contains(keyEvent.getCharacter())) {
            keyEvent.consume();
        }
    }

    /**
     * Initializes the controller class.
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        final ObservableList<FacetGeneratorMode> comboBoxData = FXCollections.observableArrayList();
        comboBoxData.addAll(FacetGeneratorMode.values());
        comboFGMode.setItems(comboBoxData);
        final ObservableList<TaskSplit> comboBoxData2 = FXCollections.observableArrayList();
        comboBoxData2.addAll(TaskSplit.values());
        comboTSVariant.setItems(comboBoxData2);
        final ObservableList<KernelType> comboBoxData3 = FXCollections.observableArrayList();
        comboBoxData3.addAll(KernelType.values());
        comboKernel.setItems(comboBoxData3);
        final ObservableList<Interpolation> comboBoxData4 = FXCollections.observableArrayList();
        comboBoxData4.addAll(Interpolation.values());
        comboInterpolation.setItems(comboBoxData4);

        final TaskContainer tc = Context.getInstance().getTc();
        if (tc == null) {
            comboFGMode.getSelectionModel().selectFirst();
            comboTSVariant.getSelectionModel().selectFirst();
            comboKernel.getSelectionModel().selectFirst();
        } else {
            Object o = tc.getParameter(TaskParameter.FACET_GENERATOR_MODE);
            if (o != null) {
                comboFGMode.getSelectionModel().select((FacetGeneratorMode) o);
            } else {
                comboFGMode.getSelectionModel().selectFirst();
            }

            o = tc.getParameter(TaskParameter.TASK_SPLIT_VARIANT);
            if (o != null) {
                comboTSVariant.getSelectionModel().select((TaskSplit) o);
            } else {
                comboTSVariant.getSelectionModel().selectFirst();
            }

            o = tc.getParameter(TaskParameter.KERNEL);
            if (o != null) {
                comboKernel.getSelectionModel().select((KernelType) o);
            } else {
                comboKernel.getSelectionModel().selectFirst();
            }

            o = tc.getParameter(TaskParameter.FACET_GENERATOR_SPACING);
            if (o != null) {
                textFGSpacing.setText(o.toString());
            }
            o = tc.getParameter(TaskParameter.TASK_SPLIT_VALUE);
            if (o != null) {
                textTSValue.setText(o.toString());
            }

            o = tc.getParameter(TaskParameter.INTERPOLATION);
            if (o != null) {
                comboInterpolation.getSelectionModel().select((Interpolation) o);
            } else {
                comboInterpolation.getSelectionModel().selectFirst();
            }
        }
    }

}
