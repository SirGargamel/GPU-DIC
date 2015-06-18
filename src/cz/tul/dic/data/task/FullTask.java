/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.data.task;

import cz.tul.dic.data.Facet;
import cz.tul.dic.data.Image;
import java.util.List;

/**
 *
 * @author Lenam s.r.o.
 */
public class FullTask {

    private final Image imageA;
    private final Image imageB;
    private final List<Facet> facets;
    private final List<double[]> deformationLimits;

    public FullTask(final Image imageA, final Image imageB, final List<Facet> facets, final List<double[]> deformationLimits) {
        this.imageA = imageA;
        this.imageB = imageB;
        this.facets = facets;
        this.deformationLimits = deformationLimits;
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
    
}
