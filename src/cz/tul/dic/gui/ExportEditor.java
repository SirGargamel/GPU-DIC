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
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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
import org.controlsfx.control.action.AbstractAction;
import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.Dialogs;
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
        final String c1 = Lang.getString("TypeMap");
        final String t1 = Lang.getString("TypeMapD");
        final String c2 = Lang.getString("TypePoint");
        final String t2 = Lang.getString("TypePointD");
        final String c3 = Lang.getString("TypeSequence");
        final String t3 = Lang.getString("TypeSequenceD");
        Action a = Dialogs.create()
                .masthead(null)
                .title(Lang.getString("Export"))
                .message(Lang.getString("ChooseDataType"))
                .showCommandLinks(null, new Dialogs.CommandLink(c1, t1), new Dialogs.CommandLink(c2, t2), new Dialogs.CommandLink(c3, t3));
        String val = a.textProperty().get();
        if (!val.equals("@@dlg.cancel.button")) {
            final TaskContainer tc = Context.getInstance().getTc();
            try {
                if (val.equals(c1)) {
                    final Direction dir = pickDirection();
                    final String d1 = Lang.getString("TypeImage");
                    final String d2 = Lang.getString("TypeCsv");
                    a = Dialogs.create()
                            .masthead(null)
                            .title(Lang.getString("Export"))
                            .message(Lang.getString("ChooseDataType"))
                            .showCommandLinks(null, new Dialogs.CommandLink(d1, null), new Dialogs.CommandLink(d2, null));
                    val = a.textProperty().get();
                    if (!val.equals("@@dlg.cancel.button")) {
                        if (val.equals(d1)) {
                            tc.addExport(ExportTask.generateSequenceExport(dir, ExportTarget.FILE, new File(NameGenerator.generateSequence(tc, dir)), ExportTask.EXPORT_SEQUENCE_BMP));
                        } else if (val.equals(d2)) {
                            tc.addExport(ExportTask.generateSequenceExport(dir, ExportTarget.FILE, new File(NameGenerator.generateSequence(tc, dir)), ExportTask.EXPORT_SEQUENCE_CSV));
                        }
                    }
                } else if (val.equals(c2)) {
                    final Optional<String> coords = Dialogs.create()
                            .masthead(null)
                            .title(Lang.getString("Export"))
                            .message(Lang.getString("ChooseCoords"))
                            .showTextInput("0; 0");
                    final String[] split = coords.get().split(SPLIT);
                    final int x = Integer.valueOf(split[0].trim());
                    final int y = Integer.valueOf(split[1].trim());
                    tc.addExport(ExportTask.generatePointExport(ExportTarget.CSV, new File(NameGenerator.generateCsvPoint(tc, x, y)), x, y));
                } else if (val.equals(c3)) {
                    final Direction dir = pickDirection();
                    tc.addExport(ExportTask.generateSequenceExport(dir, ExportTarget.FILE, new File(NameGenerator.generateSequence(tc, dir)), ExportTask.EXPORT_SEQUENCE_AVI));
                }
                actualizeExports();
            } catch (NumberFormatException | NoSuchElementException ex) {

            }
        }
    }

    private Direction pickDirection() throws NoSuchElementException {
        final Optional<Direction> a = Dialogs.create()
                .masthead(null)
                .title(Lang.getString("Export"))
                .message(Lang.getString("ChooseDirection"))
                .showChoices(Direction.values());
        return a.get();
    }

    @FXML
    private void handleButtonActionOk(ActionEvent event) {
        final Stage stage = (Stage) vBox.getScene().getWindow();
        stage.close();
    }

}
