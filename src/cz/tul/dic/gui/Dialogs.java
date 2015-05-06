/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.gui;

import cz.tul.dic.gui.lang.Lang;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.StageStyle;

/**
 *
 * @author Petr Ječmen
 */
public class Dialogs {

    public static void showWarning(final String title, final String text) {
        showDialog(Alert.AlertType.WARNING, title, text);
    }

    public static void showInfo(final String title, final String text) {
        showDialog(Alert.AlertType.INFORMATION, title, text);
    }

    public static void showError(final String text) {
        showDialog(Alert.AlertType.ERROR, Lang.getString("error"), text);
    }

    private static void showDialog(final Alert.AlertType type, final String title, final String text) {
        final Alert dlg = new Alert(type);
        dlg.initModality(Modality.APPLICATION_MODAL);
        dlg.initStyle(StageStyle.UTILITY);
        dlg.setHeaderText("");
        dlg.setTitle(title);
        dlg.getDialogPane().setContentText(text);
        dlg.showAndWait();
    }

    public static void showException(final Throwable t) {
        final Alert alert = new Alert(AlertType.ERROR);
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.initStyle(StageStyle.UTILITY);
        alert.setTitle(Lang.getString("error"));
        alert.setHeaderText(t.getLocalizedMessage());

        String exceptionText;
        try {
            // Create expandable Exception.
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            exceptionText = sw.toString();
            pw.close();
            sw.close();
        } catch (IOException ex) {
            exceptionText = "Error printing exception - " + ex.getLocalizedMessage();
        }

        final TextArea textArea = new TextArea(exceptionText);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        final GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(textArea, 0, 0);

        // Set expandable Exception into the dialog pane.
        alert.getDialogPane().setExpandableContent(expContent);

        alert.showAndWait();
    }

    public static void showProgress(final Worker<?> worker, final String text) {
        final ProgressDialog dlg = new ProgressDialog(worker);
        dlg.initModality(Modality.APPLICATION_MODAL);
        dlg.initStyle(StageStyle.UTILITY);
        dlg.setTitle(Lang.getString("Wait"));
        dlg.setHeaderText(text.concat("\n").concat(Lang.getString("EscCancel")));
        dlg.getDialogPane().setOnKeyReleased((KeyEvent event) -> {
            if (event.getCode().equals(KeyCode.ESCAPE)) {
                worker.cancel();
            }
        });
        dlg.show();
    }

    private static final class ProgressDialog extends Dialog {

        final ProgressBar progress;
        final Label message;

        public ProgressDialog(final Worker worker) {
            super();

            progress = new ProgressBar();
            message = new Label();
            message.setFont(Font.font("Verdana", FontWeight.BOLD, 12));
            

            buildGui();
            assignListeners(worker);
        }

        private void buildGui() {            
            progress.setPrefSize(Double.MAX_VALUE, Region.USE_COMPUTED_SIZE);
            message.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);

            final VBox items = new VBox(5, message, progress);
            items.setPrefSize(300, 50);

            final ButtonType cancel = new ButtonType(null, ButtonBar.ButtonData.CANCEL_CLOSE);
            getDialogPane().getButtonTypes().add(cancel);
            getDialogPane().lookupButton(cancel).setVisible(false);

            this.getDialogPane().setContent(items);

        }

        private void assignListeners(final Worker worker) {
            worker.progressProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
                progress.progressProperty().set((double) newValue);
                System.out.println("Update progress to " + newValue);
            });
            worker.messageProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
                message.textProperty().set(newValue);                
            });
            worker.runningProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
                if (!newValue) {
                    hideDialog();
                }
            });
        }

        private void hideDialog() {
            hide();
            close();
        }
    }

    private Dialogs() {
    }

}
