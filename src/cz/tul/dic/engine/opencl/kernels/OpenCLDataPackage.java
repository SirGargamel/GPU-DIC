/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine.opencl.kernels;

import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLMemory;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.List;

/**
 *
 * @author Lenam s.r.o.
 */
public class OpenCLDataPackage {

    private final CLMemory<IntBuffer> imgA;
    private final CLMemory<IntBuffer> imgB;
    private final CLBuffer<IntBuffer> subsetData;
    private final CLBuffer<FloatBuffer> subsetCenters;
    private final CLBuffer<FloatBuffer> deformationLimits;
    private final CLBuffer<LongBuffer> defStepCounts;
    private final CLBuffer<FloatBuffer> results;

    public OpenCLDataPackage(
            final CLMemory<IntBuffer> imgA, final CLMemory<IntBuffer> imgB,
            final CLBuffer<IntBuffer> subsetData, final CLBuffer<FloatBuffer> subsetCenters,
            final CLBuffer<FloatBuffer> deformationLimits, final CLBuffer<LongBuffer> defStepCounts,
            final CLBuffer<FloatBuffer> results) {
        this.imgA = imgA;
        this.imgB = imgB;
        this.subsetData = subsetData;
        this.subsetCenters = subsetCenters;
        this.deformationLimits = deformationLimits;
        this.defStepCounts = defStepCounts;
        this.results = results;
    }

    public CLMemory<IntBuffer> getImgA() {
        return imgA;
    }

    public CLMemory<IntBuffer> getImgB() {
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

    public CLMemory<?>[] getMemoryObjects() {
        return new CLMemory<?>[]{imgA, imgB, subsetData, subsetCenters, deformationLimits, defStepCounts, results};
    }

}
