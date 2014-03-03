package cz.tul.dic.generators.facet;

import cz.tul.dic.data.Facet;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.task.TaskContainerUtils;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class TightFacetGenerator implements IFacetGenerator {

    private static final int DEFAULT_SPACING = 2;

    @Override
    public List<List<Facet>> generateFacets(TaskContainer tc) {
        final int taskCount = TaskContainerUtils.getRoundCount(tc);
        List<List<Facet>> result = new ArrayList<>(taskCount);

        Object o = tc.getParameter(TaskParameter.FACET_GENERATOR_SPACING);
        final int spacing;
        if (o == null) {
            spacing = DEFAULT_SPACING;
        } else {
            spacing = (int) o;
        }
        final int facetSize = tc.getFacetSize();
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

            wCount = (roiW - facetSize + spacing) / (spacing);
            hCount = (roiH - facetSize + spacing) / (spacing);

//            gapX = (roiW - ((facetSize - spacing) * wCount + spacing)) / 2;
//            gapY = (roiH - ((facetSize - spacing) * hCount + spacing)) / 2;
//
            for (int y = 0; y < hCount; y++) {
                centerY = roi.getY1() + halfSize + (y * spacing);

                for (int x = 0; x < wCount; x++) {
                    centerX = roi.getX1() + halfSize + (x * spacing);

                    facets.add(Facet.createFacet(facetSize, centerX, centerY));
                }
            }

            result.add(facets);
        }

        return result;
    }

    @Override
    public FacetGeneratorMode getMode() {
        return FacetGeneratorMode.TIGHT;
    }

}
