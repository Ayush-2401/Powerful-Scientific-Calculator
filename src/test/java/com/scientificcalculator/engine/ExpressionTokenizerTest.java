package com.scientificcalculator.engine;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ExpressionTokenizerTest {

    @Test
    void testTokenize_simpleExpression() {
        List<String> tokens = ExpressionTokenizer.tokenize("3 + 4 * 2");
        assertEquals(List.of("3", "+", "4", "*", "2"), tokens);
    }

    @Test
    void testTokenize_complexExpression() {
        List<String> tokens = ExpressionTokenizer.tokenize("sin(x) * x + PI");
        assertEquals(List.of("sin", "(", "x", ")", "*", "x", "+", "PI"), tokens);
    }

    @Test
    void testTokenize_decimalsAndParens() {
        List<String> tokens = ExpressionTokenizer.tokenize("2.5 * (x - .5)");
        assertEquals(List.of("2.5", "*", "(", "x", "-", ".5", ")"), tokens);
    }

    @Test
    void testTokenize_invalidCharacter() {
        assertThrows(IllegalArgumentException.class, () -> {
            ExpressionTokenizer.tokenize("x $ 2");
        });
    }
}
