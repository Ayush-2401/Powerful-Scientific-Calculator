// File: src/com/scientificcalculator/ui/SystemSolverDialog.java

package com.scientificcalculator.ui;

import com.scientificcalculator.engine.CalculatorEngine;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Map;
import java.text.DecimalFormat;

public class SystemSolverDialog extends JDialog {

    private final CalculatorEngine engine;
    private final JComboBox<Integer> numVariablesSelector;
    private final JPanel matrixPanel;
    private JTextField[][] coefficientFields;
    private JTextField[] constantFields;

    public SystemSolverDialog(JFrame parent, CalculatorEngine engine) {
        super(parent, "System of Linear Equations Solver", true);
        this.engine = engine;
        setLayout(new BorderLayout(10, 10));
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        // --- Top Panel for selecting number of variables ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setBorder(new EmptyBorder(10, 10, 0, 10));
        topPanel.add(new JLabel("Number of variables (2-10):"));
        Integer[] sizes = {2, 3, 4, 5, 6, 7, 8, 9, 10};
        numVariablesSelector = new JComboBox<>(sizes);
        numVariablesSelector.setSelectedItem(3); // Default to 3x3
        numVariablesSelector.addActionListener(e -> buildMatrixInput((Integer) numVariablesSelector.getSelectedItem()));
        topPanel.add(numVariablesSelector);

        // --- Center Panel for matrix input ---
        matrixPanel = new JPanel();
        JScrollPane scrollPane = new JScrollPane(matrixPanel);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Enter Coefficients (Ax = b)"));
        
        // --- Bottom Panel for buttons ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBorder(new EmptyBorder(0, 10, 10, 10));
        JButton solveButton = new JButton("Solve");
        solveButton.addActionListener(e -> solveSystem());
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());
        buttonPanel.add(solveButton);
        buttonPanel.add(cancelButton);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        buildMatrixInput((Integer) numVariablesSelector.getSelectedItem());

        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(parent);
    }

    private void buildMatrixInput(int n) {
        matrixPanel.removeAll();
        matrixPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        coefficientFields = new JTextField[n][n];
        constantFields = new JTextField[n];

        Font labelFont = new Font(Font.SANS_SERIF, Font.BOLD, 14);

        for (int i = 0; i < n; i++) { // Rows
            gbc.gridy = i;
            for (int j = 0; j < n; j++) { // Columns for coefficients
                gbc.gridx = j * 2;
                coefficientFields[i][j] = new JTextField("0", 5);
                matrixPanel.add(coefficientFields[i][j], gbc);

                gbc.gridx = j * 2 + 1;
                String labelText = " x" + (j + 1) + (j < n - 1 ? " + " : "");
                JLabel varLabel = new JLabel(labelText);
                varLabel.setFont(labelFont);
                matrixPanel.add(varLabel, gbc);
            }

            gbc.gridx = n * 2;
            JLabel equalsLabel = new JLabel(" = ");
            equalsLabel.setFont(labelFont);
            matrixPanel.add(equalsLabel, gbc);

            gbc.gridx = n * 2 + 1;
            constantFields[i] = new JTextField("0", 5);
            matrixPanel.add(constantFields[i], gbc);
        }

        matrixPanel.revalidate();
        matrixPanel.repaint();
        pack(); // Resize dialog to fit new components
    }

    private void solveSystem() {
        int n = (Integer) numVariablesSelector.getSelectedItem();
        double[][] A = new double[n][n];
        double[] b = new double[n];

        try {
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    A[i][j] = Double.parseDouble(coefficientFields[i][j].getText().trim());
                }
                b[i] = Double.parseDouble(constantFields[i].getText().trim());
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid input. Please enter valid numbers for all coefficients.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            Map<String, Double> solution = engine.solveLinearSystem(A, b);
            JOptionPane.showMessageDialog(this, formatSolution(solution), "System Solved", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not solve the system: " + ex.getMessage(), "Solver Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String formatSolution(Map<String, Double> solution) {
        StringBuilder solutionText = new StringBuilder("<html><body><h2>Solution:</h2>");
        DecimalFormat df = new DecimalFormat("0.##########");

        solution.forEach((variable, value) -> {
            solutionText.append(variable)
                        .append(" = ")
                        .append(df.format(value))
                        .append("<br>");
        });
        solutionText.append("</body></html>");

        return solutionText.toString();
    }
}