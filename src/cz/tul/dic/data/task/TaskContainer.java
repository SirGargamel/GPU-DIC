/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.data.task;

import cz.tul.dic.ComputationException;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.Container;
import cz.tul.dic.data.roi.AbstractROI;
import cz.tul.dic.data.result.Result;
import cz.tul.dic.data.task.loaders.InputLoader;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
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
    private final List<File> input;
    private final Map<TaskParameter, Object> params;
    private final Container<Set<AbstractROI>> rois;
    private final Container<Map<AbstractROI, Integer>> subsetSizes;
    private final Container<Map<AbstractROI, double[]>> deformationLimits;
    private final Set<Hint> hints;
    // generated data
    private transient List<Image> images;
    // results    
    private final List<Result> results;
    private final Map<Integer, Map<Integer, Result>> cumulativeResults;

    public TaskContainer() {
        params = new HashMap<>();
        rois = new Container<>();
        subsetSizes = new Container<>();
        deformationLimits = new Container<>();

        results = new CopyOnWriteArrayList<>();
        cumulativeResults = new ConcurrentHashMap<>();
        hints = EnumSet.noneOf(Hint.class);

        input = new ArrayList<>();
        images = new ArrayList<>();
    }

    public TaskContainer(final TaskContainer task) {
        params = new HashMap<>(task.params);
        rois = new Container<>(task.rois);
        subsetSizes = new Container<>(task.subsetSizes);
        deformationLimits = new Container<>(task.deformationLimits);

        results = new CopyOnWriteArrayList<>(task.results);
        cumulativeResults = new ConcurrentHashMap<>(task.cumulativeResults);
        hints = EnumSet.copyOf(task.hints);

        input = new ArrayList<>(task.input);
        images = new ArrayList<>(task.images);
    }

    public static TaskContainer initTaskContainer(final Object in) throws ComputationException {
        final TaskContainer result = new TaskContainer();
        return InputLoader.loadInput(in, result);
    }

    public void setInput(final List<File> input, final List<Image> images) {
        this.input.clear();
        this.images.clear();

        this.input.addAll(input);
        this.images.addAll(images);
    }

    public List<File> getInput() {
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

    public Image getImage(final int round) {
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

    public Set<AbstractROI> getRois(final int round) {
        return rois.getItem(round);
    }

    public void addRoi(final int round, final AbstractROI roi) {
        Set<AbstractROI> r = rois.getItemPrecise(round);
        if (r == null) {
            r = new HashSet<>(1);
            rois.setItem(r, round);
        }
        r.add(roi);

        setChanged();
        notifyObservers();
    }

    public void setROIs(final int round, final Set<AbstractROI> rois) {
        this.rois.setItem(rois, round);

        setChanged();
        notifyObservers();
    }

    public Map<AbstractROI, Integer> getSubsetSizes(final int round) {
        return subsetSizes.getItem(round);
    }

    public int getSubsetSize(final int round, final AbstractROI roi) {
        final Map<AbstractROI, Integer> m = subsetSizes.getItem(round);
        final int result;
        if (m != null && m.containsKey(roi)) {
            result = m.get(roi);
        } else {
            result = (int) getParameter(TaskParameter.SUBSET_SIZE);
        }
        return result;
    }

    public void setSubsetSizes(final int round, final Map<AbstractROI, Integer> sizes) {
        subsetSizes.setItem(sizes, round);
    }

    public void addSubsetSize(final int round, final AbstractROI roi, final int subsetSize) {
        Map<AbstractROI, Integer> m = subsetSizes.getItemPrecise(round);
        if (m == null) {
            m = new HashMap<>();
            subsetSizes.setItem(m, round);
        }
        m.put(roi, subsetSize);
    }

    public void setDeformationLimits(final int round, final AbstractROI roi, final double[] limits) {
        Map<AbstractROI, double[]> m = deformationLimits.getItemPrecise(round);
        if (m == null) {
            m = new HashMap<>();
            deformationLimits.setItem(m, round);
        }
        m.put(roi, limits);
    }

    public Map<AbstractROI, double[]> getDeformationLimits(final int round) {
        return deformationLimits.getItem(round);
    }

    public double[] getDeformationLimits(final int round, final AbstractROI roi) {
        final Map<AbstractROI, double[]> m = deformationLimits.getItem(round);
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
            if (roundTo < results.size()) {
                result = results.get(roundTo);
            } else {
                result = null;
            }
        } else if (cumulativeResults.containsKey(roundFrom)) {
            result = cumulativeResults.get(roundFrom).get(roundTo);
        } else {
            result = null;
        }
        return result;
    }

    public void clearResultData() {
        results.clear();
        cumulativeResults.clear();

        for (int i = 0; i < TaskContainerUtils.getMaxRoundCount(this); i++) {
            results.add(null);
        }
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
                    Logger.error("Error loading images nr." + i);
                }
            }
        } catch (IOException ex) {
            Logger.error(ex);
            throw ex;
        }
    }
}
