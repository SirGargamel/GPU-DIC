package cz.tul.dic.data.roi;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Petr Jecmen
 */
public class RoiContainer {

    private final List<ROI> rois;

    public RoiContainer() {
        rois = new ArrayList<>();
    }

    public void addRoi(final ROI roi) {
        rois.add(roi);
    }

    public void addRoi(final ROI roi, final int position) {
        while (position >= rois.size()) {
            rois.add(null);
        }
        rois.set(position, roi);
    }

    public ROI getRoi(final int position) {
        final ROI result;
        if (position >= rois.size()) {
            if (!rois.isEmpty()) {
                result = rois.get(rois.size() - 1);
            } else {
                result = null;
            }
        } else {
            result = rois.get(position);
        }
        return result;
    }

}
