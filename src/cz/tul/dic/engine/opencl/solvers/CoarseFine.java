package cz.tul.dic.engine.opencl.solvers;

import cz.tul.dic.ComputationException;
import cz.tul.dic.data.Coordinates;
import cz.tul.dic.data.Facet;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.deformation.DeformationDegree;
import cz.tul.dic.data.deformation.DeformationLimit;
import cz.tul.dic.engine.opencl.kernels.Kernel;
import java.util.ArrayList;
import java.util.List;
import org.pmw.tinylog.Logger;

public class CoarseFine extends TaskSolver {

    private static final int COUNT_ZERO_ORDER_LIMITS = 6;

    @Override
    List<CorrelationResult> solve(Image image1, Image image2, Kernel kernel, List<Facet> facets, List<double[]> deformationLimits, DeformationDegree defDegree) throws ComputationException {
        final int facetCount = deformationLimits.size();
        final List<double[]> zeroOrderLimits = new ArrayList<>(facetCount);
        double[] temp;
        for (double[] dA : deformationLimits) {
            temp = new double[COUNT_ZERO_ORDER_LIMITS];
            System.arraycopy(dA, 0, temp, 0, COUNT_ZERO_ORDER_LIMITS);
            temp[DeformationLimit.UMIN] = Math.floor(temp[DeformationLimit.UMIN]);
            temp[DeformationLimit.UMAX] = Math.ceil(temp[DeformationLimit.UMAX]);
            temp[DeformationLimit.USTEP] = 1;
            temp[DeformationLimit.VMIN] = Math.floor(temp[DeformationLimit.VMIN]);
            temp[DeformationLimit.VMAX] = Math.ceil(temp[DeformationLimit.VMAX]);
            temp[DeformationLimit.VSTEP] = 1;
            zeroOrderLimits.add(temp);
        }
        List<CorrelationResult> coarseResults = computeTask(image1, image2, kernel, facets, zeroOrderLimits, DeformationDegree.ZERO);

        final StringBuilder sb = new StringBuilder();
        double[] coarseResult, newLimits;
        int l;        
        for (int i = 0; i < facetCount; i++) {
            coarseResult = coarseResults.get(i).getDeformation();
            temp = zeroOrderLimits.get(i);

            temp[DeformationLimit.UMIN] = coarseResult[Coordinates.X] - 1;
            temp[DeformationLimit.UMAX] = coarseResult[Coordinates.X] + 1;
            temp[DeformationLimit.USTEP] = 0.1;
            temp[DeformationLimit.VMIN] = coarseResult[Coordinates.Y] - 1;
            temp[DeformationLimit.VMAX] = coarseResult[Coordinates.Y] + 1;
            temp[DeformationLimit.VSTEP] = 0.1;

            sb.append("Coarse result for facet nr.")
                    .append(i)
                    .append(" - ")
                    .append(coarseResults.get(i))
                    .append("\n");
        }
        coarseResults = computeTask(image1, image2, kernel, facets, zeroOrderLimits, DeformationDegree.ZERO);

        final List<double[]> fineLimits = new ArrayList<>(facetCount);
        for (int i = 0; i < facetCount; i++) {
            coarseResult = coarseResults.get(i).getDeformation();
            temp = deformationLimits.get(i);
            l = temp.length;

            newLimits = new double[l];
            System.arraycopy(temp, 0, newLimits, 0, l);

            newLimits[DeformationLimit.UMIN] = coarseResult[Coordinates.X];
            newLimits[DeformationLimit.UMAX] = coarseResult[Coordinates.X];
            newLimits[DeformationLimit.USTEP] = 0;
            newLimits[DeformationLimit.VMIN] = coarseResult[Coordinates.Y];
            newLimits[DeformationLimit.VMAX] = coarseResult[Coordinates.Y];
            newLimits[DeformationLimit.VSTEP] = 0;

            fineLimits.add(newLimits);

            sb.append("Fine result for facet nr.")
                    .append(i)
                    .append(" - ")
                    .append(coarseResults.get(i))
                    .append("\n");
        }
        Logger.trace(sb);

        return computeTask(
                image1, image2,
                kernel, facets,
                fineLimits,
                defDegree);
    }

}
