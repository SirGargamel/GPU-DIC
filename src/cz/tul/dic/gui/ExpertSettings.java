package cz.tul.dic.gui;

import cz.tul.dic.ComputationException;
import cz.tul.dic.data.task.DefaultValues;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.data.task.splitter.TaskSplitMethod;
import cz.tul.dic.engine.CumulativeResultsCounter;
import cz.tul.dic.engine.opencl.KernelType;
import cz.tul.dic.engine.opencl.interpolation.Interpolation;
import cz.tul.dic.engine.strain.StrainEstimation;
import cz.tul.dic.generators.facet.FacetGeneratorMethod;
import cz.tul.dic.gui.lang.Lang;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import org.controlsfx.dialog.Dialogs;

/**
 * FXML Controller class
 *
 * @author Petr Jecmen
 */
public class ExpertSettings implements Initializable {

    private static final String ROUND_SPLITTER = ",";
    @FXML
    private ComboBox<FacetGeneratorMethod> comboFGMode;
    @FXML
    private ComboBox<TaskSplitMethod> comboTSVariant;
    @FXML
    private ComboBox<KernelType> comboKernel;
    @FXML
    private ComboBox<Interpolation> comboInterpolation;
    @FXML
    private TextField textFGSpacing;
    @FXML
    private TextField textTSValue;
    @FXML
    private TextField textRoundLimits;
    @FXML
    private TextField textWindowSize;

    @FXML
    private void handleButtonActionOk(ActionEvent event) throws InterruptedException, ExecutionException {
        final TaskContainer tc = Context.getInstance().getTc();
        if (tc != null) {
            tc.setParameter(TaskParameter.FACET_GENERATOR_METHOD, comboFGMode.getValue());
            tc.setParameter(TaskParameter.TASK_SPLIT_METHOD, comboTSVariant.getValue());
            tc.setParameter(TaskParameter.KERNEL, comboKernel.getValue());
            tc.setParameter(TaskParameter.INTERPOLATION, comboInterpolation.getValue());
            tc.setParameter(TaskParameter.FACET_GENERATOR_PARAM, Integer.valueOf(textFGSpacing.getText()));
            tc.setParameter(TaskParameter.TASK_SPLIT_PARAM, Integer.valueOf(textTSValue.getText()));

            final String limits = textRoundLimits.getText();
            final int[] newLimits;
            if (limits != null && !limits.isEmpty()) {
                final String[] split = limits.trim().split(ROUND_SPLITTER);
                if (split.length == 2) {
                    newLimits = new int[]{Integer.valueOf(split[0].trim()), Integer.valueOf(split[1].trim())};
                } else {
                    newLimits = null;
                }
            } else {
                newLimits = null;
            }
            tc.setParameter(TaskParameter.ROUND_LIMITS, newLimits);

            final int newWs = Integer.valueOf(textWindowSize.getText());
            final Object old = tc.getParameter(TaskParameter.STRAIN_ESTIMATION_PARAM);
            if (old != null) {
                final int oldWs = (int) old;
                if (newWs != oldWs) {
                    // recompute
                    Task<String> worker = new Task<String>() {

                        @Override
                        protected String call() throws Exception {
                            String result = null;
                            updateProgress(0, 2);
                            try {
                                new StrainEstimation().computeStrain(tc);
                                updateProgress(1, 2);
                                tc.setCumulativeStrain(CumulativeResultsCounter.calculate(tc, tc.getStrains()));
                            } catch (ComputationException ex) {
                                result = ex.getLocalizedMessage();
                            }
                            updateProgress(2, 2);
                            return result;
                        }
                    };
                    Dialogs.create()
                            .title(Lang.getString("Wait"))
                            .message(Lang.getString("Computing"))
                            .masthead(null)
                            .showWorkerProgress(worker);

                    Thread th = new Thread(worker);
                    th.setDaemon(true);
                    th.start();

                    th = new Thread(() -> {
                        try {
                            final String err = worker.get();
                            if (err != null) {
                                Platform.runLater(() -> {
                                    Dialogs.create()
                                            .title(Lang.getString("error"))
                                            .masthead(null)
                                            .message(err)
                                            .showWarning();
                                });

                            }
                        } catch (InterruptedException | ExecutionException ex) {
                            Platform.runLater(() -> {
                                Dialogs.create()
                                        .title(Lang.getString("error"))
                                        .masthead(null)
                                        .message(ex.getLocalizedMessage())
                                        .showException(ex);
                            });
                        }
                    });
                    th.setDaemon(true);
                    th.start();

                    tc.setParameter(TaskParameter.STRAIN_ESTIMATION_PARAM, newWs);
                }
            }
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
        final ObservableList<FacetGeneratorMethod> comboBoxData = FXCollections.observableArrayList();
        comboBoxData.addAll(FacetGeneratorMethod.values());
        comboFGMode.setItems(comboBoxData);
        final ObservableList<TaskSplitMethod> comboBoxData2 = FXCollections.observableArrayList();
        comboBoxData2.addAll(TaskSplitMethod.values());
        comboTSVariant.setItems(comboBoxData2);
        final ObservableList<KernelType> comboBoxData3 = FXCollections.observableArrayList();
        comboBoxData3.addAll(KernelType.values());
        comboKernel.setItems(comboBoxData3);
        final ObservableList<Interpolation> comboBoxData4 = FXCollections.observableArrayList();
        comboBoxData4.addAll(Interpolation.values());
        comboInterpolation.setItems(comboBoxData4);

        comboFGMode.getSelectionModel().select(DefaultValues.DEFAULT_FACET_GENERATOR);
        comboTSVariant.getSelectionModel().select(DefaultValues.DEFAULT_TASK_SPLIT_METHOD);
        comboKernel.getSelectionModel().select(DefaultValues.DEFAULT_KERNEL);
        comboInterpolation.getSelectionModel().select(DefaultValues.DEFAULT_INTERPOLATION);
        textFGSpacing.setText(String.valueOf(DefaultValues.DEFAULT_FACET_SPACING));
        textTSValue.setText(String.valueOf(DefaultValues.DEFAULT_TASK_SPLIT_PARAMETER));
        textWindowSize.setText(String.valueOf(DefaultValues.DEFAULT_STRAIN_ESTIMATION_PARAMETER));
        textRoundLimits.setText("");

        final TaskContainer tc = Context.getInstance().getTc();
        if (tc != null) {
            Object o = tc.getParameter(TaskParameter.FACET_GENERATOR_METHOD);
            if (o != null) {
                comboFGMode.getSelectionModel().select((FacetGeneratorMethod) o);
            }
            o = tc.getParameter(TaskParameter.FACET_GENERATOR_PARAM);
            if (o != null) {
                textFGSpacing.setText(o.toString());
            }

            o = tc.getParameter(TaskParameter.TASK_SPLIT_METHOD);
            if (o != null) {
                comboTSVariant.getSelectionModel().select((TaskSplitMethod) o);
            }
            o = tc.getParameter(TaskParameter.TASK_SPLIT_PARAM);
            if (o != null) {
                textTSValue.setText(o.toString());
            }

            o = tc.getParameter(TaskParameter.KERNEL);
            if (o != null) {
                comboKernel.getSelectionModel().select((KernelType) o);
            }

            o = tc.getParameter(TaskParameter.INTERPOLATION);
            if (o != null) {
                comboInterpolation.getSelectionModel().select((Interpolation) o);
            }

            o = tc.getParameter(TaskParameter.ROUND_LIMITS);
            if (o != null) {
                final int[] limits = (int[]) o;
                textRoundLimits.setText(Integer.toString(limits[0]) + ", " + Integer.toString(limits[1]));
            }

            o = tc.getParameter(TaskParameter.STRAIN_ESTIMATION_PARAM);
            if (o != null) {
                textWindowSize.setText(o.toString());
            }
        }
    }

}
