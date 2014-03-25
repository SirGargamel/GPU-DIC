package cz.tul.dic.generators.facet;

import cz.tul.dic.data.Facet;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.data.roi.ROI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TightFacetGenerator extends AbstractFacetGenerator {

    private static final int DEFAULT_SPACING = 1;

    @Override
    public void generateFacets(TaskContainer tc, int round) {
        final Object o = tc.getParameter(TaskParameter.FACET_GENERATOR_SPACING);
        final int spacing = o == null ? DEFAULT_SPACING : (int) o;

        final int width = tc.getImage(round).getWidth();
        final int height = tc.getImage(round).getHeight();

        // generate centers
        final Set<ROI> rois = tc.getRois(round);

        List<Facet> result;
        int roiW, roiH, wCount, hCount, centerX, centerY,
                gapX, gapY, facetSize, halfSize;
        for (ROI roi : rois) {
            result = new ArrayList<>();

            facetSize = tc.getFacetSize(round, roi);
            halfSize = facetSize / 2;

            if (spacing >= facetSize) {
                throw new IllegalArgumentException("Spacing cant must be smaller than facet size.");
            }

            roiW = roi.getWidth();
            roiH = roi.getHeight();

            wCount = (roiW - facetSize + spacing) / (spacing);
            hCount = (roiH - facetSize + spacing) / (spacing);

            gapX = (roiW - spacing * (wCount - 1) + facetSize) / 2;
            gapY = (roiH - spacing * (hCount - 1) + facetSize) / 2;

            for (int y = 0; y < hCount; y++) {
                centerY = gapX + roi.getY1() + halfSize + y * spacing;

                for (int x = 0; x < wCount; x++) {
                    centerX = gapY + roi.getX1() + halfSize + x * spacing;

                    if (checkAreaValidity(centerX - halfSize, centerY - halfSize, centerX + halfSize, centerY + halfSize, width, height)
                            && roi.isAreaInside(centerX - halfSize, centerY - halfSize, centerX + halfSize, centerY + halfSize)) {
                        result.add(Facet.createFacet(facetSize, centerX, centerY));
                    }
                }
            }

            tc.setFacets(result, round, roi);
        }
    }

    @Override
    public FacetGeneratorMode getMode() {
        return FacetGeneratorMode.TIGHT;
    }

}
