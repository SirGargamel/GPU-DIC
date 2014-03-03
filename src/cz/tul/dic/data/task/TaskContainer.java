package cz.tul.dic.data.task;

import cz.tul.dic.data.Facet;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.roi.RoiContainer;
import cz.tul.dic.output.ExportTask;
import java.io.File;
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
public class TaskContainer {

    private final Object input;
    private final Map<Object, Object> params;
    private final List<Image> images;
    private final RoiContainer rois;
    private final Set<ExportTask> exportTasks;
    private final List<List<double[]>> results;
    private int facetSize;
    private List<List<Facet>> facets;
    private double[] deformations;
    private final List<double[][][]> finalResults;

    public TaskContainer(final Object input) {
        params = new HashMap<>();
        images = new LinkedList<>();
        rois = new RoiContainer();
        exportTasks = new HashSet<>();
        results = new LinkedList<>();
        finalResults = new LinkedList<>();
        
        this.input = input;
    }

    public Object getInput() {
        return input;
    }

    public void addParameter(final TaskParameter key, final Object value) {
        if (value != null && value.getClass().equals(key.getType())) {
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

    public void assignFacets(final List<List<Facet>> facets) {
        this.facets = facets;
    }

    public List<Facet> getFacets(final int position) {
        return facets.get(position);
    }

    public ROI getRoi(final int round) {
        return rois.getRoi(round);
    }

    public void addRoi(ROI roi, final int round) {
        rois.addRoi(roi, round);
    }

    public int getFacetSize() {
        return facetSize;
    }

    public void setFacetSize(int facetSize) {
        this.facetSize = facetSize;
    }

    public void setDeformations(double[] deformations) {
        this.deformations = deformations;
    }

    public double[] getDeformations() {
        return deformations;
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
