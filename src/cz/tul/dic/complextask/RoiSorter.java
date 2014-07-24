package cz.tul.dic.complextask;

import cz.tul.dic.data.roi.ROI;
import java.util.Comparator;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Jecmen
 */
public class RoiSorter implements Comparator<ROI> {

    @Override
    public int compare(ROI o1, ROI o2) {
        final int y11 = o1.getY1();
        final int y12 = o1.getY2();
        final int center1 = (y11 + y12) / 2;
        final int y21 = o2.getY1();
        final int y22 = o2.getY2();

        final int result;
        if (y11 >= y21 && y11 <= y22 || y12 >= y21 && y12 <= y22 || center1 >= y21 && center1 <= y22) {
            result = Integer.compare(o1.getX1(), o2.getX1());
        } else {
            result = Integer.compare(y11, y21);
        }     
        
        final StringBuilder sb = new StringBuilder();
        sb.append(o1);
        sb.append(" vs ");
        sb.append(o2);
        sb.append(" - ");
        sb.append(result);
        Logger.trace(sb);
        
        return result;
    }

}
