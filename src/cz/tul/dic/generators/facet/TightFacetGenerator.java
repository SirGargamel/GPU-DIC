package cz.tul.dic.generators.facet;

import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.data.Facet;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.data.roi.ROI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TightFacetGenerator extends AbstractFacetGenerator {

    @Override
    public Map<ROI, List<Facet>> generateFacets(TaskContainer tc, int round) throws ComputationException {
        final Object o = tc.getParameter(TaskParameter.FACET_GENERATOR_SPACING);
        if (o == null) {
            throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "No facet generator spacing.");
        }

        final int spacing = (int) o;

        final int width = tc.getImage(round).getWidth();
        final int height = tc.getImage(round).getHeight();

        // generate centers
        final Set<ROI> rois = tc.getRois(round);
        final Map<ROI, List<Facet>> result = new HashMap<>(rois.size());

        List<Facet> facets;
        int roiW, roiH, wCount, hCount, gapX, gapY, facetSize, topLeftX, topLeftY;
        for (ROI roi : rois) {
            facets = new ArrayList<>();

            facetSize = tc.getFacetSize(round, roi);            

            if (spacing >= facetSize) {
                throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Spacing cant must be smaller than facet size.");
            }

            roiW = roi.getWidth();
            roiH = roi.getHeight();

            wCount = (roiW - facetSize + spacing) / (spacing);
            hCount = (roiH - facetSize + spacing) / (spacing);

            gapX = (roiW - (spacing * (wCount - 1) + facetSize)) / 2;
            gapY = (roiH - (spacing * (hCount - 1) + facetSize)) / 2;

            for (int y = 0; y < hCount; y++) {
                topLeftY = gapX + roi.getY1() + (y * spacing);

                for (int x = 0; x < wCount; x++) {
                    topLeftX = gapY + roi.getX1() + (x * spacing);

                    if (checkAreaValidity(topLeftX, topLeftY, topLeftX + facetSize - 1, topLeftY + facetSize - 1, width, height)
                            && roi.isAreaInside(topLeftX, topLeftY, topLeftX + facetSize - 1, topLeftY + facetSize - 1)) {
                        facets.add(Facet.createFacet(facetSize, topLeftX, topLeftY));
                    }
                }
            }

            result.put(roi, facets);
        }

        return result;
    }

    @Override
    public FacetGeneratorMode getMode() {
        return FacetGeneratorMode.TIGHT;
    }

}
