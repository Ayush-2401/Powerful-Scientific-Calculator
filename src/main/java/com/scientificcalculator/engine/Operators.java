// File: src/com/scientificcalculator/engine/Operators.java

package com.scientificcalculator.engine;

import java.util.HashMap;
import java.util.Map;

public class Operators {

    public enum Type {
        BINARY,
        UNARY_PREFIX,
        UNARY_POSTFIX,
        PARENTHESIS,
        UNARY_FUNCTION // For functions like sin, cos, log that take one argument
    }

    public enum Associativity {
        LEFT,
        RIGHT
    }

    public static class Operator {
        public final String symbol;
        public final int precedence;
        public final Type type;
        public final Associativity associativity;

        public Operator(String symbol, int precedence, Type type, Associativity associativity) {
            this.symbol = symbol;
            this.precedence = precedence;
            this.type = type;
            this.associativity = associativity;
        }
    }

    private static final Map<String, Operator> OPERATORS = new HashMap<>();

    static {
        // Binary Operators
        OPERATORS.put("+", new Operator("+", 2, Type.BINARY, Associativity.LEFT));
        OPERATORS.put("-", new Operator("-", 2, Type.BINARY, Associativity.LEFT));
        OPERATORS.put("*", new Operator("*", 3, Type.BINARY, Associativity.LEFT));
        OPERATORS.put("/", new Operator("/", 3, Type.BINARY, Associativity.LEFT));
        OPERATORS.put("^", new Operator("^", 4, Type.BINARY, Associativity.RIGHT)); // x^y
        OPERATORS.put("x^y", new Operator("x^y", 4, Type.BINARY, Associativity.RIGHT)); // Alias for display

        // Unary Prefix Operators (e.g., NEG for unary minus)
        OPERATORS.put("NEG", new Operator("NEG", 5, Type.UNARY_PREFIX, Associativity.RIGHT)); // Internal representation of unary minus

        // Unary Postfix Operators
        OPERATORS.put("!", new Operator("!", 6, Type.UNARY_POSTFIX, Associativity.LEFT));
        OPERATORS.put("%", new Operator("%", 6, Type.UNARY_POSTFIX, Associativity.LEFT)); // Changed to UNARY_POSTFIX

        // Unary Functions (typically prefix, but consume one argument)
        OPERATORS.put("sqrt", new Operator("sqrt", 5, Type.UNARY_FUNCTION, Associativity.RIGHT));
        OPERATORS.put("sin", new Operator("sin", 5, Type.UNARY_FUNCTION, Associativity.RIGHT));
        OPERATORS.put("cos", new Operator("cos", 5, Type.UNARY_FUNCTION, Associativity.RIGHT));
        OPERATORS.put("tan", new Operator("tan", 5, Type.UNARY_FUNCTION, Associativity.RIGHT));
        OPERATORS.put("log", new Operator("log", 5, Type.UNARY_FUNCTION, Associativity.RIGHT));
        OPERATORS.put("ln", new Operator("ln", 5, Type.UNARY_FUNCTION, Associativity.RIGHT));
        OPERATORS.put("1/x", new Operator("1/x", 6, Type.UNARY_POSTFIX, Associativity.LEFT)); // Reciprocal

        // Parentheses
        OPERATORS.put("(", new Operator("(", 0, Type.PARENTHESIS, Associativity.LEFT));
        OPERATORS.put(")", new Operator(")", 0, Type.PARENTHESIS, Associativity.LEFT));
    }

    public static boolean isOperator(String symbol) {
        return OPERATORS.containsKey(symbol);
    }

    public static Operator getOperator(String symbol) {
        return OPERATORS.get(symbol);
    }

    /**
     * Determines if the operator on top of the stack should be popped, based on the current operator being processed.
     * This is the core logic for the shunting-yard algorithm's operator handling.
     *
     * @param opOnStackSymbol The symbol of the operator on the stack.
     * @param currentOpSymbol The symbol of the current operator from the infix expression.
     * @return true if the operator on the stack should be popped, false otherwise.
     */
    public static boolean shouldPop(String opOnStackSymbol, String currentOpSymbol) {
        Operator opOnStack = getOperator(opOnStackSymbol);
        Operator currentOp = getOperator(currentOpSymbol);
        if (opOnStack == null || currentOp == null) {
            throw new IllegalArgumentException("Unknown operator symbol in precedence check.");
        }

        return (opOnStack.precedence > currentOp.precedence) ||
               (opOnStack.precedence == currentOp.precedence && opOnStack.associativity == Associativity.LEFT);
    }

    public static java.util.Set<String> getAllSymbols() {
        return OPERATORS.keySet();
    }
}