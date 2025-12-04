package test;

import com.scientificcalculator.engine.CalculatorEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReproductionTest {

    private CalculatorEngine engine;

    @BeforeEach
    void setUp() {
        engine = new CalculatorEngine();
    }

    @Test
    void testNegativeNumber_Subtraction() {
        // 5 - 3 should be 5 - 3, NOT 5 * -3
        engine.processCommand("5");
        engine.processCommand("-");
        engine.processCommand("3");
        String[] result = engine.processCommand("=");
        assertEquals("2", result[0]);
    }

    @Test
    void testNegativeNumber_Multiplication() {
        // 5 * -3 should be -15
        engine.processCommand("5");
        engine.processCommand("*");
        engine.processCommand("-");
        engine.processCommand("3");
        String[] result = engine.processCommand("=");
        assertEquals("-15", result[0]);
    }

    @Test
    void testImplicitMultiplication_NoBracket() {
        // "putting a multiplication where there is no bracket"
        // Maybe 2 3 -> 23?
        engine.processCommand("2");
        engine.processCommand("3");
        String[] result = engine.processCommand("=");
        assertEquals("23", result[0]);
    }

    @Test
    void testImplicitMultiplication_WithBracket() {
        // 2(3) -> 6
        engine.processCommand("2");
        engine.processCommand("(");
        engine.processCommand("3");
        engine.processCommand(")");
        String[] result = engine.processCommand("=");
        assertEquals("6", result[0]);
    }

    @Test
    void testNegativeStart() {
        // -5 + 3 -> -2
        engine.processCommand("-");
        engine.processCommand("5");
        engine.processCommand("+");
        engine.processCommand("3");
        String[] result = engine.processCommand("=");
        assertEquals("-2", result[0]);
    }

    @Test
    void testNegativeFiveMinusFive() {
        // -5 - 5 -> -10
        engine.processCommand("-");
        engine.processCommand("5");
        engine.processCommand("-");
        engine.processCommand("5");
        String[] result = engine.processCommand("=");
        assertEquals("-10", result[0]);
    }
}
