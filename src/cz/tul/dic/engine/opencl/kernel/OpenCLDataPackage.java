/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine.opencl.kernel;

import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLMemory;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

/**
 *
 * @author Lenam s.r.o.
 */
public class OpenCLDataPackage {

    private final CLMemory<ByteBuffer> imgA;
    private final CLMemory<ByteBuffer> imgB;
    private final CLBuffer<IntBuffer> subsetData;
    private final CLBuffer<FloatBuffer> subsetCenters;
    private final CLBuffer<IntBuffer> subsetWeights;
    private final CLBuffer<FloatBuffer> deformationLimits;
    private final CLBuffer<LongBuffer> defStepCounts;
    private final CLBuffer<FloatBuffer> results;

    public OpenCLDataPackage(
            final CLMemory<ByteBuffer> imgA, final CLMemory<ByteBuffer> imgB,
            final CLBuffer<IntBuffer> subsetData, final CLBuffer<FloatBuffer> subsetCenters,
            final CLBuffer<IntBuffer> subsetWeights,
            final CLBuffer<FloatBuffer> deformationLimits, final CLBuffer<LongBuffer> defStepCounts,
            final CLBuffer<FloatBuffer> results) {
        this.imgA = imgA;
        this.imgB = imgB;
        this.subsetData = subsetData;
        this.subsetCenters = subsetCenters;
        this.subsetWeights = subsetWeights;
        this.deformationLimits = deformationLimits;
        this.defStepCounts = defStepCounts;
        this.results = results;
    }

    public CLMemory<ByteBuffer> getImgA() {
        return imgA;
    }

    public CLMemory<ByteBuffer> getImgB() {
        return imgB;
    }

    public CLBuffer<IntBuffer> getSubsetData() {
        return subsetData;
    }

    public CLBuffer<FloatBuffer> getSubsetCenters() {
        return subsetCenters;
    }

    public CLBuffer<FloatBuffer> getDeformationLimits() {
        return deformationLimits;
    }

    public CLBuffer<LongBuffer> getDefStepCounts() {
        return defStepCounts;
    }

    public CLBuffer<FloatBuffer> getResults() {
        return results;
    }

    public CLBuffer<IntBuffer> getWeights() {
        return subsetWeights;
    }

    public CLMemory<?>[] getMemoryObjects() {
        return new CLMemory<?>[]{imgA, imgB, subsetData, subsetCenters, deformationLimits, defStepCounts, results};
    }

}
