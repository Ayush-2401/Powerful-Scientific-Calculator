// File: src/com/scientificcalculator/ui/PolynomialSolverDialog.java

package com.scientificcalculator.ui;

import com.scientificcalculator.engine.CalculatorEngine;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class PolynomialSolverDialog extends JDialog {

    private final CalculatorEngine engine;
    private final JComboBox<String> degreeSelector;
    private final JPanel coeffPanel;
    private final JLabel resultLabel;
    private final List<JTextField> coeffFields = new ArrayList<>();

    public PolynomialSolverDialog(JFrame parent, CalculatorEngine engine) {
        super(parent, "Polynomial Root Finder", true);
        this.engine = engine;
        setLayout(new BorderLayout(10, 10));
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        // --- Top Panel for Degree Selection ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setBorder(new EmptyBorder(10, 10, 0, 10));
        topPanel.add(new JLabel("Polynomial Degree:"));
        String[] degrees = {"Quadratic (ax²+bx+c=0)", "Cubic (ax³+bx²+cx+d=0)", "Quartic (ax⁴+...+e=0)"};
        degreeSelector = new JComboBox<>(degrees);
        degreeSelector.addActionListener(e -> buildCoeffInput());
        topPanel.add(degreeSelector);

        // --- Center Panel for Coefficients ---
        coeffPanel = new JPanel(new GridBagLayout());
        coeffPanel.setBorder(BorderFactory.createTitledBorder("Enter Coefficients"));

        // --- Bottom Panel for Controls and Result ---
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setBorder(new EmptyBorder(0, 10, 10, 10));

        resultLabel = new JLabel("Enter coefficients and press 'Solve'.");
        resultLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        bottomPanel.add(resultLabel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton solveButton = new JButton("Solve");
        solveButton.addActionListener(e -> solvePolynomial());
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        buttonPanel.add(solveButton);
        buttonPanel.add(closeButton);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);
        add(coeffPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        buildCoeffInput();

        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(parent);
    }

    private void buildCoeffInput() {
        coeffPanel.removeAll();
        coeffFields.clear();
        int degree = degreeSelector.getSelectedIndex() + 2; // 0 -> degree 2, 1 -> degree 3

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        for (int i = 0; i <= degree; i++) {
            int power = degree - i;
            char variable = (char) ('a' + i);
            String labelText = variable + " (x^" + power + "):";
            if (power == 1) labelText = variable + " (x):";
            if (power == 0) labelText = variable + " (constant):";

            gbc.gridx = 0;
            gbc.gridy = i;
            coeffPanel.add(new JLabel(labelText), gbc);

            JTextField field = new JTextField("0", 8);
            coeffFields.add(field);
            gbc.gridx = 1;
            coeffPanel.add(field, gbc);
        }

        coeffPanel.revalidate();
        coeffPanel.repaint();
        pack();
    }

    private void solvePolynomial() {
        double[] coeffs = new double[coeffFields.size()];
        try {
            for (int i = 0; i < coeffFields.size(); i++) {
                coeffs[i] = Double.parseDouble(coeffFields.get(i).getText().trim());
            }
        } catch (NumberFormatException ex) {
            resultLabel.setText("<html><b style='color:red;'>Error:</b> Please enter valid numbers.</html>");
            return;
        }

        String solution;
        try {
            if (degreeSelector.getSelectedIndex() == 0) { // Quadratic
                solution = engine.solveQuadratic(coeffs[0], coeffs[1], coeffs[2]);
            } else if (degreeSelector.getSelectedIndex() == 1) { // Cubic
                solution = engine.solveCubic(coeffs[0], coeffs[1], coeffs[2], coeffs[3]);
            } else { // Quartic
                solution = engine.solveQuartic(coeffs[0], coeffs[1], coeffs[2], coeffs[3], coeffs[4]);
            }
            resultLabel.setText("<html><b>Roots:</b> " + solution + "</html>");
        } catch (Exception ex) {
            resultLabel.setText("<html><b style='color:red;'>Error:</b> " + ex.getMessage() + "</html>");
        }
    }
}