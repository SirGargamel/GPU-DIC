package cz.tul.dic.data.task;

import cz.tul.dic.data.Image;
import cz.tul.dic.data.Container;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.input.InputLoader;
import cz.tul.dic.output.ExportTask;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Set;
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
    // generated data
    private transient List<Image> images;
    // results
    private final List<Map<ROI, List<double[][]>>> results;
    private final List<double[][][]> displacement, strain;
    private final List<double[][][]> cumulativeDisplacement, cumulativeStrain;

    public TaskContainer(final Object input) {
        params = new HashMap<>();
        rois = new Container<>();
        facetSizes = new Container<>();
        deformationLimits = new Container<>();
        exports = new HashSet<>();
        results = Collections.synchronizedList(new LinkedList<>());
        displacement = Collections.synchronizedList(new LinkedList<>());
        strain = Collections.synchronizedList(new LinkedList<>());
        cumulativeDisplacement = Collections.synchronizedList(new LinkedList<>());
        cumulativeStrain = Collections.synchronizedList(new LinkedList<>());

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

    private void prepareImages() {
        try {
            images = new LinkedList<>();
            InputLoader.loadInput(this);
        } catch (IOException ex) {
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
        displacement.add(null);
        strain.add(null);
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
        final double[] result = m == null ? null : m.get(roi);
        return result;
    }

    public void setResult(final int round, final ROI roi, final List<double[][]> result) {
        Map<ROI, List<double[][]>> m = results.get(round);
        if (m == null) {
            m = new HashMap<>();
            results.add(round, m);
        }

        m.put(roi, result);
    }

    public void setResults(final int round, final Map<ROI, List<double[][]>> result) {
        while (results.size() <= round) {
            results.add(null);
        }

        results.set(round, result);
    }

    public List<double[][]> getResult(final int round, final ROI roi) {
        final Map<ROI, List<double[][]>> m = results.get(round);
        final List<double[][]> result = m == null ? null : m.get(roi);
        return result;
    }

    public Map<ROI, List<double[][]>> getResults(final int round) {
        return results.get(round);
    }

    public double[][][] getDisplacement(final int round) {
        return displacement.get(round);
    }
    
    public List<double[][][]> getDisplacements() {
        return Collections.unmodifiableList(displacement);
    }

    public void setDisplacement(final int round, final double[][][] result) {
        while (displacement.size() <= round) {
            displacement.add(null);
        }

        displacement.set(round, result);

        setChanged();
        notifyObservers();
    }

    public double[][][] getStrain(final int round) {
        return strain.get(round);
    }
    
    public List<double[][][]> getStrains() {
        return Collections.unmodifiableList(strain);
    }

    public void setStrain(final int round, final double[][][] result) {
        while (strain.size() <= round) {
            strain.add(null);
        }

        strain.set(round, result);

        setChanged();
        notifyObservers();
    }

    public double[][][] getCumulativeDisplacement(final int round) {
        return cumulativeDisplacement.get(round);
    }

    public void setCumulativeDisplacements(final List<double[][][]> data) {
        cumulativeDisplacement.clear();
        cumulativeDisplacement.addAll(data);

        setChanged();
        notifyObservers();
    }

    public double[][][] getCumulativeStrain(final int round) {
        return cumulativeStrain.get(round);
    }

    public void setCumulativeStrain(final List<double[][][]> data) {
        cumulativeStrain.clear();
        cumulativeStrain.addAll(data);

        setChanged();
        notifyObservers();
    }

    public void addExport(final ExportTask et) {
        exports.add(et);
    }

    public Set<ExportTask> getExports() {
        return exports;
    }

    public void clearResultData() {
        results.clear();
        displacement.clear();
        strain.clear();

        for (int i = 0; i < TaskContainerUtils.getMaxRoundCount(this); i++) {
            results.add(null);
            displacement.add(null);
            strain.add(null);
        }
    }

    private void readObject(ObjectInputStream stream)
            throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
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
}
