package cz.tul.dic.data.task;

import cz.tul.dic.data.Facet;
import cz.tul.dic.data.Image;
import java.util.List;

/**
 *
 * @author Petr Ječmen
 */
public class ComputationTask {

    private final Image imageA, imageB;
    private final List<Facet> facets;
    private final double[] deformations;
    private float[] results;

    public ComputationTask(Image imageA, Image imageB, List<Facet> facets, double[] deformations) {
        this.imageA = imageA;
        this.imageB = imageB;
        this.facets = facets;
        this.deformations = deformations;
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

    public double[] getDeformations() {
        return deformations;
    }

    public float[] getResults() {
        return results;
    }

    public void setResults(float[] results) {
        this.results = results;
    }

}
