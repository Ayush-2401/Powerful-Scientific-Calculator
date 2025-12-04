// File: src/com/scientificcalculator/ui/UnifiedEquationSolverDialog.java

package com.scientificcalculator.ui;

import com.scientificcalculator.engine.CalculatorEngine;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.stream.Stream;
import java.util.Map;
import javax.swing.JMenuItem;

public class UnifiedEquationSolverDialog extends JDialog {
    private final JTextArea equationInput;
    private JButton solveButton;
    private String equationText;
    private String resultToTransfer;
    private final CalculatorEngine engine;
    private JLabel resultDisplayLabel;
    private JButton transferButton;


    public UnifiedEquationSolverDialog(JFrame parent, CalculatorEngine engine) {
        super(parent, "Equation Solver", true);
        this.engine = engine;
        setLayout(new BorderLayout());
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        equationInput = new JTextArea();
        equationInput.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 16));
        JScrollPane scrollPane = new JScrollPane(equationInput);
        add(scrollPane, BorderLayout.CENTER);

        // Create a panel for additional buttons
        JPanel insertPanel = new JPanel();
        insertPanel.setLayout(new FlowLayout());

        // Define the buttons to insert
        String[] staticButtons = {"x", "+", "-", "*", "/", "^", "(", ")", "="};
        String[] functionButtons = {"sin(", "cos(", "tan(", "log(", "ln(", "sqrt("};

        // Add buttons for static symbols
        Stream.of(staticButtons).forEach(buttonLabel -> {
            JButton button = new JButton(buttonLabel); // Create a new button for each label
           button.addActionListener(new ActionListener() {
                @Override


                public void actionPerformed(ActionEvent e) {
                    String currentText = equationInput.getText();
                    String selectedText = equationInput.getSelectedText();
                    int caretPosition = equationInput.getCaretPosition();

                    String textToInsert = buttonLabel;
                    equationInput.insert(textToInsert, caretPosition);
                    equationInput.setCaretPosition(caretPosition + textToInsert.length());
                 }
            });
            insertPanel.add(button);
        });

        // Create a menu bar for the dialog
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        // Add a "Constants" dropdown menu
        JMenu constantsMenu = new JMenu("Constants");
        Map<String, Map<String, Double>> categorizedConstants = engine.getCategorizedConstants();

        if (categorizedConstants == null || categorizedConstants.isEmpty()) {
            JMenuItem errorItem = new JMenuItem("Constants not loaded. Check logs.");
            errorItem.setEnabled(false);
            constantsMenu.add(errorItem);
        } else {
            // Sort categories alphabetically
            categorizedConstants.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(categoryEntry -> {
                    JMenu categoryMenu = new JMenu(categoryEntry.getKey());
    
                    // Sort constants within category alphabetically
                    categoryEntry.getValue().entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .forEach(constantEntry -> {
                            String name = constantEntry.getKey();
                            JMenuItem constantItem = new JMenuItem(name);
                            constantItem.addActionListener(e -> {
                                int caretPosition = equationInput.getCaretPosition();
                                equationInput.insert(name, caretPosition);
                                equationInput.setCaretPosition(caretPosition + name.length());
                            });
                            categoryMenu.add(constantItem);
                        });
                    constantsMenu.add(categoryMenu);
                });
        }

        menuBar.add(constantsMenu);

        // Add buttons for functions
        Stream.of(functionButtons).forEach(buttonLabel -> {
            JButton button = new JButton(buttonLabel);
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String currentText = equationInput.getText();
                    String selectedText = equationInput.getSelectedText();
                    int caretPosition = equationInput.getCaretPosition();

                    String textToInsert = buttonLabel;
                    equationInput.insert(textToInsert, caretPosition);
                    equationInput.setCaretPosition(caretPosition + textToInsert.length());
                }
            });
            insertPanel.add(button);
        });

        add(insertPanel, BorderLayout.NORTH);

        // --- South Panel with controls and result display ---
        JPanel southPanel = new JPanel(new BorderLayout(10, 5));
        southPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

        // Result display
        resultDisplayLabel = new JLabel("Enter an equation and press \'Solve\'.");
        resultDisplayLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        southPanel.add(resultDisplayLabel, BorderLayout.CENTER);

        // Action buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        solveButton = new JButton("Solve");
        transferButton = new JButton("Transfer Result");
        JButton closeButton = new JButton("Close");

        transferButton.setEnabled(false); // Initially disabled

        solveButton.addActionListener(e -> solveEquation());
        transferButton.addActionListener(e -> dispose()); // Result is already stored for transfer
        closeButton.addActionListener(e -> {
            resultToTransfer = null; // Ensure no result is transferred on close
            dispose();
        });

        buttonPanel.add(solveButton);
        buttonPanel.add(transferButton);
        buttonPanel.add(closeButton);

        southPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(southPanel, BorderLayout.SOUTH);

        setSize(550, 350);
        setLocationRelativeTo(parent); // Center relative to the parent frame
    }

    private void solveEquation() {
        String equation = equationInput.getText();
        System.out.println("Solving equation: " + equation);
        try {
            equationText = equation; // Store the equation text for later
            String solution;
            try {
                System.out.println("Attempting to solve as a polynomial.");
                double[] coeffs = parsePolynomial(equation);
                System.out.println("Parsed coefficients: " + java.util.Arrays.toString(coeffs));
                solution = engine.solvePolynomial(coeffs);
            } catch (IllegalArgumentException e) {
                System.out.println("Not a valid polynomial or parsing failed: " + e.getMessage() + ". Falling back to general solver.");
                e.printStackTrace();
                solution = engine.solveEquation(equation);
            }
            
            System.out.println("Solution: " + solution);
            resultToTransfer = solution; // Store for potential transfer
            resultDisplayLabel.setText("<html><b>Solution:</b> " + solution + "</html>");

            // Enable transfer only if the solution is a single real number.
            try {
                Double.parseDouble(solution);
                transferButton.setEnabled(true);
            } catch (NumberFormatException e) {
                transferButton.setEnabled(false);
            }
        } catch (IllegalArgumentException | ArithmeticException ex) {
            resultDisplayLabel.setText("<html><b style=\'color:red;\'>Error:</b> " + ex.getMessage() + "</html>");
            transferButton.setEnabled(false);
            resultToTransfer = null;
        }
    }

    private double[] parsePolynomial(String equation) throws IllegalArgumentException {
        if (!equation.contains("=")) {
            throw new IllegalArgumentException("Polynomial equation must contain an '='.");
        }

        String[] sides = equation.split("=");
        if (sides.length > 2) {
            throw new IllegalArgumentException("Equation cannot have more than one '='.");
        }
        String leftSide = sides[0];
        String rightSide = (sides.length == 2) ? sides[1] : "0";

        Map<Integer, Double> leftCoeffs = parsePolynomialSide(leftSide);
        Map<Integer, Double> rightCoeffs = parsePolynomialSide(rightSide);

        // Subtract right side coefficients from the left side to get LHS = 0 form
        rightCoeffs.forEach((degree, coeff) -> leftCoeffs.merge(degree, -coeff, Double::sum));

        if (leftCoeffs.isEmpty()) {
            throw new IllegalArgumentException("No terms found in the equation.");
        }

        int maxDegree = 0;
        for (int deg : leftCoeffs.keySet()) {
            if (deg > maxDegree) {
                maxDegree = deg;
            }
        }
        
        if (maxDegree == 0) {
            throw new IllegalArgumentException("Not a valid polynomial equation for the solver (no variable found).");
        }

        double[] result = new double[maxDegree + 1];
        for (int i = 0; i <= maxDegree; i++) {
            result[maxDegree - i] = leftCoeffs.getOrDefault(i, 0.0);
        }

        return result;
    }

    private Map<Integer, Double> parsePolynomialSide(String side) throws IllegalArgumentException {
        Map<Integer, Double> coeffs = new java.util.HashMap<>();
        String sideTrimmed = side.trim();
        if (sideTrimmed.isEmpty()) {
            return coeffs;
        }

        // Replace subtraction with addition of a negative to simplify splitting, but don't replace the sign of an exponent.
        String processedSide = sideTrimmed.replaceAll("(?<!\\^)-", "+-");
        if (processedSide.startsWith("+-")) {
            processedSide = processedSide.substring(1); // Handle cases like "-x^2..."
        }

        String[] terms = processedSide.split("[+]");

        for (String term : terms) {
            term = term.trim();
            if (term.isEmpty()) continue;

            double coeff = 1.0;
            int degree = 0;

            if (term.contains("x")) {
                String[] parts = term.split("x", 2);
                String coeffPart = parts[0].trim();

                if (coeffPart.isEmpty()) {
                    coeff = 1.0;
                } else if (coeffPart.equals("-")) {
                    coeff = -1.0;
                } else {
                    try {
                        coeff = Double.parseDouble(coeffPart.replace("*", ""));
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Invalid coefficient: " + coeffPart);
                    }
                }

                if (parts.length > 1 && !parts[1].trim().isEmpty()) {
                    String degreePart = parts[1].trim();
                    if (degreePart.startsWith("^")) {
                        try {
                            degree = Integer.parseInt(degreePart.substring(1).trim());
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException("Invalid degree: " + degreePart);
                        }
                    } else {
                        throw new IllegalArgumentException("Invalid term format after variable \'x\': " + term);
                    }
                } else {
                    degree = 1;
                }
            } else { // Constant term
                try {
                    coeff = Double.parseDouble(term);
                    degree = 0;
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid constant term: " + term);
                }
            }
            coeffs.put(degree, coeffs.getOrDefault(degree, 0.0) + coeff);
        }
        return coeffs;
    }


    /**
     * Returns the solved equation, or null if the dialog was cancelled.
     * @return The result to transfer.
     */
    public String getResultToTransfer() {
        return resultToTransfer;
    }

    public String getEquation() {
        return equationText;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Test Frame");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(300, 200);
            frame.setLocationRelativeTo(null);

            CalculatorEngine testEngine = new CalculatorEngine();
            JButton openDialogButton = new JButton("Open Equation Solver");
            frame.add(openDialogButton, BorderLayout.CENTER);
            frame.setVisible(true);

            openDialogButton.addActionListener(e -> {
                UnifiedEquationSolverDialog dialog = new UnifiedEquationSolverDialog(frame, testEngine);
                dialog.setVisible(true);
            });
        });
    }
}
