// File: src/com/scientificcalculator/ui/EquationContourDataset.java

package com.scientificcalculator.ui;

import com.scientificcalculator.engine.CalculatorEngine;
import org.jfree.data.contour.DefaultContourDataset;

import java.util.List;
import java.util.Map;

public class EquationContourDataset extends DefaultContourDataset {

    public EquationContourDataset(CalculatorEngine engine, String equation, List<String> variables, double xMin, double xMax, double yMin, double yMax, int xSteps, int ySteps) {
        if (variables.size() != 2) {
            throw new IllegalArgumentException("EquationContourDataset only supports 2 variables.");
        }

        double[] xValues = new double[xSteps];
        double[] yValues = new double[ySteps];
        double[][] zValues = new double[ySteps][xSteps];

        double xStep = (xMax - xMin) / (xSteps - 1);
        double yStep = (yMax - yMin) / (ySteps - 1);

        for (int i = 0; i < xSteps; i++) {
            xValues[i] = xMin + i * xStep;
        }

        for (int i = 0; i < ySteps; i++) {
            yValues[i] = yMin + i * yStep;
        }

        for (int i = 0; i < ySteps; i++) {
            for (int j = 0; j < xSteps; j++) {
                double[] values = new double[2];
                values[0] = xValues[j];
                values[1] = yValues[i];
                try {
                    zValues[i][j] = evaluate(engine, equation, variables, values);
                } catch (Exception e) {
                    zValues[i][j] = Double.NaN;
                }
            }
        }

        Double[] xValuesDouble = new Double[xSteps];
        for (int i = 0; i < xSteps; i++) {
            xValuesDouble[i] = xValues[i];
        }

        Double[] yValuesDouble = new Double[ySteps];
        for (int i = 0; i < ySteps; i++) {
            yValuesDouble[i] = yValues[i];
        }

        Double[][] zValuesDouble = new Double[ySteps][xSteps];
        for (int i = 0; i < ySteps; i++) {
            for (int j = 0; j < xSteps; j++) {
                zValuesDouble[i][j] = zValues[i][j];
            }
        }

        initialize(xValuesDouble, yValuesDouble, zValuesDouble);
    }

    private double evaluate(CalculatorEngine engine, String equation, List<String> variables, double[] values) {
        try {
            String processedEquation = equation;
            for (int i = 0; i < variables.size(); i++) {
                processedEquation = processedEquation.replace(variables.get(i), "(" + Double.toString(values[i]) + ")");
            }
            return engine.evaluate(processedEquation);
        } catch (Exception e) {
            return Double.NaN;
        }
    }
}
