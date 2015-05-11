/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.data.task;

import cz.tul.dic.ComputationException;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.Container;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.result.Result;
import cz.tul.dic.input.InputLoader;
import cz.tul.dic.output.ExportTask;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.imageio.ImageIO;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Jecmen
 */
public class TaskContainer extends Observable implements Serializable {

    // input data
    private final Object input;
    private final Map<Object, Object> params;
    private final Container<Set<ROI>> rois;
    private final Container<Map<ROI, Integer>> facetSizes;
    private final Container<Map<ROI, double[]>> deformationLimits;
    private final Set<ExportTask> exports;
    private final Set<Hint> hints;
    // generated data
    private transient List<Image> images;
    // results    
    private final List<Result> results;
    private final Map<Integer, Map<Integer, Result>> cumulativeResults;

    public TaskContainer(final Object input) {
        params = new HashMap<>();
        rois = new Container<>();
        facetSizes = new Container<>();
        deformationLimits = new Container<>();
        exports = new HashSet<>();
        results = new CopyOnWriteArrayList<>();
        cumulativeResults = new ConcurrentHashMap<>();
        hints = EnumSet.noneOf(Hint.class);

        this.input = input;
    }

    public Object getInput() {
        return input;
    }

    public void setParameter(final TaskParameter key, final Object value) {
        if (key != null) {
            if (value != null) {
                final Class<?> cK = key.getType();
                final Class<?> cV = value.getClass();
                if (cK.isAssignableFrom(cV)) {
                    params.put(key, value);
                } else {
                    throw new IllegalArgumentException("Illegal value datatype - " + value.getClass().getSimpleName() + ", required " + key.getType().getSimpleName());
                }
            } else {
                params.remove(key);
            }
        } else {
            throw new IllegalArgumentException("Null key not supported.");
        }
    }

    public Object getParameter(final TaskParameter key) {
        return params.get(key);
    }

    public void addHint(final Hint hint) {
        hints.add(hint);
    }

    public Set<Hint> getHints() {
        return hints;
    }

    private void prepareImages() {
        try {
            images = new LinkedList<>();
            InputLoader.loadInput(this);
        } catch (IOException | ComputationException ex) {
            Logger.error("Error loading input files");
            Logger.trace(ex);
        }
    }

    public void addImage(final Image image) {
        if (images == null) {
            images = new LinkedList<>();
        }

        images.add(image);
        results.add(null);
    }

    public Image getImage(final int round) {
        if (images == null) {
            prepareImages();
        }

        int counter = -1;
        Image img = null;
        for (int i = 0; i < images.size(); i++) {
            img = images.get(i);
            counter++;
            if (counter == round) {
                break;
            }
        }

        return img;
    }

    public List<Image> getImages() {
        return Collections.unmodifiableList(images);
    }

    public Set<ROI> getRois(final int round) {
        return rois.getItem(round);
    }

    public void addRoi(final int round, final ROI roi) {
        Set<ROI> r = rois.getItemPrecise(round);
        if (r == null) {
            r = new HashSet<>(1);
            rois.setItem(r, round);
        }
        r.add(roi);

        setChanged();
        notifyObservers();
    }

    public void setROIs(final int round, final Set<ROI> rois) {
        this.rois.setItem(rois, round);

        setChanged();
        notifyObservers();
    }

    public Map<ROI, Integer> getFacetSizes(final int round) {
        return facetSizes.getItem(round);
    }

    public int getFacetSize(final int round, final ROI roi) {
        final Map<ROI, Integer> m = facetSizes.getItem(round);
        final int result;
        if (m != null && m.containsKey(roi)) {
            result = m.get(roi);
        } else {
            result = (int) getParameter(TaskParameter.FACET_SIZE);
        }
        return result;
    }

    public void setFacetSizes(final int round, final Map<ROI, Integer> sizes) {
        facetSizes.setItem(sizes, round);
    }

    public void addFacetSize(final int round, final ROI roi, final int facetSize) {
        Map<ROI, Integer> m = facetSizes.getItemPrecise(round);
        if (m == null) {
            m = new HashMap<>();
            facetSizes.setItem(m, round);
        }
        m.put(roi, facetSize);
    }

    public void setDeformationLimits(final int round, final ROI roi, final double[] limits) {
        Map<ROI, double[]> m = deformationLimits.getItemPrecise(round);
        if (m == null) {
            m = new HashMap<>();
            deformationLimits.setItem(m, round);
        }
        m.put(roi, limits);
    }

    public Map<ROI, double[]> getDeformationLimits(final int round) {
        return deformationLimits.getItem(round);
    }

    public double[] getDeformationLimits(final int round, final ROI roi) {
        final Map<ROI, double[]> m = deformationLimits.getItem(round);
        double[] result = null;
        if (m != null && m.containsKey(roi)) {
            result = m.get(roi);
        }
        if (result == null) {
            result = (double[]) getParameter(TaskParameter.DEFORMATION_LIMITS);
        }
        return result;
    }

    public void setResult(final int roundFrom, final int roundTo, final Result result) {
        if (roundFrom + 1 == roundTo) {
            if (results.size() < roundTo + 1) {
                for (int i = 0; i < roundTo - results.size(); i++) {
                    results.add(null);
                }
                results.add(result);
            } else {
                results.set(roundTo, result);
            }
        } else {
            Map<Integer, Result> m = cumulativeResults.get(roundFrom);
            if (m == null) {
                m = new HashMap<>(1);
                cumulativeResults.put(roundFrom, m);
            }

            m.put(roundTo, result);
        }
    }

    public Result getResult(final int roundFrom, final int roundTo) {
        final Result result;
        if (roundFrom + 1 == roundTo) {
            result = results.get(roundTo);
        } else if (cumulativeResults.containsKey(roundFrom)) {
            result = cumulativeResults.get(roundFrom).get(roundTo);
        } else {
            result = null;
        }
        return result;
    }

    public void addExport(final ExportTask et) {
        exports.add(et);
    }

    public Set<ExportTask> getExports() {
        return exports;
    }

    public void clearResultData() {
        results.clear();
        cumulativeResults.clear();

        for (int i = 0; i < TaskContainerUtils.getMaxRoundCount(this); i++) {
            results.add(null);
        }
    }

    public TaskContainer cloneInputTask() {
        final TaskContainer result = new TaskContainer(input);
        result.images = images;
        result.params.putAll(params);
        result.exports.addAll(exports);

        return result;
    }

    @Override
    public String toString() {
        return params.toString();
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        try {
            out.defaultWriteObject();
            out.writeInt(images.size()); // how many images are serialized?
            for (BufferedImage eachImage : images) {
                ImageIO.write(eachImage, "bmp", out); // bmp is lossless            
            }
            out.flush();
        } catch (IOException ex) {
            Logger.error(ex);
            throw ex;
        }
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        try {
            in.defaultReadObject();
            final int imageCount = in.readInt();
            images = new ArrayList<>(imageCount);
            BufferedImage img;
            for (int i = 0; i < imageCount; i++) {
                img = ImageIO.read(in);
                if (img != null) {
                    images.add(Image.createImage(img));
                } else {
                    System.out.println(i);
                }
            }
        } catch (IOException ex) {
            Logger.error(ex);
            throw ex;
        }
    }
}
