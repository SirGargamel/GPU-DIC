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
import javafx.collections.ObservableListBase;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
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

public class ROISelector implements Initializable {

    @FXML
    private AnchorPane imagePane;
    @FXML
    private ChoiceBox choiceRoi;
    private int index;
    private double startX, startY;
    private Set<Shape> rois;
    private Shape actualShape;

    @FXML
    private void handleButtonActionNext(ActionEvent event) {
        saveRois();
        changeIndex(1);
        displayImage();
    }

    private void saveRois() {
        final TaskContainer tc = Context.getInstance().getTc();
        final Set<ROI> taskRois = new HashSet<>();
        taskRois.clear();
        for (Shape s : rois) {
            if (s instanceof Rectangle) {
                final Rectangle r = (Rectangle) s;
                taskRois.add(new RectangleROI(r.getX(), r.getY(), r.getX() + r.getWidth(), r.getY() + r.getHeight()));
            } else if (s instanceof Circle) {
                final Circle c = (Circle) s;
                taskRois.add(new CircularROI(c.getCenterX(), c.getCenterY(), c.getRadius()));
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
        final Image img = SwingFXUtils.toFXImage(tc.getImage(index), null);

        final Background b = new Background(new BackgroundImage(img, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER, BackgroundSize.DEFAULT));
        imagePane.setBackground(b);

        loadRois();
    }

    private void loadRois() {
        rois.clear();
        final TaskContainer tc = Context.getInstance().getTc();
        final Set<ROI> taskRois = tc.getRois(index);
        Shape s = null;
        if (taskRois != null) {
            for (ROI r : taskRois) {
                if (r instanceof RectangleROI) {
                    s = new Rectangle(r.getX1(), r.getX2(), r.getWidth(), r.getHeight());
                } else if (r instanceof CircularROI) {
                    final CircularROI c = (CircularROI) r;
                    s = new Circle(c.getCenterX(), c.getCenterY(), c.getRadius());
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

    @FXML
    private void handleButtonActionPrev(ActionEvent event) {
        saveRois();
        changeIndex(-1);
        displayImage();
    }

    @FXML
    private void handleButtonActionDel(ActionEvent event) {
        rois.clear();
        imagePane.getChildren().clear();
    }

    @FXML
    private void onMouseClick(MouseEvent event) {
        startX = event.getSceneX();
        startY = event.getSceneY();

        if (RoiType.CIRCLE.equals(choiceRoi.getValue())) {
            actualShape = new Circle(startX, startY, 10);
        } else if (RoiType.RECTANGLE.equals(choiceRoi.getValue())) {
            actualShape = new Rectangle(startX, startY, 10, 10);
        }

        actualShape.setFill(new Color(1, 1, 1, 0));
        actualShape.setStroke(new Color(1, 1, 1, 1));
        imagePane.getChildren().add(actualShape);
    }

    @FXML
    private void onMouseDrag(MouseEvent event) {
        if (actualShape instanceof Circle) {
            Circle circle = (Circle) actualShape;
            final double dx = startX - event.getSceneX();
            final double dy = startY - event.getSceneY();
            final double radius = Math.sqrt(dx * dx + dy * dy);
            circle.setRadius(radius);
        } else if (actualShape instanceof Rectangle) {
            Rectangle rect = (Rectangle) actualShape;
            final double dx = event.getSceneX() - startX;
            final double dy = event.getSceneY() - startY;
            rect.setWidth(dx);
            rect.setHeight(dy);
        }
    }

    @FXML
    private void onMouseRelease(MouseEvent event) {
        rois.add(actualShape);
        actualShape = null;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        choiceRoi.setItems(new ObservableListBase() {

            @Override
            public Object get(int index) {
                return RoiType.values()[index];
            }

            @Override
            public int size() {
                return RoiType.values().length;
            }
        });
        choiceRoi.setValue(RoiType.CIRCLE);
        rois = new HashSet<>();

        index = 0;
        displayImage();
    }

}
