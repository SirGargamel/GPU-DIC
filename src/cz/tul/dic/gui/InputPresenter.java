/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.tul.dic.gui;

import cz.tul.dic.data.roi.CircularROI;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.roi.RectangleROI;
import cz.tul.dic.data.task.TaskContainer;
import java.net.URL;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.Initializable;
import javafx.scene.image.Image;
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

/**
 *
 * @author Petr Jecmen
 */
public class InputPresenter extends AnchorPane implements Initializable, ChangeListener<String> {

    protected int imageIndex;
    protected Set<Shape> rois;
    private StringProperty imageIndexProperty;

    public void nextImage() {
        changeIndex(1);
        displayImage();
    }

    protected void changeIndex(int change) {
        imageIndex += change;

        if (imageIndex < 0) {
            imageIndex = Context.getInstance().getTc().getImages().size() - 1;
        } else if (imageIndex >= Context.getInstance().getTc().getImages().size()) {
            imageIndex = 0;
        }

        if (imageIndexProperty != null) {
            imageIndexProperty.setValue(Integer.toString(imageIndex));
        }
    }

    public void displayImage() {
        final TaskContainer tc = Context.getInstance().getTc();
        final cz.tul.dic.data.Image i = tc.getImage(imageIndex);
        final Image img = SwingFXUtils.toFXImage(i, null);

        final Background b = new Background(new BackgroundImage(img, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER, BackgroundSize.DEFAULT));
        this.setBackground(b);

        loadRois();
    }

    void loadRois() {
        final Image i = getBackground().getImages().get(0).getImage();
        final double dX = (this.getWidth() - i.getWidth()) / 2.0;
        final double dY = (this.getHeight() - i.getHeight()) / 2.0;

        rois.clear();
        final TaskContainer tc = Context.getInstance().getTc();
        final Set<ROI> taskRois = tc.getRois(imageIndex);
        Shape s = null;
        double[] roiCoords = new double[2];
        if (taskRois != null) {
            for (ROI r : taskRois) {
                if (r instanceof RectangleROI) {
                    imageToPaneXY(r.getX1(), r.getY1(), dX, dY, roiCoords);
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

            this.getChildren().clear();
            this.getChildren().addAll(rois);
        }
    }

    private void imageToPaneXY(final double imageX, final double imageY, final double dX, final double dY, final double[] paneXY) {
        paneXY[0] = imageX + dX;
        paneXY[1] = imageY + dY;
    }

    public void previousImage() {
        changeIndex(-1);
        displayImage();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        rois = new HashSet<>();
        imageIndex = 0;
    }

    public void assignImageIndexTextField(final StringProperty imageIndexProperty) {
        this.imageIndexProperty = imageIndexProperty;
        imageIndexProperty.addListener(this);
    }

    @Override
    public void changed(ObservableValue<? extends String> ov, String t, String t1) {
        try {
            imageIndex = Integer.valueOf(t1);
            displayImage();
        } catch (NumberFormatException ex) {
            imageIndexProperty.setValue(t);
        }
    }

}
