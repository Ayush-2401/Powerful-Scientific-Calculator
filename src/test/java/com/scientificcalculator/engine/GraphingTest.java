package com.scientificcalculator.engine;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class GraphingTest {

    @BeforeAll
    static void setUp() {
        ConstantsLoader.loadConstants();
    }

    @Test
    void testGraphFunctionEvaluation() {
        // Test expression: sin(x) * x
        String expression = "sin(x) * x";
        List<String> infixTokens = ExpressionTokenizer.tokenize(expression);
        assertEquals(List.of("sin", "(", "x", ")", "*", "x"), infixTokens);

        List<String> postfixTokens = InfixToPostfixConverter.convert(infixTokens, "x");
        assertEquals(List.of("x", "sin", "x", "*"), postfixTokens);

        PostfixEvaluator evaluator = new PostfixEvaluator(false); // radian mode

        // sin(PI/2) * (PI/2) = 1 * PI/2 = PI/2
        BigDecimal result = evaluator.evaluate(postfixTokens, "x", BigDecimal.valueOf(Math.PI / 2.0));
        assertEquals(Math.PI / 2.0, result.doubleValue(), 1e-9);
    }

    @Test
    void testGraphFunctionEvaluation_withErrorPoint() {
        // Test expression: 1 / x
        String expression = "1 / x";
        List<String> infixTokens = ExpressionTokenizer.tokenize(expression);
        List<String> postfixTokens = InfixToPostfixConverter.convert(infixTokens, "x");
        PostfixEvaluator evaluator = new PostfixEvaluator(false);

        // At x = 0, should throw ArithmeticException
        assertThrows(ArithmeticException.class, () -> {
            evaluator.evaluate(postfixTokens, "x", BigDecimal.ZERO);
        });

        // At x = 2, should be 0.5
        BigDecimal result = evaluator.evaluate(postfixTokens, "x", BigDecimal.valueOf(2.0));
        assertEquals(0.5, result.doubleValue(), 1e-9);
    }
}
