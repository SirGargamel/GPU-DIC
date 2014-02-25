package cz.tul.dic.engine;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLDevice.Type;
import com.jogamp.opencl.CLErrorHandler;
import com.jogamp.opencl.CLImage2d;
import com.jogamp.opencl.CLImageFormat;
import com.jogamp.opencl.CLKernel;
import com.jogamp.opencl.CLMemory.Mem;
import com.jogamp.opencl.CLPlatform;
import com.jogamp.opencl.CLProgram;
import com.jogamp.opencl.CLResource;
import com.jogamp.opencl.llb.CLKernelBinding;
import com.jogamp.opencl.util.Filter;
import cz.tul.dic.data.Coordinates;
import cz.tul.dic.data.Facet;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.engine.opencl.WorkSizeManager;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Petr Jecmen
 */
public final class Engine {

    private static final Type DEVICE_TYPE = Type.GPU;
    private static final CLImageFormat IMAGE_FORMAT = new CLImageFormat(CLImageFormat.ChannelOrder.RGBA, CLImageFormat.ChannelType.UNSIGNED_INT8);
    private static final String KERNEL_NAME = "CL1D_I_V_LL_MC_D";
    private static final int ARGUMENT_INDEX = 12;
    private final Set<CLResource> clMemRound, clMemTask;
    private final WorkSizeManager wsm;
    private final CLPlatform platform;
    private final CLContext context;
    private final CLDevice device;

    public Engine(final WorkSizeManager wsm) {
        clMemRound = new HashSet<>();
        clMemTask = new HashSet<>();
        this.wsm = wsm;

        final CLPlatform tmpP = CLPlatform.getDefault(new Filter<CLPlatform>() {

            @Override
            public boolean accept(CLPlatform i) {
                return i.getMaxFlopsDevice(Type.GPU) != null && i.listCLDevices(CLDevice.Type.CPU).length == 0;
            }
        });
        if (tmpP == null) {
            platform = CLPlatform.getDefault();
        } else {
            platform = tmpP;
        }

        final CLDevice tmpD = platform.getMaxFlopsDevice(DEVICE_TYPE);
        if (tmpD == null) {
            device = platform.getMaxFlopsDevice();
        } else {
            device = tmpD;
        }

        context = CLContext.create(device);
        context.addCLErrorHandler(new CLErrorHandler() {

            @Override
            public void onError(String string, ByteBuffer bb, long l) {
                System.err.println("CLError - " + string);
            }
        });

        System.out.println("TODO Support multiple kernels.");
    }

    public void computeTask(final TaskContainer tc) throws IOException {
        final int roundCount = tc.getRoundCount();
        final int facetSize = tc.getFacetSize();
        final int facetArea = facetSize * facetSize;

        final CLCommandQueue queue = device.createCommandQueue();
        clMemTask.add(queue);

        List<Facet> facets;
        Image img;
        CLImage2d<IntBuffer> imgA, imgB;
        CLBuffer<IntBuffer> facetData;
        CLBuffer<FloatBuffer> facetCenters;
        CLBuffer<FloatBuffer> deformations, results;
        CLProgram program;
        CLKernel kernel;
        float[] roundResult;
        int facetCount, kernelRoundCount, lws0base, lws0, deformationCount;
        List<double[]> bestResults;
        for (int round = 0; round < roundCount; round++) {
            // generate data for OpenCL            
            img = tc.getImage(round);
            imgA = generateImage(img);
            queue.putWriteImage(imgA, false);
            imgB = generateImage(tc.getImage(round + 1));
            queue.putWriteImage(imgB, false);

            facets = tc.getFacets(round);
            facetCount = facets.size();

            facetData = generateFacetData(facets, facetSize);
            queue.putWriteBuffer(facetData, false);

            facetCenters = generateFacetCenters(facets);
            queue.putWriteBuffer(facetCenters, false);

            deformations = generateDeformations(tc.getDeformations());
            queue.putWriteBuffer(deformations, false);
            deformationCount = TaskContainerUtils.getDeformationCount(tc);

            results = context.createFloatBuffer(facetCount * deformationCount, Mem.WRITE_ONLY);
            clMemRound.add(results);
            // create kernel and parameters
            // how many facets will be computed during one round
            final int facetSubCount = Math.min(wsm.getWorkSize(this.getClass()), facetCount);

            program = context.createProgram(KernelPreparator.prepareKernel(KERNEL_NAME, tc)).build();
            clMemRound.add(program);

            kernel = program.createCLKernel(KERNEL_NAME);
            clMemRound.add(kernel);

            lws0base = calculateLws0base(kernel);
            if (deformationCount > facetArea) {
                lws0 = EngineMath.roundUp(lws0base, facetArea);
            } else {
                lws0 = EngineMath.roundUp(lws0base, deformationCount);
            }
            final int facetGlobalWorkSize = EngineMath.roundUp(lws0, deformationCount) * facetSubCount;
            // number of groups, which will be participating on computation of one facet
            int groupCountPerFacet = deformationCount / lws0;
            if (deformationCount % lws0 > 0) {
                groupCountPerFacet++;
            }

            kernel.putArgs(imgA, imgB, facetData, facetCenters, deformations, results)
                    .putArg(img.getWidth())
                    .putArg(deformationCount)
                    .putArg(facetSize)
                    .putArg(facetCount)
                    .putArg(groupCountPerFacet)
                    .putArg(facetSubCount)
                    .putArg(0)
                    .rewind();
            // copy data and execute kernel
            kernelRoundCount = (int) Math.ceil(facetCount / (double) facetSubCount);

            for (int kernelRound = 0; kernelRound < kernelRoundCount; kernelRound++) {
                kernel.setArg(ARGUMENT_INDEX, kernelRound * facetSubCount);
                queue.put1DRangeKernel(kernel, 0, facetGlobalWorkSize, lws0);
            }
            // copy data back            
            queue.putReadBuffer(results, true);
            roundResult = readBuffer(results.getBuffer());
            analyze(roundResult);
            // pick best values            
            bestResults = pickBestResults(roundResult, tc, facetCount);
            // store data           
            tc.storeResult(bestResults, round);
            buildFinalResults(tc, round);
            clearMem(clMemRound);
        }

        queue.finish();

        clearMem(clMemTask);
    }

    private void clearMem(final Set<CLResource> mems) {
        // round memory cleanup
        for (CLResource mem : mems) {
            if (!mem.isReleased()) {
                mem.release();
            }
        }
    }

    private CLImage2d<IntBuffer> generateImage(final Image image) {
        final int[] imageData = image.toArray();
        final int imageWidth = image.getWidth();
        final IntBuffer imageBuffer = Buffers.newDirectIntBuffer(imageData);
        final CLImage2d<IntBuffer> result = context.createImage2d(imageBuffer, imageWidth, imageData.length / imageWidth, IMAGE_FORMAT, Mem.READ_ONLY);
        clMemRound.add(result);
        return result;
    }

    private CLBuffer<IntBuffer> generateFacetData(final List<Facet> facets, final int facetSize) {
        final int facetArea = facetSize * facetSize;
        final int dataSize = facetArea * Coordinates.DIMENSION;
        final int[] completeData = new int[facets.size() * dataSize];

        int pointer = 0;
        int[] facetData;
        for (Facet f : facets) {
            facetData = f.getData();
            System.arraycopy(facetData, 0, completeData, pointer, dataSize);
            pointer += dataSize;
        }

        final CLBuffer<IntBuffer> result = context.createIntBuffer(completeData.length, Mem.READ_ONLY);
        final IntBuffer resultBuffer = result.getBuffer();
        int index;
        for (int i = 0; i < facetArea; i++) {
            for (int f = 0; f < facets.size(); f++) {
                index = f * dataSize + 2 * i;
                resultBuffer.put(completeData[index]);
                resultBuffer.put(completeData[index + 1]);
            }
        }
        resultBuffer.rewind();

        clMemRound.add(result);
        return result;
    }

    private CLBuffer<FloatBuffer> generateFacetCenters(final List<Facet> facets) {
        final int dataSize = Coordinates.DIMENSION;
        final float[] data = new float[facets.size() * dataSize];

        int pointer = 0;
        float[] centerData;
        for (Facet f : facets) {
            centerData = f.getCenter();
            System.arraycopy(centerData, 0, data, pointer, dataSize);
            pointer += dataSize;
        }

        final CLBuffer<FloatBuffer> result = context.createFloatBuffer(data.length, Mem.READ_ONLY);
        final FloatBuffer buffer = result.getBuffer();
        for (float f : data) {
            buffer.put(f);
        }
        buffer.rewind();

        clMemRound.add(result);
        return result;
    }

    private CLBuffer<FloatBuffer> generateDeformations(final double[] deformations) {
        final CLBuffer<FloatBuffer> result = context.createFloatBuffer(deformations.length, Mem.READ_ONLY);
        final FloatBuffer buffer = result.getBuffer();
        for (double d : deformations) {
            buffer.put((float) d);
        }
        buffer.rewind();

        clMemRound.add(result);
        return result;
    }

    private int calculateLws0base(final CLKernel kernel) {
        final IntBuffer val = Buffers.newDirectIntBuffer(2);
        context.getCL().clGetKernelWorkGroupInfo(kernel.getID(), device.getID(), CLKernelBinding.CL_KERNEL_PREFERRED_WORK_GROUP_SIZE_MULTIPLE, Integer.SIZE, val, null);
        return val.get(0);
    }

    private float[] readBuffer(final FloatBuffer buffer) {
        buffer.rewind();
        float[] result = new float[buffer.remaining()];
        for (int i = 0; i < result.length; i++) {
            result[i] = buffer.get(i);
        }
        return result;
    }

    private List<double[]> pickBestResults(final float[] completeResults, final TaskContainer tc, final int facetCount) {
        final List<double[]> result = new ArrayList<>(facetCount);

        final int deformationCount = TaskContainerUtils.getDeformationCount(tc);

        float val, best;
        List<Integer> candidates = new LinkedList<>();
        int baseIndex, bestIndex;
        for (int facet = 0; facet < facetCount; facet++) {
            best = -Float.MAX_VALUE;
            baseIndex = facet * deformationCount;
            for (int def = 0; def < deformationCount; def++) {
                val = completeResults[baseIndex + def];
                if (val > best) {
                    best = val;

                    candidates.clear();
                    candidates.add(def);
                } else if (val == best) {
                    candidates.add(def);
                }
            }

            if (candidates.isEmpty()) {
//                System.err.println("No best value found for facet nr." + facet);
                result.add(new double[]{0, 0});
            } else {
                if (candidates.size() > 1) {
                    Collections.sort(candidates, new DeformationResultSorter(tc));
                }
                bestIndex = candidates.get(0);

                result.add(TaskContainerUtils.extractDeformation(tc, bestIndex));
            }
        }

        return result;
    }

    private void analyze(final float[] completeResults) {
        int counter = 0;
        int groupCount = 0;
        boolean lastNotNan = false;
        for (float f : completeResults) {
            if (Float.isNaN(f)) {
                counter++;
                lastNotNan = false;
            } else if (!lastNotNan) {
                lastNotNan = true;
                groupCount++;
            }
        }
        System.out.println("Found " + counter + " NaN values in " + groupCount + " groups out of " + completeResults.length);        
    }

    private void buildFinalResults(final TaskContainer tc, final int round) {
        final Image img = tc.getImage(round);
        final List<Facet> facets = tc.getFacets(round);
        final List<double[]> results = tc.getResults(round);

        final int width = img.getWidth();
        final int height = img.getHeight();

        final double[][][] finalResults = new double[width][height][];
        final int[][] counter = new int[width][height];

        Facet f;
        double[] d = new double[2];
        int[] data;
        for (int i = 0; i < facets.size(); i++) {
            f = facets.get(i);
            d = results.get(i);

            data = f.getData();

            for (int j = 0; j < data.length - 1; j += 2) {
                if (finalResults[data[j]][data[j + 1]] == null) {
                    finalResults[data[j]][data[j + 1]] = new double[d.length];
                    System.arraycopy(d, 0, finalResults[data[j]][data[j + 1]], 0, d.length);
                    counter[data[j]][data[j + 1]] = 1;
                } else {
                    for (int k = 0; k < d.length; k++) {
                        finalResults[data[j]][data[j + 1]][k] += d[k];
                    }
                    counter[data[j]][data[j + 1]]++;
                }
            }
        }

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                for (int k = 0; k < d.length; k++) {
                    if (counter[i][j] > 1) {
                        finalResults[i][j][k] /= (double) counter[i][j];
                    } else if (counter[i][j] == 0) {
                        finalResults[i][j] = new double[d.length];
                    }
                }
            }
        }

        tc.storeFinalResults(finalResults, round);
    }

}
