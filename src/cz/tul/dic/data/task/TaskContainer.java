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
    private final Container<double[]> deformations;
    private final Set<ExportTask> exportTasks;    
    // generated data
    private final List<Image> images;
    private final Container<List<Facet>> facets;
    private final List<List<double[]>> results;
    private final List<double[][][]> finalResults;

    public TaskContainer(final Object input) {
        params = new HashMap<>();
        images = new LinkedList<>();
        rois = new Container<>();
        facets = new Container<>();
        exportTasks = new HashSet<>();
        results = new LinkedList<>();
        finalResults = new LinkedList<>();
        deformations = new Container<>();

        this.input = input;
    }

    public Object getInput() {
        return input;
    }

    public void addParameter(final TaskParameter key, final Object value) {
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

    public void assignFacets(final List<Facet> facets, final int round) {
        this.facets.addItem(facets, round);
    }

    public List<Facet> getFacets(final int position) {
        return facets.getItem(position);
    }

    public Set<ROI> getRoi(final int round) {
        return rois.getItem(round);
    }

    public void addRoi(ROI roi, final int round) {
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

    public void setFacetSize(int facetSize) {
        this.facetSize = facetSize;
    }

    public void setDeformations(double[] deformations, final int round) {
        this.deformations.addItem(deformations, round);
    }

    public double[] getDeformations(final int round) {
        return deformations.getItem(round);
    }

    public void storeResult(final List<double[]> result, final int round) {
        while (results.size() <= round) {
            results.add(null);
        }

        results.set(round, result);
    }

    public List<double[]> getResults(final int round) {
        return results.get(round);
    }

    public double[][][] getFinalResults(final int position) {
        return finalResults.get(position);
    }

    public void storeFinalResults(final double[][][] result, final int round) {
        while (finalResults.size() <= round) {
            finalResults.add(null);
        }

        finalResults.set(round, result);
    }

    public void addExportTask(final ExportTask task) {
        exportTasks.add(task);
    }

    public Set<ExportTask> getExportTasks() {
        return Collections.unmodifiableSet(exportTasks);
    }

}
