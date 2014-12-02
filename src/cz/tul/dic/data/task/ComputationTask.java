package cz.tul.dic.data.task;

import cz.tul.dic.data.Facet;
import cz.tul.dic.data.Image;
import cz.tul.dic.engine.CorrelationResult;
import java.util.List;

/**
 *
 * @author Petr Ječmen
 */
public class ComputationTask {

    private final Image imageA, imageB;
    private final List<Facet> facets;
    private final List<double[]> deformationLimits;
    private boolean subtask;
    private List<CorrelationResult> results;

    public ComputationTask(Image imageA, Image imageB, List<Facet> facets, List<double[]> deformationLimits, boolean subtask) {
        this.imageA = imageA;
        this.imageB = imageB;
        this.facets = facets;
        this.deformationLimits = deformationLimits;
        this.subtask = subtask;
    }

    public Image getImageA() {
        return imageA;
    }

    public Image getImageB() {
        return imageB;
    }

    public List<Facet> getFacets() {
        return facets;
    }

    public List<double[]> getDeformationLimits() {
        return deformationLimits;
    }

    public List<CorrelationResult> getResults() {
        return results;
    }

    public void setResults(List<CorrelationResult> results) {
        this.results = results;
    }

    public void setSubtask(boolean subtask) {
        this.subtask = subtask;
    }        

    public boolean isSubtask() {
        return subtask;
    }

}
