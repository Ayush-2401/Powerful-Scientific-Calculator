// File: src/com/scientificcalculator/ui/EquationSolverDialog.java

package com.scientificcalculator.ui;

import com.scientificcalculator.engine.CalculatorEngine;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.stream.Stream;
import java.util.Map;
import javax.swing.JMenuItem;

public class EquationSolverDialog extends JDialog {
    private final JTextArea equationInput;
    private JButton solveButton;
    private String equationText;
    private String resultToTransfer;
    private final CalculatorEngine engine;
    private JLabel resultDisplayLabel;
    private JButton transferButton;


    public EquationSolverDialog(JFrame parent, CalculatorEngine engine) {
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
        resultDisplayLabel = new JLabel("Enter an equation and press 'Solve'.");
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
        try {
            equationText = equation; // Store the equation text for later
            String solution = engine.solveEquation(equation);
            resultToTransfer = solution; // Store for potential transfer
            resultDisplayLabel.setText("<html><b>Solution:</b> " + solution + "</html>");

            // Only enable transfer if the result is a single number that the main calculator can use.
            if (isSingleNumber(solution)) {
                transferButton.setEnabled(true);
            } else {
                transferButton.setEnabled(false);
            }
        } catch (IllegalArgumentException | ArithmeticException ex) {
            resultDisplayLabel.setText("<html><b style='color:red;'>Error:</b> " + ex.getMessage() + "</html>");
            transferButton.setEnabled(false);
            resultToTransfer = null;
        }
    }

    private boolean isSingleNumber(String s) {
        if (s == null) {
            return false;
        }
        try {
            Double.parseDouble(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
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
                EquationSolverDialog dialog = new EquationSolverDialog(frame, testEngine);
                dialog.setVisible(true);
            });
        });
    }
}