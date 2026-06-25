package com.scientificcalculator.engine;

public class MatrixMath {

    // 1. Matrix Multiplication
    public static double[][] multiply(double[][] A, double[][] B) {
        if (A == null || B == null || A.length == 0 || B.length == 0) {
            throw new IllegalArgumentException("Matrices cannot be null or empty.");
        }
        int r1 = A.length;
        int c1 = A[0].length;
        int r2 = B.length;
        int c2 = B[0].length;
        if (c1 != r2) {
            throw new IllegalArgumentException("Matrix dimension mismatch: column count of A (" + c1 + ") must match row count of B (" + r2 + ").");
        }

        double[][] result = new double[r1][c2];
        for (int i = 0; i < r1; i++) {
            for (int j = 0; j < c2; j++) {
                double sum = 0.0;
                for (int k = 0; k < c1; k++) {
                    sum += A[i][k] * B[k][j];
                }
                result[i][j] = sum;
            }
        }
        return result;
    }

    // 2. Matrix Transpose
    public static double[][] transpose(double[][] A) {
        if (A == null || A.length == 0) {
            throw new IllegalArgumentException("Matrix cannot be null or empty.");
        }
        int r = A.length;
        int c = A[0].length;
        double[][] result = new double[c][r];
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < c; j++) {
                result[j][i] = A[i][j];
            }
        }
        return result;
    }

    // 3. Matrix Determinant (Cofactor Expansion)
    public static double determinant(double[][] A) {
        if (A == null || A.length == 0) {
            throw new IllegalArgumentException("Matrix cannot be null or empty.");
        }
        int n = A.length;
        for (int i = 0; i < n; i++) {
            if (A[i].length != n) {
                throw new IllegalArgumentException("Matrix must be square.");
            }
        }

        if (n == 1) {
            return A[0][0];
        }
        if (n == 2) {
            return A[0][0] * A[1][1] - A[0][1] * A[1][0];
        }

        double det = 0.0;
        for (int j = 0; j < n; j++) {
            det += Math.pow(-1, j) * A[0][j] * determinant(submatrix(A, 0, j));
        }
        return det;
    }

    private static double[][] submatrix(double[][] A, int row, int col) {
        int n = A.length;
        double[][] sub = new double[n - 1][n - 1];
        int r = 0;
        for (int i = 0; i < n; i++) {
            if (i == row) continue;
            int c = 0;
            for (int j = 0; j < n; j++) {
                if (j == col) continue;
                sub[r][c] = A[i][j];
                c++;
            }
            r++;
        }
        return sub;
    }

    // 4. Matrix Inverse (Gauss-Jordan Elimination)
    public static double[][] invert(double[][] A) {
        if (A == null || A.length == 0) {
            throw new IllegalArgumentException("Matrix cannot be null or empty.");
        }
        int n = A.length;
        for (int i = 0; i < n; i++) {
            if (A[i].length != n) {
                throw new IllegalArgumentException("Matrix must be square.");
            }
        }

        double[][] augmented = new double[n][2 * n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                augmented[i][j] = A[i][j];
            }
            augmented[i][n + i] = 1.0;
        }

        for (int i = 0; i < n; i++) {
            // Find pivot row
            int pivot = i;
            for (int j = i + 1; j < n; j++) {
                if (Math.abs(augmented[j][i]) > Math.abs(augmented[pivot][i])) {
                    pivot = j;
                }
            }

            // Swap rows
            double[] temp = augmented[i];
            augmented[i] = augmented[pivot];
            augmented[pivot] = temp;

            if (Math.abs(augmented[i][i]) < 1e-12) {
                throw new ArithmeticException("Matrix is singular and cannot be inverted.");
            }

            // Scale pivot row
            double pivotVal = augmented[i][i];
            for (int j = i; j < 2 * n; j++) {
                augmented[i][j] /= pivotVal;
            }

            // Eliminate column values in other rows
            for (int j = 0; j < n; j++) {
                if (j != i) {
                    double factor = augmented[j][i];
                    for (int k = i; k < 2 * n; k++) {
                        augmented[j][k] -= factor * augmented[i][k];
                    }
                }
            }
        }

        double[][] inverse = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                inverse[i][j] = augmented[i][n + j];
            }
        }
        return inverse;
    }

    // 5. System Solver (Gaussian elimination with partial pivoting)
    public static double[] solveSystem(double[][] A, double[] b) {
        if (A == null || b == null || A.length == 0 || A.length != b.length) {
            throw new IllegalArgumentException("Matrix A and vector b dimensions must match.");
        }
        int n = b.length;
        for (int i = 0; i < n; i++) {
            if (A[i].length != n) {
                throw new IllegalArgumentException("Coefficient matrix A must be square.");
            }
        }

        // Make copies to avoid modifying original parameters
        double[][] aCopy = new double[n][n];
        for (int i = 0; i < n; i++) {
            System.arraycopy(A[i], 0, aCopy[i], 0, n);
        }
        double[] bCopy = new double[n];
        System.arraycopy(b, 0, bCopy, 0, n);

        for (int p = 0; p < n; p++) {
            // Find pivot row
            int max = p;
            for (int i = p + 1; i < n; i++) {
                if (Math.abs(aCopy[i][p]) > Math.abs(aCopy[max][p])) {
                    max = i;
                }
            }

            // Swap rows
            double[] tempA = aCopy[p];
            aCopy[p] = aCopy[max];
            aCopy[max] = tempA;
            double tempB = bCopy[p];
            bCopy[p] = bCopy[max];
            bCopy[max] = tempB;

            if (Math.abs(aCopy[p][p]) <= 1e-12) {
                throw new ArithmeticException("System coefficient matrix is singular. No unique solution exists.");
            }

            // Pivot column elimination
            for (int i = p + 1; i < n; i++) {
                double alpha = aCopy[i][p] / aCopy[p][p];
                bCopy[i] -= alpha * bCopy[p];
                for (int j = p; j < n; j++) {
                    aCopy[i][j] -= alpha * aCopy[p][j];
                }
            }
        }

        // Back substitution
        double[] x = new double[n];
        for (int i = n - 1; i >= 0; i--) {
            double sum = 0.0;
            for (int j = i + 1; j < n; j++) {
                sum += aCopy[i][j] * x[j];
            }
            x[i] = (bCopy[i] - sum) / aCopy[i][i];
        }
        return x;
    }
}
