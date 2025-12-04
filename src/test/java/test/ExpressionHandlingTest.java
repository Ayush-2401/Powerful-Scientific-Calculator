package test;

import com.scientificcalculator.engine.CalculatorEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExpressionHandlingTest {

    private CalculatorEngine engine;

    @BeforeEach
    void setUp() {
        engine = new CalculatorEngine();
    }

    @Test
    void testImplicitMultiplication_NumberAndParen() {
        // 2(3) = 6
        engine.processCommand("2");
        engine.processCommand("(");
        engine.processCommand("3");
        engine.processCommand(")");
        String[] result = engine.processCommand("=");
        assertEquals("6", result[0]);
    }

    @Test
    void testImplicitMultiplication_ParenAndParen() {
        // (2)(3) = 6
        engine.processCommand("(");
        engine.processCommand("2");
        engine.processCommand(")");
        engine.processCommand("(");
        engine.processCommand("3");
        engine.processCommand(")");
        String[] result = engine.processCommand("=");
        assertEquals("6", result[0]);
    }

    @Test
    void testUnaryMinus_StartOfExpression() {
        // -5 + 3 = -2
        engine.processCommand("-");
        engine.processCommand("5");
        engine.processCommand("+");
        engine.processCommand("3");
        String[] result = engine.processCommand("=");
        assertEquals("-2", result[0]);
    }

    @Test
    void testUnaryMinus_BeforeParen() {
        // -(2+3) = -5
        engine.processCommand("-");
        engine.processCommand("(");
        engine.processCommand("2");
        engine.processCommand("+");
        engine.processCommand("3");
        engine.processCommand(")");
        String[] result = engine.processCommand("=");
        assertEquals("-5", result[0]);
    }

    @Test
    void testImplicitMultiplication_NumberAndFunction() {
        // 2sin(90) = 2 (in degrees)
        engine.processCommand("2");
        engine.processCommand("sin");
        engine.processCommand("90");
        engine.processCommand(")");
        String[] result = engine.processCommand("=");
        assertEquals("2", result[0]);
    }
}
