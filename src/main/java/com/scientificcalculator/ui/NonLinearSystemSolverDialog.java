package com.scientificcalculator.ui;

import com.scientificcalculator.engine.CalculatorEngine;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class NonLinearSystemSolverDialog extends JDialog {

    private final CalculatorEngine engine;
    private final JTextArea equationsArea;
    private final JTextField variablesField;
    private final JTextField initialGuessesField;
    private JTabbedPane tabbedPane;
    private JFreeChartGraphPanel freeChartPanel;
    private Jzy3dGraphPanel jzy3dPanel;

    public NonLinearSystemSolverDialog(JFrame parent, CalculatorEngine engine) {
        super(parent, "System of Non-Linear Equations Solver", true);
        this.engine = engine;
        setLayout(new BorderLayout(10, 10));
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        tabbedPane = new JTabbedPane();

        // --- Solver Panel ---
        JPanel solverPanel = new JPanel(new BorderLayout(10, 10));
        solverPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        equationsArea = new JTextArea("x^2+y^2=9\ny=x");
        equationsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        equationsArea.setToolTipText("Enter one equation per line, e.g., x^2 + y^2 = 25");
        JScrollPane scrollPane = new JScrollPane(equationsArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Equations"));
        solverPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel variablesPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        variablesField = new JTextField("x,y");
        variablesField.setToolTipText("Enter variables separated by commas, e.g., x, y");
        variablesPanel.add(new JLabel("Variables:"));
        variablesPanel.add(variablesField);

        initialGuessesField = new JTextField("1,1");
        initialGuessesField.setToolTipText("Enter initial guesses separated by commas, e.g., 1.0, 1.0");
        variablesPanel.add(new JLabel("Initial Guesses:"));
        variablesPanel.add(initialGuessesField);
        
        JPanel solverControls = new JPanel(new BorderLayout());
        solverControls.add(variablesPanel, BorderLayout.NORTH);
        
        JButton solveButton = new JButton("Solve");
        solveButton.addActionListener(e -> solveSystem());
        JPanel solveButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        solveButtonPanel.add(solveButton);
        solverControls.add(solveButtonPanel, BorderLayout.SOUTH);

        solverPanel.add(solverControls, BorderLayout.SOUTH);
        tabbedPane.addTab("Solver", solverPanel);

        // --- 2D Graph Panel ---
        JPanel panel2D = new JPanel(new BorderLayout());
        JButton plot2DButton = new JButton("Plot 2D");
        plot2DButton.addActionListener(e -> plot2D());
        panel2D.add(plot2DButton, BorderLayout.SOUTH);
        tabbedPane.addTab("2D Graph", panel2D);

        // --- 3D Graph Panel ---
        JPanel panel3D = new JPanel(new BorderLayout());
        JButton plot3DButton = new JButton("Plot 3D");
        plot3DButton.addActionListener(e -> plot3D());
        panel3D.add(plot3DButton, BorderLayout.SOUTH);
        tabbedPane.addTab("3D Graph", panel3D);


        // --- Bottom Panel for cancel button ---
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setBorder(new EmptyBorder(0, 10, 10, 10));
        JButton cancelButton = new JButton("Close");
        cancelButton.addActionListener(e -> dispose());
        bottomPanel.add(cancelButton);

        add(tabbedPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        setSize(600, 500);
        setLocationRelativeTo(parent);
    }

    private void plot2D() {
        String[] equationLines = equationsArea.getText().split("\\\\n");
        List<String> equations = Arrays.asList(equationLines);
        String[] variableNames = variablesField.getText().split(",");
        List<String> variables = Arrays.stream(variableNames).map(String::trim).collect(Collectors.toList());

        if (variables.size() != 2) {
            JOptionPane.showMessageDialog(this, "2D graphing is only supported for systems with two variables.", "Graphing Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (freeChartPanel != null) {
            ((JPanel)tabbedPane.getComponentAt(1)).remove(freeChartPanel);
        }
        freeChartPanel = new JFreeChartGraphPanel(engine, equations, variables);
        ((JPanel)tabbedPane.getComponentAt(1)).add(freeChartPanel, BorderLayout.CENTER);
        tabbedPane.revalidate();
        tabbedPane.repaint();
    }

    private void plot3D() {
        String[] equationLines = equationsArea.getText().split("\\\\n");
        if (equationLines.length > 1) {
            JOptionPane.showMessageDialog(this, "3D plotting is only supported for a single equation.", "Graphing Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String equation = equationLines[0];
        
        if (!equation.contains("z")) {
             JOptionPane.showMessageDialog(this, "3D plotting requires an equation with 'z'.", "Graphing Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // For now, we only support equations in the form z = f(x,y)
        if (!equation.trim().startsWith("z")) {
             JOptionPane.showMessageDialog(this, "3D plotting currently only supports equations of the form z = f(x,y).", "Graphing Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String expression = equation.substring(equation.indexOf("=") + 1);

        if (jzy3dPanel != null) {
            ((JPanel)tabbedPane.getComponentAt(2)).remove(jzy3dPanel);
        }
        jzy3dPanel = new Jzy3dGraphPanel(engine, expression);
        ((JPanel)tabbedPane.getComponentAt(2)).add(jzy3dPanel, BorderLayout.CENTER);
        tabbedPane.revalidate();
        tabbedPane.repaint();
    }

    private void solveSystem() {
        String[] equationLines = equationsArea.getText().split("\\\\n");
        List<String> equations = Arrays.asList(equationLines);

        String[] variableNames = variablesField.getText().split(",");
        List<String> variables = Arrays.stream(variableNames).map(String::trim).collect(Collectors.toList());

        String[] initialGuessStrings = initialGuessesField.getText().split(",");
        double[] initialGuesses = new double[initialGuessStrings.length];

        try {
            for (int i = 0; i < initialGuessStrings.length; i++) {
                initialGuesses[i] = Double.parseDouble(initialGuessStrings[i].trim());
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid input for initial guesses. Please enter valid numbers.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            Map<String, Double> solution = engine.solveNonLinearSystem(equations, variables, initialGuesses);
            JOptionPane.showMessageDialog(this, formatSolution(solution), "System Solved", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not solve the system: " + ex.getMessage(), "Solver Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String formatSolution(Map<String, Double> solution) {
        StringBuilder solutionText = new StringBuilder("<html><body><h2>Solution:</h2>");
        solution.forEach((variable, value) -> {
            solutionText.append(variable)
                        .append(" = ")
                        .append(String.format("%.8f", value))
                        .append("<br>");
        });
        solutionText.append("</body></html>");

        return solutionText.toString();
    }
}