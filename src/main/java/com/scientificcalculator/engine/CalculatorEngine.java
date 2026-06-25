// File: src/com/scientificcalculator/engine/CalculatorEngine.java

package com.scientificcalculator.engine;

import com.scientificcalculator.engine.Operators.Operator;
import com.scientificcalculator.engine.Operators.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.lang.Math;
import java.util.LinkedHashMap;
import java.text.DecimalFormat;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class CalculatorEngine {

    // REFACTORED: Using a list of tokens for robust state management.
    private List<String> infixTokens;

    private String primaryDisplayText;
    private String secondaryDisplayText;

    private BigDecimal memory;
    private boolean isDegreesMode;

    // New, more granular state flags
    private boolean lastInputWasNumber;
    private boolean lastInputWasOperator; // Includes binary, unary, and parentheses
    private boolean lastInputWasConstant;
    private boolean lastInputWasEquals;

    public CalculatorEngine() {
        infixTokens = new ArrayList<>(List.of("0"));
        primaryDisplayText = "0";
        secondaryDisplayText = "0";
        memory = BigDecimal.ZERO;
        isDegreesMode = true;

        lastInputWasNumber = false;
        lastInputWasOperator = false;
        lastInputWasConstant = false;
        lastInputWasEquals = false;

        ConstantsLoader.loadConstants();
    }

    private void resetStateFlags() {
        lastInputWasNumber = false;
        lastInputWasOperator = false;
        lastInputWasConstant = false;
        lastInputWasEquals = false;
    }

    public String[] processCommand(String command) {
        System.out.println("Engine processing command: " + command);

        // Determine command type for clearer logic
        boolean currentCommandIsNumber = isNumeric(command) || command.equals(".");
        boolean currentCommandIsBinaryOperator = Operators.isOperator(command)
                && Operators.getOperator(command).type == Type.BINARY;
        boolean currentCommandIsUnaryFunction = Operators.isOperator(command)
                && (Operators.getOperator(command).type == Type.UNARY_FUNCTION
                        || Operators.getOperator(command).type == Type.UNARY_POSTFIX);
        boolean currentCommandIsConstant = isKnownConstant(command);
        boolean currentCommandIsParenOpen = command.equals("(");
        boolean currentCommandIsParenClose = command.equals(")");
        // A function that should be followed by an opening parenthesis
        boolean currentCommandIsParenFunction = command.equals("sin") || command.equals("cos") || command.equals("tan")
                || command.equals("log") || command.equals("ln") || command.equals("sqrt");

        // --- Clear / CE / Backspace / Sign Change / Angle Mode / Memory ---
        if (command.equals("C")) {
            infixTokens.clear();
            infixTokens.add("0");
            resetStateFlags();
        } else if (command.equals("CE")) {
            // CE clears the current number entry.
            if (lastInputWasNumber) {
                if (infixTokens.size() > 1) {
                    infixTokens.remove(infixTokens.size() - 1);
                } else {
                    infixTokens.set(0, "0");
                }
            }
            // If not entering a number, CE does nothing.
            resetStateFlags();
        } else if (command.equals("<--")) {
            String lastToken = getLastToken();
            if (lastInputWasNumber && isNumeric(lastToken)) {
                // If entering a number, backspace a character from it.
                String newText = lastToken.substring(0, lastToken.length() - 1);
                if (newText.isEmpty() || newText.equals("-")) {
                    // If the number is gone, remove the token.
                    if (infixTokens.size() > 1) {
                        infixTokens.remove(infixTokens.size() - 1);
                    } else {
                        infixTokens.set(0, "0");
                    }
                    resetStateFlags();
                } else {
                    infixTokens.set(infixTokens.size() - 1, newText);
                }
            } else {
                // If not entering a number, remove the entire last token.
                if (infixTokens.size() > 1) {
                    infixTokens.remove(infixTokens.size() - 1);
                } else {
                    infixTokens.set(0, "0");
                }
                resetStateFlags();
            }
        } else if (command.equals("+/-")) {
            String lastToken = getLastToken();
            if (isNumeric(lastToken)) {
                if (lastToken.startsWith("-")) {
                    infixTokens.set(infixTokens.size() - 1, lastToken.substring(1));
                } else if (!lastToken.equals("0")) {
                    infixTokens.set(infixTokens.size() - 1, "-" + lastToken);
                }
            } else if (lastInputWasOperator) {
                // Start a negative number entry
                infixTokens.add("-");
            }
            lastInputWasNumber = true;
        } else if (command.equals("DEG/RAD")) {
            isDegreesMode = !isDegreesMode;
            System.out.println("Angle Mode toggled to: " + (isDegreesMode ? "Degrees" : "Radians"));
        } else if (command.startsWith("SOLVE_QUAD")) {
            // Expected format: SOLVE_QUAD a,b,c
            String params = command.substring(11);
            String[] parts = params.split(",");
            if (parts.length == 3) {
                try {
                    double a = Double.parseDouble(parts[0]);
                    double b = Double.parseDouble(parts[1]);
                    double c = Double.parseDouble(parts[2]);
                    String result = solveQuadratic(a, b, c);
                    infixTokens.clear();
                    infixTokens.add(result);
                    lastInputWasEquals = true;
                } catch (NumberFormatException e) {
                    infixTokens.clear();
                    infixTokens.add("Error: Invalid Input");
                }
            }
        } else if (command.startsWith("SOLVE_CUBIC")) {
            // Expected format: SOLVE_CUBIC a,b,c,d
            String params = command.substring(12);
            String[] parts = params.split(",");
            if (parts.length == 4) {
                try {
                    double a = Double.parseDouble(parts[0]);
                    double b = Double.parseDouble(parts[1]);
                    double c = Double.parseDouble(parts[2]);
                    double d = Double.parseDouble(parts[3]);
                    String result = solveCubic(a, b, c, d);
                    infixTokens.clear();
                    infixTokens.add(result);
                    lastInputWasEquals = true;
                } catch (NumberFormatException e) {
                    infixTokens.clear();
                    infixTokens.add("Error: Invalid Input");
                }
            }
        } else if (command.startsWith("M")) {
            handleMemoryOperation(command, primaryDisplayText);
        }
        // --- Equals Logic ---
        else if (command.equals("=")) {
            try {
                List<String> postfixTokens = InfixToPostfixConverter.convert(infixTokens);
                PostfixEvaluator evaluator = new PostfixEvaluator(isDegreesMode);
                BigDecimal result = evaluator.evaluate(postfixTokens);

                infixTokens.clear();
                infixTokens.add(formatResult(result));

            } catch (Exception e) {
                infixTokens.clear();
                infixTokens.add("Error");
                System.err.println("Calculation Error: " + e.getMessage());
                e.printStackTrace();
            } finally {
                lastInputWasEquals = true;
            }
        }
        // --- Number / Decimal Point Logic ---
        else if (currentCommandIsNumber) {
            handleNumberInput(command);
            lastInputWasNumber = true;
            lastInputWasOperator = false;
            lastInputWasConstant = false;
            lastInputWasEquals = false;
        }
        // --- Operator / Function / Constant / Parenthesis Logic ---
        else if (currentCommandIsBinaryOperator || currentCommandIsUnaryFunction || currentCommandIsConstant
                || currentCommandIsParenOpen || currentCommandIsParenClose) {
            handleOperatorInput(command, currentCommandIsParenFunction, currentCommandIsConstant,
                    currentCommandIsParenOpen, currentCommandIsParenClose);
            lastInputWasNumber = false;
            lastInputWasOperator = true; // This covers any operator, function, or parenthesis
            lastInputWasConstant = currentCommandIsConstant; // Specific for constants
            lastInputWasEquals = false;
        } else {
            handleOperatorInput(command, currentCommandIsParenFunction, currentCommandIsConstant,
                    currentCommandIsParenOpen, currentCommandIsParenClose);
        }

        // --- Final Cleanup and Display Update ---
        updateDisplaysFromState();

        System.out.println("  Primary Display: " + primaryDisplayText);
        System.out.println("  Secondary Display: " + secondaryDisplayText);
        System.out.println("  Infix Tokens: " + infixTokens);
        System.out.println("  Last Num: " + lastInputWasNumber + ", Last Op: " + lastInputWasOperator + ", Last Const: "
                + lastInputWasConstant + ", Last Eq: " + lastInputWasEquals);

        return getDisplayState();
    }

    private void handleMemoryOperation(String command, String currentValue) {
        BigDecimal val = BigDecimal.ZERO;
        if (isNumeric(currentValue)) {
            val = new BigDecimal(currentValue);
        }

        switch (command) {
            case "MC":
                memory = BigDecimal.ZERO;
                break;
            case "MR":
                infixTokens.clear();
                infixTokens.add(formatResult(memory));
                lastInputWasNumber = true;
                lastInputWasEquals = true; // Treat recall like a result so next input clears if number
                break;
            case "M+":
                memory = memory.add(val);
                break;
            case "M-":
                memory = memory.subtract(val);
                break;
        }
    }

    private String formatResult(BigDecimal value) {
        // Strip trailing zeros and avoid scientific notation for small numbers if
        // possible. Limit to 15 digits of precision.
        // Prevent extremely large precision output (e.g. 1/3)
        try {
            if (value.scale() > 15) {
                value = value.setScale(15, RoundingMode.HALF_UP);
            }
            return value.stripTrailingZeros().toPlainString();
        } catch (Exception e) {
            return value.toPlainString();
        }
    }

    private String formatResult(double value) {
        if (Double.isNaN(value))
            return "Error";
        if (Double.isInfinite(value))
            return "Infinity";
        // Limit to 15 decimal places
        return new DecimalFormat("0.###############").format(value);
    }

    private String formatComplex(Complex c) {
        boolean isReal = Math.abs(c.imag) < 1e-9;
        if (isReal) {
            return formatResult(c.real);
        } else {
            String realPart = formatResult(c.real);
            String imagPart = formatResult(c.imag);
            if (realPart.equals("0")) {
                return imagPart + "i";
            }
            return realPart + (c.imag > 0 ? "+" : "") + imagPart + "i";
        }
    }

    private void handleNumberInput(String command) {
        String lastToken = getLastToken();

        // If the last action was '=', or if the calculator is in its initial "0" state,
        // start a new expression.
        if (lastInputWasEquals || (infixTokens.size() == 1 && lastToken.equals("0"))) {
            infixTokens.clear();
            lastInputWasEquals = false;
        }

        // If the last token is just a '-', we check if it should be treated as a
        // negative sign
        // for the number we are typing, or if it is a subtraction operator.
        // It is a negative sign if it's the very first token, or if the token BEFORE it
        // was an operator or '('.
        if (getLastToken().equals("-")) {
            boolean isUnary = false;
            if (infixTokens.size() == 1) {
                isUnary = true;
            } else {
                String tokenBeforeMinus = infixTokens.get(infixTokens.size() - 2);
                boolean prevIsOp = Operators.isOperator(tokenBeforeMinus);
                boolean prevIsParenOpen = tokenBeforeMinus.equals("(");
                if (prevIsOp || prevIsParenOpen) {
                    isUnary = true;
                }
            }

            if (isUnary) {
                infixTokens.set(infixTokens.size() - 1, "-" + command);
                return; // We've handled this case.
            }
        }

        // If the last input was a number, we append to it. Otherwise, we add a new
        // number token.
        if (lastInputWasNumber && isNumeric(lastToken)) {
            // Prevent adding multiple decimal points.
            if (command.equals(".") && lastToken.contains(".")) {
                return;
            }
            String newNumber = lastToken.equals("0") && !command.equals(".") ? command : lastToken + command;
            if (lastToken.equals("-0") && !command.equals(".")) {
                newNumber = "-" + command;
            } else if (lastToken.equals("-0") && command.equals(".")) {
                newNumber = "-0.";
            }
            infixTokens.set(infixTokens.size() - 1, newNumber);
        } else {
            // Handle implicit multiplication (e.g., after ')' or a constant).
            if (lastToken.equals(")") || isKnownConstant(lastToken)) {
                infixTokens.add("*");
            }
            // Add the new number token.
            infixTokens.add(command.equals(".") ? "0." : command);
        }
    }

    private void handleOperatorInput(String command, boolean isParenFunction, boolean isConstant, boolean isParenOpen,
            boolean isParenClose) {
        String lastToken = getLastToken();

        // --- Refactored common logic for adding tokens that can have implicit
        // multiplication ---
        // This handles functions (sin, cos), opening parentheses, and constants (PI,
        // E).
        if (isParenFunction || isParenOpen || isConstant) {
            boolean needsImplicitMultiplication = lastInputWasNumber || lastToken.equals(")")
                    || isKnownConstant(lastToken);

            // If we are at the initial "0" state, we replace it instead of multiplying it.
            if (infixTokens.size() == 1 && lastToken.equals("0")) {
                infixTokens.remove(0);
            } else if (needsImplicitMultiplication) {
                infixTokens.add("*");
            }

            // Add the actual tokens for the command
            infixTokens.add(command);
            if (isParenFunction) {
                // Functions are followed by an opening parenthesis
                infixTokens.add("(");
            }
            return;
        }

        // --- Logic for other operators that don't follow the same pattern ---

        // Case: Closing Parenthesis
        if (isParenClose) {
            long openParens = infixTokens.stream().filter(t -> t.equals("(")).count();
            long closeParens = infixTokens.stream().filter(t -> t.equals(")")).count();
            // Only add ')' if there's an open one to match
            if (openParens > closeParens) {
                infixTokens.add(command);
            } else {
                System.out.println("  Warning: Mismatched parentheses. Not appending ')'.");
            }
            return; // Done with this case
        }

        // Case: All other operators (binary, postfix)
        // Special handling for starting with a minus sign.
        if (command.equals("-") && (infixTokens.size() == 1 && lastToken.equals("0"))) {
            infixTokens.set(0, "-");
            return;
        }

        // Handle operator replacement logic (e.g., 5 + * becomes 5 *).
        if (lastInputWasOperator) {
            Operator newOp = Operators.getOperator(command);
            Operator lastOp = Operators.getOperator(lastToken);
            if (newOp != null && lastOp != null && newOp.type == Type.BINARY && lastOp.type == Type.BINARY) {
                // If the new operator is '-', it's for a negative number, so don't replace.
                if (!command.equals("-")) {
                    infixTokens.set(infixTokens.size() - 1, command); // Replace
                    return;
                }
            }
        }

        // Default action: append the operator.
        infixTokens.add(command);
    }

    private void updateDisplaysFromState() {
        if (infixTokens.isEmpty()) {
            infixTokens.add("0");
        }

        String lastToken = getLastToken();

        if (lastInputWasEquals) {
            secondaryDisplayText = String.join(" ", infixTokens) + " =";
            primaryDisplayText = lastToken;
        } else {
            secondaryDisplayText = String.join(" ", infixTokens);
            if (lastInputWasNumber) {
                primaryDisplayText = lastToken;
            } else if (isKnownConstant(lastToken)) {
                primaryDisplayText = lastToken;
            } else {
                primaryDisplayText = "0";
            }
        }

        if (getLastToken().equals("Error")) {
            primaryDisplayText = "Error";
        }
    }

    private String getLastToken() {
        if (infixTokens.isEmpty()) {
            return "";
        }
        return infixTokens.get(infixTokens.size() - 1);
    }

    /**
     * Solves the cubic equation ax³+bx²+cx+d=0 and updates the display.
     * Uses Cardano's method.
     */
    public String solveCubic(double coeffA, double coeffB, double coeffC, double coeffD) {
        if (Math.abs(coeffA) < 1e-12) { // Degenerates to a quadratic if a=0
            return solveQuadratic(coeffB, coeffC, coeffD);
        }

        // Normalize to x³ + ax² + bx + c = 0 form for simplicity
        double a = coeffB / coeffA;
        double b = coeffC / coeffA;
        double c = coeffD / coeffA;

        double p = (3 * b - a * a) / 3.0;
        double q = (2 * a * a * a - 9 * a * b + 27 * c) / 27.0;

        double discriminant = (q / 2.0) * (q / 2.0) + (p / 3.0) * (p / 3.0) * (p / 3.0);

        Complex[] roots = new Complex[3];

        if (discriminant >= 0) { // One real root, two complex conjugate
            double u_val = -q / 2.0 + Math.sqrt(discriminant);
            double v_val = -q / 2.0 - Math.sqrt(discriminant);
            double u = Math.cbrt(u_val);
            double v = Math.cbrt(v_val);

            roots[0] = new Complex(u + v - (a / 3.0), 0);
            double realPart = -0.5 * (u + v) - (a / 3.0);
            double imagPart = (Math.sqrt(3.0) / 2.0) * (u - v);
            roots[1] = new Complex(realPart, imagPart);
            roots[2] = new Complex(realPart, -imagPart);

        } else { // Three real roots
            double r = Math.sqrt(-(p * p * p) / 27.0);
            double phi = Math.acos(-q / (2.0 * r));

            roots[0] = new Complex(2.0 * Math.cbrt(r) * Math.cos(phi / 3.0) - (a / 3.0), 0);
            roots[1] = new Complex(2.0 * Math.cbrt(r) * Math.cos((phi + 2.0 * Math.PI) / 3.0) - (a / 3.0), 0);
            roots[2] = new Complex(2.0 * Math.cbrt(r) * Math.cos((phi + 4.0 * Math.PI) / 3.0) - (a / 3.0), 0);
        }

        return "x₁ = " + formatComplex(roots[0]) + ", x₂ = " + formatComplex(roots[1]) + ", x₃ = "
                + formatComplex(roots[2]);
    }

    /**
     * Solves the quadratic equation ax^2 + bx + c = 0.
     */
    public String solveQuadratic(double coeffA, double coeffB, double coeffC) {
        if (Math.abs(coeffA) < 1e-12) { // Use a small epsilon for floating point comparison
            return "Error: 'a' cannot be 0";
        }
        Complex[] roots = solveQuadraticInternal(coeffA, coeffB, coeffC);
        if (Math.abs(roots[0].imag) < 1e-9 && Math.abs(roots[1].imag) < 1e-9) {
            if (Math.abs(roots[0].real - roots[1].real) < 1e-9) {
                return "x = " + formatResult(roots[0].real);
            }
            return "x₁ = " + formatResult(roots[0].real) + ", x₂ = " + formatResult(roots[1].real);
        } else {
            return "x₁ = " + formatComplex(roots[0]) + ", x₂ = " + formatComplex(roots[1]);
        }
    }

    /**
     * A general-purpose polynomial solver.
     * Delegates to specific, fast analytical solvers for degrees 2, 3, and 4.
     * Uses the numerical Durand-Kerner method for degrees 5 and higher.
     *
     * @param coeffs The coefficients of the polynomial, from the highest degree to
     *               the constant term.
     * @return A formatted string containing all the roots of the polynomial.
     */
    public String solvePolynomial(double[] coeffs) {
        int degree = coeffs.length - 1;
        if (coeffs.length == 0 || (coeffs.length == 1 && coeffs[0] == 0)) {
            return "Error: No polynomial provided.";
        }
        // Skip leading zero coefficients
        int firstNonZero = 0;
        while (firstNonZero < coeffs.length && Math.abs(coeffs[firstNonZero]) < 1e-12) {
            firstNonZero++;
        }
        int effectiveDegree = coeffs.length - 1 - firstNonZero;
        if (effectiveDegree < 1) {
            return "Error: Not a valid polynomial (degree < 1).";
        }

        // Create a clean array of coefficients for the actual polynomial
        double[] effectiveCoeffs = new double[effectiveDegree + 1];
        System.arraycopy(coeffs, firstNonZero, effectiveCoeffs, 0, effectiveDegree + 1);

        switch (effectiveDegree) {
            case 1: // Linear: ax + b = 0
                return "x = " + formatResult(-effectiveCoeffs[1] / effectiveCoeffs[0]);
            case 2: // Quadratic
                return solveQuadratic(effectiveCoeffs[0], effectiveCoeffs[1], effectiveCoeffs[2]);
            case 3: // Cubic
                return solveCubic(effectiveCoeffs[0], effectiveCoeffs[1], effectiveCoeffs[2], effectiveCoeffs[3]);
            case 4: // Quartic
                return solveQuartic(effectiveCoeffs[0], effectiveCoeffs[1], effectiveCoeffs[2], effectiveCoeffs[3],
                        effectiveCoeffs[4]);
            default: // Degree 5+ use numerical method
                try {
                    Complex[] roots = findRootsDurandKerner(effectiveCoeffs);
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < roots.length; i++) {
                        sb.append("x").append(i + 1).append(" = ").append(formatComplex(roots[i]));
                        if (i < roots.length - 1) {
                            sb.append(", ");
                        }
                    }
                    return sb.toString();
                } catch (Exception e) {
                    return "Error: " + e.getMessage();
                }
        }
    }

    /**
     * Implements the Durand-Kerner method to find all roots of a polynomial
     * simultaneously.
     * This is an iterative numerical method suitable for high-degree polynomials.
     *
     * @param coeffs The coefficients of the polynomial (a_n, a_n-1, ..., a_0).
     * @return An array of Complex numbers representing the roots.
     */
    private Complex[] findRootsDurandKerner(double[] coeffs) {
        int degree = coeffs.length - 1;
        double a_n = coeffs[0];

        // Normalize the polynomial so the leading coefficient is 1.
        Complex[] pCoeffs = new Complex[degree];
        for (int i = 0; i < degree; i++) {
            pCoeffs[i] = new Complex(coeffs[i + 1] / a_n, 0);
        }

        // Initialize roots with an initial guess (e.g., points on a circle).
        Complex[] roots = new Complex[degree];
        Complex initialGuess = new Complex(0.4, 0.9);
        for (int i = 0; i < degree; i++) {
            roots[i] = initialGuess.pow(i);
        }

        int maxIterations = 500;
        double tolerance = 1e-9;

        for (int iter = 0; iter < maxIterations; iter++) {
            boolean converged = true;
            Complex[] nextRoots = new Complex[degree];

            for (int i = 0; i < degree; i++) {
                Complex currentRoot = roots[i];
                Complex numerator = evalPolynomial(pCoeffs, currentRoot).add(currentRoot.pow(degree));

                Complex denominator = new Complex(1, 0);
                for (int j = 0; j < degree; j++) {
                    if (i != j) {
                        denominator = denominator.multiply(currentRoot.subtract(roots[j]));
                    }
                }

                Complex correction = numerator.divide(denominator);
                nextRoots[i] = currentRoot.subtract(correction);

                if (nextRoots[i].subtract(currentRoot).magnitude() > tolerance) {
                    converged = false;
                }
            }

            roots = nextRoots;

            if (converged) {
                return roots;
            }
        }

        throw new ArithmeticException("Durand-Kerner method failed to converge.");
    }

    // Helper to evaluate a polynomial with complex coefficients at a complex point.
    private Complex evalPolynomial(Complex[] coeffs, Complex z) {
        Complex result = new Complex(0, 0);
        for (int i = 0; i < coeffs.length; i++) {
            result = result.add(coeffs[i].multiply(z.pow(coeffs.length - 1 - i)));
        }
        return result;
    }

    // A more capable, public Complex number class
    public static class Complex {
        public final double real;
        public final double imag;

        public Complex(double real, double imag) {
            this.real = real;
            this.imag = imag;
        }

        public Complex add(Complex other) {
            return new Complex(this.real + other.real, this.imag + other.imag);
        }

        public Complex subtract(Complex other) {
            return new Complex(this.real - other.real, this.imag - other.imag);
        }

        public Complex multiply(Complex other) {
            double r = this.real * other.real - this.imag * other.imag;
            double i = this.real * other.imag + this.imag * other.real;
            return new Complex(r, i);
        }

        public Complex divide(Complex other) {
            double denominator = other.real * other.real + other.imag * other.imag;
            if (denominator == 0) {
                return new Complex(Double.NaN, Double.NaN); // Or throw exception
            }
            double r = (this.real * other.real + this.imag * other.imag) / denominator;
            double i = (this.imag * other.real - this.real * other.imag) / denominator;
            return new Complex(r, i);
        }

        public Complex pow(int exponent) {
            Complex result = new Complex(1, 0);
            Complex base = this;
            if (exponent < 0) {
                base = new Complex(1, 0).divide(base);
                exponent = -exponent;
            }
            for (int i = 0; i < exponent; i++) {
                result = result.multiply(base);
            }
            return result;
        }

        public double magnitude() {
            return Math.sqrt(real * real + imag * imag);
        }

        @Override
        public String toString() {
            return "(" + real + ", " + imag + "i)";
        }
    }

    /**
     * Solves a quadratic equation and returns the roots as Complex numbers.
     */
    private Complex[] solveQuadraticInternal(double a, double b, double c) {
        double discriminant = b * b - 4 * a * c;
        if (discriminant >= 0) {
            double sqrtDisc = Math.sqrt(discriminant);
            return new Complex[] {
                    new Complex((-b + sqrtDisc) / (2 * a), 0),
                    new Complex((-b - sqrtDisc) / (2 * a), 0)
            };
        } else {
            double sqrtDisc = Math.sqrt(-discriminant);
            return new Complex[] {
                    new Complex(-b / (2 * a), sqrtDisc / (2 * a)),
                    new Complex(-b / (2 * a), -sqrtDisc / (2 * a))
            };
        }
    }

    /**
     * Finds one real root of a cubic equation. Necessary for the quartic solver's
     * resolvent cubic.
     */
    private double findOneRealCubicRoot(double a, double b, double c, double d) {
        if (Math.abs(a) < 1e-12) { // Degenerates to quadratic
            double disc = c * c - 4 * b * d;
            return (disc >= 0) ? (-c + Math.sqrt(disc)) / (2 * b) : 0;
        }
        // Normalize to x³ + p_x² + q_x + r_x = 0
        double p_x = b / a;
        double q_x = c / a;
        double r_x = d / a;

        // Depress to y³ + py + q = 0
        double p = q_x - p_x * p_x / 3.0;
        double q = r_x - p_x * q_x / 3.0 + 2.0 * p_x * p_x * p_x / 27.0;

        double discriminant = (q / 2.0) * (q / 2.0) + (p / 3.0) * (p / 3.0) * (p / 3.0);

        if (discriminant >= 0) { // One real root
            double u = Math.cbrt(-q / 2.0 + Math.sqrt(discriminant));
            double v = Math.cbrt(-q / 2.0 - Math.sqrt(discriminant));
            return u + v - (p_x / 3.0);
        } else { // Three real roots, just return one
            double r_val = Math.sqrt(-(p * p * p) / 27.0);
            double phi = Math.acos(-q / (2.0 * r_val));
            return 2.0 * Math.cbrt(r_val) * Math.cos(phi / 3.0) - (p_x / 3.0);
        }
    }

    public String solveQuartic(double a, double b, double c, double d, double e) {
        if (Math.abs(a) < 1e-12) {
            return solveCubic(b, c, d, e);
        }

        // Normalize
        double B = b / a;
        double C = c / a;
        double D = d / a;
        double E = e / a;

        // Depress the quartic: x = y - B/4, results in y^4 + py^2 + qy + r = 0
        double p = C - 3.0 * B * B / 8.0;
        double q = D + B * B * B / 8.0 - B * C / 2.0;
        double r = E - 3.0 * B * B * B * B / 256.0 + B * B * C / 16.0 - B * D / 4.0;

        // Resolvent cubic: 8m^3 + 8pm^2 + (2p^2 - 8r)m - q^2 = 0
        double m = findOneRealCubicRoot(8, 8 * p, 2 * p * p - 8 * r, -q * q);

        double alpha_sq = 2 * m + p;
        if (alpha_sq < 0) {
            // This case requires complex coefficients for the quadratic factors.
            // While solvable, it significantly complicates the implementation.
            // A robust numerical method would be better for these edge cases.
            return "Error: Solver does not support this type of quartic equation.";
        }
        double alpha = Math.sqrt(alpha_sq);

        List<Complex> roots = new ArrayList<>();
        if (Math.abs(alpha) < 1e-9) { // Biquadratic case (q=0, alpha=0)
            Complex[] z_roots = solveQuadraticInternal(1, p, r);
            for (Complex z : z_roots) {
                double re = z.real, im = z.imag;
                double mod = Math.sqrt(re * re + im * im), arg = Math.atan2(im, re);
                double sqrt_mod = Math.sqrt(mod);
                roots.add(new Complex(sqrt_mod * Math.cos(arg / 2.0), sqrt_mod * Math.sin(arg / 2.0)));
                roots.add(new Complex(-sqrt_mod * Math.cos(arg / 2.0), -sqrt_mod * Math.sin(arg / 2.0)));
            }
        } else {
            Complex[] roots1 = solveQuadraticInternal(1, alpha, (p / 2.0 + m - q / (2.0 * alpha)));
            Complex[] roots2 = solveQuadraticInternal(1, -alpha, (p / 2.0 + m + q / (2.0 * alpha)));
            roots.addAll(List.of(roots1));
            roots.addAll(List.of(roots2));
        }

        // Shift roots back: x = y - B/4
        List<Complex> finalRoots = new ArrayList<>();
        for (Complex y_root : roots) {
            finalRoots.add(new Complex(y_root.real - B / 4.0, y_root.imag));
        }

        // Format the output string, grouping conjugate pairs
        List<Complex> processedRoots = new ArrayList<>(finalRoots);
        StringBuilder sb = new StringBuilder();
        int rootNum = 1;
        while (!processedRoots.isEmpty()) {
            Complex r1 = processedRoots.remove(0);
            sb.append("x").append(rootNum).append("=").append(formatComplex(r1));
            rootNum++;
            if (!processedRoots.isEmpty())
                sb.append(", ");
        }
        return sb.toString();
    }

    /**
     * Solves a system of N linear equations with N variables using Gaussian
     * elimination with partial pivoting.
     * The system is represented as Ax = b.
     *
     * @param A The NxN matrix of coefficients for the variables.
     * @param b The N-element vector of constants (the right-hand side of the
     *          equations).
     * @return A map of variable names ("x1", "x2", etc.) to their solved values.
     * @throws IllegalArgumentException if the matrix is not square, or if
     *                                  dimensions are mismatched.
     * @throws ArithmeticException      if the system has no unique solution
     *                                  (singular matrix).
     */
    public Map<String, Double> solveLinearSystem(double[][] A, double[] b) {
        if (A == null || A.length == 0 || A.length != b.length) {
            throw new IllegalArgumentException("Matrix A and vector b dimensions are mismatched.");
        }

        int n = b.length;
        for (int i = 0; i < n; i++) {
            if (A[i].length != n) {
                throw new IllegalArgumentException("Coefficient matrix A must be square.");
            }
        }

        // --- Forward Elimination with Partial Pivoting ---
        for (int p = 0; p < n; p++) {
            // Find pivot row
            int max = p;
            for (int i = p + 1; i < n; i++) {
                if (Math.abs(A[i][p]) > Math.abs(A[max][p])) {
                    max = i;
                }
            }

            // Swap rows
            double[] tempA = A[p];
            A[p] = A[max];
            A[max] = tempA;
            double tempB = b[p];
            b[p] = b[max];
            b[max] = tempB;

            // Check for singularity
            if (Math.abs(A[p][p]) <= 1e-12) {
                throw new ArithmeticException("Matrix is singular or nearly singular. No unique solution exists.");
            }

            // Pivot within A and b
            for (int i = p + 1; i < n; i++) {
                double alpha = A[i][p] / A[p][p];
                b[i] -= alpha * b[p];
                for (int j = p; j < n; j++) {
                    A[i][j] -= alpha * A[p][j];
                }
            }
        }

        // --- Back Substitution ---
        double[] x = new double[n];
        for (int i = n - 1; i >= 0; i--) {
            double sum = 0.0;
            for (int j = i + 1; j < n; j++) {
                sum += A[i][j] * x[j];
            }
            x[i] = (b[i] - sum) / A[i][i];
        }

        Map<String, Double> results = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            results.put("x" + (i + 1), x[i]);
        }
        return results;
    }

    // Helper to return display state
    public String[] getDisplayState() {
        return new String[] { primaryDisplayText, secondaryDisplayText };
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