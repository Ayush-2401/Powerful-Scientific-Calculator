package com.scientificcalculator.ui;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JPanel;

import org.jzy3d.chart.Chart;
import org.jzy3d.chart.factories.AWTChartFactory;
import org.jzy3d.colors.Color;
import org.jzy3d.colors.ColorMapper;
import org.jzy3d.colors.colormaps.ColorMapRainbow;
import org.jzy3d.maths.Range;
import org.jzy3d.plot3d.builder.Mapper;
import org.jzy3d.plot3d.builder.SurfaceBuilder;
import org.jzy3d.plot3d.builder.orthonormal.OrthonormalGrid;
import org.jzy3d.plot3d.primitives.Shape;
import org.jzy3d.plot3d.rendering.canvas.Quality;

import com.scientificcalculator.engine.CalculatorEngine;

public class Jzy3dGraphPanel extends JPanel {
    private Chart chart;
    private CalculatorEngine engine;
    private Shape surface;
    private String equation;

    public Jzy3dGraphPanel(CalculatorEngine engine, String initialEquation) {
        super(new BorderLayout());
        this.engine = engine;
        this.equation = initialEquation;

        // Create a chart with advanced quality
        chart = new AWTChartFactory().newChart(Quality.Advanced);
        
        // Create the initial surface and add it to the chart
        surface = createSurface();
        chart.getScene().getGraph().add(surface);

        // Add the chart canvas to the panel
        add((Component) chart.getCanvas(), BorderLayout.CENTER);
    }

    public void setEquation(String equation) {
        this.equation = equation;
    }

    /**
     * Updates the plot by removing the old surface and adding a new one based on the current equation.
     */
    public void updatePlot() {
        chart.getScene().getGraph().remove(surface);
        surface = createSurface();
        chart.getScene().getGraph().add(surface);
    }

    /**
     * Creates a 3D surface based on the equation.
     * @return The 3D surface shape.
     */
    private Shape createSurface() {
        // Define a mapper to translate the (x, y) coordinates to a z value using the engine.
        Mapper mapper = new Mapper() {
            @Override
            public double f(double x, double y) {
                String processedEquation = equation;
                // Replace x and y with their values. Encapsulate values in parentheses to handle negative numbers correctly.
                processedEquation = processedEquation.replace("x", "(" + Double.toString(x) + ")");
                processedEquation = processedEquation.replace("y", "(" + Double.toString(y) + ")");
                try {
                    return engine.evaluate(processedEquation);
                } catch (Exception e) {
                    // If the equation is invalid, return 0.
                    return 0;
                }
            }
        };

        // Define the range and number of steps for the grid.
        Range range = new Range(-3, 3);
        int steps = 80;

        // Create the surface using a builder.
        final Shape surface = new SurfaceBuilder().orthonormal(new OrthonormalGrid(range, steps, range, steps), mapper);
        
        // Set up the color mapping for the surface.
        surface.setColorMapper(new ColorMapper(new ColorMapRainbow(), surface.getBounds().getZmin(), surface.getBounds().getZmax(), new Color(1, 1, 1, .5f)));
        surface.setFaceDisplayed(true);
        surface.setWireframeDisplayed(true);
        surface.setWireframeColor(Color.BLACK);

        return surface;
    }
}