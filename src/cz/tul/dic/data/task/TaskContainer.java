package cz.tul.dic.data.task;

import cz.tul.dic.data.Image;
import cz.tul.dic.data.Container;
import cz.tul.dic.data.roi.ROI;
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
import java.util.Set;

/**
 *
 * @author Petr Jecmen
 */
public class TaskContainer implements Serializable {

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
    private final List<double[][][]> finalResults;

    public TaskContainer(final Object input) {
        params = new HashMap<>();
        images = new LinkedList<>();
        rois = new Container<>();
        facetSizes = new Container<>();
        results = new LinkedList<>();
        finalResults = new LinkedList<>();
        deformationLimits = new Container<>();
        exports = new HashSet<>();

        this.input = input;
    }

    public Object getInput() {
        return input;
    }

    public void setParameter(final TaskParameter key, final Object value) {
        if (value != null && key != null) {
            final Class<?> cK = key.getType();
            final Class<?> cV = value.getClass();
            if (cK.isAssignableFrom(cV)) {
                params.put(key, value);
            } else {
                throw new IllegalArgumentException("Illegal value datatype - " + value.getClass().getSimpleName() + ", required " + key.getType().getSimpleName());
            }
        } else {
            throw new IllegalArgumentException("Null values not supported.");
        }
    }

    public Object getParameter(final TaskParameter key) {
        return params.get(key);
    }

    public void addImage(final Image image) {
        images.add(image);
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
    }

    public void setROIs(final int round, final Set<ROI> rois) {
        this.rois.setItem(rois, round);
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
        while (results.size() <= round) {
            results.add(null);
        }

        Map<ROI, List<double[][]>> m = results.get(round);
        if (m == null) {
            m = new HashMap<>();
            results.add(round, m);
        }

        m.put(roi, result);
    }

    public List<double[][]> getResults(final int round, final ROI roi) {
        final Map<ROI, List<double[][]>> m = results.get(round);
        final List<double[][]> result = m == null ? null : m.get(roi);
        return result;
    }

    public double[][][] getPerPixelResult(final int round) {
        return finalResults.get(round);
    }

    public void setPerPixelResult(final int round, final double[][][] result) {
        while (finalResults.size() <= round) {
            finalResults.add(null);
        }

        finalResults.set(round, result);
    }

    public void addExport(final ExportTask et) {
        exports.add(et);
    }

    public Set<ExportTask> getExports() {
        return exports;
    }

    public void clearResultData() {
        results.clear();
        finalResults.clear();
    }

    private void readObject(ObjectInputStream stream)
            throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        images = new LinkedList<>();
    }
}
