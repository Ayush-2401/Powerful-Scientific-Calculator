package com.scientificcalculator.engine;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PostfixEvaluatorTest {

    private final PostfixEvaluator evaluator = new PostfixEvaluator(false);

    @BeforeAll
    static void setUp() {
        ConstantsLoader.loadConstants();
    }

    @Test
    void testEvaluate_simpleAddition() {
        List<String> postfix = List.of("3", "4", "+");
        BigDecimal result = evaluator.evaluate(postfix);
        assertEquals(0, new BigDecimal("7").compareTo(result));
    }

    @Test
    void testEvaluate_operatorPrecedence() {
        List<String> postfix = List.of("3", "4", "2", "*", "+");
        BigDecimal result = evaluator.evaluate(postfix);
        assertEquals(0, new BigDecimal("11").compareTo(result));
    }

    @Test
    void testEvaluate_parentheses() {
        List<String> postfix = List.of("3", "4", "+", "2", "*");
        BigDecimal result = evaluator.evaluate(postfix);
        assertEquals(0, new BigDecimal("14").compareTo(result));
    }

    @Test
    void testEvaluate_allOperators() {
        List<String> postfix = List.of("3", "4", "2", "*", "1", "5", "-", "2", "^", "/", "+");
        BigDecimal result = evaluator.evaluate(postfix);
        // 3 + (4*2) / ((1-5)^2) = 3 + 8 / 16 = 3.5
        assertEquals(0, new BigDecimal("3.5").compareTo(result));
    }

    @Test
    void testEvaluate_unaryMinus() {
        List<String> postfix = List.of("3", "NEG", "4", "+");
        BigDecimal result = evaluator.evaluate(postfix);
        assertEquals(0, new BigDecimal("1").compareTo(result));
    }

    @Test
    void testEvaluate_functions() {
        PostfixEvaluator degreeEvaluator = new PostfixEvaluator(true);
        List<String> postfix = List.of("90", "sin");
        BigDecimal result = degreeEvaluator.evaluate(postfix);
        // sin(90) is 1. Using compareTo with a small tolerance might be needed if
        // precision issues arise,
        // but BigDecimal implementation should be exact for this if handled correctly.
        // However, since we use Math.sin which returns double, and then convert to
        // BigDecimal,
        // we might have small differences. But let's try exact match first or use
        // stripTrailingZeros.
        assertEquals(0, new BigDecimal("1").compareTo(result.stripTrailingZeros()));
    }

    @Test
    void testEvaluate_constants() {
        List<String> postfix = List.of("PI", "2", "*");
        BigDecimal result = evaluator.evaluate(postfix);
        // PI * 2
        BigDecimal expected = new BigDecimal(Math.PI * 2);
        // Compare with some tolerance or rounding
        assertEquals(expected.doubleValue(), result.doubleValue(), 1e-9);
    }
}
