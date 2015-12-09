/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.data.task;

import cz.tul.dic.data.subset.AbstractSubset;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.deformation.DeformationOrder;
import cz.tul.dic.data.result.CorrelationResult;
import java.util.List;

/**
 *
 * @author Petr Ječmen
 */
public class ComputationTask {

    private final Image imageA, imageB;
    private final List<AbstractSubset> subsets;
    private final List<Integer> subsetWeights;
    private final List<double[]> deformations;
    private final boolean usesLimits;
    private final DeformationOrder order;
    private List<CorrelationResult> results;

    public ComputationTask(final Image imageA, final Image imageB, final List<AbstractSubset> subsets, final List<Integer> subsetWeights, final List<double[]> deformations, final DeformationOrder order, final boolean usesLimits) {
        this.imageA = imageA;
        this.imageB = imageB;
        this.subsets = subsets;
        this.subsetWeights = subsetWeights;
        this.deformations = deformations;
        this.order = order;
        this.usesLimits = usesLimits;
    }

    public Image getImageA() {
        return imageA;
    }

    public Image getImageB() {
        return imageB;
    }

    public List<AbstractSubset> getSubsets() {
        return subsets;
    }

    public List<Integer> getSubsetWeights() {
        return subsetWeights;
    }

    public List<double[]> getDeformations() {
        return deformations;
    }

    public DeformationOrder getOrder() {
        return order;
    }

    public boolean usesLimits() {
        return usesLimits;
    }

    public List<CorrelationResult> getResults() {
        return results;
    }

    public void setResults(final List<CorrelationResult> results) {
        this.results = results;
    }
}
