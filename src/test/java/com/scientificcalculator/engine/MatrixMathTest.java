package com.scientificcalculator.engine;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class MatrixMathTest {

    @Test
    void testMultiply_2x2() {
        double[][] A = {{1, 2}, {3, 4}};
        double[][] B = {{2, 0}, {1, 2}};
        double[][] result = MatrixMath.multiply(A, B);
        assertArrayEquals(new double[]{4, 4}, result[0], 1e-9);
        assertArrayEquals(new double[]{10, 8}, result[1], 1e-9);
    }

    @Test
    void testTranspose_2x3() {
        double[][] A = {{1, 2, 3}, {4, 5, 6}};
        double[][] result = MatrixMath.transpose(A);
        assertEquals(3, result.length);
        assertEquals(2, result[0].length);
        assertArrayEquals(new double[]{1, 4}, result[0], 1e-9);
        assertArrayEquals(new double[]{2, 5}, result[1], 1e-9);
        assertArrayEquals(new double[]{3, 6}, result[2], 1e-9);
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
    void testArithmetic() {
        double[][] A = {{1, 2}, {3, 4}};
        double[][] B = {{5, 6}, {7, 8}};
        
        // Add
        double[][] addRes = MatrixMath.add(A, B);
        assertArrayEquals(new double[]{6, 8}, addRes[0], 1e-9);
        
        // Subtract
        double[][] subRes = MatrixMath.subtract(A, B);
        assertArrayEquals(new double[]{-4, -4}, subRes[0], 1e-9);
        
        // Scalar multiply
        double[][] scaleRes = MatrixMath.scalarMultiply(A, 2.0);
        assertArrayEquals(new double[]{2, 4}, scaleRes[0], 1e-9);
        
        // Hadamard product
        double[][] prod = MatrixMath.hadamardProduct(A, B);
        assertArrayEquals(new double[]{5, 12}, prod[0], 1e-9);
        
        // Hadamard divide
        double[][] div = MatrixMath.hadamardDivide(A, B);
        assertArrayEquals(new double[]{1.0/5.0, 2.0/6.0}, div[0], 1e-9);
    }

    @Test
    void testPowerAndKroneckerAndDirectSum() {
        double[][] A = {{1, 2}, {3, 4}};
        
        // Power 2
        double[][] pow2 = MatrixMath.power(A, 2);
        double[][] mult = MatrixMath.multiply(A, A);
        assertArrayEquals(mult[0], pow2[0], 1e-9);
        
        // Kronecker product
        double[][] B = {{1, 0}, {0, 1}};
        double[][] kron = MatrixMath.kroneckerProduct(A, B);
        assertEquals(4, kron.length);
        assertEquals(4, kron[0].length);
        assertEquals(1.0, kron[0][0], 1e-9);
        assertEquals(0.0, kron[0][1], 1e-9);
        assertEquals(2.0, kron[0][2], 1e-9);
        
        // Direct sum
        double[][] sum = MatrixMath.directSum(A, B);
        assertEquals(4, sum.length);
        assertEquals(4, sum[0].length);
        assertEquals(1.0, sum[0][0], 1e-9);
        assertEquals(2.0, sum[0][1], 1e-9);
        assertEquals(0.0, sum[0][2], 1e-9);
        assertEquals(1.0, sum[2][2], 1e-9);
    }

    @Test
    void testMinorsAndCofactorsAndPermanent() {
        double[][] A = {{1, 2}, {3, 4}};
        
        // Minors
        double[][] M = MatrixMath.minors(A);
        assertArrayEquals(new double[]{4, 3}, M[0], 1e-9);
        assertArrayEquals(new double[]{2, 1}, M[1], 1e-9);
        
        // Cofactors
        double[][] C = MatrixMath.cofactors(A);
        assertArrayEquals(new double[]{4, -3}, C[0], 1e-9);
        assertArrayEquals(new double[]{-2, 1}, C[1], 1e-9);
        
        // Adjugate
        double[][] adj = MatrixMath.adjugate(A);
        assertArrayEquals(new double[]{4, -2}, adj[0], 1e-9);
        
        // Permanent
        double perm = MatrixMath.permanent(A);
        assertEquals(1*4 + 2*3, perm, 1e-9);
        
        // Cofactor steps
        Map<String, Object> steps = MatrixMath.cofactorExpansionSteps(A, 0, true);
        assertNotNull(steps.get("formula"));
        assertNotNull(steps.get("terms"));
        assertEquals(-2.0, steps.get("result"));
    }

    @Test
    void testLeftRightInverses() {
        // Tall matrix: 3x2
        double[][] A = {
            {1, 2},
            {3, 4},
            {5, 6}
        };
        double[][] leftInv = MatrixMath.leftInverse(A);
        double[][] ident = MatrixMath.multiply(leftInv, A); // left inverse * A = I_2
        assertEquals(2, ident.length);
        assertEquals(1.0, ident[0][0], 1e-9);
        assertEquals(0.0, ident[0][1], 1e-9);
        assertEquals(1.0, ident[1][1], 1e-9);
        
        // Wide matrix: 2x3
        double[][] B = MatrixMath.transpose(A);
        double[][] rightInv = MatrixMath.rightInverse(B);
        double[][] ident2 = MatrixMath.multiply(B, rightInv); // B * right inverse = I_2
        assertEquals(2, ident2.length);
        assertEquals(1.0, ident2[0][0], 1e-9);
        assertEquals(0.0, ident2[0][1], 1e-9);
        assertEquals(1.0, ident2[1][1], 1e-9);
    }

    @Test
    void testRowOperationsAndSubspaces() {
        double[][] A = {
            {1, 2, 3},
            {2, 4, 6},
            {3, 6, 9}
        };
        
        // REF / RREF
        double[][] R = MatrixMath.rref(A);
        assertArrayEquals(new double[]{1, 2, 3}, R[0], 1e-9);
        assertArrayEquals(new double[]{0, 0, 0}, R[1], 1e-9);
        
        // Rank & Nullity
        assertEquals(1, MatrixMath.rank(A));
        assertEquals(2, MatrixMath.nullity(A));
        
        // Null space basis
        List<double[]> nullBasis = MatrixMath.nullSpace(A);
        assertEquals(2, nullBasis.size());
        // Verify A * v = 0
        for (double[] v : nullBasis) {
            double[] b = new double[3];
            for (int i = 0; i < 3; i++) {
                double sum = 0;
                for (int j = 0; j < 3; j++) sum += A[i][j] * v[j];
                b[i] = sum;
            }
            assertArrayEquals(new double[]{0, 0, 0}, b, 1e-9);
        }
        
        // Column space basis
        List<double[]> colBasis = MatrixMath.columnSpace(A);
        assertEquals(1, colBasis.size());
        assertArrayEquals(new double[]{1, 2, 3}, colBasis.get(0), 1e-9);
    }

    @Test
    void testDecompositions() {
        double[][] A = {
            {4, 12, -16},
            {12, 37, -43},
            {-16, -43, 98}
        };
        
        // LU
        Map<String, double[][]> lu = MatrixMath.decomposeLU(A);
        double[][] L = lu.get("L");
        double[][] U = lu.get("U");
        double[][] P = lu.get("P");
        double[][] LU = MatrixMath.multiply(L, U);
        double[][] PA = MatrixMath.multiply(P, A);
        for (int i = 0; i < 3; i++) {
            assertArrayEquals(PA[i], LU[i], 1e-9);
        }
        
        // Cholesky (A is symmetric positive-definite)
        double[][] choleskyL = MatrixMath.decomposeCholesky(A);
        double[][] LLT = MatrixMath.multiply(choleskyL, MatrixMath.transpose(choleskyL));
        for (int i = 0; i < 3; i++) {
            assertArrayEquals(A[i], LLT[i], 1e-9);
        }
        
        // QR
        Map<String, double[][]> qr = MatrixMath.decomposeQR(A);
        double[][] Q = qr.get("Q");
        double[][] R = qr.get("R");
        double[][] QR = MatrixMath.multiply(Q, R);
        for (int i = 0; i < 3; i++) {
            assertArrayEquals(A[i], QR[i], 1e-9);
        }
    }

    @Test
    void testEigenvaluesAndEigenvectors() {
        double[][] A = {
            {2, 0, 0},
            {0, 3, 4},
            {0, 4, 9}
        };
        
        // Char Poly
        double[] coeff = MatrixMath.characteristicPolynomial(A);
        assertEquals(4, coeff.length);
        
        // Roots / Eigenvalues
        MatrixMath.Complex[] ev = MatrixMath.eigenvalues(A);
        assertEquals(3, ev.length);
        
        // Since eigenvalues are real, verify they satisfy characteristic equation
        for (MatrixMath.Complex val : ev) {
            assertEquals(0.0, val.im, 1e-9);
            // Verify (A - lambda I) is singular (has determinant close to 0)
            double[][] term = {
                {A[0][0] - val.re, A[0][1], A[0][2]},
                {A[1][0], A[1][1] - val.re, A[1][2]},
                {A[2][0], A[2][1], A[2][2] - val.re}
            };
            assertEquals(0.0, MatrixMath.determinant(term), 1e-5);
        }
    }

    @Test
    void testPropertiesAndPresets() {
        double[][] A = {{1, 0}, {0, 1}};
        assertEquals(2.0, MatrixMath.trace(A));
        
        Map<String, Double> norms = MatrixMath.norms(A);
        assertEquals(Math.sqrt(2.0), norms.get("frobenius"), 1e-9);
        assertEquals(1.0, norms.get("oneNorm"), 1e-9);
        
        List<String> classify = MatrixMath.classifyMatrix(A);
        assertTrue(classify.contains("identity"));
        assertTrue(classify.contains("symmetric"));
        
        // Presets
        double[][] hilbert = MatrixMath.generatePreset("hilbert", 2, 2, null);
        assertEquals(1.0, hilbert[0][0], 1e-9);
        assertEquals(0.5, hilbert[0][1], 1e-9);
    }
}
