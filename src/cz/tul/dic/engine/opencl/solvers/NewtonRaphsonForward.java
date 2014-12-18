package cz.tul.dic.engine.opencl.solvers;

import cz.tul.dic.ComputationException;
import cz.tul.dic.data.Facet;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.deformation.DeformationDegree;
import cz.tul.dic.data.deformation.DeformationUtils;
import cz.tul.dic.debug.IGPUResultsReceiver;
import cz.tul.dic.engine.opencl.kernels.Kernel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.QRDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.SingularMatrixException;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Ječmen
 */
public class NewtonRaphsonForward extends TaskSolver implements IGPUResultsReceiver {

    private static final int COUNT_ZERO_ORDER_LIMITS = 6;
    private static final int COUNT_STEP = 5;
    private static final int LIMITS_ROUNDS = 20;
    private static final double LIMIT_MIN_GROWTH = 0.01;
    private float[] gpuData;

    @Override
    List<CorrelationResult> solve(Image image1, Image image2, Kernel kernel, List<Facet> facets, List<double[]> deformationLimits, DeformationDegree defDegree) throws ComputationException {
        Kernel.registerListener(this);

        final int facetCount = deformationLimits.size();
        final int coeffCount = DeformationUtils.getDeformationCoeffCount(defDegree);

        final List<double[]> zeroOrderLimits = new ArrayList<>(facetCount);
        double[] temp;
        for (double[] dA : deformationLimits) {
            temp = new double[COUNT_ZERO_ORDER_LIMITS];
            System.arraycopy(dA, 0, temp, 0, COUNT_ZERO_ORDER_LIMITS);
            zeroOrderLimits.add(temp);
        }
        final List<CorrelationResult> coarseResults = computeTask(image1, image2, kernel, facets, zeroOrderLimits, DeformationDegree.ZERO);

        final List<double[]> limitsList = new ArrayList<>(facetCount);
        final List<double[]> solutionList = new ArrayList<>(facetCount);
        double[] newLimits, coarseResult, solution;
        int l;
        for (int i = 0; i < facetCount; i++) {
            coarseResult = coarseResults.get(i).getDeformation();
            temp = deformationLimits.get(i);
            l = temp.length;

            newLimits = new double[l];
            System.arraycopy(temp, 0, newLimits, 0, l);
            for (int j = 0; j < coeffCount; j++) {
                newLimits[j * 3 + 2] /= 10.0;
            }

            solution = Arrays.copyOf(coarseResult, coeffCount);
            solutionList.add(solution);
            limitsList.add(generateLimits(solution, newLimits));
        }

        final int[] indices = prepareIndices(coeffCount);
        final List<Facet> facetsToCompute = new ArrayList<>(facets);

        List<CorrelationResult> results = coarseResults;
        RealVector gradient, solutionVec;
        RealMatrix hessianMatrix;
        DecompositionSolver solver;
        CorrelationResult newResult;
        double[] limits;
        float increment;        
        Iterator<Facet> it;
        Facet f;
        int facetIndexGlobal, facetIndexLocal;
        final List<Facet> finishedFacets = new LinkedList<>();

        final StringBuilder sb = new StringBuilder();
        final long time = System.nanoTime();
        for (int i = 0; i < LIMITS_ROUNDS; i++) {
            sb.setLength(0);            

            computeTask(image1, image2, kernel, facetsToCompute, limitsList, defDegree);

            finishedFacets.clear();
            it = facetsToCompute.iterator();
            facetIndexLocal = 0;
            while (it.hasNext()) {
                f = it.next();
                facetIndexGlobal = facets.indexOf(f);

                try {
                    // store results with computed quality
                    newResult = new CorrelationResult(gpuData[generateIndex(indices)], solutionList.get(facetIndexLocal));
                    increment = newResult.getValue() - results.get(facetIndexGlobal).getValue();
                    sb.append("New results for facet nr.")
                            .append(facetIndexGlobal)
                            .append(" - ")
                            .append(results.get(facetIndexGlobal));
                    results.set(facetIndexGlobal, newResult);
                    if (increment > LIMIT_MIN_GROWTH) {
                        // prepare data for next step
                        limits = limitsList.get(facetIndexLocal);
                        gradient = generateGradient(gpuData, facetIndexLocal, facetCount, coeffCount, limits);
                        hessianMatrix = generateHessianMatrix(gpuData, facetIndexLocal, facetCount, coeffCount, limits);
                        // calculate next step
                        solver = new QRDecomposition(hessianMatrix).getSolver();
                        solutionVec = solver.solve(gradient);
                        // prepare data for next step
                        solution = solutionVec.toArray();
                        solutionList.set(facetIndexLocal, solution);
                        limitsList.set(facetIndexLocal, generateLimits(solution, limits));
                    } else if (i > 0) {
                        sb.append(", stopping computation for facet nr.")
                                .append(facetIndexGlobal)
                                .append(" due to low quality increment.");
                        finishedFacets.add(f);
                    }                    
                } catch (SingularMatrixException ex) {
                    sb.append(", stopping computation for facet nr.")
                            .append(facetIndexGlobal)
                            .append(" due to singular hessian matrix.");
                    finishedFacets.add(f);
                }

                sb.append("\n");
                facetIndexLocal++;
            }
            for (Facet facet : finishedFacets) {
                facetIndexLocal = facetsToCompute.indexOf(facet);
                facetsToCompute.remove(facetIndexLocal);
                limitsList.remove(facetIndexLocal);
            }

            Logger.trace(sb);            

            if (facetsToCompute.isEmpty()) {
                break;
            }
        }
        Logger.trace("Round time: " + ((System.nanoTime() - time) / 1_000_000) + "ms.");

        Kernel.deregisterListener(this);

        return results;
    }

    private static double[] generateLimits(final double[] solution, final double[] oldLimits) {
        final double[] newLimits = Arrays.copyOf(oldLimits, oldLimits.length);
        double step;
        for (int i = 0; i < solution.length; i++) {
            step = oldLimits[i * 3 + 2];
            newLimits[i * 3] = solution[i] - 2 * step;
            newLimits[i * 3 + 1] = solution[i] + 2 * step;
        }
        return newLimits;
    }

    private static int generateIndex(final int... indices) {
        int result = indices[0];
        for (int i = 1; i < indices.length; i++) {
            result *= COUNT_STEP;
            result += indices[i];
        }
        return result;
    }

    // forward difference
    private static RealVector generateGradient(float[] resultData, final int facetIndex, final int facetCount, final int coeffCount, final double[] deformationLimits) {
        final double[] data = new double[coeffCount];

        final int deformationCount = resultData.length / facetCount;
        final int resultsBase = facetIndex * deformationCount;
        final int[] indices = prepareIndices(coeffCount);
        for (int i = 0; i < coeffCount; i++) {
            // right index
            indices[i]++;
            data[i] = resultData[resultsBase + generateIndex(indices)];
            // left index
            indices[i]--;
            data[i] -= resultData[resultsBase + generateIndex(indices)];
            data[i] /= deformationLimits[i * 3 + 2];
        }
        return new ArrayRealVector(data);
    }

    private static int[] prepareIndices(final int coeffCount) {
        final int[] indices = new int[coeffCount];
        Arrays.fill(indices, COUNT_STEP / 2);
        return indices;
    }

    private static RealMatrix generateHessianMatrix(float[] resultData, final int facetIndex, final int facetCount, final int coeffCount, final double[] deformationLimits) {
        final double[][] data = new double[coeffCount][coeffCount];

        final int deformationCount = resultData.length / facetCount;
        final int resultsBase = facetIndex * deformationCount;
        final int[] indices = prepareIndices(coeffCount);

        // direct approach with forward difference
        double subResultA, subResultB;
        for (int i = 0; i < coeffCount; i++) {
            for (int j = i; j < coeffCount; j++) {
                indices[i]++;
                indices[j]++;
                subResultA = resultData[resultsBase + generateIndex(indices)];
                indices[j]--;
                subResultA -= resultData[resultsBase + generateIndex(indices)];
                subResultA /= deformationLimits[j * 3 + 2];

                indices[i]--;
                indices[j]++;
                subResultB = resultData[resultsBase + generateIndex(indices)];
                indices[j]--;
                subResultB -= resultData[resultsBase + generateIndex(indices)];
                subResultB /= deformationLimits[j * 3 + 2];

                data[i][j] = (subResultA - subResultB) / deformationLimits[i * 3 + 2];
                data[j][i] = data[i][j];
            }
        }

        return new Array2DRowRealMatrix(data);
    }

    @Override
    public void dumpGpuResults(float[] resultData, List<Facet> facets, List<double[]> deformationLimits) {
        this.gpuData = resultData;
    }

}
