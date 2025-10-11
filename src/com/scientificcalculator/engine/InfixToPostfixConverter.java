// File: src/com/scientificcalculator/engine/InfixToPostfixConverter.java

package com.scientificcalculator.engine;

import com.scientificcalculator.engine.Operators.Operator;
import com.scientificcalculator.engine.Operators.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InfixToPostfixConverter {

    public static List<String> convert(List<String> infixTokens) {
        return convert(infixTokens, null);
    }

    public static List<String> convert(List<String> infixTokens, String variableName) {
         if (infixTokens == null || infixTokens.isEmpty() || (infixTokens.size() == 1 && infixTokens.get(0).equals("0"))) {
            return new ArrayList<>();
        }

        List<String> postfix = new ArrayList<>();
        Stack<String> operatorStack = new Stack<>();

        String lastTokenType = "START"; // Tracks the type of the last processed token for unary minus logic

        for (String initialToken : infixTokens) {
            String token = initialToken.equals("x^y") ? "^" : initialToken;

            System.out.println("InfixToPostfixConverter Debug: Processing token: '" + token + "' (Last Token Type: " + lastTokenType + ")");

            // Handle unary minus: if '-' appears at the start of the expression,
            // or immediately after an operator, an opening parenthesis, or a function,
            // it's a unary negation, not a binary subtraction.
            if (token.equals("-")) {
                if (lastTokenType.equals("START") ||
                    lastTokenType.equals("OPERATOR") ||
                    lastTokenType.equals("PAREN_OPEN") ||
                    lastTokenType.equals("FUNCTION")) {
                    token = "NEG"; // Convert binary minus to unary negation
                    System.out.println("InfixToPostfixConverter Debug:   - Converted '-' to UNARY NEGATION (NEG)");
                }
            }

            if (isNumeric(token)) {
                System.out.println("InfixToPostfixConverter Debug:   - Identified as NUMERIC");
                postfix.add(token);
                lastTokenType = "NUMBER";
            } else if (variableName != null && token.equalsIgnoreCase(variableName)) {
                System.out.println("InfixToPostfixConverter Debug:   - Identified as VARIABLE");
                postfix.add(token);
                lastTokenType = "NUMBER"; // Treat as an operand for unary minus logic
            } else if (isKnownConstant(token)) {
                System.out.println("InfixToPostfixConverter Debug:   - Identified as CONSTANT");
                postfix.add(token);
                lastTokenType = "CONSTANT";
            }
            else if (Operators.isOperator(token)) {
                System.out.println("InfixToPostfixConverter Debug:   - Identified as OPERATOR");
                Operator op = Operators.getOperator(token);

                if (op == null) {
                    throw new IllegalArgumentException("Internal Error: Operator symbol '" + token + "' recognized by regex but not found in Operators map.");
                }

                if (op.type == Type.PARENTHESIS) {
                    if (op.symbol.equals("(")) {
                        operatorStack.push(token);
                        lastTokenType = "PAREN_OPEN";
                    } else if (op.symbol.equals(")")) {
                        while (!operatorStack.isEmpty() && !operatorStack.peek().equals("(")) {
                            postfix.add(operatorStack.pop());
                        }
                        if (operatorStack.isEmpty()) {
                            throw new IllegalArgumentException("Mismatched parentheses: No matching '(' for ')'");
                        }
                        operatorStack.pop(); // Pop the '('
                        lastTokenType = "PAREN_CLOSE";
                    }
                } else {
                    while (!operatorStack.isEmpty() && Operators.isOperator(operatorStack.peek())) {
                        String stackTopSymbol = operatorStack.peek();
                        Operator stackTopOp = Operators.getOperator(stackTopSymbol);

                        if (stackTopOp == null) {
                            System.err.println("InfixToPostfixConverter Debug: Operator on stack '" + stackTopSymbol + "' not found in Operators map. Breaking loop.");
                            break;
                        }

                        if (stackTopOp.symbol.equals("(")) {
                            break;
                        }

                        if (Operators.shouldPop(stackTopSymbol, token)) {
                            postfix.add(operatorStack.pop());
                        } else {
                            break;
                        }
                    }
                    operatorStack.push(token);
                    lastTokenType = (op.type == Type.UNARY_FUNCTION || op.type == Type.UNARY_PREFIX) ? "FUNCTION" : "OPERATOR";
                }
            } else {
                throw new IllegalArgumentException("Invalid or unrecognized token in expression: '" + token + "'");
            }
        }

        while (!operatorStack.isEmpty()) {
            String op = operatorStack.pop();
            if (op.equals("(")) {
                throw new IllegalArgumentException("Mismatched parentheses: Missing ')' for '('");
            }
            postfix.add(op);
        }

        System.out.println("InfixToPostfixConverter Debug: Infix: " + infixTokens + " -> Postfix: " + postfix);
        return postfix;
    }

    private static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isKnownConstant(String str) {
        // Constants are loaded at application startup by the CalculatorEngine.
        return ConstantsLoader.getAllConstants().containsKey(str.toUpperCase());
    }

    public static void main(String[] args) {
        System.out.println("--- Testing InfixToPostfixConverter ---");
        ConstantsLoader.loadConstants();

        try {
            System.out.println("Test 1: 3 + 4 * 2 / (1 - 5)^2 + 6");
            List<String> postfix1 = InfixToPostfixConverter.convert(List.of("3", "+", "4", "*", "2", "/", "(", "1", "-", "5", ")", "^", "2", "+", "6"));
            System.out.println("Result: " + postfix1 + " (Expected: [3, 4, 2, *, 1, 5, -, 2, ^, /, +, 6, +])\n");

            System.out.println("Test 2: 25.25 * 0.2"); // Crucial test for this fix
            List<String> postfix10 = InfixToPostfixConverter.convert(List.of("25.25", "*", "0.2"));
            System.out.println("Result: " + postfix10 + " (Expected: [25.25, 0.2, *])\n");

            System.out.println("Test 3: .5 + .3"); // Test numbers starting with a decimal
            List<String> postfix11 = InfixToPostfixConverter.convert(List.of(".5", "+", ".3"));
            System.out.println("Result: " + postfix11 + " (Expected: [.5, .3, +])\n");

            System.out.println("Test 4: 1. + 2."); // Test numbers ending with a decimal
            List<String> postfix12 = InfixToPostfixConverter.convert(List.of("1.", "+", "2."));
            System.out.println("Result: " + postfix12 + " (Expected: [1., 2., +])\n");

            System.out.println("Test 5: -0.5 * 2"); // Test negative decimals
            List<String> postfix13 = InfixToPostfixConverter.convert(List.of("-0.5", "*", "2"));
            System.out.println("Result: " + postfix13 + " (Expected: [-0.5, 2, *])\n");

            System.out.println("Test 6: 10 + .25"); // Test integer + leading decimal
            List<String> postfix14 = InfixToPostfixConverter.convert(List.of("10", "+", ".25"));
            System.out.println("Result: " + postfix14 + " (Expected: [10, .25, +])\n");

        } catch (IllegalArgumentException e) {
            System.err.println("General Error during testing: " + e.getMessage());
        }
        System.out.println("\n--- Testing InfixToPostfixConverter Complete ---");
    }
}