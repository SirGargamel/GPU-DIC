package cz.tul.dic.gui;

import cz.tul.dic.data.roi.CircularROI;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.roi.RectangleROI;
import cz.tul.dic.data.roi.RoiType;
import cz.tul.dic.data.task.TaskContainer;
import java.net.URL;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ChoiceBox;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.text.Text;

public class ROISelector implements Initializable {

    @FXML
    private AnchorPane imagePane;
    @FXML
    private ChoiceBox<RoiType> choiceRoi;
    private int index;
    private double lastX, lastY;
    private Set<Shape> rois;
    private Shape actualShape;

    @FXML
    private void handleButtonActionNext(ActionEvent event) {
        saveRois();
        changeIndex(1);
        displayImage();
        actualShape = null;
        event.consume();
    }

    private void saveRois() {
        final TaskContainer tc = Context.getInstance().getTc();

        final cz.tul.dic.data.Image i = tc.getImage(index);
        final double dX = (imagePane.getWidth() - i.getWidth()) / 2.0;
        final double dY = (imagePane.getHeight() - i.getHeight()) / 2.0;

        final Set<ROI> taskRois = new HashSet<>();
        taskRois.clear();
        double[] roiCoords = new double[2];
        for (Shape s : rois) {
            if (s instanceof Rectangle) {
                final Rectangle r = (Rectangle) s;
                paneToImageXY(r.getX(), r.getY(), dX, dY, roiCoords);
                taskRois.add(new RectangleROI(roiCoords[0], roiCoords[1], roiCoords[0] + r.getWidth(), roiCoords[1] + r.getHeight()));
            } else if (s instanceof Circle) {
                final Circle c = (Circle) s;
                paneToImageXY(c.getCenterX(), c.getCenterY(), dX, dY, roiCoords);
                taskRois.add(new CircularROI(roiCoords[0], roiCoords[1], c.getRadius()));
            }
        }
        tc.setROIs(taskRois, index);
    }

    private void changeIndex(int change) {
        index += change;

        if (index < 0) {
            index = Context.getInstance().getTc().getImages().size() - 1;
        } else if (index >= Context.getInstance().getTc().getImages().size()) {
            index = 0;
        }
    }

    private void displayImage() {
        final TaskContainer tc = Context.getInstance().getTc();
        final cz.tul.dic.data.Image i = tc.getImage(index);
        final Image img = SwingFXUtils.toFXImage(i, null);

        final Background b = new Background(new BackgroundImage(img, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER, BackgroundSize.DEFAULT));
        imagePane.setBackground(b);

        loadRois();
    }

    private void loadRois() {
        final cz.tul.dic.data.Image i = Context.getInstance().getTc().getImage(index);
        final double dX = (imagePane.getWidth() - i.getWidth()) / 2.0;
        final double dY = (imagePane.getHeight() - i.getHeight()) / 2.0;

        rois.clear();
        final TaskContainer tc = Context.getInstance().getTc();
        final Set<ROI> taskRois = tc.getRois(index);
        Shape s = null;
        double[] roiCoords = new double[2];
        if (taskRois != null) {
            for (ROI r : taskRois) {
                if (r instanceof RectangleROI) {
                    imageToPaneXY(r.getX1(), r.getX2(), dX, dY, roiCoords);
                    s = new Rectangle(roiCoords[0], roiCoords[1], r.getWidth(), r.getHeight());
                } else if (r instanceof CircularROI) {
                    final CircularROI c = (CircularROI) r;
                    imageToPaneXY(c.getCenterX(), c.getCenterY(), dX, dY, roiCoords);
                    s = new Circle(roiCoords[0], roiCoords[1], c.getRadius());
                }
                if (s != null) {
                    rois.add(s);
                    s.setFill(new Color(1, 1, 1, 0));
                    s.setStroke(new Color(1, 1, 1, 1));
                }
            }

            imagePane.getChildren().clear();
            imagePane.getChildren().addAll(rois);
        }
    }

    private void imageToPaneXY(final double imageX, final double imageY, final double dX, final double dY, final double[] paneXY) {
        paneXY[0] = imageX + dX;
        paneXY[1] = imageY + dY;
    }

    private void paneToImageXY(final double paneX, final double paneY, final double dX, final double dY, final double[] imageXY) {
        imageXY[0] = paneX - dX;
        imageXY[1] = paneY - dY;
    }

    @FXML
    private void handleButtonActionPrev(ActionEvent event) {
        saveRois();
        changeIndex(-1);
        displayImage();
        actualShape = null;
        event.consume();
    }

    @FXML
    private void handleButtonActionDel(ActionEvent event) {
        rois.clear();
        imagePane.getChildren().clear();
        actualShape = null;
        event.consume();
    }

    @FXML
    private void onMousePressed(MouseEvent event) {
        if (event.isPrimaryButtonDown()) {
            lastX = event.getSceneX();
            lastY = event.getSceneY();

            final Shape s;
            if (RoiType.CIRCLE.equals(choiceRoi.getValue())) {
                s = new Circle(lastX, lastY, 5);
            } else if (RoiType.RECTANGLE.equals(choiceRoi.getValue())) {
                s = new Rectangle(lastX, lastY, 5, 5);
            } else {
                s = new Text("ERROR");
            }

            s.setFill(new Color(1, 1, 1, 0));
            s.setStroke(new Color(1, 1, 1, 1));

            s.setOnMousePressed(new EventHandler<MouseEvent>() {

                @Override
                public void handle(MouseEvent t) {
                    if (t.isPrimaryButtonDown()) {
                        s.setFill(new Color(1, 1, 1, 0.5));
                        actualShape = s;
                        lastX = t.getSceneX();
                        lastY = t.getSceneY();
                        t.consume();
                    } else if (t.isSecondaryButtonDown()) {
                        rois.remove(s);
                        imagePane.getChildren().remove(s);
                        actualShape = null;
                        t.consume();
                    }
                }
            });
            s.setOnMouseDragged(new EventHandler<MouseEvent>() {

                @Override
                public void handle(MouseEvent t) {
                    final double offsetX = t.getSceneX() - lastX;
                    final double offsetY = t.getSceneY() - lastY;

                    s.setLayoutX(s.getLayoutX() + offsetX);
                    s.setLayoutY(s.getLayoutY() + offsetY);

                    lastX = t.getSceneX();
                    lastY = t.getSceneY();

                    t.consume();
                }
            });
            s.setOnMouseReleased(new EventHandler<MouseEvent>() {

                @Override
                public void handle(MouseEvent t) {
                    if (!t.isDragDetect()) {
                        s.setFill(new Color(1, 1, 1, 0));
                        actualShape = null;
                    }
                    t.consume();
                }
            });

            imagePane.getChildren().add(s);
            rois.add(s);
            actualShape = s;
            event.consume();
        }
    }

    @FXML
    private void onMouseDrag(MouseEvent event) {
        if (actualShape instanceof Circle) {
            Circle circle = (Circle) actualShape;
            final double dx = lastX - event.getSceneX();
            final double dy = lastY - event.getSceneY();
            final double radius = Math.sqrt(dx * dx + dy * dy);
            circle.setRadius(radius);
        } else if (actualShape instanceof Rectangle) {
            Rectangle rect = (Rectangle) actualShape;
            final double dx = Math.abs(event.getSceneX() - lastX);
            final double dy = Math.abs(event.getSceneY() - lastY);
            rect.setX(Math.min(event.getSceneX(), lastX));
            rect.setY(Math.min(event.getSceneY(), lastY));
            rect.setWidth(dx);
            rect.setHeight(dy);
        }
        event.consume();
    }

    @FXML
    private void onMouseRelease(MouseEvent event) {
        actualShape = null;
    }

    @FXML
    private void init(MouseEvent event) {
        displayImage();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        ObservableList<RoiType> comboBoxData = FXCollections.observableArrayList();
        comboBoxData.addAll(RoiType.values());
        choiceRoi.setItems(comboBoxData);
        choiceRoi.getSelectionModel().selectFirst();

        rois = new HashSet<>();

        index = 0;
    }

}
