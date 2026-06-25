package com.scientificcalculator.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MatrixMath {

    // Nested Complex class for eigenvalues
    public static class Complex {
        public double re, im;
        public Complex(double re, double im) {
            this.re = re;
            this.im = im;
        }
        public Complex add(Complex o) {
            return new Complex(this.re + o.re, this.im + o.im);
        }
        public Complex sub(Complex o) {
            return new Complex(this.re - o.re, this.im - o.im);
        }
        public Complex multiply(Complex o) {
            return new Complex(this.re * o.re - this.im * o.im, this.re * o.im + this.im * o.re);
        }
        public Complex divide(Complex o) {
            double denom = o.re * o.re + o.im * o.im;
            if (Math.abs(denom) < 1e-15) {
                return new Complex(0, 0);
            }
            return new Complex((this.re * o.re + this.im * o.im) / denom, (this.im * o.re - this.re * o.im) / denom);
        }
        public double abs() {
            return Math.sqrt(this.re * this.re + this.im * this.im);
        }
    }

    // 1. Matrix Addition
    public static double[][] add(double[][] A, double[][] B) {
        if (A == null || B == null || A.length == 0 || B.length == 0) {
            throw new IllegalArgumentException("Matrices cannot be null or empty.");
        }
        int r = A.length;
        int c = A[0].length;
        if (B.length != r || B[0].length != c) {
            throw new IllegalArgumentException("Matrix dimensions must match for addition.");
        }
        double[][] result = new double[r][c];
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < c; j++) {
                result[i][j] = A[i][j] + B[i][j];
            }
        }
        return result;
    }

    // 2. Matrix Subtraction
    public static double[][] subtract(double[][] A, double[][] B) {
        if (A == null || B == null || A.length == 0 || B.length == 0) {
            throw new IllegalArgumentException("Matrices cannot be null or empty.");
        }
        int r = A.length;
        int c = A[0].length;
        if (B.length != r || B[0].length != c) {
            throw new IllegalArgumentException("Matrix dimensions must match for subtraction.");
        }
        double[][] result = new double[r][c];
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < c; j++) {
                result[i][j] = A[i][j] - B[i][j];
            }
        }
        return result;
    }

    // 3. Scalar Multiplication
    public static double[][] scalarMultiply(double[][] A, double k) {
        if (A == null || A.length == 0) {
            throw new IllegalArgumentException("Matrix cannot be null or empty.");
        }
        int r = A.length;
        int c = A[0].length;
        double[][] result = new double[r][c];
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < c; j++) {
                result[i][j] = A[i][j] * k;
            }
        }
        return result;
    }

    // 4. Element-wise (Hadamard) Product
    public static double[][] hadamardProduct(double[][] A, double[][] B) {
        if (A == null || B == null || A.length == 0 || B.length == 0) {
            throw new IllegalArgumentException("Matrices cannot be null or empty.");
        }
        int r = A.length;
        int c = A[0].length;
        if (B.length != r || B[0].length != c) {
            throw new IllegalArgumentException("Matrix dimensions must match for Hadamard product.");
        }
        double[][] result = new double[r][c];
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < c; j++) {
                result[i][j] = A[i][j] * B[i][j];
            }
        }
        return result;
    }

    // 5. Element-wise Division
    public static double[][] hadamardDivide(double[][] A, double[][] B) {
        if (A == null || B == null || A.length == 0 || B.length == 0) {
            throw new IllegalArgumentException("Matrices cannot be null or empty.");
        }
        int r = A.length;
        int c = A[0].length;
        if (B.length != r || B[0].length != c) {
            throw new IllegalArgumentException("Matrix dimensions must match for element-wise division.");
        }
        double[][] result = new double[r][c];
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < c; j++) {
                if (Math.abs(B[i][j]) < 1e-15) {
                    throw new ArithmeticException("Division by zero in element-wise division.");
                }
                result[i][j] = A[i][j] / B[i][j];
            }
        }
        return result;
    }

    // 6. Matrix Power A^n (including negative power)
    public static double[][] power(double[][] A, int n) {
        if (A == null || A.length == 0) {
            throw new IllegalArgumentException("Matrix cannot be null or empty.");
        }
        int r = A.length;
        if (A[0].length != r) {
            throw new IllegalArgumentException("Matrix must be square for powers.");
        }
        if (n < 0) {
            A = invert(A);
            n = -n;
        }
        double[][] result = new double[r][r];
        for (int i = 0; i < r; i++) {
            result[i][i] = 1.0;
        }
        double[][] base = cloneMatrix(A);
        while (n > 0) {
            if ((n & 1) == 1) {
                result = multiply(result, base);
            }
            base = multiply(base, base);
            n >>= 1;
        }
        return result;
    }

    // 7. Kronecker Product (A ⊗ B)
    public static double[][] kroneckerProduct(double[][] A, double[][] B) {
        if (A == null || B == null || A.length == 0 || B.length == 0) {
            throw new IllegalArgumentException("Matrices cannot be null or empty.");
        }
        int m = A.length, n = A[0].length;
        int p = B.length, q = B[0].length;
        double[][] result = new double[m * p][n * q];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                for (int k = 0; k < p; k++) {
                    for (int l = 0; l < q; l++) {
                        result[i * p + k][j * q + l] = A[i][j] * B[k][l];
                    }
                }
            }
        }
        return result;
    }

    // 8. Direct Sum (A ⊕ B)
    public static double[][] directSum(double[][] A, double[][] B) {
        if (A == null || B == null || A.length == 0 || B.length == 0) {
            throw new IllegalArgumentException("Matrices cannot be null or empty.");
        }
        int r1 = A.length, c1 = A[0].length;
        int r2 = B.length, c2 = B[0].length;
        double[][] result = new double[r1 + r2][c1 + c2];
        for (int i = 0; i < r1; i++) {
            System.arraycopy(A[i], 0, result[i], 0, c1);
        }
        for (int i = 0; i < r2; i++) {
            System.arraycopy(B[i], 0, result[r1 + i], c1, c2);
        }
        return result;
    }

    // 9. Matrix Multiplication
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

    // 10. Matrix Transpose
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

    // 11. Matrix Determinant
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

    public static double[][] submatrix(double[][] A, int row, int col) {
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

    // 12. Minors Matrix
    public static double[][] minors(double[][] A) {
        if (A == null || A.length == 0) {
            throw new IllegalArgumentException("Matrix cannot be null or empty.");
        }
        int n = A.length;
        if (A[0].length != n) {
            throw new IllegalArgumentException("Matrix must be square for minors.");
        }
        double[][] result = new double[n][n];
        if (n == 1) {
            result[0][0] = 1.0;
            return result;
        }
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                result[i][j] = determinant(submatrix(A, i, j));
            }
        }
        return result;
    }

    // 13. Cofactor Matrix
    public static double[][] cofactors(double[][] A) {
        int n = A.length;
        double[][] M = minors(A);
        double[][] C = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                C[i][j] = Math.pow(-1, i + j) * M[i][j];
            }
        }
        return C;
    }

    // 14. Adjugate Matrix
    public static double[][] adjugate(double[][] A) {
        return transpose(cofactors(A));
    }

    // 15. Permanent of Matrix
    public static double permanent(double[][] A) {
        if (A == null || A.length == 0) {
            throw new IllegalArgumentException("Matrix cannot be null or empty.");
        }
        int n = A.length;
        if (A[0].length != n) {
            throw new IllegalArgumentException("Matrix must be square for permanent.");
        }
        if (n > 8) {
            throw new IllegalArgumentException("Permanent is only supported up to 8x8 matrices.");
        }
        return permanentRecursive(A);
    }

    private static double permanentRecursive(double[][] A) {
        int n = A.length;
        if (n == 1) return A[0][0];
        if (n == 2) return A[0][0] * A[1][1] + A[0][1] * A[1][0];
        double perm = 0.0;
        for (int j = 0; j < n; j++) {
            perm += A[0][j] * permanentRecursive(submatrix(A, 0, j));
        }
        return perm;
    }

    // 16. Step-by-step Cofactor Expansion along row or col
    public static Map<String, Object> cofactorExpansionSteps(double[][] A, int index, boolean isRow) {
        if (A == null || A.length == 0) {
            throw new IllegalArgumentException("Matrix cannot be null or empty.");
        }
        int n = A.length;
        if (A[0].length != n) {
            throw new IllegalArgumentException("Matrix must be square.");
        }
        if (index < 0 || index >= n) {
            throw new IllegalArgumentException("Index out of bounds.");
        }

        List<Map<String, Object>> terms = new ArrayList<>();
        double total = 0.0;
        StringBuilder formulaSb = new StringBuilder("det(A) = ");

        for (int k = 0; k < n; k++) {
            int r = isRow ? index : k;
            int c = isRow ? k : index;
            double coeff = A[r][c];
            int sign = ((r + c) % 2 == 0) ? 1 : -1;
            double[][] sub = submatrix(A, r, c);
            double subDet = determinant(sub);
            double termVal = coeff * sign * subDet;
            total += termVal;

            Map<String, Object> term = new HashMap<>();
            term.put("row", r);
            term.put("col", c);
            term.put("coefficient", coeff);
            term.put("sign", sign);
            term.put("submatrix", sub);
            term.put("submatrixDeterminant", subDet);
            term.put("termValue", termVal);
            terms.add(term);

            if (k > 0) {
                formulaSb.append(sign > 0 ? " + " : " - ");
            } else if (sign < 0) {
                formulaSb.append("-");
            }
            formulaSb.append(Math.abs(coeff)).append(" * det(M_").append(r + 1).append(c + 1).append(")");
        }

        Map<String, Object> steps = new HashMap<>();
        steps.put("formula", formulaSb.toString());
        steps.put("terms", terms);
        steps.put("result", total);
        return steps;
    }

    // 17. Matrix Inverse (Gauss-Jordan)
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
            int pivot = i;
            for (int j = i + 1; j < n; j++) {
                if (Math.abs(augmented[j][i]) > Math.abs(augmented[pivot][i])) {
                    pivot = j;
                }
            }

            double[] temp = augmented[i];
            augmented[i] = augmented[pivot];
            augmented[pivot] = temp;

            if (Math.abs(augmented[i][i]) < 1e-12) {
                throw new ArithmeticException("Matrix is singular and cannot be inverted.");
            }

            double pivotVal = augmented[i][i];
            for (int j = i; j < 2 * n; j++) {
                augmented[i][j] /= pivotVal;
            }

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

    // 18. Left Inverse: (A^T * A)^-1 * A^T
    public static double[][] leftInverse(double[][] A) {
        if (A == null || A.length == 0) {
            throw new IllegalArgumentException("Matrix cannot be null or empty.");
        }
        double[][] AT = transpose(A);
        double[][] ATA = multiply(AT, A);
        double[][] ATA_inv = invert(ATA);
        return multiply(ATA_inv, AT);
    }

    // 19. Right Inverse: A^T * (A * A^T)^-1
    public static double[][] rightInverse(double[][] A) {
        if (A == null || A.length == 0) {
            throw new IllegalArgumentException("Matrix cannot be null or empty.");
        }
        double[][] AT = transpose(A);
        double[][] AAT = multiply(A, AT);
        double[][] AAT_inv = invert(AAT);
        return multiply(AT, AAT_inv);
    }

    // 20. Row Echelon Form (REF)
    public static double[][] ref(double[][] A) {
        if (A == null || A.length == 0) {
            throw new IllegalArgumentException("Matrix cannot be null or empty.");
        }
        int rows = A.length;
        int cols = A[0].length;
        double[][] M = cloneMatrix(A);
        int lead = 0;
        for (int r = 0; r < rows; r++) {
            if (lead >= cols) break;
            int i = r;
            while (Math.abs(M[i][lead]) < 1e-12) {
                i++;
                if (i == rows) {
                    i = r;
                    lead++;
                    if (lead == cols) return M;
                }
            }
            if (i != r) {
                double[] temp = M[r];
                M[r] = M[i];
                M[i] = temp;
            }
            for (int k = r + 1; k < rows; k++) {
                double factor = M[k][lead] / M[r][lead];
                for (int j = lead; j < cols; j++) {
                    M[k][j] -= factor * M[r][j];
                }
                M[k][lead] = 0.0;
            }
            lead++;
        }
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (Math.abs(M[i][j]) < 1e-11) M[i][j] = 0.0;
            }
        }
        return M;
    }

    // 21. Reduced Row Echelon Form (RREF)
    public static double[][] rref(double[][] A) {
        return (double[][]) rrefWithSteps(A).get("result");
    }

    // 22. RREF Steps Generator
    public static Map<String, Object> rrefWithSteps(double[][] A) {
        if (A == null || A.length == 0) {
            throw new IllegalArgumentException("Matrix cannot be null or empty.");
        }
        int rows = A.length;
        int cols = A[0].length;
        double[][] M = cloneMatrix(A);

        List<Map<String, Object>> steps = new ArrayList<>();
        Map<String, Object> initialStep = new HashMap<>();
        initialStep.put("description", "Initial Matrix");
        initialStep.put("matrix", cloneMatrix(M));
        steps.add(initialStep);

        int lead = 0;
        for (int r = 0; r < rows; r++) {
            if (lead >= cols) break;
            int i = r;
            while (Math.abs(M[i][lead]) < 1e-12) {
                i++;
                if (i == rows) {
                    i = r;
                    lead++;
                    if (lead == cols) return createRrefResult(M, steps);
                }
            }
            if (i != r) {
                double[] temp = M[r];
                M[r] = M[i];
                M[i] = temp;
                Map<String, Object> step = new HashMap<>();
                step.put("description", "Swap row " + (r + 1) + " and row " + (i + 1));
                step.put("matrix", cloneMatrix(M));
                steps.add(step);
            }

            double val = M[r][lead];
            if (Math.abs(val) > 1e-12 && Math.abs(val - 1.0) > 1e-12) {
                for (int j = 0; j < cols; j++) {
                    M[r][j] /= val;
                }
                M[r][lead] = 1.0;
                Map<String, Object> step = new HashMap<>();
                step.put("description", "Scale row " + (r + 1) + " by " + formatDouble(1.0 / val));
                step.put("matrix", cloneMatrix(M));
                steps.add(step);
            }

            for (int k = 0; k < rows; k++) {
                if (k != r) {
                    double factor = M[k][lead];
                    if (Math.abs(factor) > 1e-12) {
                        for (int j = 0; j < cols; j++) {
                            M[k][j] -= factor * M[r][j];
                        }
                        M[k][lead] = 0.0;
                        Map<String, Object> step = new HashMap<>();
                        step.put("description", "Add " + formatDouble(-factor) + " * row " + (r + 1) + " to row " + (k + 1));
                        step.put("matrix", cloneMatrix(M));
                        steps.add(step);
                    }
                }
            }
            lead++;
        }

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (Math.abs(M[i][j]) < 1e-11) M[i][j] = 0.0;
            }
        }

        return createRrefResult(M, steps);
    }

    private static Map<String, Object> createRrefResult(double[][] M, List<Map<String, Object>> steps) {
        Map<String, Object> res = new HashMap<>();
        res.put("result", M);
        res.put("steps", steps);
        return res;
    }

    // 23. Matrix Rank
    public static int rank(double[][] A) {
        double[][] R = rref(A);
        int rank = 0;
        for (double[] row : R) {
            boolean nonZero = false;
            for (double val : row) {
                if (Math.abs(val) > 1e-9) {
                    nonZero = true;
                    break;
                }
            }
            if (nonZero) rank++;
        }
        return rank;
    }

    // 24. Matrix Nullity
    public static int nullity(double[][] A) {
        return A[0].length - rank(A);
    }

    // 25. Null Space Basis
    public static List<double[]> nullSpace(double[][] A) {
        if (A == null || A.length == 0) {
            throw new IllegalArgumentException("Matrix cannot be null or empty.");
        }
        int m = A.length;
        int n = A[0].length;
        double[][] R = rref(A);

        int[] pivotRow = new int[n];
        java.util.Arrays.fill(pivotRow, -1);
        boolean[] isPivotCol = new boolean[n];

        int r = 0;
        for (int c = 0; c < n; c++) {
            if (r < m && Math.abs(R[r][c] - 1.0) < 1e-9) {
                boolean clean = true;
                for (int i = 0; i < m; i++) {
                    if (i != r && Math.abs(R[i][c]) > 1e-9) {
                        clean = false;
                        break;
                    }
                }
                if (clean) {
                    pivotRow[c] = r;
                    isPivotCol[c] = true;
                    r++;
                }
            }
        }

        List<double[]> basis = new ArrayList<>();
        for (int j = 0; j < n; j++) {
            if (!isPivotCol[j]) {
                double[] vec = new double[n];
                vec[j] = 1.0;
                for (int c = 0; c < n; c++) {
                    if (isPivotCol[c]) {
                        int rowIdx = pivotRow[c];
                        if (rowIdx >= 0 && rowIdx < m) {
                            vec[c] = -R[rowIdx][j];
                        }
                    }
                }
                basis.add(vec);
            }
        }
        return basis;
    }

    // 26. Column Space Basis
    public static List<double[]> columnSpace(double[][] A) {
        if (A == null || A.length == 0) {
            throw new IllegalArgumentException("Matrix cannot be null or empty.");
        }
        int m = A.length;
        int n = A[0].length;
        double[][] R = rref(A);
        List<double[]> basis = new ArrayList<>();
        int r = 0;
        for (int c = 0; c < n; c++) {
            if (r < m && Math.abs(R[r][c] - 1.0) < 1e-9) {
                double[] colVec = new double[m];
                for (int i = 0; i < m; i++) colVec[i] = A[i][c];
                basis.add(colVec);
                r++;
            }
        }
        return basis;
    }

    // 27. Row Space Basis
    public static List<double[]> rowSpace(double[][] A) {
        double[][] R = rref(A);
        int n = A[0].length;
        List<double[]> basis = new ArrayList<>();
        for (double[] row : R) {
            boolean nonZero = false;
            for (double val : row) {
                if (Math.abs(val) > 1e-9) {
                    nonZero = true;
                    break;
                }
            }
            if (nonZero) {
                double[] copy = new double[n];
                System.arraycopy(row, 0, copy, 0, n);
                basis.add(copy);
            }
        }
        return basis;
    }

    // 28. Left Null Space Basis
    public static List<double[]> leftNullSpace(double[][] A) {
        return nullSpace(transpose(A));
    }

    // 29. Cramer's Rule solver
    public static double[] solveCramer(double[][] A, double[] b) {
        if (A == null || b == null) {
            throw new IllegalArgumentException("Arguments cannot be null.");
        }
        int n = A.length;
        if (A[0].length != n || b.length != n) {
            throw new IllegalArgumentException("Matrix A must be square and match vector b size.");
        }
        double detA = determinant(A);
        if (Math.abs(detA) < 1e-12) {
            throw new ArithmeticException("Determinant is zero. Cramer's Rule cannot solve singular systems.");
        }

        double[] x = new double[n];
        for (int i = 0; i < n; i++) {
            double[][] Ai = cloneMatrix(A);
            for (int r = 0; r < n; r++) {
                Ai[r][i] = b[r];
            }
            x[i] = determinant(Ai) / detA;
        }
        return x;
    }

    // 30. LU Solver
    public static double[] solveLU(double[][] A, double[] b) {
        int n = A.length;
        Map<String, double[][]> lu = decomposeLU(A);
        double[][] L = lu.get("L");
        double[][] U = lu.get("U");
        double[][] P = lu.get("P");

        double[] Pb = new double[n];
        for (int i = 0; i < n; i++) {
            double sum = 0.0;
            for (int j = 0; j < n; j++) {
                sum += P[i][j] * b[j];
            }
            Pb[i] = sum;
        }

        double[] y = new double[n];
        for (int i = 0; i < n; i++) {
            double sum = 0.0;
            for (int j = 0; j < i; j++) {
                sum += L[i][j] * y[j];
            }
            y[i] = Pb[i] - sum;
        }

        double[] x = new double[n];
        for (int i = n - 1; i >= 0; i--) {
            if (Math.abs(U[i][i]) < 1e-12) {
                throw new ArithmeticException("Matrix is singular. LU solver cannot solve this system.");
            }
            double sum = 0.0;
            for (int j = i + 1; j < n; j++) {
                sum += U[i][j] * x[j];
            }
            x[i] = (y[i] - sum) / U[i][i];
        }
        return x;
    }

    // 31. System Solver (Gaussian elimination with partial pivoting)
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

        double[][] aCopy = new double[n][n];
        for (int i = 0; i < n; i++) {
            System.arraycopy(A[i], 0, aCopy[i], 0, n);
        }
        double[] bCopy = new double[n];
        System.arraycopy(b, 0, bCopy, 0, n);

        for (int p = 0; p < n; p++) {
            int max = p;
            for (int i = p + 1; i < n; i++) {
                if (Math.abs(aCopy[i][p]) > Math.abs(aCopy[max][p])) {
                    max = i;
                }
            }

            double[] tempA = aCopy[p];
            aCopy[p] = aCopy[max];
            aCopy[max] = tempA;
            double tempB = bCopy[p];
            bCopy[p] = bCopy[max];
            bCopy[max] = tempB;

            if (Math.abs(aCopy[p][p]) <= 1e-12) {
                throw new ArithmeticException("System coefficient matrix is singular. No unique solution exists.");
            }

            for (int i = p + 1; i < n; i++) {
                double alpha = aCopy[i][p] / aCopy[p][p];
                bCopy[i] -= alpha * bCopy[p];
                for (int j = p; j < n; j++) {
                    aCopy[i][j] -= alpha * aCopy[p][j];
                }
            }
        }

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

    // 32. Least Squares Solver
    public static double[] solveLeastSquares(double[][] A, double[] b) {
        if (A == null || b == null) {
            throw new IllegalArgumentException("Arguments cannot be null.");
        }
        int m = A.length;
        int n = A[0].length;
        double[][] AT = transpose(A);
        double[][] ATA = multiply(AT, A);

        double[] ATb = new double[n];
        for (int i = 0; i < n; i++) {
            double sum = 0.0;
            for (int j = 0; j < m; j++) {
                sum += AT[i][j] * b[j];
            }
            ATb[i] = sum;
        }

        return solveSystem(ATA, ATb);
    }

    // 33. LU Decomposition
    public static Map<String, double[][]> decomposeLU(double[][] A) {
        if (A == null || A.length == 0) {
            throw new IllegalArgumentException("Matrix cannot be null or empty.");
        }
        int n = A.length;
        if (A[0].length != n) {
            throw new IllegalArgumentException("Matrix must be square for LU decomposition.");
        }

        double[][] L = new double[n][n];
        double[][] U = new double[n][n];
        double[][] P = new double[n][n];

        for (int i = 0; i < n; i++) {
            P[i][i] = 1.0;
            L[i][i] = 1.0;
            System.arraycopy(A[i], 0, U[i], 0, n);
        }

        for (int i = 0; i < n; i++) {
            int pivot = i;
            for (int j = i + 1; j < n; j++) {
                if (Math.abs(U[j][i]) > Math.abs(U[pivot][i])) {
                    pivot = j;
                }
            }
            if (pivot != i) {
                double[] tempU = U[i];
                U[i] = U[pivot];
                U[pivot] = tempU;

                double[] tempP = P[i];
                P[i] = P[pivot];
                P[pivot] = tempP;

                for (int k = 0; k < i; k++) {
                    double tempL = L[i][k];
                    L[i][k] = L[pivot][k];
                    L[pivot][k] = tempL;
                }
            }

            for (int j = i + 1; j < n; j++) {
                if (Math.abs(U[i][i]) < 1e-12) {
                    L[j][i] = 0.0;
                } else {
                    double factor = U[j][i] / U[i][i];
                    L[j][i] = factor;
                    for (int k = i; k < n; k++) {
                        U[j][k] -= factor * U[i][k];
                    }
                    U[j][i] = 0.0;
                }
            }
        }

        Map<String, double[][]> decomp = new HashMap<>();
        decomp.put("L", L);
        decomp.put("U", U);
        decomp.put("P", P);
        return decomp;
    }

    // 34. QR Decomposition
    public static Map<String, double[][]> decomposeQR(double[][] A) {
        if (A == null || A.length == 0) {
            throw new IllegalArgumentException("Matrix cannot be null or empty.");
        }
        int m = A.length;
        int n = A[0].length;
        double[][] R = cloneMatrix(A);
        double[][] Q = new double[m][m];
        for (int i = 0; i < m; i++) Q[i][i] = 1.0;

        for (int k = 0; k < Math.min(m, n); k++) {
            double[] x = new double[m - k];
            for (int i = k; i < m; i++) x[i - k] = R[i][k];

            double normX = 0.0;
            for (double val : x) normX += val * val;
            normX = Math.sqrt(normX);

            if (normX < 1e-12) continue;

            double sign = (x[0] >= 0) ? 1.0 : -1.0;
            double v0 = x[0] + sign * normX;

            double[] v = new double[m - k];
            v[0] = v0;
            for (int i = 1; i < m - k; i++) v[i] = x[i];

            double normV = 0.0;
            for (double val : v) normV += val * val;
            normV = Math.sqrt(normV);

            if (normV < 1e-12) continue;
            for (int i = 0; i < m - k; i++) v[i] /= normV;

            for (int j = k; j < n; j++) {
                double dot = 0.0;
                for (int i = k; i < m; i++) {
                    dot += R[i][j] * v[i - k];
                }
                for (int i = k; i < m; i++) {
                    R[i][j] -= 2.0 * dot * v[i - k];
                }
            }

            for (int i = 0; i < m; i++) {
                double dot = 0.0;
                for (int j = k; j < m; j++) {
                    dot += Q[i][j] * v[j - k];
                }
                for (int j = k; j < m; j++) {
                    Q[i][j] -= 2.0 * dot * v[j - k];
                }
            }
        }

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (i > j) R[i][j] = 0.0;
                else if (Math.abs(R[i][j]) < 1e-11) R[i][j] = 0.0;
            }
            for (int j = 0; j < m; j++) {
                if (Math.abs(Q[i][j]) < 1e-11) Q[i][j] = 0.0;
            }
        }

        Map<String, double[][]> decomp = new HashMap<>();
        decomp.put("Q", Q);
        decomp.put("R", R);
        return decomp;
    }

    // 35. Cholesky Decomposition
    public static double[][] decomposeCholesky(double[][] A) {
        if (A == null || A.length == 0) {
            throw new IllegalArgumentException("Matrix cannot be null or empty.");
        }
        int n = A.length;
        if (A[0].length != n) {
            throw new IllegalArgumentException("Matrix must be square for Cholesky decomposition.");
        }
        double[][] L = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j <= i; j++) {
                double sum = 0.0;
                for (int k = 0; k < j; k++) {
                    sum += L[i][k] * L[j][k];
                }
                if (i == j) {
                    double val = A[i][i] - sum;
                    if (val <= 0.0) {
                        throw new IllegalArgumentException("Matrix is not symmetric positive-definite.");
                    }
                    L[i][j] = Math.sqrt(val);
                } else {
                    L[i][j] = (A[i][j] - sum) / L[j][j];
                }
            }
        }
        return L;
    }

    // 36. Characteristic Polynomial Coefficients
    public static double[] characteristicPolynomial(double[][] A) {
        if (A == null || A.length == 0) {
            throw new IllegalArgumentException("Matrix cannot be null or empty.");
        }
        int n = A.length;
        if (A[0].length != n) {
            throw new IllegalArgumentException("Matrix must be square.");
        }

        double[] c = new double[n + 1];
        c[n] = 1.0;

        double[][] B = new double[n][n];
        for (int i = 0; i < n; i++) B[i][i] = 1.0;

        for (int k = 1; k <= n; k++) {
            double[][] AB = multiply(A, B);
            double tr = 0.0;
            for (int i = 0; i < n; i++) tr += AB[i][i];
            double ck = -tr / k;
            c[n - k] = ck;

            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    B[i][j] = AB[i][j];
                }
                B[i][i] += ck;
            }
        }
        return c;
    }

    // 37. Complex Eigenvalues Finder via Durand-Kerner on Char Poly
    public static Complex[] eigenvalues(double[][] A) {
        double[] poly = characteristicPolynomial(A);
        return findRoots(poly);
    }

    private static Complex evaluatePolynomial(double[] coeffs, Complex z) {
        Complex sum = new Complex(0, 0);
        Complex zPower = new Complex(1, 0);
        for (double c : coeffs) {
            sum = sum.add(zPower.multiply(new Complex(c, 0)));
            zPower = zPower.multiply(z);
        }
        return sum;
    }

    public static Complex[] findRoots(double[] coeffs) {
        int n = coeffs.length - 1;
        Complex[] roots = new Complex[n];

        Complex init = new Complex(0.4, 0.9);
        Complex current = new Complex(1.0, 0.0);
        for (int i = 0; i < n; i++) {
            roots[i] = current;
            current = current.multiply(init);
        }

        int maxIter = 200;
        double eps = 1e-12;
        for (int iter = 0; iter < maxIter; iter++) {
            boolean converged = true;
            for (int i = 0; i < n; i++) {
                Complex pVal = evaluatePolynomial(coeffs, roots[i]);
                Complex denom = new Complex(1.0, 0.0);
                for (int j = 0; j < n; j++) {
                    if (i != j) {
                        denom = denom.multiply(roots[i].sub(roots[j]));
                    }
                }
                Complex delta = pVal.divide(denom);
                if (delta.abs() > eps) {
                    converged = false;
                }
                roots[i] = roots[i].sub(delta);
            }
            if (converged) break;
        }

        // Round near-zero parts to exactly 0 to avoid noise (e.g. 1e-15 imaginary part)
        for (int i = 0; i < n; i++) {
            if (Math.abs(roots[i].im) < 1e-9) roots[i].im = 0.0;
            if (Math.abs(roots[i].re) < 1e-9) roots[i].re = 0.0;
        }
        return roots;
    }

    // 38. Eigenvectors for real eigenvalues
    public static List<double[]> eigenvectors(double[][] A, double lambda) {
        int n = A.length;
        double[][] A_minus_lambdaI = cloneMatrix(A);
        for (int i = 0; i < n; i++) {
            A_minus_lambdaI[i][i] -= lambda;
        }
        return nullSpace(A_minus_lambdaI);
    }

    // 39. Matrix Trace
    public static double trace(double[][] A) {
        if (A == null || A.length == 0) {
            throw new IllegalArgumentException("Matrix cannot be null or empty.");
        }
        int n = A.length;
        double sum = 0.0;
        for (int i = 0; i < n; i++) {
            sum += A[i][i];
        }
        return sum;
    }

    // 40. Matrix Norms
    public static Map<String, Double> norms(double[][] A) {
        if (A == null || A.length == 0) {
            throw new IllegalArgumentException("Matrix cannot be null or empty.");
        }
        int r = A.length;
        int c = A[0].length;

        double frobenius = 0.0;
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < c; j++) {
                frobenius += A[i][j] * A[i][j];
            }
        }
        frobenius = Math.sqrt(frobenius);

        double oneNorm = 0.0;
        for (int j = 0; j < c; j++) {
            double colSum = 0.0;
            for (int i = 0; i < r; i++) {
                colSum += Math.abs(A[i][j]);
            }
            if (colSum > oneNorm) oneNorm = colSum;
        }

        double infNorm = 0.0;
        for (int i = 0; i < r; i++) {
            double rowSum = 0.0;
            for (int j = 0; j < c; j++) {
                rowSum += Math.abs(A[i][j]);
            }
            if (rowSum > infNorm) infNorm = rowSum;
        }

        Map<String, Double> result = new HashMap<>();
        result.put("frobenius", frobenius);
        result.put("oneNorm", oneNorm);
        result.put("infinityNorm", infNorm);
        return result;
    }

    // 41. Matrix Classification
    public static List<String> classifyMatrix(double[][] A) {
        if (A == null || A.length == 0) {
            throw new IllegalArgumentException("Matrix cannot be null or empty.");
        }
        int r = A.length;
        int c = A[0].length;
        List<String> labels = new ArrayList<>();

        if (r != c) {
            labels.add("rectangular");
            return labels;
        }

        labels.add("square");

        boolean isSymmetric = true;
        boolean isSkewSymmetric = true;
        boolean isDiagonal = true;
        boolean isIdentity = true;
        boolean isUpper = true;
        boolean isLower = true;

        for (int i = 0; i < r; i++) {
            for (int j = 0; j < c; j++) {
                double val = A[i][j];
                if (i != j) {
                    if (Math.abs(val) > 1e-9) isDiagonal = false;
                    if (i > j && Math.abs(val) > 1e-9) isUpper = false;
                    if (i < j && Math.abs(val) > 1e-9) isLower = false;
                } else {
                    if (Math.abs(val - 1.0) > 1e-9) isIdentity = false;
                }

                if (Math.abs(val - A[j][i]) > 1e-9) isSymmetric = false;
                if (Math.abs(val + A[j][i]) > 1e-9) isSkewSymmetric = false;
            }
        }

        if (isSymmetric) labels.add("symmetric");
        if (isSkewSymmetric) labels.add("skew-symmetric");
        if (isDiagonal) labels.add("diagonal");
        if (isIdentity && isDiagonal) labels.add("identity");
        if (isUpper) labels.add("upper-triangular");
        if (isLower) labels.add("lower-triangular");

        try {
            double[][] AT = transpose(A);
            double[][] ATA = multiply(AT, A);
            boolean isOrthogonal = true;
            for (int i = 0; i < r; i++) {
                for (int j = 0; j < c; j++) {
                    double expected = (i == j) ? 1.0 : 0.0;
                    if (Math.abs(ATA[i][j] - expected) > 1e-9) {
                        isOrthogonal = false;
                        break;
                    }
                }
            }
            if (isOrthogonal) labels.add("orthogonal");
        } catch (Exception e) {}

        try {
            double[][] AT = transpose(A);
            double[][] ATA = multiply(AT, A);
            double[][] AAT = multiply(A, AT);
            boolean isNormal = true;
            for (int i = 0; i < r; i++) {
                for (int j = 0; j < c; j++) {
                    if (Math.abs(ATA[i][j] - AAT[i][j]) > 1e-9) {
                        isNormal = false;
                        break;
                    }
                }
            }
            if (isNormal) labels.add("normal");
        } catch (Exception e) {}

        try {
            double det = determinant(A);
            if (Math.abs(det) < 1e-9) labels.add("singular");
            else labels.add("non-singular");
        } catch (Exception e) {}

        return labels;
    }

    // 42. Preset Matrix Generators
    public static double[][] generatePreset(String name, int rows, int cols, double[] params) {
        double[][] M = new double[rows][cols];
        if (name.equalsIgnoreCase("identity")) {
            for (int i = 0; i < Math.min(rows, cols); i++) M[i][i] = 1.0;
        } else if (name.equalsIgnoreCase("zero")) {
            // zero matrix
        } else if (name.equalsIgnoreCase("random")) {
            java.util.Random rand = new java.util.Random();
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    M[i][j] = Math.round((rand.nextDouble() * 20.0 - 10.0) * 10.0) / 10.0;
                }
            }
        } else if (name.equalsIgnoreCase("hilbert")) {
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    M[i][j] = 1.0 / (i + j + 1);
                }
            }
        } else if (name.equalsIgnoreCase("pascal")) {
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    M[i][j] = choose(i + j, i);
                }
            }
        } else if (name.equalsIgnoreCase("vandermonde")) {
            for (int i = 0; i < rows; i++) {
                double x = (params != null && i < params.length) ? params[i] : (i + 1);
                for (int j = 0; j < cols; j++) {
                    M[i][j] = Math.pow(x, j);
                }
            }
        } else {
            throw new IllegalArgumentException("Unknown preset name: " + name);
        }
        return M;
    }

    private static double choose(int n, int k) {
        double val = 1.0;
        for (int i = 1; i <= k; i++) {
            val = val * (n - k + i) / i;
        }
        return Math.round(val);
    }

    // Helper functions
    private static double[][] cloneMatrix(double[][] M) {
        double[][] copy = new double[M.length][M[0].length];
        for (int i = 0; i < M.length; i++) {
            System.arraycopy(M[i], 0, copy[i], 0, M[i].length);
        }
        return copy;
    }

    private static String formatDouble(double val) {
        if (Math.abs(val - Math.round(val)) < 1e-9) {
            return String.valueOf((long) Math.round(val));
        }
        return String.format("%.4f", val);
    }
}
