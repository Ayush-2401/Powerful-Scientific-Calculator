// File: src/com/scientificcalculator/ui/JGradientPanel.java

package com.scientificcalculator.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;

public class JGradientPanel extends JPanel {

    private Color color1;
    private Color color2;
    private boolean horizontal;

    public JGradientPanel(Color color1, Color color2, boolean horizontal) {
        this.color1 = color1;
        this.color2 = color2;
        this.horizontal = horizontal;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);


        Point2D startPoint;
        Point2D endPoint;

        if (horizontal) {
            startPoint = new Point2D.Float(0, 0);
            endPoint = new Point2D.Float(getWidth(), 0);
        } else {
            startPoint = new Point2D.Float(0, 0);
            endPoint = new Point2D.Float(0, getHeight());
        }

        LinearGradientPaint gradient = new LinearGradientPaint(startPoint, endPoint,
                                                               new float[]{0.0f, 1.0f},
                                                               new Color[]{color1, color2});

        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, getWidth(), getHeight());
    }

    public Color getColor1() { return color1; }
    public void setColor1(Color color1) { this.color1 = color1; repaint(); }

    public Color getColor2() { return color2; }
    public void setColor2(Color color2) { this.color2 = color2; repaint(); }

    public boolean isHorizontal() { return horizontal; }
    public void setHorizontal(boolean horizontal) { this.horizontal = horizontal; repaint(); }
}