# Matrix & Determinant Features — Powerful Scientific Calculator

A complete reference of all matrix and determinant operations to include for a world-class scientific calculator, organized from essential to most advanced.

---

## 1. Input & Setup

| Feature | Description |
|---|---|
| Matrix size selector | Let user define N×M matrices (up to at least 8×8) |
| Multi-matrix storage | Store named matrices A, B, C… for reuse across operations |
| Fraction / decimal mode | Accept entries like `3/4` and return exact fractional results |
| Preset matrices | Quick-fill: Identity (I), Zero, Random, Hilbert, Pascal, Vandermonde |
| Complex number entries | Support entries like `3+2i` for Hermitian / unitary matrix work |
| Symbolic entries | Allow variables like `a`, `b`, `x` for exact algebraic output |

---

## 2. Basic Matrix Operations

| Operation | Formula / Notes |
|---|---|
| Addition / subtraction | A + B, A − B (same dimensions required) |
| Scalar multiplication | k · A — multiply every element by a constant |
| Matrix multiplication | A × B (columns of A must equal rows of B) |
| Transpose | Aᵀ — flip rows and columns |
| Conjugate transpose (Hermitian) | A† = (Ā)ᵀ — used for complex matrices |
| Element-wise (Hadamard) product | A ⊙ B — multiply corresponding elements |
| Element-wise division | A ÷ B — divide corresponding elements |
| Integer matrix power | Aⁿ for integer n, including A⁻¹ shorthand for n = −1 |
| Kronecker product | A ⊗ B — block matrix product used in quantum computing |
| Khatri-Rao product | Column-wise Kronecker product |
| Direct sum | A ⊕ B — block diagonal combination of two matrices |

---

## 3. Determinants

### Core determinant operations

| Operation | Description |
|---|---|
| det(A) | Compute determinant via cofactor expansion or LU decomposition |
| Minors Mᵢⱼ | Sub-determinant with row i and column j removed |
| Cofactors Cᵢⱼ | Cᵢⱼ = (−1)^(i+j) · Mᵢⱼ |
| Cofactor expansion toggle | Let user pick any row or column to expand along |
| Adjugate / classical adjoint | adj(A) = transpose of the cofactor matrix |
| Permanent | Like determinant but all signs positive — combinatorics use |

### Determinant properties (verify and display)

| Property | What to show |
|---|---|
| Multiplicativity | det(AB) = det(A) · det(B) |
| Transpose invariance | det(Aᵀ) = det(A) |
| Row / column operations | Show effect on det when scaling, swapping, or adding rows |
| Block determinant | det of block matrix: det(A)·det(D − CA⁻¹B) (Schur complement) |
| Leibniz formula | Explicit sum over all permutations (show for small n) |

---

## 4. Matrix Inverse & Generalized Inverses

| Operation | Method |
|---|---|
| Inverse A⁻¹ | Via adj(A)/det(A) or Gauss-Jordan elimination |
| Pseudo-inverse (Moore-Penrose) A⁺ | For non-square or singular matrices via SVD |
| Left inverse | (AᵀA)⁻¹Aᵀ — when A is tall (more rows than columns) |
| Right inverse | Aᵀ(AAᵀ)⁻¹ — when A is wide (more columns than rows) |
| Drazin inverse | Generalized inverse for square singular matrices |
| Group inverse | Special case of Drazin inverse when index = 1 |
| Sherman-Morrison formula | (A + uvᵀ)⁻¹ — rank-1 update of existing inverse |
| Woodbury identity | (A + UCV)⁻¹ — rank-k update using matrix inversion lemma |

---

## 5. Rank, Nullity & Row Operations

### Row reduction

| Operation | Description |
|---|---|
| Row Echelon Form (REF) | Forward elimination to upper-triangular form |
| Reduced Row Echelon Form (RREF) | Full Gauss-Jordan — pivots = 1, zeros above & below |
| Partial pivoting | Swap rows to choose largest pivot (improves numerical stability) |
| Step-by-step mode | Show each elementary row operation applied |

### Space properties

| Property | Formula |
|---|---|
| Rank | Number of pivot rows after row reduction |
| Nullity | n − rank(A) (rank-nullity theorem) |
| Null space (kernel) | Solve Ax = 0 — return basis vectors |
| Column space (image) | Basis of linearly independent columns |
| Row space | Basis of linearly independent rows |
| Left null space | Null space of Aᵀ |
| Four fundamental subspaces | Display all four: row space, null space, column space, left null space |

---

## 6. Solving Linear Systems

| Operation | Description |
|---|---|
| Augmented matrix [A \| b] | Solve Ax = b using RREF; report unique / infinite / no solution |
| Cramer's rule | xᵢ = det(Aᵢ) / det(A) for each variable |
| LU solve | Forward and back substitution using LU decomposition |
| Least squares solution | x = A⁺b — minimize ‖Ax − b‖ for overdetermined systems |
| Homogeneous system | Solve Ax = 0 and return null space basis |
| Multiple right-hand sides | Solve AX = B for matrix B in one step |

---

## 7. Matrix Decompositions

| Decomposition | Formula | Use |
|---|---|---|
| LU decomposition | A = LU (or PA = LU with pivoting) | Solving systems, det computation |
| QR decomposition | A = QR | Least squares, eigenvalue algorithms |
| Cholesky | A = LLᵀ | Symmetric positive-definite systems |
| SVD | A = UΣVᵀ | Pseudo-inverse, data compression, rank |
| Eigendecomposition | A = PDP⁻¹ | Diagonalizable square matrices |
| Schur decomposition | A = QTQ* | Every square matrix; T upper-triangular |
| Jordan Normal Form | A = PJP⁻¹ | Non-diagonalizable matrices; J has Jordan blocks |
| Polar decomposition | A = UP | U unitary, P positive semi-definite |
| Hessenberg reduction | A = QHQᵀ | Pre-processing step for eigenvalue algorithms |
| Bidiagonal decomposition | A = UBVᵀ, B bidiagonal | Intermediate step for SVD algorithms |
| Complete orthogonal decomp. | A = UTV* | Rank-revealing, combines QR + SVD ideas |
| Rank-revealing QR (RRQR) | AP = QR with column pivoting | Numerically stable rank detection |

---

## 8. Eigenvalues & Eigenvectors

| Feature | Description |
|---|---|
| Eigenvalues λ | Solve characteristic equation det(A − λI) = 0 |
| Eigenvectors | For each λ, solve (A − λI)x = 0 |
| Characteristic polynomial | Display p(λ) = det(A − λI) symbolically |
| Algebraic multiplicity | How many times each λ appears as a root |
| Geometric multiplicity | Dimension of the eigenspace for each λ |
| Diagonalization | Express A = PDP⁻¹; show P (eigenvectors) and D (eigenvalues) |
| Generalized eigenvectors | For defective matrices where full set of eigenvectors doesn't exist |
| Spectral radius | ρ(A) = max|λᵢ| — largest absolute eigenvalue |
| Dominant eigenvalue (power method) | Iterative algorithm — show convergence |
| Singular values | σᵢ = √(eigenvalues of AᵀA) — from SVD |
| Gershgorin circle theorem | Visualize regions that bound all eigenvalues |
| Cayley-Hamilton theorem | Show that A satisfies its own characteristic equation p(A) = 0 |

---

## 9. Matrix Properties & Classification

| Property | What to compute / display |
|---|---|
| Trace | tr(A) = Σaᵢᵢ |
| Determinant sign & magnitude | Indicate positive / negative / zero |
| Frobenius norm | ‖A‖_F = √(Σ aᵢⱼ²) |
| 1-norm (column sum norm) | max column sum of absolute values |
| ∞-norm (row sum norm) | max row sum of absolute values |
| 2-norm (spectral norm) | Largest singular value σ_max |
| Nuclear norm | Sum of all singular values Σσᵢ |
| Condition number | κ(A) = ‖A‖ · ‖A⁻¹‖ — measures numerical sensitivity |
| Positive definiteness | Check all eigenvalues > 0 (or use Sylvester's criterion) |
| Sylvester's criterion | All leading principal minors > 0 ↔ positive definite |
| Type auto-detection | Label: symmetric, orthogonal, unitary, Hermitian, diagonal, triangular, idempotent, involutory, nilpotent, normal, circulant, Toeplitz, Hankel |

---

## 10. Advanced Determinant Identities

| Identity | Formula |
|---|---|
| Matrix determinant lemma | det(A + uvᵀ) = (1 + vᵀA⁻¹u) · det(A) |
| Sylvester's identity | Ratio of determinants of sub-matrices |
| Jacobi's identity | Relationship between det(A⁻¹) and cofactors |
| Cauchy-Binet formula | det(AB) for non-square A, B |
| Dodgson condensation | Recursive determinant via 2×2 sub-determinants |
| Vandermonde determinant | det = Πᵢ>ⱼ (xᵢ − xⱼ) |
| Circulant determinant | det = Π f(ωᵏ) where ω is an nth root of unity |

---

## 11. Special Matrix Functions

| Function | Formula / Notes |
|---|---|
| Matrix exponential eᴬ | Via diagonalization or Padé approximation — used in ODEs |
| Matrix logarithm ln(A) | Inverse of matrix exponential |
| Matrix square root √A | B such that B² = A |
| Matrix sine / cosine | sin(A), cos(A) via Taylor series on eigenvalues |
| Matrix polynomial p(A) | Evaluate any polynomial on a matrix |
| Sign function sgn(A) | A(A²)^(−1/2) — used in polar decomposition |
| Matrix absolute value |A| | (AᵀA)^(1/2) |
| Resolvent | (λI − A)⁻¹ — fundamental in spectral theory |

---

## 12. Numerical & Iterative Methods

| Method | Purpose |
|---|---|
| Gaussian elimination with partial pivoting | Numerically stable linear system solver |
| Jacobi iteration | Iterative solver for diagonally dominant systems |
| Gauss-Seidel iteration | Faster convergence than Jacobi for many systems |
| Successive over-relaxation (SOR) | Accelerated Gauss-Seidel with relaxation parameter ω |
| Conjugate gradient method | For large symmetric positive-definite systems |
| Power iteration | Find dominant eigenvalue/eigenvector iteratively |
| Inverse iteration | Find eigenvalue nearest to a shift μ |
| QR algorithm | Full eigenvalue computation via repeated QR steps |
| Gram-Schmidt orthogonalization | Compute orthonormal basis from any set of vectors |
| Modified Gram-Schmidt | Numerically stable version |
| Householder reflections | Stable QR decomposition method |
| Givens rotations | Sparse-friendly QR decomposition |

---

## 13. Bonus / Specialized Features

| Feature | Description |
|---|---|
| Tensor product display | Visualize A ⊗ B as a structured block matrix |
| Commutator [A, B] | AB − BA — measures non-commutativity |
| Anti-commutator {A, B} | AB + BA — used in quantum mechanics |
| Hadamard inequality | Upper bound: |det(A)| ≤ Π ‖column_i‖ |
| Interlacing eigenvalue theorem | Eigenvalue bounds for sub-matrices |
| Perron-Frobenius theorem | Dominant eigenvalue for non-negative matrices |
| Hurwitz stability check | All eigenvalues have negative real parts? |
| Schur complement | M/D = A − BD⁻¹C for block matrix M |
| Kronecker sum | A ⊕ B = A ⊗ I + I ⊗ B |
| Vectorization vec(A) | Stack columns into a single vector |
| Trace inner product | ⟨A, B⟩ = tr(AᵀB) |
| Step-by-step proof mode | Show worked derivations for any operation |
| Export results | Copy as LaTeX, plain text, CSV, or JSON |

---

## Priority Build Order

1. **Phase 1 — Core:** Input grid, basic operations, determinant (with cofactor steps), inverse, RREF, rank, linear system solver
2. **Phase 2 — Scientific:** Eigenvalues/eigenvectors, characteristic polynomial, LU / QR / SVD decompositions, norms, condition number
3. **Phase 3 — Advanced:** Jordan form, Schur, polar decomposition, matrix functions (eᴬ, √A), generalized inverses, 4 subspaces
4. **Phase 4 — Research-grade:** Numerical iterative methods, advanced determinant identities, tensor operations, symbolic computation, complex number support

---

*Reference compiled for building a powerful matrix & determinant scientific calculator.*
