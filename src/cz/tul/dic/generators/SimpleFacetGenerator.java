package cz.tul.dic.generators;

import cz.tul.dic.data.Facet;
import cz.tul.dic.data.TaskContainer;
import cz.tul.dic.data.TaskParameter;
import cz.tul.dic.data.roi.ROI;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class SimpleFacetGenerator implements IFacetGenerator {

    private static final int DEFAULT_SPACING = 1;

    @Override
    public List<List<Facet>> generateFacets(TaskContainer tc) {
        final int taskCount = tc.getImages().size() - 1;
        List<List<Facet>> result = new ArrayList<>(taskCount);

        Object o = tc.getParameter(TaskParameter.FACET_GENERATOR_SPACING);
        final int spacing;
        if (o == null) {
            spacing = DEFAULT_SPACING;
        } else {
            spacing = (int) o;
        }
        final int facetSize = tc.getFacetSize();

        if (spacing >= facetSize) {
            throw new IllegalArgumentException("Spacing cant must be smaller than facet size.");
        }

        final int halfSize = facetSize / 2;
        
        ROI roi;
        List<Facet> facets;
        int wCount, hCount;
        int roiW, roiH;
        int centerX, centerY;        
        for (int i = 0; i < taskCount; i++) {
            facets = new LinkedList<>();

            // generate centers
            roi = tc.getRoi(i);
            roiW = roi.getWidth();
            roiH = roi.getHeight();

            wCount = roiW / (facetSize - spacing);
            hCount = roiH / (facetSize - spacing);

            for (int y = 0; y < hCount; y++) {
                centerY = roi.getY1() + halfSize + (y * (facetSize - spacing));

                for (int x = 0; x < wCount; x++) {
                    centerX = roi.getX1() + halfSize + (x * (facetSize - spacing));
                    
                    facets.add(Facet.createFacet(facetSize, centerX, centerY));
                }
            }

            result.add(facets);
        }

        return result;
    }

    @Override
    public FacetGeneratorMode getMode() {
        return FacetGeneratorMode.CLASSIC;
    }

}
