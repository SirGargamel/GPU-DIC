package cz.tul.dic.data.task;

import cz.tul.dic.data.Facet;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.roi.Container;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.output.ExportTask;
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
    private int facetSize;
    private final Map<Object, Object> params;
    private final Container<Set<ROI>> rois;
    private final Container<Map<ROI, double[]>> deformationLimits;
    // generated data
    private final List<Image> images;
    private final Container<Map<ROI, List<Facet>>> facets;
    private final Container<Map<ROI, double[]>> deformations;
    private final List<Map<ROI, List<double[]>>> results;
    private final List<double[][][]> finalResults;

    public TaskContainer(final Object input) {
        params = new HashMap<>();
        images = new LinkedList<>();
        rois = new Container<>();
        facets = new Container<>();
        results = new LinkedList<>();
        finalResults = new LinkedList<>();
        deformations = new Container<>();
        deformationLimits = new Container<>();

        this.input = input;
    }

    public Object getInput() {
        return input;
    }

    public void setParameter(final TaskParameter key, final Object value) {
        if (value != null && key.getType().isAssignableFrom(value.getClass())) {
            params.put(key, value);
        } else if (key != null && value != null) {
            throw new IllegalArgumentException("Illegal value datatype - " + value.getClass().getSimpleName() + ", required " + key.getType().getSimpleName());
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
            if (img.isEnabled()) {
                counter++;
            }
            if (counter == round) {
                break;
            }
        }

        if (counter < round) {
            throw new IllegalArgumentException("Illegal round number - " + round + ", total round count = " + counter);
        }

        return img;
    }

    public List<Image> getImages() {
        return Collections.unmodifiableList(images);
    }

    public void setFacets(final List<Facet> facets, final int round, final ROI roi) {
        Map<ROI, List<Facet>> m = this.facets.getItem(round);
        if (m == null) {
            m = new HashMap<>();
            this.facets.addItem(m, round);
        }
        m.put(roi, facets);
    }

    public List<Facet> getFacets(final int position, final ROI roi) {
        final Map<ROI, List<Facet>> m = facets.getItem(position);
        final List<Facet> result = m == null ? null : m.get(roi);
        return result;
    }

    public Set<ROI> getRois(final int round) {
        return rois.getItem(round);
    }

    public void addRoi(final ROI roi, final int round) {
        Set<ROI> r = rois.getItem(round);
        if (r == null) {
            r = new HashSet<>(1);
            rois.addItem(r, round);
        }
        r.add(roi);
    }

    public int getFacetSize() {
        return facetSize;
    }

    public void setFacetSize(final int facetSize) {
        this.facetSize = facetSize;
    }

    public void setDeformationLimits(final double[] limits, final int round, final ROI roi) {
        Map<ROI, double[]> m = deformationLimits.getItem(round);
        if (m == null) {
            m = new HashMap<>();
            deformationLimits.addItem(m, round);
        }
        m.put(roi, limits);
    }

    public double[] getDeformationLimits(final int round, final ROI roi) {
        final Map<ROI, double[]> m = deformationLimits.getItem(round);
        final double[] result = m == null ? null : m.get(roi);
        return result;
    }

    public void setDeformations(final double[] deformations, final int round, final ROI roi) {
        Map<ROI, double[]> m = this.deformations.getItem(round);
        if (m == null) {
            m = new HashMap<>();
            this.deformations.addItem(m, round);
        }

        m.put(roi, deformations);
    }

    public double[] getDeformations(final int round, final ROI roi) {
        final Map<ROI, double[]> m = this.deformations.getItem(round);
        final double[] result = m == null ? null : m.get(roi);
        return result;
    }

    public void setResult(final List<double[]> result, final int round, final ROI roi) {
        while (results.size() <= round) {
            results.add(null);
        }

        Map<ROI, List<double[]>> m = results.get(round);
        if (m == null) {
            m = new HashMap<>();
            results.add(round, m);
        }

        m.put(roi, result);
    }

    public List<double[]> getResults(final int round, final ROI roi) {
        final Map<ROI, List<double[]>> m = results.get(round);
        final List<double[]> result = m == null ? null : m.get(roi);
        return result;
    }

    public double[][][] getPerPixelResult(final int position) {
        return finalResults.get(position);
    }

    public void setPerPixelResult(final double[][][] result, final int round) {
        while (finalResults.size() <= round) {
            finalResults.add(null);
        }

        finalResults.set(round, result);
    }
}
