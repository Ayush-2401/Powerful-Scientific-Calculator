// File: src/com/scientificcalculator/ui/JFreeChartGraphPanel.java

package com.scientificcalculator.ui;

import com.scientificcalculator.engine.CalculatorEngine;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.ContourPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.contour.ContourDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class JFreeChartGraphPanel extends JPanel {

    private final CalculatorEngine engine;
    private final List<String> equations;
    private final List<String> variables;

    public JFreeChartGraphPanel(CalculatorEngine engine, List<String> equations, List<String> variables) {
        this.engine = engine;
        this.equations = equations;
        this.variables = variables;
        setLayout(new BorderLayout());
        JFreeChart chart = createChart();
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(600, 400));
        add(chartPanel);
    }

    private JFreeChart createChart() {
        if (variables.size() != 2) {
            // Return an empty chart or a message
            return ChartFactory.createXYLineChart("Graphing Error", "x", "y", new XYSeriesCollection());
        }

        XYPlot plot = new XYPlot();
        plot.setDomainAxis(new NumberAxis(variables.get(0)));
        plot.setRangeAxis(new NumberAxis(variables.get(1)));

        for (int i = 0; i < equations.size(); i++) {
            String equation = equations.get(i);
            String processedEquation = equation;
            if (equation.contains("=")) {
                String[] parts = equation.split("=", 2);
                processedEquation = parts[0] + " - (" + parts[1] + ")";
            }

            ContourDataset dataset = new EquationContourDataset(engine, processedEquation, variables, -10, 10, -10, 10, 100, 100);
            ContourPlot contourPlot = new ContourPlot(dataset, new NumberAxis(), new NumberAxis(), null);
            contourPlot.getDomainAxis().setRange(-10, 10);
            contourPlot.getRangeAxis().setRange(-10, 10);

            // We want to plot where the equation equals 0
            XYSeries series = contourPlot.getContourLine(0);
            series.setKey("Equation " + (i + 1));

            plot.setDataset(i, new XYSeriesCollection(series));
            XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
            plot.setRenderer(i, renderer);
        }

        plot.setOrientation(PlotOrientation.VERTICAL);
        return new JFreeChart("Equations Graph", JFreeChart.DEFAULT_TITLE_FONT, plot, true);
    }


}