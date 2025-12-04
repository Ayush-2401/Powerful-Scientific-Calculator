package com.scientificcalculator.ui;

import com.scientificcalculator.engine.CalculatorEngine;
import javax.swing.*;
import java.awt.*;

public class Plotter3DDialog extends JDialog {

    private JTextField xMinField, xMaxField, yMinField, yMaxField, equationField;
    private Jzy3dGraphPanel graphPanel;
    private CalculatorEngine engine;

    public Plotter3DDialog(Frame owner, String equation, CalculatorEngine engine) {
        super(owner, "3D Plotter", true);
        this.engine = engine;
        setSize(800, 600);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        JPanel controlPanel = new JPanel(new FlowLayout());
        equationField = new JTextField(equation, 20);
        xMinField = new JTextField("-5", 5);
        xMaxField = new JTextField("5", 5);
        yMinField = new JTextField("-5", 5);
        yMaxField = new JTextField("5", 5);
        JButton plotButton = new JButton("Plot");

        controlPanel.add(new JLabel("z = "));
        controlPanel.add(equationField);
        controlPanel.add(new JLabel("x min:"));
        controlPanel.add(xMinField);
        controlPanel.add(new JLabel("x max:"));
        controlPanel.add(xMaxField);
        controlPanel.add(new JLabel("y min:"));
        controlPanel.add(yMinField);
        controlPanel.add(new JLabel("y max:"));
        controlPanel.add(yMaxField);
        controlPanel.add(plotButton);

        add(controlPanel, BorderLayout.NORTH);

        plotButton.addActionListener(e -> plot());

        // Initial chart
        graphPanel = new Jzy3dGraphPanel(engine, equationField.getText());
        add(graphPanel, BorderLayout.CENTER);
    }

    private void plot() {
        graphPanel.setEquation(equationField.getText());
        graphPanel.updatePlot();
    }
}