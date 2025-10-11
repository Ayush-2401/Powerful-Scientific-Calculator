// File: src/com/scientificcalculator/engine/CalculatorEngine.java

package com.scientificcalculator.engine;

import com.scientificcalculator.engine.Operators.Operator;
import com.scientificcalculator.engine.Operators.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.lang.Math;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.DecimalFormat;
import static java.lang.Math.PI;

public class CalculatorEngine {

    // REFACTORED: Using a list of tokens for robust state management.
    private List<String> infixTokens;

    private String primaryDisplayText;
    private String secondaryDisplayText;

    private double memory;
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
        memory = 0.0;
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
        boolean currentCommandIsBinaryOperator = Operators.isOperator(command) && Operators.getOperator(command).type == Type.BINARY;
        boolean currentCommandIsUnaryFunction = Operators.isOperator(command) && (Operators.getOperator(command).type == Type.UNARY_FUNCTION || Operators.getOperator(command).type == Type.UNARY_POSTFIX);
        boolean currentCommandIsConstant = isKnownConstant(command);
        boolean currentCommandIsParenOpen = command.equals("(");
        boolean currentCommandIsParenClose = command.equals(")");
        // A function that should be followed by an opening parenthesis
        boolean currentCommandIsParenFunction = command.equals("sin") || command.equals("cos") || command.equals("tan") || command.equals("log") || command.equals("ln") || command.equals("sqrt");


        // --- Clear / CE / Backspace / Sign Change / Angle Mode / Memory ---
        if (command.equals("C")) {
            infixTokens.clear();
            infixTokens.add("0");
            resetStateFlags();
        }
        else if (command.equals("CE")) {
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
        }
        else if (command.equals("<--")) {
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
        }
        else if (command.equals("+/-")) {
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
        }
        else if (command.equals("DEG/RAD")) {
            isDegreesMode = !isDegreesMode;
            System.out.println("Angle Mode toggled to: " + (isDegreesMode ? "Degrees" : "Radians"));
        }
        else if (command.startsWith("M")) {
            handleMemoryOperation(command, primaryDisplayText);
        }
        // --- Equals Logic ---
        else if (command.equals("=")) {
            try {
                List<String> postfixTokens = InfixToPostfixConverter.convert(infixTokens);
                PostfixEvaluator evaluator = new PostfixEvaluator(isDegreesMode);
                double result = evaluator.evaluate(postfixTokens);

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
        else if (currentCommandIsBinaryOperator || currentCommandIsUnaryFunction || currentCommandIsConstant || currentCommandIsParenOpen || currentCommandIsParenClose) {
            handleOperatorInput(command, currentCommandIsParenFunction, currentCommandIsConstant, currentCommandIsParenOpen, currentCommandIsParenClose);
            lastInputWasNumber = false;
            lastInputWasOperator = true; // This covers any operator, function, or parenthesis
            lastInputWasConstant = currentCommandIsConstant; // Specific for constants
            lastInputWasEquals = false;
        }
        else {
           handleOperatorInput(command, currentCommandIsParenFunction, currentCommandIsConstant, currentCommandIsParenOpen, currentCommandIsParenClose);
        }

        // --- Final Cleanup and Display Update ---
        updateDisplaysFromState();

        System.out.println("  Primary Display: " + primaryDisplayText);
        System.out.println("  Secondary Display: " + secondaryDisplayText);
        System.out.println("  Infix Tokens: " + infixTokens);
        System.out.println("  Last Num: " + lastInputWasNumber + ", Last Op: " + lastInputWasOperator + ", Last Const: " + lastInputWasConstant + ", Last Eq: " + lastInputWasEquals);

        return getDisplayState();
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

        // If the last action was '=', or if the calculator is in its initial "0" state, start a new expression.
        if (lastInputWasEquals || (infixTokens.size() == 1 && lastToken.equals("0"))) {
            infixTokens.clear();
            lastInputWasEquals = false;
        }

        // If the last token is just a '-', we are building a negative number.
        if (getLastToken().equals("-")) { // Re-check last token in case it was cleared
            infixTokens.set(infixTokens.size() - 1, "-" + command);
            return; // We've handled this case.
        }

        // If the last input was a number, we append to it. Otherwise, we add a new number token.
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

    private void handleOperatorInput(String command, boolean isParenFunction, boolean isConstant, boolean isParenOpen, boolean isParenClose) {
        String lastToken = getLastToken();

        // --- Refactored common logic for adding tokens that can have implicit multiplication ---
        // This handles functions (sin, cos), opening parentheses, and constants (PI, E).
        if (isParenFunction || isParenOpen || isConstant) {
            boolean needsImplicitMultiplication = lastInputWasNumber || lastToken.equals(")") || isKnownConstant(lastToken);

            // If we are at the initial "0" state, we replace it instead of multiplying it.
            if (infixTokens.size() == 1 && lastToken.equals("0")) {
                infixTokens.remove(0);
            } else if (needsImplicitMultiplication) {
                infixTokens.add("*");
            }

            // Add the actual tokens for the command
            infixTokens.add(command);
            if (isParenFunction || isParenOpen) {
                // Functions and parentheses are followed by an opening parenthesis
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

        if (discriminant >= 0) { // One real root, two complex
            double u = Math.cbrt(-q / 2.0 + Math.sqrt(discriminant));
            double v = Math.cbrt(-q / 2.0 - Math.sqrt(discriminant));
            double root1 = u + v - (a / 3.0);

            double realPart = -0.5 * (u + v) - (a / 3.0);
            double imagPart = (Math.sqrt(3.0) / 2.0) * (u - v);

            return "x₁=" + formatResult(root1) + " (real), x₂,₃=" + formatResult(realPart) + " ± " + formatResult(Math.abs(imagPart)) + "i";

        } else { // Three real roots
            double r = Math.sqrt(-(p * p * p) / 27.0);
            double phi = Math.acos(-q / (2.0 * r));

            double root1 = 2.0 * Math.cbrt(r) * Math.cos(phi / 3.0) - (a / 3.0);
            double root2 = 2.0 * Math.cbrt(r) * Math.cos((phi + 2.0 * Math.PI) / 3.0) - (a / 3.0);
            double root3 = 2.0 * Math.cbrt(r) * Math.cos((phi + 4.0 * Math.PI) / 3.0) - (a / 3.0);

            return "x₁=" + formatResult(root1) + ", x₂=" + formatResult(root2) + ", x₃=" + formatResult(root3);
        }
    }


    /**
     * Solves the quadratic equation ax^2 + bx + c = 0.
     */
    public String solveQuadratic(double coeffA, double coeffB, double coeffC) {
        if (Math.abs(coeffA) < 1e-12) { // Use a small epsilon for floating point comparison
            return "Error: 'a' cannot be 0";
        }

        double discriminant = (coeffB * coeffB) - (4 * coeffA * coeffC);

        if (discriminant > 1e-12) { // Discriminant is positive
            double root1 = (-coeffB + Math.sqrt(discriminant)) / (2 * coeffA);
            double root2 = (-coeffB - Math.sqrt(discriminant)) / (2 * coeffA);
            return "x₁=" + formatResult(root1) + ", x₂=" + formatResult(root2);
        } else if (Math.abs(discriminant) < 1e-12) { // Discriminant is zero
            double root = -coeffB / (2 * coeffA);
            return "x=" + formatResult(root);
        } else { // Discriminant is negative (complex roots)
            double realPart = -coeffB / (2 * coeffA);
            double imaginaryPart = Math.sqrt(-discriminant) / (2 * coeffA);
            return "x=" + formatResult(realPart) + " ± " + formatResult(Math.abs(imaginaryPart)) + "i";
        }
    }

    // Helper class for complex number arithmetic within solvers.
    private static class Complex {
        final double real;
        final double imag;

        Complex(double real, double imag) {
            this.real = real;
            this.imag = imag;
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
     * Finds one real root of a cubic equation. Necessary for the quartic solver's resolvent cubic.
     */
    private double findOneRealCubicRoot(double a, double b, double c, double d) {
        if (Math.abs(a) < 1e-12) { // Degenerates to quadratic
            double disc = c*c - 4*b*d;
            return (disc >= 0) ? (-c + Math.sqrt(disc)) / (2*b) : 0;
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
                double mod = Math.sqrt(re*re + im*im), arg = Math.atan2(im, re);
                double sqrt_mod = Math.sqrt(mod);
                roots.add(new Complex(sqrt_mod * Math.cos(arg/2.0), sqrt_mod * Math.sin(arg/2.0)));
                roots.add(new Complex(-sqrt_mod * Math.cos(arg/2.0), -sqrt_mod * Math.sin(arg/2.0)));
            }
        } else {
            Complex[] roots1 = solveQuadraticInternal(1, alpha, (p/2.0 + m - q/(2.0*alpha)));
            Complex[] roots2 = solveQuadraticInternal(1, -alpha, (p/2.0 + m + q/(2.0*alpha)));
            roots.addAll(List.of(roots1));
            roots.addAll(List.of(roots2));
        }

        // Shift roots back: x = y - B/4
        List<Complex> finalRoots = new ArrayList<>();
        for (Complex y_root : roots) {
            finalRoots.add(new Complex(y_root.real - B/4.0, y_root.imag));
        }

        // Format the output string, grouping conjugate pairs
        List<Complex> processedRoots = new ArrayList<>(finalRoots);
        StringBuilder sb = new StringBuilder();
        int rootNum = 1;
        while (!processedRoots.isEmpty()) {
            Complex r1 = processedRoots.remove(0);
            sb.append("x").append(rootNum).append("=").append(formatComplex(r1));
            rootNum++;
            if (!processedRoots.isEmpty()) sb.append(", ");
        }
        return sb.toString();
    }

    /**
     * Solves a system of N linear equations with N variables using Gaussian elimination with partial pivoting.
     * The system is represented as Ax = b.
     *
     * @param A The NxN matrix of coefficients for the variables.
     * @param b The N-element vector of constants (the right-hand side of the equations).
     * @return A map of variable names ("x1", "x2", etc.) to their solved values.
     * @throws IllegalArgumentException if the matrix is not square, or if dimensions are mismatched.
     * @throws ArithmeticException if the system has no unique solution (singular matrix).
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
            double[] tempA = A[p]; A[p] = A[max]; A[max] = tempA;
            double tempB = b[p]; b[p] = b[max]; b[max] = tempB;

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
        return new String[]{primaryDisplayText, secondaryDisplayText};
    }

    /**
     * Solves an equation for a single variable using a numerical root-finding algorithm (Secant Method).
     * The method rewrites an equation like L(x) = R(x) into a function f(x) = L(x) - R(x)
     * and finds the value of x for which f(x) is zero.
     *
     * @param equation The equation string, e.g., "2*x + 5 = 10" or "sin(y)=0.5".
     * @return The formatted numerical solution for the variable.
     * @throws IllegalArgumentException if the equation is malformed or contains more than one variable.
     * @throws ArithmeticException if the solver fails to find any roots or encounters a mathematical error.
     */
    public String solveEquation(String equation) {
        if (equation == null || equation.trim().isEmpty()) {
            throw new IllegalArgumentException("Equation cannot be empty.");
        }
        if (!equation.contains("=")) {
            throw new IllegalArgumentException("Equation must contain an equals sign.");
        }

        // 1. Tokenize the entire equation to correctly identify constants, functions, and variables.
        List<String> allTokens = tokenizeExpression(equation);

        // 2. Find the variable in the token list.
        Set<String> variablesFound = new HashSet<>();
        for (String token : allTokens) {
            // A token is a variable if it's not a number, not an operator, and not a known constant.
            // Also, explicitly exclude the equals sign from being considered a variable.
            if (!token.equals("=") && !isNumeric(token) && !Operators.isOperator(token) && !isKnownConstant(token)) {
                variablesFound.add(token);
            }
        }

        if (variablesFound.size() > 1) {
            throw new IllegalArgumentException("Solver currently supports only one variable. Found: " + variablesFound);
        }
        if (variablesFound.isEmpty()) {
            throw new IllegalArgumentException("No variable (like x, y, z) found to solve for.");
        }
        String variable = variablesFound.iterator().next();

        // 3. Split the token list into Left and Right sides at the "=".
        int equalsIndex = allTokens.indexOf("=");
        if (equalsIndex == -1) {
            throw new IllegalArgumentException("Equation must have an equals sign."); // Should be caught by string check
        }
        List<String> leftTokens = new ArrayList<>(allTokens.subList(0, equalsIndex));
        List<String> rightTokens = new ArrayList<>(allTokens.subList(equalsIndex + 1, allTokens.size()));

        // 4. Define the function f(var) = Left(var) - Right(var).
        final String finalVariable = variable;
        Function<Double, Double> f = (val) -> {
            try {
                double leftVal = evaluateExpressionFromTokens(leftTokens, finalVariable, val);
                double rightVal = rightTokens.isEmpty() ? 0 : evaluateExpressionFromTokens(rightTokens, finalVariable, val);
                return leftVal - rightVal;
            } catch (Exception e) {
                return Double.NaN; // Return NaN if the expression is invalid for a given value.
            }
        };

        // 5. Find multiple roots by searching from different starting points.
        List<double[]> initialGuesses = List.of(
            new double[]{0.0, 1.0},        // Near origin
            new double[]{-1.0, -2.0},      // Negative small
            new double[]{1.0, 2.0},        // Positive small
            new double[]{10.0, 11.0},       // Positive medium
            new double[]{-10.0, -11.0},     // Negative medium
            new double[]{100.0, 101.0},     // Positive large
            new double[]{-100.0, -101.0},   // Negative large
            new double[]{0.1, 0.2},        // Near zero positive
            new double[]{-0.1, -0.2},      // Near zero negative
            new double[]{PI, PI - 0.1},    // Near pi
            new double[]{-PI, -PI + 0.1}   // Near -pi
        );

        List<Double> foundRoots = new ArrayList<>();
        for (double[] guess : initialGuesses) {
            try {
                // Use a helper method for the Secant algorithm
                double root = findRootSecant(f, guess[0], guess[1], 100, 1e-9);
                foundRoots.add(root);
            } catch (ArithmeticException e) {
                // This guess failed to converge or had an issue, just try the next one.
                System.out.println("Solver: A guess failed to converge. " + e.getMessage());
            }
        }

        if (foundRoots.isEmpty()) {
            throw new ArithmeticException("Solver could not find any real roots with the given search ranges.");
        }

        // Post-process to get unique roots within a tolerance
        double uniquenessTolerance = 1e-7;
        foundRoots.sort(Double::compare);
        List<Double> uniqueRoots = new ArrayList<>();
        if (!foundRoots.isEmpty()) {
            uniqueRoots.add(foundRoots.get(0));
            for (int i = 1; i < foundRoots.size(); i++) {
                if (Math.abs(foundRoots.get(i) - foundRoots.get(i-1)) > uniquenessTolerance) {
                    uniqueRoots.add(foundRoots.get(i));
                }
            }
        }

        // Format the output string
        if (uniqueRoots.size() == 1) {
            return formatResult(uniqueRoots.get(0));
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(variable).append(" = {");
            for (int i = 0; i < uniqueRoots.size(); i++) {
                sb.append(formatResult(uniqueRoots.get(i)));
                if (i < uniqueRoots.size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append("}");
            return sb.toString();
        }
    }

    /**
     * Finds a single root of a function using the Secant Method.
     * @param f The function for which to find a root, f(x) = 0.
     * @param x0 First initial guess.
     * @param x1 Second initial guess.
     * @param maxIterations Maximum number of iterations.
     * @param tolerance The desired precision of the root.
     * @return The found root.
     * @throws ArithmeticException if the method fails to converge or encounters an error.
     */
    private double findRootSecant(Function<Double, Double> f, double x0, double x1, int maxIterations, double tolerance) throws ArithmeticException {
        double f0 = f.apply(x0);
        double f1 = f.apply(x1);

        if (Double.isNaN(f0) || Double.isNaN(f1)) {
            throw new ArithmeticException("Cannot evaluate function at initial points.");
        }

        for (int i = 0; i < maxIterations; i++) {
            if (Math.abs(f1 - f0) < tolerance) { // Avoid division by zero
                if (Math.abs(f1) < tolerance) {
                    return x1; // Found a root where function is flat and near zero
                }
                throw new ArithmeticException("Convergence failure (division by zero).");
            }

            double x2 = x1 - f1 * (x1 - x0) / (f1 - f0);

            if (Math.abs(x2 - x1) < tolerance) {
                return x2; // Success: solution converged.
            }

            x0 = x1;
            f0 = f1;
            x1 = x2;
            f1 = f.apply(x1);

            if (Double.isNaN(f1)) {
                throw new ArithmeticException("Function became undefined during solving process.");
            }
        }

        throw new ArithmeticException("Failed to converge within " + maxIterations + " iterations.");
    }
    private List<String> tokenizeExpression(String expression) {
        List<String> tokens = new ArrayList<>();
        String tempExpression = expression.replaceAll("\\s+", "");

        java.util.Set<String> knownSymbols = new java.util.HashSet<>();
        knownSymbols.addAll(ConstantsLoader.getAllConstants().keySet());
        knownSymbols.addAll(Operators.getAllSymbols());

        List<String> sortedSymbols = new ArrayList<>(knownSymbols);
        sortedSymbols.sort((s1, s2) -> s2.length() - s1.length());

        int i = 0;
        while (i < tempExpression.length()) {
            boolean matched = false;
            for (String symbol : sortedSymbols) {
                if (tempExpression.startsWith(symbol, i)) {
                    tokens.add(symbol);
                    i += symbol.length();
                    matched = true;
                    break;
                }
            }
            if (matched) {
                continue;
            }

            char c = tempExpression.charAt(i);
            if (Character.isDigit(c) || (c == '.' && i + 1 < tempExpression.length() && Character.isDigit(tempExpression.charAt(i + 1)))) {
                StringBuilder number = new StringBuilder();
                while (i < tempExpression.length() && (Character.isDigit(tempExpression.charAt(i)) || tempExpression.charAt(i) == '.')) {
                    number.append(tempExpression.charAt(i));
                    i++;
                }
                tokens.add(number.toString());
            } else {
                tokens.add(String.valueOf(c));
                i++;
            }
        }
        return tokens;
    }

    /**
     * Evaluates a list of expression tokens that contains a single variable.
     * It substitutes the variable with a given numerical value before evaluation.
     *
     * @param tokens The list of tokens (e.g., ["2", "*", "x", "+", "5"]).
     * @param varName The name of the variable to be substituted (e.g., "x").
     * @param varValue The numerical value to substitute for the variable.
     * @return The result of the evaluated expression.
     */
    private double evaluateExpressionFromTokens(List<String> tokens, String varName, double varValue) {
        // Pass the variable name to the converter and evaluator instead of pre-substituting.
        List<String> postfix = InfixToPostfixConverter.convert(tokens, varName);
        PostfixEvaluator evaluator = new PostfixEvaluator(isDegreesMode);
        return evaluator.evaluate(postfix, varName, varValue);
    }

    public void setExpression(String expression) {
        lastInputWasEquals = true;
        lastInputWasNumber = true;
        lastInputWasOperator = false;
        lastInputWasConstant = false;
    }

    /**
     * Formats a double result into a clean string, removing trailing .0.
     * @param result The number to format.
     * @return The formatted string.
     */
    public String formatResult(double result) {
        if (Double.isNaN(result)) {
            return "Error";
        }
        if (Double.isInfinite(result)) {
            return "Infinity";
        }
        DecimalFormat df = new DecimalFormat("0.##########");
        String formatted = df.format(result);

        if (formatted.endsWith(".0")) {
            formatted = formatted.substring(0, formatted.length() - 2);
        }
        return formatted;
    }

    private void handleMemoryOperation(String command, String currentDisplay) {
        double displayValue;
        try {
            displayValue = Double.parseDouble(currentDisplay);
        } catch (NumberFormatException e) {
            System.err.println("Memory Error: Invalid number in display for memory operation.");
            return;
        }

        switch (command) {
            case "M+":
                memory += displayValue;
                System.out.println("Memory M+: " + displayValue + ", New Memory: " + memory);
                break;
            case "M-":
                memory -= displayValue;
                System.out.println("Memory M-: " + displayValue + ", New Memory: " + memory);
                break;
            case "MR":
                String memString = formatResult(memory);
                if (lastInputWasOperator || lastInputWasEquals) {
                    infixTokens.add(memString);
                } else {
                    infixTokens.set(infixTokens.size() - 1, memString);
                }
                lastInputWasNumber = true;
                System.out.println("Memory MR: " + memString);
                break;
            case "MC":
                memory = 0.0;
                System.out.println("Memory MC: Cleared.");
                break;
        }
    }

    private boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isKnownConstant(String str) {
        // Constants are loaded in the constructor, so this call is not needed here.
        return ConstantsLoader.getAllConstants().containsKey(str.toUpperCase());
    }

    public Map<String, Double> getAllConstants() {
        return ConstantsLoader.getAllConstants();
    }

    public Map<String, Map<String, Double>> getCategorizedConstants() {
        return ConstantsLoader.getCategorizedConstants();
    }
}