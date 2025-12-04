
package com.scientificcalculator.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CalculatorEngineTest {

    private CalculatorEngine engine;

    @BeforeEach
    void setUp() {
        engine = new CalculatorEngine();
    }

    // --- Quadratic Solver Tests ---

    @Test
    void testSolveQuadratic_twoRealRoots() {
        // x^2 - 3x + 2 = 0 -> (x-1)(x-2) -> roots are 1, 2
        String result = engine.solveQuadratic(1, -3, 2);
        assertEquals("x₁=2, x₂=1", result, "Expected roots 1 and 2");
    }

    @Test
    void testSolveQuadratic_oneRealRoot() {
        // x^2 - 2x + 1 = 0 -> (x-1)^2 -> root is 1
        String result = engine.solveQuadratic(1, -2, 1);
        assertEquals("x=1", result);
    }

    @Test
    void testSolveQuadratic_complexRoots() {
        // x^2 + x + 1 = 0 -> roots are -0.5 ± 0.866...i
        String result = engine.solveQuadratic(1, 1, 1);
        assertEquals("x₁ = -0.5+0.8660254038i, x₂ = -0.5-0.8660254038i", result);
    }

    @Test
    void testSolveQuadratic_aIsZero() {
        String result = engine.solveQuadratic(0, 2, -4);
        assertEquals("Error: 'a' cannot be 0", result);
    }

    // --- Cubic Solver Tests ---

    @Test
    void testSolveCubic_threeRealRoots() {
        // (x-1)(x-2)(x-3) = x^3 - 6x^2 + 11x - 6 = 0 -> roots 1, 2, 3
        String result = engine.solveCubic(1, -6, 11, -6);
        assertTrue(result.contains("x₁=3") && result.contains("x₂=2") && result.contains("x₃=1"), "Expected roots 1, 2, 3");
    }

    @Test
    void testSolveCubic_oneRealRoot() {
        // x^3 - 1 = 0 -> root is 1, and two complex
        String result = engine.solveCubic(1, 0, 0, -1);
        assertTrue(result.contains("x₁ = 1.0") && result.contains("x₂ = -0.5+0.8660254038i") && result.contains("x₃ = -0.5-0.8660254038i"), "Expected one real root and two complex");
    }
    
    @Test
    void testSolveCubic_degeneratesToQuadratic() {
        // 0x^3 + x^2 - 3x + 2 = 0 -> same as quadratic test
        String result = engine.solveCubic(0, 1, -3, 2);
        assertEquals("x₁=2, x₂=1", result, "Expected quadratic roots 1 and 2");
    }

    // --- Linear System Tests ---

    @Test
    void testSolveLinearSystem_2x2_uniqueSolution() {
        // 2x + 3y = 8
        // 5x - y = 3  -> x=1, y=2
        double[][] A = {{2, 3}, {5, -1}};
        double[] b = {8, 3};
        Map<String, Double> result = engine.solveLinearSystem(A, b);
        assertEquals(1.0, result.get("x1"), 1e-9);
        assertEquals(2.0, result.get("x2"), 1e-9);
    }

    @Test
    void testSolveLinearSystem_2x2_noUniqueSolution() {
        // x + y = 1
        // 2x + 2y = 3 -> parallel lines, det=0
        double[][] A = {{1, 1}, {2, 2}};
        double[] b = {1, 3};
        assertThrows(ArithmeticException.class, () -> {
            engine.solveLinearSystem(A, b);
        });
    }

    @Test
    void testSolveLinearSystem_3x3_uniqueSolution() {
        // x + y + z = 6
        // 2x - y + z = 3
        // x + 2y - 3z = -4 -> x=1, y=2, z=3
        double[][] A = {{1, 1, 1}, {2, -1, 1}, {1, 2, -3}};
        double[] b = {6, 3, -4};
        Map<String, Double> result = engine.solveLinearSystem(A, b);
        assertEquals(1.0, result.get("x1"), 1e-9);
        assertEquals(2.0, result.get("x2"), 1e-9);
        assertEquals(3.0, result.get("x3"), 1e-9);
    }

    @Test
    void testSolveLinearSystem_3x3_noUniqueSolution() {
        // x + y + z = 1
        // x + y + z = 2
        // x + y + z = 3 -> det=0
        double[][] A = {{1, 1, 1}, {1, 1, 1}, {1, 1, 1}};
        double[] b = {1, 2, 3};
        assertThrows(ArithmeticException.class, () -> {
            engine.solveLinearSystem(A, b);
        });
    }
}
