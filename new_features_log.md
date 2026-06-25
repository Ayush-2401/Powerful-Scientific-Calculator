# New Features Log

This log documents the specifications and design details of the new features being added to the Scientific Calculator, along with coordination details between the backend and frontend agents.

---

## 1. Feature Specifications

### A. Interactive 2D Graphing & Function Plotting
*   **Description**: Plot and inspect mathematical expressions on a coordinate plane.
*   **Backend Requirement**: Evaluate expressions (e.g., `sin(x) * x`) dynamically across a range of $x$-values (e.g., from $x_{min}$ to $x_{max}$ with a specified step size) and return an array/list of $(x, y)$ coordinate pairs.
*   **Frontend Requirement**: A responsive 2D canvas interface under a new **"Graphing"** mode tab. It should support panning, zooming, and drawing the coordinates under the user's cursor.

### B. Matrix & Linear Algebra Workspace
*   **Description**: Create and calculate operations on matrices up to $4 \times 4$.
*   **Backend Requirement**: Implement matrix math engine routines in a class (e.g., `MatrixEngine.java`) to calculate:
    *   Matrix addition and subtraction
    *   Matrix multiplication ($A \times B$)
    *   Determinants ($\det(A)$)
    *   Transposes ($A^T$)
    *   Inverses ($A^{-1}$)
    *   Systems of linear equations using Cramer's Rule or Gaussian elimination
*   **Frontend Requirement**: A **"Matrix Solver"** mode tab presenting input grids for Matrix A and Matrix B with dynamic dimension settings and action buttons.

### C. Engineering Unit & Number Base Converter
*   **Description**: High-precision conversion between scientific/engineering units and computer science number bases.
*   **Backend Requirement**: Handle conversion formulas using `BigDecimal` for zero-loss precision conversions.
    *   *Unit Dimensions*: Energy (Joules, eV, Calories), Pressure (Pascals, atm, bar, psi), Temperature (Kelvin, Celsius, Fahrenheit).
    *   *Number Bases*: Conversions between Binary, Octal, Decimal, and Hexadecimal.
*   **Frontend Requirement**: A **"Converter"** tab with responsive dropdown selectors for conversion category, source unit, and destination unit.

---

## 2. Agent Git Branches
Before starting the development work, the active branches for the two agents are:
*   **Backend Agent Branch**: `cla-endine-and-server`
*   **UI/UX Directory Assessment Agent Branch**: `ui`
