/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.gui;

import cz.tul.dic.ComputationException;
import cz.tul.dic.data.task.TaskDefaultValues;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.engine.strain.StrainEstimator;
import cz.tul.dic.gui.lang.Lang;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import org.pmw.tinylog.Logger;

/**
 * FXML Controller class
 *
 * @author Petr Jecmen
 */
public class ExpertSettings implements Initializable {

    private static final String ROUND_SPLITTER = ",";
    private static final String LIMITS_SPLITTER = ";";
    @FXML
    private TextField textRoundLimits;
    @FXML
    private TextField textWindowSize;
    @FXML
    private TextField textDefLimits;

    @FXML
    private void handleButtonActionOk(ActionEvent event) throws InterruptedException, ExecutionException {
        final TaskContainer tc = Context.getInstance().getTc();
        if (tc != null) {
            String limits = textRoundLimits.getText();
            int[] newLimits = null;
            if (!limits.isEmpty()) {
                try {
                    final String[] split = limits.trim().split(ROUND_SPLITTER);
                    if (split.length == 2) {
                        newLimits = new int[]{Integer.parseInt(split[0].trim()), Integer.parseInt(split[1].trim())};
                    }
                } catch (NumberFormatException | NullPointerException ex) {
                }
                if (newLimits == null) {
                    signalizeIllegalLimits();
                }
            }
            tc.setParameter(TaskParameter.ROUND_LIMITS, newLimits);

            limits = textDefLimits.getText();
            double[] newLimitsD = null;
            if (!limits.isEmpty()) {
                newLimitsD = doubleArrayFromString(limits);
                if (newLimitsD == null) {
                    Dialogs.showInfo(
                            Lang.getString("Warning"),
                            Lang.getString("IllegalLimitsD"));
                }
            }
            tc.setParameter(TaskParameter.DEFORMATION_LIMITS, newLimitsD);

            final double newWs = Double.parseDouble(textWindowSize.getText());
            final Object old = tc.getParameter(TaskParameter.STRAIN_ESTIMATION_PARAM);
            if (old != null) {
                final double oldWs = (double) old;
                if (Double.compare(newWs, oldWs) != 0) {
                    // recompute
                    tc.setParameter(TaskParameter.STRAIN_ESTIMATION_PARAM, newWs);
                    final Task<String> worker = new Task<String>() {

                        @Override
                        protected String call() throws Exception {
                            String result = null;
                            updateProgress(0, 2);
                            try {
                                StrainEstimator.computeStrain(tc);
                                updateProgress(1, 2);
                            } catch (ComputationException ex) {
                                result = ex.getLocalizedMessage();
                                Logger.debug(ex);
                            }
                            updateProgress(2, 2);
                            return result;
                        }
                    };
                    Dialogs.showProgress(worker, Lang.getString("Computing"));

                    Thread th = new Thread(worker);
                    th.setDaemon(true);
                    th.start();

                    th = new Thread(() -> {
                        try {
                            final String err = worker.get();
                            if (err != null) {
                                Platform.runLater(()
                                        -> Dialogs.showWarning(
                                                Lang.getString("error"),
                                                err)
                                );

                            }
                        } catch (InterruptedException | ExecutionException ex) {
                            Platform.runLater(()
                                    -> Dialogs.showException(ex)
                            );
                        }
                    });
                    th.setDaemon(true);
                    th.start();
                }
            }
        }

        closeWindow();
    }

    private static void signalizeIllegalLimits() {
        Dialogs.showInfo(
                Lang.getString("Warning"),
                Lang.getString("IllegalLimitsR"));
    }

    private void closeWindow() {
        final Stage stage = (Stage) textRoundLimits.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleButtonActionCancel(ActionEvent event) {
        closeWindow();
    }

    @FXML
    private void handleTextKeyTypedNumbers(KeyEvent keyEvent) {
        if (!"0123456789".contains(keyEvent.getCharacter())) {
            keyEvent.consume();
        }
    }

    @FXML
    private void handleTextKeyTypedRounds(KeyEvent keyEvent) {
        if (!"0123456789,".contains(keyEvent.getCharacter())) {
            keyEvent.consume();
        }
    }

    @FXML
    private void handleTextKeyTypedDeformations(KeyEvent keyEvent) {
        if (!"0123456789;-+".contains(keyEvent.getCharacter())) {
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
        textWindowSize.setText(String.valueOf(TaskDefaultValues.DEFAULT_STRAIN_ESTIMATION_PARAMETER));
        textRoundLimits.setText("");
        textDefLimits.setText(toString(TaskDefaultValues.DEFAULT_DEFORMATION_LIMITS_FIRST));

        final TaskContainer tc = Context.getInstance().getTc();
        if (tc != null) {
            Object o;
            o = tc.getParameter(TaskParameter.ROUND_LIMITS);
            if (o != null) {
                final int[] limits = (int[]) o;
                textRoundLimits.setText(Integer.toString(limits[0]) + ", " + Integer.toString(limits[1]));
            }

            o = tc.getParameter(TaskParameter.STRAIN_ESTIMATION_PARAM);
            if (o != null) {
                textWindowSize.setText(o.toString());
            }

            o = tc.getParameter(TaskParameter.DEFORMATION_LIMITS);
            if (o != null) {
                textDefLimits.setText(toString((double[]) o));
            }
        }
    }

    private static String toString(final double[] data) {
        final StringBuilder sb = new StringBuilder();

        for (double d : data) {
            sb.append(d);
            sb.append(LIMITS_SPLITTER);
        }
        sb.setLength(sb.length() - LIMITS_SPLITTER.length());

        return sb.toString();
    }

    private static double[] doubleArrayFromString(final String data) {
        double[] result;
        try {
            final String[] split = data.split(LIMITS_SPLITTER);
            result = new double[split.length];
            for (int i = 0; i < split.length; i++) {
                result[i] = Double.valueOf(split[i].trim());
            }
        } catch (NumberFormatException | NullPointerException ex) {
            result = null;
        }

        return result;
    }

}
