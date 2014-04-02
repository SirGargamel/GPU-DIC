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
import javafx.beans.property.ReadOnlyProperty;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.text.Text;

public class EditableInputPresenter extends InputPresenter {

    private Shape actualShape;
    private ReadOnlyProperty<RoiType> roiType;
    private double lastX, lastY;

    @Override
    public void nextImage() {
        saveRois();        
        actualShape = null;
        super.nextImage();
    }

    @Override
    public void previousImage() {
        saveRois();
        actualShape = null;
        super.previousImage();               
    }

    public void deleteAllRois() {
        rois.clear();
        this.getChildren().clear();
        actualShape = null;
        saveRois();
    }

    private void saveRois() {        
        final TaskContainer tc = Context.getInstance().getTc();

        final Image i = getBackground().getImages().get(0).getImage();
        final double dX = (this.getWidth() - i.getWidth()) / 2.0;
        final double dY = (this.getHeight() - i.getHeight()) / 2.0;

        final Set<ROI> taskRois = new HashSet<>();
        double[] roiCoords = new double[2];
        rois.stream().forEach((s) -> {
            if (s instanceof Rectangle) {                
                final Rectangle r = (Rectangle) s;                
                paneToImageXY(r.getX() + r.getTranslateX(), r.getY() + r.getTranslateY(), dX, dY, roiCoords);
                taskRois.add(new RectangleROI(roiCoords[0], roiCoords[1], roiCoords[0] + r.getWidth(), roiCoords[1] + r.getHeight()));
            } else if (s instanceof Circle) {
                final Circle c = (Circle) s;
                paneToImageXY(c.getCenterX() + c.getTranslateX(), c.getCenterY() + c.getTranslateY(), dX, dY, roiCoords);
                taskRois.add(new CircularROI(roiCoords[0], roiCoords[1], c.getRadius()));
            }
        });
        tc.setROIs(taskRois, imageIndex);
    }

    @Override
    void loadRois() {
        super.loadRois();
        rois.stream().forEach((s) -> {
            makeShapeActive(s);
        });
    }

    private void paneToImageXY(final double paneX, final double paneY, final double dX, final double dY, final double[] imageXY) {
        imageXY[0] = paneX - dX;
        imageXY[1] = paneY - dY;
    }

    public void setRoiTypeProperty(final ReadOnlyProperty<RoiType> roiType) {
        this.roiType = roiType;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        super.initialize(url, rb);

        setOnMousePressed((MouseEvent t) -> {
            onMousePressed(t);
        });
        setOnMouseDragged((MouseEvent t) -> {
            onMouseDrag(t);
        });
        setOnMouseReleased((MouseEvent t) -> {
            onMouseRelease(t);
        });

    }

    private void onMousePressed(MouseEvent event) {
        if (event.isPrimaryButtonDown()) {
            lastX = event.getSceneX();
            lastY = event.getSceneY();

            final Shape s;
            final RoiType type = roiType.getValue();
            if (RoiType.CIRCLE.equals(type)) {
                s = new Circle(lastX, lastY, 5);
            } else if (RoiType.RECTANGLE.equals(type)) {
                s = new Rectangle(lastX, lastY, 5, 5);
            } else {
                s = new Text("ERROR");
            }

            s.setFill(new Color(1, 1, 1, 0));
            s.setStroke(new Color(1, 1, 1, 1));

            makeShapeActive(s);

            this.getChildren().add(s);
            rois.add(s);
            actualShape = s;
            event.consume();
        }
    }

    private void makeShapeActive(final Shape s) {
        s.setOnMousePressed((MouseEvent t) -> {
            if (t.isPrimaryButtonDown()) {
                s.setFill(new Color(1, 1, 1, 0.5));
                actualShape = s;
                lastX = t.getSceneX();
                lastY = t.getSceneY();
                t.consume();
            } else if (t.isSecondaryButtonDown()) {
                rois.remove(s);
                EditableInputPresenter.this.getChildren().remove(s);
                actualShape = null;
                t.consume();
            }
        });
        s.setOnMouseDragged((MouseEvent t) -> {
            final double offsetX = t.getSceneX() - lastX;
            final double offsetY = t.getSceneY() - lastY;

//            s.setLayoutX(s.getLayoutX() + offsetX);
//            s.setLayoutY(s.getLayoutY() + offsetY);
            s.setTranslateX(s.getTranslateX() + offsetX);
            s.setTranslateY(s.getTranslateY() + offsetY);

            lastX = t.getSceneX();
            lastY = t.getSceneY();                        

            t.consume();
        });
        s.setOnMouseReleased((MouseEvent t) -> {
            if (!t.isDragDetect()) {
                s.setFill(new Color(1, 1, 1, 0));
                actualShape = null;               
            }
            t.consume();
        });
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

}
