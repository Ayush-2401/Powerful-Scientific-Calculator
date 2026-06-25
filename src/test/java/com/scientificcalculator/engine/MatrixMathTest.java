package com.scientificcalculator.engine;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MatrixMathTest {

    @Test
    void testMultiply_2x2() {
        double[][] A = {{1, 2}, {3, 4}};
        double[][] B = {{2, 0}, {1, 2}};
        double[][] result = MatrixMath.multiply(A, B);
        assertArrayEquals(new double[]{4, 4}, result[0]);
        assertArrayEquals(new double[]{10, 8}, result[1]);
    }

    @Test
    void testTranspose_2x3() {
        double[][] A = {{1, 2, 3}, {4, 5, 6}};
        double[][] result = MatrixMath.transpose(A);
        assertEquals(3, result.length);
        assertEquals(2, result[0].length);
        assertArrayEquals(new double[]{1, 4}, result[0]);
        assertArrayEquals(new double[]{2, 5}, result[1]);
        assertArrayEquals(new double[]{3, 6}, result[2]);
    }

    @Test
    void testDeterminant_3x3() {
        double[][] A = {
            {1, 2, 3},
            {0, 1, 4},
            {5, 6, 0}
        };
        double det = MatrixMath.determinant(A);
        assertEquals(1.0, det, 1e-9);
    }

    @Test
    void testInverse_3x3() {
        double[][] A = {
            {1, 2, 3},
            {0, 1, 4},
            {5, 6, 0}
        };
        double[][] inv = MatrixMath.invert(A);
        // Inverse A^-1 should satisfy A * A^-1 = I
        double[][] ident = MatrixMath.multiply(A, inv);
        assertEquals(1.0, ident[0][0], 1e-9);
        assertEquals(0.0, ident[0][1], 1e-9);
        assertEquals(0.0, ident[0][2], 1e-9);
        assertEquals(1.0, ident[1][1], 1e-9);
        assertEquals(1.0, ident[2][2], 1e-9);
    }

    @Test
    void testSolveSystem_3x3() {
        double[][] A = {
            {1, 1, 1},
            {2, -1, 1},
            {1, 2, -3}
        };
        double[] b = {6, 3, -4};
        double[] x = MatrixMath.solveSystem(A, b);
        assertArrayEquals(new double[]{1.0, 2.0, 3.0}, x, 1e-9);
    }

    @Test
    void testSingularMatrix_throws() {
        double[][] A = {{1, 2}, {2, 4}};
        assertThrows(ArithmeticException.class, () -> {
            MatrixMath.invert(A);
        });
    }
}
