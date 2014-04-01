package cz.tul.dic.output;

import cz.tul.dic.data.Facet;
import cz.tul.dic.data.FacetUtils;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.task.TaskContainer;
import java.util.Set;

/**
 *
 * @author Petr Jecmen
 */
public class OutputUtils {

    public static void checkExportValidity(final Set<ExportTask> exports) {
        for (ExportTask et : exports) {
            if (!Exporter.isExportSupported(et)) {
                throw new IllegalArgumentException("Unsupported exeport task - " + et);
            }
        }
    }

    public static boolean isPointInsideROIs(final int x, final int y, final ROI[] rois, final TaskContainer tc, final int round) {
        boolean result = false;

        for (ROI roi : rois) {
            if (roi == null) {
                continue;
            }

            if (roi.isPointInside(x, y)) {
                // check facets                
                for (Facet f : tc.getFacets(round, roi)) {
                    if (FacetUtils.isPointInsideFacet(f, x, y)) {
                        result = true;
                        break;
                    }
                }
            }
        }

        return result;
    }

}
