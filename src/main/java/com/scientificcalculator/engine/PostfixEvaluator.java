// File: src/com/scientificcalculator/engine/PostfixEvaluator.java

package com.scientificcalculator.engine;

import com.scientificcalculator.engine.Operators.Operator;
import com.scientificcalculator.engine.Operators.Type;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.Stack;
import java.util.Map;

public class PostfixEvaluator {

    private Stack<BigDecimal> operandStack;
    private boolean isDegreesMode;
    // Use a precision of 34 digits (DECIMAL128) for internal calculations
    private static final MathContext MC = new MathContext(34, RoundingMode.HALF_UP);

    public PostfixEvaluator(boolean isDegreesMode) {
        this.operandStack = new Stack<>();
        this.isDegreesMode = isDegreesMode;
    }

    public BigDecimal evaluate(List<String> postfixTokens) {
        return evaluate(postfixTokens, null);
    }

    public BigDecimal evaluate(List<String> postfixTokens, String varName, BigDecimal varValue) {
        return evaluate(postfixTokens, varName != null ? Map.of(varName, varValue) : null);
    }

    public BigDecimal evaluate(List<String> postfixTokens, Map<String, BigDecimal> variables) {
        operandStack.clear(); // Clear stack for a new evaluation
        System.out.println("PostfixEvaluator Debug: Starting evaluation for tokens: " + postfixTokens);

        for (String token : postfixTokens) {
            System.out.println("PostfixEvaluator Debug:   Processing token: '" + token + "'");
            if (isNumeric(token)) {
                BigDecimal value = new BigDecimal(token, MC);
                operandStack.push(value);
                System.out.println(
                        "PostfixEvaluator Debug:     -> Pushed Number: " + value + ". Current Stack: " + operandStack);
            } else if (variables != null && variables.containsKey(token)) {
                operandStack.push(variables.get(token));
                System.out.println("PostfixEvaluator Debug:     -> Pushed Variable '" + token + "' as value: "
                        + variables.get(token) + ". Current Stack: " + operandStack);
            } else if (isKnownConstant(token)) {
                // ConstantsLoader should return double, we convert to BigDecimal
                double value = ConstantsLoader.getConstantValue(token);
                operandStack.push(new BigDecimal(value, MC));
                System.out.println("PostfixEvaluator Debug:     -> Pushed Constant: " + value + ". Current Stack: "
                        + operandStack);
            } else if (Operators.isOperator(token)) {
                Operator op = Operators.getOperator(token);
                if (op == null) {
                    throw new IllegalArgumentException("Unknown operator: " + token);
                }
                System.out.println("PostfixEvaluator Debug:     - Identified as OPERATOR: " + op.symbol + " (Type: "
                        + op.type + ")");

                if (op.type == Type.BINARY) {
                    if (operandStack.size() < 2) {
                        throw new IllegalArgumentException("Insufficient operands for binary operator '" + op.symbol
                                + "'. Stack size: " + operandStack.size());
                    }
                    BigDecimal b = operandStack.pop();
                    BigDecimal a = operandStack.pop();
                    BigDecimal result = performBinaryOperation(a, b, op.symbol);
                    operandStack.push(result);
                    System.out.println("PostfixEvaluator Debug:     - Popped for binary op: " + a + ", " + b);
                    System.out.println("PostfixEvaluator Debug:     -> Pushed Binary Result: " + result
                            + ". Current Stack: " + operandStack);
                } else if (op.type == Type.UNARY_PREFIX || op.type == Type.UNARY_FUNCTION
                        || op.type == Type.UNARY_POSTFIX) {
                    if (operandStack.size() < 1) {
                        throw new IllegalArgumentException("Insufficient operands for unary operator '" + op.symbol
                                + "'. Stack size: " + operandStack.size());
                    }
                    BigDecimal a = operandStack.pop();
                    BigDecimal result = performUnaryOperation(a, op.symbol);
                    operandStack.push(result);
                    System.out.println("PostfixEvaluator Debug:     - Popped for unary op: " + a);
                    System.out.println("PostfixEvaluator Debug:     -> Pushed Unary Result: " + result
                            + ". Current Stack: " + operandStack);
                } else {
                    throw new IllegalArgumentException(
                            "Unsupported operator type: " + op.type + " for symbol " + op.symbol);
                }
            } else {
                throw new IllegalArgumentException("Invalid token in postfix expression: " + token);
            }
            System.out.println("PostfixEvaluator Debug:   End of token processing. Stack: " + operandStack);
        }

        if (operandStack.size() != 1) {
            throw new IllegalArgumentException(
                    "Invalid postfix expression. Operand stack size: " + operandStack.size());
        }

        System.out.println("PostfixEvaluator Debug: Evaluation complete. Final stack size: " + operandStack.size()
                + ". Final Stack: " + operandStack);
        return operandStack.pop();
    }

    private BigDecimal performBinaryOperation(BigDecimal a, BigDecimal b, String operator) {
        switch (operator) {
            case "+":
                return a.add(b, MC);
            case "-":
                return a.subtract(b, MC);
            case "*":
                return a.multiply(b, MC);
            case "/":
                if (b.compareTo(BigDecimal.ZERO) == 0)
                    throw new ArithmeticException("Division by zero");
                return a.divide(b, MC);
            case "^":
                // BigDecimal pow(int) only accepts integer exponents. For double exponents, we
                // use Math.pow
                // This is a limitation where we must fallback to double for non-integer powers
                try {
                    int exponent = b.intValueExact();
                    // Check for negative exponent which is not supported by pow(int) in older Java
                    // versions or behaves differently
                    // Actually BigDecimal.pow(int) works for positive integers. For negative, we
                    // need to handle.
                    if (exponent < 0) {
                        return BigDecimal.ONE.divide(a.pow(-exponent, MC), MC);
                    }
                    return a.pow(exponent, MC);
                } catch (ArithmeticException e) {
                    // Fallback to double for non-integer exponents
                    return new BigDecimal(Math.pow(a.doubleValue(), b.doubleValue()), MC);
                }
            default:
                throw new IllegalArgumentException("Unknown binary operator: " + operator);
        }
    }

    private BigDecimal performUnaryOperation(BigDecimal a, String operator) {
        switch (operator) {
            case "NEG":
                return a.negate(MC);
            case "sqrt":
                if (a.compareTo(BigDecimal.ZERO) < 0)
                    throw new ArithmeticException("Square root of negative number");
                return a.sqrt(MC);
            case "sin":
                double valSin = a.doubleValue();
                return new BigDecimal(isDegreesMode ? Math.sin(Math.toRadians(valSin)) : Math.sin(valSin), MC);
            case "cos":
                double valCos = a.doubleValue();
                return new BigDecimal(isDegreesMode ? Math.cos(Math.toRadians(valCos)) : Math.cos(valCos), MC);
            case "tan":
                double valTan = a.doubleValue();
                double angleRad = isDegreesMode ? Math.toRadians(valTan) : valTan;
                if (Math.abs(Math.cos(angleRad)) < 1e-9) {
                    throw new ArithmeticException("Tangent is undefined.");
                }
                return new BigDecimal(Math.tan(angleRad), MC);
            case "log":
                if (a.compareTo(BigDecimal.ZERO) <= 0)
                    throw new ArithmeticException("Logarithm of non-positive number");
                return new BigDecimal(Math.log10(a.doubleValue()), MC);
            case "ln":
                if (a.compareTo(BigDecimal.ZERO) <= 0)
                    throw new ArithmeticException("Natural logarithm of non-positive number");
                return new BigDecimal(Math.log(a.doubleValue()), MC);
            case "1/x":
                if (a.compareTo(BigDecimal.ZERO) == 0)
                    throw new ArithmeticException("Division by zero");
                return BigDecimal.ONE.divide(a, MC);
            case "!":
                if (a.compareTo(BigDecimal.ZERO) < 0 || a.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0)
                    throw new ArithmeticException("Factorial of negative or non-integer number");
                return new BigDecimal(factorial(a.intValueExact()));
            case "%":
                return a.divide(new BigDecimal(100), MC);
            default:
                throw new IllegalArgumentException("Unknown unary operator: " + operator);
        }
    }

    // Helper for factorial calculation
    private long factorial(int n) {
        if (n > 20) {
            throw new ArithmeticException("Factorial input " + n + " is too large for accurate calculation (max 20).");
        }
        if (n == 0)
            return 1;
        long result = 1;
        for (int i = 1; i <= n; i++) {
            result *= i;
        }
        return result;
    }

    private boolean isNumeric(String str) {
        try {
            new BigDecimal(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isKnownConstant(String str) {
        return ConstantsLoader.getAllConstants().containsKey(str.toUpperCase());
    }
}
