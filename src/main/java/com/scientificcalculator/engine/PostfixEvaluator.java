// File: src/com/scientificcalculator/engine/PostfixEvaluator.java

package com.scientificcalculator.engine;

import com.scientificcalculator.engine.Operators.Operator;
import com.scientificcalculator.engine.Operators.Type;
import java.util.List;
import java.util.Stack;
import java.lang.Math;
import java.util.Map;

public class PostfixEvaluator {

    private Stack<Double> operandStack;
    private boolean isDegreesMode;

    public PostfixEvaluator(boolean isDegreesMode) {
        this.operandStack = new Stack<>();
        this.isDegreesMode = isDegreesMode;
    }

    public double evaluate(List<String> postfixTokens) {
        return evaluate(postfixTokens, null);
    }

    public double evaluate(List<String> postfixTokens, String varName, double varValue) {
        return evaluate(postfixTokens, varName != null ? Map.of(varName, varValue) : null);
    }

    public double evaluate(List<String> postfixTokens, Map<String, Double> variables) {
        operandStack.clear(); // Clear stack for a new evaluation
        System.out.println("PostfixEvaluator Debug: Starting evaluation for tokens: " + postfixTokens);

        for (String token : postfixTokens) {
            System.out.println("PostfixEvaluator Debug:   Processing token: '" + token + "'");
            if (isNumeric(token)) {
                double value = Double.parseDouble(token);
                operandStack.push(value);
                System.out.println("PostfixEvaluator Debug:     -> Pushed Number: " + value + ". Current Stack: " + operandStack);
            } else if (variables != null && variables.containsKey(token)) {
                operandStack.push(variables.get(token));
                System.out.println("PostfixEvaluator Debug:     -> Pushed Variable '" + token + "' as value: " + variables.get(token) + ". Current Stack: " + operandStack);
            } else if (isKnownConstant(token)) {
                double value = ConstantsLoader.getConstantValue(token);
                operandStack.push(value);
                System.out.println("PostfixEvaluator Debug:     -> Pushed Constant: " + value + ". Current Stack: " + operandStack);
            }
            else if (Operators.isOperator(token)) {
                Operator op = Operators.getOperator(token);
                if (op == null) {
                    throw new IllegalArgumentException("Unknown operator: " + token);
                }
                System.out.println("PostfixEvaluator Debug:     - Identified as OPERATOR: " + op.symbol + " (Type: " + op.type + ")");

                if (op.type == Type.BINARY) {
                    if (operandStack.size() < 2) {
                        throw new IllegalArgumentException("Insufficient operands for binary operator '" + op.symbol + "'. Stack size: " + operandStack.size());
                    }
                    double b = operandStack.pop();
                    double a = operandStack.pop();
                    double result = performBinaryOperation(a, b, op.symbol);
                    operandStack.push(result);
                    System.out.println("PostfixEvaluator Debug:     - Popped for binary op: " + a + ", " + b);
                    System.out.println("PostfixEvaluator Debug:     -> Pushed Binary Result: " + result + ". Current Stack: " + operandStack);
                } else if (op.type == Type.UNARY_PREFIX || op.type == Type.UNARY_FUNCTION || op.type == Type.UNARY_POSTFIX) {
                    if (operandStack.size() < 1) {
                        throw new IllegalArgumentException("Insufficient operands for unary operator '" + op.symbol + "'. Stack size: " + operandStack.size());
                    }
                    double a = operandStack.pop();
                    double result = performUnaryOperation(a, op.symbol);
                    operandStack.push(result);
                    System.out.println("PostfixEvaluator Debug:     - Popped for unary op: " + a);
                    System.out.println("PostfixEvaluator Debug:     -> Pushed Unary Result: " + result + ". Current Stack: " + operandStack);
                } else {
                    throw new IllegalArgumentException("Unsupported operator type: " + op.type + " for symbol " + op.symbol);
                }
            } else {
                throw new IllegalArgumentException("Invalid token in postfix expression: " + token);
            }
            System.out.println("PostfixEvaluator Debug:   End of token processing. Stack: " + operandStack);
        }

        if (operandStack.size() != 1) {
            throw new IllegalArgumentException("Invalid postfix expression. Operand stack size: " + operandStack.size());
        }

        System.out.println("PostfixEvaluator Debug: Evaluation complete. Final stack size: " + operandStack.size() + ". Final Stack: " + operandStack);
        return operandStack.pop();
    }

    private double performBinaryOperation(double a, double b, String operator) {
        switch (operator) {
            case "+": return a + b;
            case "-": return a - b;
            case "*": return a * b;
            case "/":
                if (b == 0) throw new ArithmeticException("Division by zero");
                return a / b;
            case "^": return Math.pow(a, b);
            default: throw new IllegalArgumentException("Unknown binary operator: " + operator);
        }
    }

    private double performUnaryOperation(double a, String operator) {
        switch (operator) {
            case "NEG": return -a;
            case "sqrt":
                if (a < 0) throw new ArithmeticException("Square root of negative number");
                return Math.sqrt(a);
            case "sin": return isDegreesMode ? Math.sin(Math.toRadians(a)) : Math.sin(a);
            case "cos": return isDegreesMode ? Math.cos(Math.toRadians(a)) : Math.cos(a);
            case "tan":
                if (isDegreesMode) {
                    double angleRad = Math.toRadians(a);
                    if (Math.abs(Math.cos(angleRad)) < 1e-9) { // Check for values close to 90/270 degrees
                        throw new ArithmeticException("Tangent of " + a + " degrees is undefined.");
                    }
                    return Math.tan(angleRad);
                } else {
                    if (Math.abs(Math.cos(a)) < 1e-9) { // Check for values close to pi/2, 3pi/2 radians
                        throw new ArithmeticException("Tangent of " + a + " radians is undefined.");
                    }
                    return Math.tan(a);
                }
            case "log":
                if (a <= 0) throw new ArithmeticException("Logarithm of non-positive number");
                return Math.log10(a);
            case "ln":
                if (a <= 0) throw new ArithmeticException("Natural logarithm of non-positive number");
                return Math.log(a);
            case "1/x":
                if (a == 0) throw new ArithmeticException("Division by zero");
                return 1.0 / a;
            case "!":
                if (a < 0 || a != Math.floor(a)) throw new ArithmeticException("Factorial of negative or non-integer number");
                return factorial((int) a);
            case "%": // This is the new UNARY_POSTFIX percentage logic
                return a / 100.0;
            default: throw new IllegalArgumentException("Unknown unary operator: " + operator);
        }
    }

    // Helper for factorial calculation
    private long factorial(int n) {
        if (n > 20) { // factorial(21) overflows long
            throw new ArithmeticException("Factorial input " + n + " is too large for accurate calculation (max 20).");
        }
        if (n == 0) return 1;
        long result = 1;
        for (int i = 1; i <= n; i++) {
            result *= i;
        }
        return result;
    }

    private boolean isNumeric(String str) {
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
}
