/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.data.task;

import cz.tul.dic.data.subset.AbstractSubset;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.result.CorrelationResult;
import java.util.List;

/**
 *
 * @author Petr Ječmen
 */
public class ComputationTask {

    private final Image imageA, imageB;
    private final List<AbstractSubset> subsets;
    private final List<double[]> deformationLimits;
    private boolean subtask;
    private List<CorrelationResult> results;

    public ComputationTask(final Image imageA, final Image imageB, final List<AbstractSubset> subsets, final List<double[]> deformationLimits, final boolean subtask) {
        this.imageA = imageA;
        this.imageB = imageB;
        this.subsets = subsets;
        this.deformationLimits = deformationLimits;
        this.subtask = subtask;
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

    public List<double[]> getDeformationLimits() {
        return deformationLimits;
    }

    public List<CorrelationResult> getResults() {
        return results;
    }

    public void setResults(final List<CorrelationResult> results) {
        this.results = results;
    }

    public void setSubtask(final boolean subtask) {
        this.subtask = subtask;
    }

    public boolean isSubtask() {
        return subtask;
    }

}
