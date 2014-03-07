package cz.tul.dic.generators.facet;

import cz.tul.dic.data.Facet;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.data.roi.ROI;
import java.util.ArrayList;
import java.util.List;

public class TightFacetGenerator implements IFacetGenerator {

    private static final int DEFAULT_SPACING = 2;

    @Override
    public List<Facet> generateFacets(TaskContainer tc, int round) {
        final Object o = tc.getParameter(TaskParameter.FACET_GENERATOR_SPACING);
        final int spacing;
        if (o == null) {
            spacing = DEFAULT_SPACING;
        } else {
            spacing = (int) o;
        }
        final int facetSize = tc.getFacetSize();
        final int halfSize = facetSize / 2;

        // generate centers
        final ROI roi = tc.getRoi(round);
        final int roiW = roi.getWidth();
        final int roiH = roi.getHeight();

        final int wCount = (roiW - facetSize + spacing) / (spacing);
        final int hCount = (roiH - facetSize + spacing) / (spacing);

        final List<Facet> result = new ArrayList<>(wCount * hCount);

//            gapX = (roiW - ((facetSize - spacing) * wCount + spacing)) / 2;
//            gapY = (roiH - ((facetSize - spacing) * hCount + spacing)) / 2;

        int centerX, centerY;
        for (int y = 0; y < hCount; y++) {
            centerY = roi.getY1() + halfSize + (y * spacing);

            for (int x = 0; x < wCount; x++) {
                centerX = roi.getX1() + halfSize + (x * spacing);

                result.add(Facet.createFacet(facetSize, centerX, centerY));
            }
        }

        return result;
    }

    @Override
    public FacetGeneratorMode getMode() {
        return FacetGeneratorMode.TIGHT;
    }

}
