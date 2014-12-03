package cz.tul.dic.engine.opencl.solvers;

import cz.tul.dic.ComputationException;
import cz.tul.dic.data.Facet;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.deformation.DeformationDegree;
import cz.tul.dic.engine.opencl.kernels.Kernel;
import java.util.List;

public class BruteForce extends TaskSolver {

    @Override
    List<CorrelationResult> solve(
            Image image1, Image image2,
            final Kernel kernel, List<Facet> facets,
            List<double[]> deformationLimits, DeformationDegree defDegree) throws ComputationException {
        return computeTask(image1, image2, kernel, facets, deformationLimits, defDegree);
    }

}
