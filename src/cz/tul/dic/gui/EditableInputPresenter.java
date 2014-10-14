package cz.tul.dic.gui;

import cz.tul.dic.data.roi.CircularROI;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.roi.RectangleROI;
import cz.tul.dic.data.roi.RoiType;
import cz.tul.dic.data.task.TaskContainer;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.Set;
import javafx.beans.property.ReadOnlyProperty;
import javafx.scene.Node;
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
    private boolean roisChanged;

    @Override
    public boolean nextImage() {
        actualShape = null;
        return super.nextImage();
    }

    @Override
    public boolean previousImage() {
        actualShape = null;
        return super.previousImage();
    }

    public void deleteAllRois() {
        rois.clear();
        final Iterator<Node> it = this.getChildren().iterator();
        Node n;
        while (it.hasNext()) {
            n = it.next();
            if (n instanceof Shape) {
                it.remove();
            }
        }
        actualShape = null;
        saveRois();
    }

    public void saveRois() {
        if (roisChanged) {
            final TaskContainer tc = Context.getInstance().getTc();

            final Set<ROI> taskRois;
            if (!rois.isEmpty()) {
                taskRois = new HashSet<>();
                rois.stream().forEach((s) -> {
                    if (s instanceof Rectangle) {
                        final Rectangle r = (Rectangle) s;
                        taskRois.add(new RectangleROI(r.getX() + r.getTranslateX(), r.getY() + r.getTranslateY(), r.getX() + r.getTranslateX() + r.getWidth(), r.getY() + r.getTranslateY() + r.getHeight()));
                    } else if (s instanceof Circle) {
                        final Circle c = (Circle) s;
                        taskRois.add(new CircularROI(c.getCenterX() + c.getTranslateX(), c.getCenterY() + c.getTranslateY(), c.getRadius()));
                    }
                });
            } else {
                taskRois = null;
            }
            tc.setROIs(imageIndex, taskRois);
            roisChanged = false;
        }
    }

    @Override
    void loadRois() {
        super.loadRois();
        rois.stream().forEach((s) -> {
            makeShapeActive(s);
        });
        roisChanged = false;
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
        setOnMouseReleased((MouseEvent t) -> {
            onMouseRelease(t);
        });
        image.setOnMouseDragged((MouseEvent event) -> {
            onMouseDrag(event);
        });
    }

    private void onMousePressed(MouseEvent event) {
        if (event.isPrimaryButtonDown()) {
            lastX = event.getSceneX();
            lastY = event.getSceneY();

            final Shape s;
            final RoiType type = roiType.getValue();
            if (RoiType.CIRCLE.equals(type)) {
                s = new Circle(lastX, lastY, 0);
            } else if (RoiType.RECTANGLE.equals(type)) {
                s = new Rectangle(lastX, lastY, 0, 0);
            } else {
                s = new Text("ERROR");
            }

            s.setFill(new Color(1, 1, 1, 0));
            s.setStroke(new Color(1, 1, 1, 1));

            makeShapeActive(s);

            this.getChildren().add(s);
            rois.add(s);
            actualShape = s;

            roisChanged = true;
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
                saveRois();

                roisChanged = true;
                t.consume();
            }
        });
        s.setOnMouseDragged((MouseEvent t) -> {
            final double offsetX = t.getSceneX() - lastX;
            final double offsetY = t.getSceneY() - lastY;

            s.setTranslateX(s.getTranslateX() + offsetX);
            s.setTranslateY(s.getTranslateY() + offsetY);

            lastX = t.getSceneX();
            lastY = t.getSceneY();

            roisChanged = true;
            t.consume();
        });
        s.setOnMouseReleased((MouseEvent t) -> {
            if (!t.isDragDetect()) {
                s.setFill(new Color(1, 1, 1, 0));
                actualShape = null;
                saveRois();
            }

            roisChanged = true;
            t.consume();
        });
    }

    private void onMouseDrag(MouseEvent event) {
        handleShapeSize(event);
    }

    private void handleShapeSize(MouseEvent event) {
        if (actualShape instanceof Circle) {
            Circle circle = (Circle) actualShape;
            final double dx = lastX - event.getSceneX();
            final double dy = lastY - event.getSceneY();
            final double radius = Math.sqrt(dx * dx + dy * dy);
            circle.setRadius(radius);
            roisChanged = true;
        } else if (actualShape instanceof Rectangle) {
            Rectangle rect = (Rectangle) actualShape;
            final double dx = Math.abs(event.getSceneX() - lastX);
            final double dy = Math.abs(event.getSceneY() - lastY);
            rect.setX(Math.min(event.getSceneX(), lastX));
            rect.setY(Math.min(event.getSceneY(), lastY));
            rect.setWidth(dx);
            rect.setHeight(dy);
            roisChanged = true;
        }
    }

    private void onMouseRelease(MouseEvent event) {
        handleShapeSize(event);
        actualShape = null;        
        saveRois();
    }

}
