package com.noted.ai.main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;

class App {
    private static Font loadCustomFont(float size) {
        Font fallback = new JLabel().getFont().deriveFont(size);

        try (InputStream fontStream = App.class.getResourceAsStream("/fonts/static/Kalnia-Regular.ttf")) {
            if (fontStream == null) {
                System.err.println("Custom font not found: /fonts/static/Kalnia-Regular.ttf");
                return fallback;
            }

            Font custom = Font.createFont(Font.TRUETYPE_FONT, fontStream).deriveFont(size);
            return custom;
        } catch (IOException | FontFormatException e) {
            System.err.println("Failed to load custom font: " + e.getMessage());
            return fallback;
        }
    }

    public static void main(String[] args) {
        // Frame Setup
        JFrame frame = new JFrame("Java Noted.AI");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);

        // Left panel
        JPanel left = new JPanel(new BorderLayout());
        left.setBackground(Color.white);
        left.setBorder(BorderFactory.createEmptyBorder(100, 35, 25, 35));

        // App name aligned to the top-left
        JLabel appName = new JLabel("Java Noted.AI");
        appName.setFont(loadCustomFont(42f));
        appName.setHorizontalAlignment(SwingConstants.LEFT);
        left.add(appName, BorderLayout.NORTH);

        // Right (content) panel
        JPanel right = new JPanel();
        right.setBackground(Color.decode("#383F4D"));
        right.setBorder(null);

        // Split pane keeps left and right halves; resize weight 0.5 makes them equal
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setResizeWeight(0.5);
        split.setContinuousLayout(true);
        split.setDividerSize(0);

        frame.getContentPane().add(split);

        frame.setVisible(true);
    }
}