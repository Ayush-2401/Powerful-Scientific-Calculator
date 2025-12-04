
package com.scientificcalculator.engine;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
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
        double result = evaluator.evaluate(postfix);
        assertEquals(7.0, result, 1e-9);
    }

    @Test
    void testEvaluate_operatorPrecedence() {
        List<String> postfix = List.of("3", "4", "2", "*", "+");
        double result = evaluator.evaluate(postfix);
        assertEquals(11.0, result, 1e-9);
    }

    @Test
    void testEvaluate_parentheses() {
        List<String> postfix = List.of("3", "4", "+", "2", "*");
        double result = evaluator.evaluate(postfix);
        assertEquals(14.0, result, 1e-9);
    }

    @Test
    void testEvaluate_allOperators() {
        List<String> postfix = List.of("3", "4", "2", "*", "1", "5", "-", "2", "^", "/", "+");
        double result = evaluator.evaluate(postfix);
        assertEquals(3.5, result, 1e-9);
    }

    @Test
    void testEvaluate_unaryMinus() {
        List<String> postfix = List.of("3", "NEG", "4", "+");
        double result = evaluator.evaluate(postfix);
        assertEquals(1.0, result, 1e-9);
    }

    @Test
    void testEvaluate_functions() {
        PostfixEvaluator degreeEvaluator = new PostfixEvaluator(true);
        List<String> postfix = List.of("90", "sin");
        double result = degreeEvaluator.evaluate(postfix);
        assertEquals(1.0, result, 1e-9);
    }

    @Test
    void testEvaluate_constants() {
        List<String> postfix = List.of("PI", "2", "*");
        double result = evaluator.evaluate(postfix);
        assertEquals(6.2831853072, result, 1e-9);
    }
}
