/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.gui;

import cz.tul.dic.gui.lang.Lang;
import javafx.concurrent.Worker;
import javafx.scene.control.Alert;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Modality;
import org.controlsfx.dialog.ExceptionDialog;
import org.controlsfx.dialog.ProgressDialog;

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
        dlg.setHeaderText("");        
        dlg.setTitle(title);        
        dlg.getDialogPane().setContentText(text);        
        dlg.showAndWait();
    }

    public static void showException(Throwable t) {
        final ExceptionDialog dlg = new ExceptionDialog(t);
        dlg.initModality(Modality.APPLICATION_MODAL);
        dlg.setTitle(Lang.getString("error"));
        dlg.setHeaderText(t.getLocalizedMessage());
        dlg.showAndWait();
    }

    public static void showProgress(final Worker<?> worker, final String text) {
        final ProgressDialog dlg = new ProgressDialog(worker);
        dlg.initModality(Modality.APPLICATION_MODAL);
        dlg.setTitle(Lang.getString("Wait"));
        dlg.setHeaderText(text.concat("\n").concat(Lang.getString("EscCancel")));
        dlg.getDialogPane().setOnKeyReleased((KeyEvent event) -> {            
            if (event.getCode().equals(KeyCode.ESCAPE)) {
                worker.cancel();
            }
        });
        dlg.show();
    }

}
