/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.gui;

import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.gui.lang.Lang;
import cz.tul.dic.output.Direction;
import cz.tul.dic.output.ExportTask;
import cz.tul.dic.output.NameGenerator;
import cz.tul.dic.output.target.ExportTarget;
import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import org.pmw.tinylog.Logger;

/**
 * FXML Controller class
 *
 * @author Petr Jecmen
 */
public class ExportEditor implements Initializable {

    private static final String SPLIT = ";";
    @FXML
    private VBox vBox;

    /**
     * Initializes the controller class.
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
    }

    public void actualizeExports() {
        vBox.getChildren().clear();
        final TaskContainer tc = Context.getInstance().getTc();
        for (ExportTask et : tc.getExports()) {
            vBox.getChildren().add(buildExportNode(et));
        }
    }

    private Node buildExportNode(ExportTask et) {
        Label l, l2;
        final Font def = Font.getDefault();
        final Font lf = Font.font(def.getFamily(), FontWeight.BOLD, def.getSize());

        final BorderPane bp = new BorderPane();
        bp.setBorder(new Border(new BorderStroke(new Color(0, 0, 0, 1), BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));

        final Button cancel = new Button("Del");
        cancel.setUserData(et);
        cancel.setOnAction((ActionEvent event) -> {
            Context.getInstance().getTc().getExports().remove((ExportTask) cancel.getUserData());
            actualizeExports();
        });

        final HBox hb = new HBox();
        hb.setSpacing(10);
        hb.setPadding(new Insets(5));

        switch (et.getMode()) {
            case SEQUENCE:
                l = new Label(Lang.getString("Type").concat(" : "));
                l.setFont(lf);
                final int param = et.getDataParams()[0];
                final String val;
                switch (param) {
                    case 0:
                        val = Lang.getString("TypeAvi");
                        break;
                    case 1:
                        val = Lang.getString("TypeCsv");
                        break;
                    case 2:
                        val = Lang.getString("TypeBmp");
                        break;
                    default:
                        val = "ERROR";
                }
                l2 = new Label(val);
                hb.getChildren().add(new HBox(l, l2));

                l = new Label(Lang.getString("Direction").concat(" : "));
                l.setFont(lf);
                l2 = new Label(et.getDirection().toString());
                hb.getChildren().add(new HBox(l, l2));
                break;
            case POINT:
                l = new Label(Lang.getString("Type").concat(" : "));
                l.setFont(lf);
                l2 = new Label(Lang.getString("TypePoint"));
                hb.getChildren().add(new HBox(l, l2));

                l = new Label(Lang.getString("Coords").concat(" : "));
                l.setFont(lf);
                l2 = new Label(Arrays.toString(et.getDataParams()));
                hb.getChildren().add(new HBox(l, l2));
                break;
            default:
                Logger.warn("Illegal export mode - {0}", et.getMode());
                break;
        }

        bp.setCenter(hb);
        bp.setRight(cancel);

        return bp;
    }

    @FXML
    private void handleButtonActionAdd(ActionEvent event) {
        final ButtonType map = new ButtonType(Lang.getString("TypeMap"));
        final ButtonType point = new ButtonType(Lang.getString("TypePoint"));
        final ButtonType sequence = new ButtonType(Lang.getString("TypeSequence"));
        final ButtonType cancel = new ButtonType(Lang.getString("Cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        final Alert dlg = new Alert(Alert.AlertType.CONFIRMATION, null, map, point, sequence, cancel);
        dlg.setTitle(Lang.getString("Export"));
        dlg.setHeaderText(Lang.getString("ChooseDataType"));
        dlg.showAndWait().ifPresent((ButtonType t) -> {
            final TaskContainer tc = Context.getInstance().getTc();
            if (t == map) {
                final Direction dir = pickDirection();
                if (dir != null) {
                    final ButtonType img = new ButtonType(Lang.getString("TypeImage"));
                    final ButtonType csv = new ButtonType(Lang.getString("TypeCsv"));                    
                    final Alert dlg2 = new Alert(Alert.AlertType.CONFIRMATION, null, img, csv, cancel);
                    dlg2.setTitle(Lang.getString("Export"));
                    dlg2.setHeaderText(Lang.getString("ChooseDataType"));
                    dlg2.showAndWait().ifPresent((ButtonType t2) -> {
                        if (t2 == img) {
                            tc.addExport(ExportTask.generateSequenceExport(dir, ExportTarget.FILE, new File(NameGenerator.generateSequence(tc, dir)), null));
                        } else if (t2 == csv) {
                            tc.addExport(ExportTask.generateSequenceExport(dir, ExportTarget.CSV, new File(NameGenerator.generateSequence(tc, dir)), null));
                        }
                    });
                }
            } else if (t == point) {
                final TextInputDialog dlg2 = new TextInputDialog("0; 0");
                dlg2.setTitle(Lang.getString("Export"));
                dlg2.setContentText(Lang.getString("ChooseCoords"));
                dlg2.showAndWait().ifPresent((String t1) -> {
                    final String[] split = t1.split(SPLIT);
                    final int x = Integer.parseInt(split[0].trim());
                    final int y = Integer.parseInt(split[1].trim());
                    tc.addExport(ExportTask.generatePointExport(ExportTarget.CSV, new File(NameGenerator.generateCsvPoint(tc, x, y)), x, y));
                });
            } else if (t == sequence) {
                final Direction dir = pickDirection();
                if (dir != null) {
                    tc.addExport(ExportTask.generateVideoExport(dir, new File(NameGenerator.generateSequence(tc, dir))));
                }
            }
        });
        actualizeExports();
    }

    private Direction pickDirection() throws NoSuchElementException {
        final ChoiceDialog<Direction> dlg = new ChoiceDialog<>(Direction.Dabs, Direction.values());
        dlg.setHeaderText(Lang.getString("ChooseDirection"));
        dlg.setTitle(Lang.getString("Export"));
        return dlg.showAndWait().orElse(null);
    }

    @FXML
    private void handleButtonActionOk(ActionEvent event) {
        final Stage stage = (Stage) vBox.getScene().getWindow();
        stage.close();
    }

}
