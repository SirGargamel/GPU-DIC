/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic;

import cz.tul.dic.data.deformation.DeformationOrder;
import cz.tul.dic.data.deformation.DeformationUtils;
import java.util.LinkedList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Lenam s.r.o.
 */
public class DeformationUtilsTest {

    private static final String WRONG_DEGREE = "Wrong degree.";

    @Test
    public void testAbsoluteValue() {
        double[] deformation = new double[]{};
        assertEquals(DeformationUtils.getAbs(deformation), 0, 0.0001);

        deformation = new double[]{0, 0};
        assertEquals(DeformationUtils.getAbs(deformation), 0, 0.0001);

        deformation = new double[]{3, 4};
        assertEquals(DeformationUtils.getAbs(deformation), 5.0, 0.0001);

        deformation = new double[]{-1, 2, 3, 1, 0, 1};
        assertEquals(DeformationUtils.getAbs(deformation), 4.0, 0.0001);
    }

    @Test
    public void testGetDegree() throws ComputationException {
        double[] limits = new double[0];
        try {
            DeformationUtils.getOrderFromLimits(limits);
            fail("Should have failed because of empty limits.");
        } catch (IllegalArgumentException ex) {
            assert true;
        }

        limits = new double[5];
        try {
            DeformationUtils.getOrderFromLimits(limits);
            fail("Should have failed because of illegal count of limits.");
        } catch (IllegalArgumentException ex) {
            assert true;
        }

        limits = new double[16];
        try {
            DeformationUtils.getOrderFromLimits(limits);
            fail("Should have failed because of illegal count of limits.");
        } catch (IllegalArgumentException ex) {
            assert true;
        }

        limits = new double[]{0, 0, 0, 0, 0, 0};
        assertEquals(WRONG_DEGREE, DeformationOrder.ZERO, DeformationUtils.getOrderFromLimits(limits));

        limits = new double[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        assertEquals(WRONG_DEGREE, DeformationOrder.FIRST, DeformationUtils.getOrderFromLimits(limits));

        limits = new double[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        assertEquals(WRONG_DEGREE, DeformationOrder.SECOND, DeformationUtils.getOrderFromLimits(limits));

        double[] deformation = new double[0];
        try {
            DeformationUtils.getDegreeFromValue(deformation);
            fail("Should have failed because of empty deformation.");
        } catch (IllegalArgumentException ex) {
            assert true;
        }

        deformation = new double[1];
        try {
            DeformationUtils.getDegreeFromValue(deformation);
            fail("Should have failed because of empty deformation.");
        } catch (IllegalArgumentException ex) {
            assert true;
        }

        deformation = new double[8];
        try {
            DeformationUtils.getDegreeFromValue(deformation);
            fail("Should have failed because of empty deformation.");
        } catch (IllegalArgumentException ex) {
            assert true;
        }

        deformation = new double[]{0, 0};
        assertEquals(WRONG_DEGREE, DeformationOrder.ZERO, DeformationUtils.getDegreeFromValue(deformation));

        deformation = new double[]{0, 0, 0, 0, 0, 0};
        assertEquals(WRONG_DEGREE, DeformationOrder.FIRST, DeformationUtils.getDegreeFromValue(deformation));

        deformation = new double[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        assertEquals(WRONG_DEGREE, DeformationOrder.SECOND, DeformationUtils.getDegreeFromValue(deformation));
    }

    @Test
    public void testDeformationCountsGeneration() {
        final List<double[]> deformationLimits = new LinkedList<>();

        double[] limits = new double[]{-1, 1, 1, -1, 1, 1};
        deformationLimits.add(limits);
        long[] counts = DeformationUtils.generateDeformationCounts(limits);
        assertEquals(3, counts.length);
        assertArrayEquals(new long[]{3, 3, 9}, counts);

        limits = new double[]{-1, 1, 1, 1, 1, 0};
        deformationLimits.add(limits);
        counts = DeformationUtils.generateDeformationCounts(limits);
        assertEquals(3, counts.length);
        assertArrayEquals(new long[]{3, 1, 3}, counts);

        limits = new double[]{-1, 1, 1, -1, 1, 1, -1, 1, 1, -1, 1, 1, -1, 1, 1, -1, 1, 1};
        deformationLimits.add(limits);
        counts = DeformationUtils.generateDeformationCounts(limits);
        assertEquals(7, counts.length);
        assertArrayEquals(new long[]{3, 3, 3, 3, 3, 3, 729}, counts);

        limits = new double[]{
            -1, 1, 1, -1, -1, 0, -1, 1, 1, -1, 1, 1, -1, 1, 1, -1, 1, 1,
            1, 1, 0, -1, 1, 1, -1, 1, 1, -1, 1, 1, -1, 1, 1, -1, 1, 1};
        deformationLimits.add(limits);
        counts = DeformationUtils.generateDeformationCounts(limits);
        assertEquals(13, counts.length);
        assertArrayEquals(new long[]{3, 1, 3, 3, 3, 3, 1, 3, 3, 3, 3, 3, 59049}, counts);

        final List<long[]> countsList = DeformationUtils.generateDeformationCounts(deformationLimits);
        assertEquals(3, countsList.get(0).length);
        assertArrayEquals(new long[]{3, 3, 9}, countsList.get(0));
        assertEquals(3, countsList.get(1).length);
        assertArrayEquals(new long[]{3, 1, 3}, countsList.get(1));
        assertEquals(7, countsList.get(2).length);
        assertArrayEquals(new long[]{3, 3, 3, 3, 3, 3, 729}, countsList.get(2));
        assertEquals(13, countsList.get(3).length);
        assertArrayEquals(new long[]{3, 1, 3, 3, 3, 3, 1, 3, 3, 3, 3, 3, 59049}, countsList.get(3));
    }

    @Test
    public void testExtractDeformation() throws ComputationException {
        double[] limits = new double[]{-1, 1, 1, -1, 1, 1};
        long[] counts = DeformationUtils.generateDeformationCounts(limits);

        double[] deformation = DeformationUtils.extractDeformationFromLimits(0, limits, counts);
        assertArrayEquals(new double[]{-1, -1}, deformation, 0.001);

        deformation = DeformationUtils.extractDeformationFromLimits(7, limits, counts);
        assertArrayEquals(new double[]{0, 1}, deformation, 0.001);
        
        deformation = DeformationUtils.extractDeformationFromLimits(8, limits, counts);
        assertArrayEquals(new double[]{1, 1}, deformation, 0.001);

        limits = new double[]{-1, 1, 1, -1, 1, 1, -1, 1, 1, -1, 1, 1, -1, 1, 1, -1, 1, 1};
        counts = DeformationUtils.generateDeformationCounts(limits);

        deformation = DeformationUtils.extractDeformationFromLimits(0, limits, counts);
        assertArrayEquals(new double[]{-1, -1, -1, -1, -1, -1}, deformation, 0.001);

        deformation = DeformationUtils.extractDeformationFromLimits(20, limits, counts);
        assertArrayEquals(new double[]{1, -1, 1, -1, -1, -1}, deformation, 0.001);
        
        deformation = DeformationUtils.extractDeformationFromLimits(728, limits, counts);
        assertArrayEquals(new double[]{1, 1, 1, 1, 1, 1}, deformation, 0.001);
    }
    
    @Test
    public void testGetDeformationCoeffCount() {
        DeformationOrder deg = DeformationOrder.ZERO;
        assertEquals(2, DeformationUtils.getDeformationCoeffCount(deg));
        
        deg = DeformationOrder.FIRST;
        assertEquals(6, DeformationUtils.getDeformationCoeffCount(deg));
        
        deg = DeformationOrder.SECOND;
        assertEquals(12, DeformationUtils.getDeformationCoeffCount(deg));
    }
    
    @Test
    public void testGetDeformationLimitsArrayLength() {
        DeformationOrder deg = DeformationOrder.ZERO;
        assertEquals(6, DeformationUtils.getDeformationLimitsArrayLength(deg));
        
        deg = DeformationOrder.FIRST;
        assertEquals(18, DeformationUtils.getDeformationLimitsArrayLength(deg));
        
        deg = DeformationOrder.SECOND;
        assertEquals(36, DeformationUtils.getDeformationLimitsArrayLength(deg));
    }

}
