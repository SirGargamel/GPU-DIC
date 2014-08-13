package cz.tul.dic.gui;

import cz.tul.dic.data.task.TaskParameter;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.stage.Stage;
import org.pmw.tinylog.Logger;

/**
 * FXML Controller class
 *
 * @author Petr Ječmen
 */
public class PxToMmMapperController implements Initializable {

    public static final int EXTRA_WIDTH = 6;
    public static final int EXTRA_HEIGHT = 60;
    public static final int MIN_WIDTH = 230;
    @FXML
    private ImageView image;
    @FXML
    private TextField textSize;
    @FXML
    private AnchorPane anchor;
    private Line line;

    /**
     * Initializes the controller class.
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        final BufferedImage i = Context.getInstance().getTc().getImage(0);
        if (i != null) {
            final Image img = SwingFXUtils.toFXImage(i, null);
            image.setImage(img);
            image.setFitHeight(img.getHeight());
            image.setFitWidth(img.getWidth());
        } else {
            image.setImage(null);
            Logger.error("Error obtaining first image.");
        }
        image.setOnMousePressed((MouseEvent event) -> {
            anchor.getChildren().remove(line);
            final double x = event.getSceneX();
            final double y = event.getSceneY();
            line = new Line(x, y, x, y);
            line.setStroke(new Color(0, 1, 0, 1));
            anchor.getChildren().add(line);            
        });
        image.setOnMouseDragged((MouseEvent event) -> {
            line.setEndX(event.getSceneX());
        });
    }

    @FXML
    private void handleButtonActionOk(ActionEvent event) {
        if (line != null && !textSize.getText().isEmpty()) {
            final double mmSize = Double.valueOf(textSize.getText());
            final double pxSize = Math.abs(line.getEndX() - line.getStartX());
            final double ratio = pxSize / mmSize;
            Context.getInstance().getTc().setParameter(TaskParameter.MM_TO_PX_RATIO, ratio);
            final Stage stage = (Stage) textSize.getScene().getWindow();
            stage.close();
        }
    }

    @FXML
    private void handleTextKeyTyped(KeyEvent keyEvent) {
        if (!"0123456789".contains(keyEvent.getCharacter())) {
            keyEvent.consume();
        }
    }

}
