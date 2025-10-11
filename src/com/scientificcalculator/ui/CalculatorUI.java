// File: src/com/scientificcalculator/ui/CalculatorUI.java

package com.scientificcalculator.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Cursor;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import com.scientificcalculator.engine.CalculatorEngine;

public class CalculatorUI extends JFrame implements ActionListener {

    private final JTextField primaryDisplay;
    private final JTextField secondaryDisplay;

    private final Map<String, JButton> buttons;
    private JPopupMenu modesMenu;

    private CalculatorEngine engine;

    public CalculatorUI() {
        setTitle("Advanced Scientific Calculator");
        setSize(500, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));
        buttons = new LinkedHashMap<>(); // Use LinkedHashMap to maintain insertion order

        engine = new CalculatorEngine();

        Color displayPanelColor1 = new Color(30, 30, 30);
        Color displayPanelColor2 = new Color(10, 10, 10);
        Color buttonPanelColor1 = new Color(20, 20, 20);
        Color buttonPanelColor2 = new Color(40, 40, 40);

        Color numBtnBase = new Color(55, 55, 55);
        Color numBtnHighlight = new Color(75, 75, 75);
        Color opBtnBase = new Color(170, 100, 30);
        Color opBtnHighlight = new Color(200, 120, 40);
        Color sciBtnBase = new Color(60, 90, 150);
        Color sciBtnHighlight = new Color(80, 110, 180);
        Color clearBtnBase = new Color(150, 60, 60);
        Color clearBtnHighlight = new Color(180, 80, 80);
        Color equalsBtnBase = new Color(60, 120, 90);
        Color equalsBtnHighlight = new Color(80, 140, 110);

        Color primaryDisplayTextColor = Color.WHITE;
        Color secondaryDisplayTextColor = new Color(180, 180, 180);
        Color buttonText = Color.WHITE;

        Font displayFontPrimary = new Font(Font.SANS_SERIF, Font.BOLD, 48);
        Font displayFontSecondary = new Font(Font.SANS_SERIF, Font.PLAIN, 22);
        Font buttonFont = new Font(Font.SANS_SERIF, Font.BOLD, 20);

        JGradientPanel displayPanel = new JGradientPanel(displayPanelColor1, displayPanelColor2, false);
        displayPanel.setLayout(new BoxLayout(displayPanel, BoxLayout.Y_AXIS));
        displayPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        secondaryDisplay = new JTextField("0");
        secondaryDisplay.setEditable(false);
        secondaryDisplay.setHorizontalAlignment(JTextField.RIGHT);
        secondaryDisplay.setFont(displayFontSecondary);
        secondaryDisplay.setOpaque(true);
        secondaryDisplay.setBackground(displayPanelColor1);
        secondaryDisplay.setForeground(secondaryDisplayTextColor);
        secondaryDisplay.setBorder(BorderFactory.createEmptyBorder());

        primaryDisplay = new JTextField("0");
        primaryDisplay.setEditable(false);
        primaryDisplay.setHorizontalAlignment(JTextField.RIGHT);
        primaryDisplay.setFont(displayFontPrimary);
        primaryDisplay.setOpaque(true);
        primaryDisplay.setBackground(displayPanelColor1);
        primaryDisplay.setForeground(primaryDisplayTextColor);
        primaryDisplay.setBorder(BorderFactory.createEmptyBorder());

        displayPanel.add(Box.createVerticalGlue());
        displayPanel.add(secondaryDisplay);
        displayPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        displayPanel.add(primaryDisplay);

        add(displayPanel, BorderLayout.NORTH);

        JGradientPanel buttonPanel = new JGradientPanel(buttonPanelColor1, buttonPanelColor2, false);
        buttonPanel.setLayout(new GridLayout(8, 5, 8, 8));
        buttonPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        String[] buttonLabels = {
            "DEG/RAD", "MC", "MR", "M+", "M-",
            "C", "CE", "%", "/", "sqrt",
            "7", "8", "9", "*", "1/x",
            "4", "5", "6", "-", "x^y",
            "1", "2", "3", "+", "log",
            "PI", "E", "(", ")", "ln",
            "sin", "cos", "tan", "!", "=",
            "0", ".", "+/-", "<--", "Modes"
        };

        class RoundedButton extends JButton {
            private final Color baseColor;
            private final Color highlightColor;
            private boolean hovered = false;
            private boolean pressed = false;

            public RoundedButton(String text, Color baseColor, Color highlightColor) {
                super(text);
                this.baseColor = baseColor;
                this.highlightColor = highlightColor;
                setContentAreaFilled(false);
                setFocusPainted(false);
                setBorderPainted(false);
                setFont(buttonFont);
                setForeground(buttonText);
                setCursor(new Cursor(Cursor.HAND_CURSOR));

                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent evt) { hovered = true; repaint(); }
                    @Override
                    public void mouseExited(MouseEvent evt) { hovered = false; repaint(); }
                    @Override
                    public void mousePressed(MouseEvent evt) { pressed = true; repaint(); }
                    @Override
                    public void mouseReleased(MouseEvent evt) { pressed = false; repaint(); }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

                int width = getWidth();
                int height = getHeight();
                int arc = 20;

                Color fillColor = baseColor;
                if (pressed) {
                    fillColor = highlightColor.darker().darker();
                } else if (hovered) {
                    fillColor = highlightColor;
                }

                g2.setColor(fillColor);
                g2.fill(new RoundRectangle2D.Double(0, 0, width, height, arc, arc));

                g2.setColor(fillColor.darker().darker());
                g2.draw(new RoundRectangle2D.Double(0, 0, width - 1, height - 1, arc, arc));

                super.paintComponent(g);
                g2.dispose();
            }
        }

        for (String label : buttonLabels) {
            RoundedButton button;
            Color baseC, highlightC;

            switch (label) {
                case "=":
                    baseC = equalsBtnBase;
                    highlightC = equalsBtnHighlight;
                    break;
                case "C":
                case "CE":
                case "<--":
                    baseC = clearBtnBase;
                    highlightC = clearBtnHighlight;
                    break;
                case "+", "-", "*", "/", "%", "x^y", "sqrt", "1/x":
                    baseC = opBtnBase;
                    highlightC = opBtnHighlight;
                    break;
                case "sin", "cos", "tan", "log", "ln", "PI", "E", "!", "DEG/RAD", "Modes", "MC", "MR", "M+", "M-", "(", ")":
                    baseC = sciBtnBase;
                    highlightC = sciBtnHighlight;
                    break;
                case "+/-":
                    baseC = sciBtnBase;
                    highlightC = sciBtnHighlight;
                    break;
                default:
                    baseC = numBtnBase;
                    highlightC = numBtnHighlight;
                    break;
            }

            button = new RoundedButton(label, baseC, highlightC);
            // Special listener for the Modes button
            if (label.equals("Modes")) {
                button.addActionListener(e -> {
                    modesMenu.show(button, 0, button.getHeight());
                });
            } else {
                button.addActionListener(this);
            }
            buttons.put(label, button);
            buttonPanel.add(button);
        }

        add(buttonPanel, BorderLayout.CENTER);
        initializeModesMenu();

        this.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                char keyChar = e.getKeyChar();
                String command = String.valueOf(keyChar);

                // Let keyTyped handle character inputs, as it correctly respects keyboard layouts and shift keys.
                if (buttons.containsKey(command)) {
                    handleCommand(command);
                }
            }

            @Override
            public void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode();
                // Let keyPressed handle special, non-character keys.
                switch (keyCode) {
                    case KeyEvent.VK_ENTER:       handleCommand("="); break;
                    case KeyEvent.VK_BACK_SPACE:  handleCommand("<--"); break;
                    case KeyEvent.VK_ESCAPE:      handleCommand("C"); break;
                }
            }
        });

        this.setFocusable(true);
        this.requestFocusInWindow();

        setVisible(true);
    }

    private void initializeModesMenu() {
        modesMenu = new JPopupMenu();
        String[] modeNames = {
            "Single Variable Equation",
            "Complex Calculator",
            "System of Linear Equations",
            "Polynomial Root Finder",
            "Base-N",
            "Matrix",
            "Vector",
            "Statistics",
            "Distribution",
            "Spreadsheet",
            "Table",
            "Inequality",
            "Ratio"
        };

        for (String modeName : modeNames) {
            JMenuItem menuItem = new JMenuItem(modeName);
            if (modeName.equals("Single Variable Equation")) {
                menuItem.addActionListener(e -> openEquationSolver());
            } else if (modeName.equals("System of Linear Equations")) {
                menuItem.addActionListener(e -> openSystemEquationSolver());
            } else if (modeName.equals("Polynomial Root Finder")) {
                menuItem.addActionListener(e -> openPolynomialSolver());
            } else {
                menuItem.addActionListener(e -> showModeNotImplemented(modeName));
            }
            modesMenu.add(menuItem);
        }
    }

    private void openEquationSolver() {
       EquationSolverDialog solverDialog = new EquationSolverDialog(this, engine);
        solverDialog.setVisible(true);

        String result = solverDialog.getResultToTransfer();
        if (result != null) {
            // The result from the solver is a final answer, not a new expression.
            // We can display it directly and update the secondary display to show what was solved.
            primaryDisplay.setText(result);
            secondaryDisplay.setText("Solved: " + solverDialog.getEquation());
        }
    }

    private void openSystemEquationSolver() {
        SystemSolverDialog solverDialog = new SystemSolverDialog(this, engine);
        solverDialog.setVisible(true);
    }

    private void openPolynomialSolver() {
        PolynomialSolverDialog solverDialog = new PolynomialSolverDialog(this, engine);
        solverDialog.setVisible(true);
    }

    private void showModeNotImplemented(String modeName) {
        JOptionPane.showMessageDialog(this, modeName + " mode is not yet implemented.", "Coming Soon", JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();
        handleCommand(command);
    }

    /**
     * Centralized method to process a command from any source (button click, key press).
     * @param command The command string to be processed by the engine.
     */
    private void handleCommand(String command) {
        if (command == null) return;
        System.out.println("UI processing command: " + command);
        String[] displayUpdates = engine.processCommand(command);

        primaryDisplay.setText(displayUpdates[0]);
        secondaryDisplay.setText(displayUpdates[1]);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CalculatorUI());
    }
}