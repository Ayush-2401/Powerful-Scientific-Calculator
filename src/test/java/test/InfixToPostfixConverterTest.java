
package com.scientificcalculator.engine;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class InfixToPostfixConverterTest {

    @BeforeAll
    static void setUp() {
        ConstantsLoader.loadConstants();
    }

    @Test
    void testConvert_simpleAddition() {
        List<String> infix = List.of("3", "+", "4");
        List<String> postfix = InfixToPostfixConverter.convert(infix);
        assertEquals(List.of("3", "4", "+"), postfix);
    }

    @Test
    void testConvert_operatorPrecedence() {
        List<String> infix = List.of("3", "+", "4", "*", "2");
        List<String> postfix = InfixToPostfixConverter.convert(infix);
        assertEquals(List.of("3", "4", "2", "*", "+"), postfix);
    }

    @Test
    void testConvert_parentheses() {
        List<String> infix = List.of("(", "3", "+", "4", ")", "*", "2");
        List<String> postfix = InfixToPostfixConverter.convert(infix);
        assertEquals(List.of("3", "4", "+", "2", "*"), postfix);
    }

    @Test
    void testConvert_allOperators() {
        List<String> infix = List.of("3", "+", "4", "*", "2", "/", "(", "1", "-", "5", ")", "^", "2");
        List<String> postfix = InfixToPostfixConverter.convert(infix);
        assertEquals(List.of("3", "4", "2", "*", "1", "5", "-", "2", "^", "/", "+"), postfix);
    }

    @Test
    void testConvert_unaryMinus() {
        List<String> infix = List.of("-", "3", "+", "4");
        List<String> postfix = InfixToPostfixConverter.convert(infix);
        assertEquals(List.of("3", "NEG", "4", "+"), postfix);
    }

    @Test
    void testConvert_functions() {
        List<String> infix = List.of("sin", "(", "3", ")", "+", "cos", "(", "4", ")");
        List<String> postfix = InfixToPostfixConverter.convert(infix);
        assertEquals(List.of("3", "sin", "4", "cos", "+"), postfix);
    }

    @Test
    void testConvert_constants() {
        List<String> infix = List.of("PI", "*", "2");
        List<String> postfix = InfixToPostfixConverter.convert(infix);
        assertEquals(List.of("PI", "2", "*"), postfix);
    }
}
