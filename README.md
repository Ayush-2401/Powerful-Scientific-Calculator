# Scientific Calculator V1

A modern, web-based scientific calculator with a Java backend and React frontend.

## Prerequisites

-   **Java JDK 21** or higher
-   **Maven**
-   **Node.js** and **npm**

## Getting Started

### Quick Start (Windows)

You can run both the backend and frontend simultaneously using the provided PowerShell script:

1.  Right-click `run_app.ps1` and select "Run with PowerShell".
2.  **Or** run it from the terminal:
    ```powershell
    .\run_app.ps1
    ```

### Manual Start

If you prefer to run them manually:

1.  **Backend**: `mvn compile exec:java -Dexec.mainClass="com.scientificcalculator.server.WebServer"`
2.  **Frontend**: `cd calculator-ui-react` then `npm run dev`

The application will typically be available at `http://localhost:5173` (check the terminal output for the exact URL).

## Running Tests

To verify the backend logic, run the unit tests from the root directory:

```bash
mvn test
```

## Features

-   **High Precision**: Uses `BigDecimal` for accurate calculations.
-   **Session Management**: Supports multiple concurrent users.
-   **Scientific Functions**: Trigonometry, logarithms, exponents, and more.
-   **Interactive Graphing Sandbox**: A robust workspace for visual mathematics supporting:
    -   *Plotting Modes*: Explicit, Implicit, Parametric, and 3D graphing.
    -   *Calculus Visualizer*: Live tangent lines, Riemann sums, and definite integration overlays.
    -   *Parameter Sliders*: Dynamic slider controls for evaluating equations with varying variables.
    -   *Regression Analysis*: Fit linear and quadratic curves directly to input data points.
-   **Modern UI**: Responsive, dark-themed interface.

