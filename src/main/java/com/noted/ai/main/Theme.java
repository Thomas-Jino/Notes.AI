// Theme.java
package com.noted.ai.main;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;

/**
 * Shared color palette and branded font for every GUI class in this package
 * and its components sub-package. Holds no application state.
 */
public final class Theme {

    public static final Color PANEL_DARK   = Color.decode("#383F4D");
    public static final Color PANEL_HOVER  = Color.decode("#464E5F");
    public static final Color CARD_BG      = Color.WHITE;
    public static final Color TEXT_DARK    = Color.decode("#1B2029");
    public static final Color TEXT_MUTED   = Color.decode("#6C7480");
    public static final Color FIELD_BORDER = Color.decode("#E4E7ED");
    public static final Color ICON_COLOR   = Color.decode("#3A4150");
    public static final Color SCROLL_THUMB = Color.decode("#D5D9E1");
    public static final Color USER_AVATAR  = Color.decode("#5A6478");

    private static final String FONT_PATH = "/fonts/static/Kalnia-Regular.ttf";
    private static Font baseFont;

    private Theme() {
        // static utility, not meant to be instantiated
    }

    public static synchronized Font font(float size) {
        if (baseFont == null) {
            baseFont = loadBaseFont();
        }
        return baseFont.deriveFont(size);
    }

    private static Font loadBaseFont() {
        try (InputStream in = Theme.class.getResourceAsStream(FONT_PATH)) {
            if (in == null) {
                System.err.println("Custom font not found: " + FONT_PATH);
                return new Font(Font.SERIF, Font.PLAIN, 12);
            }
            Font loaded = Font.createFont(Font.TRUETYPE_FONT, in);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(loaded);
            return loaded;
        } catch (IOException | FontFormatException e) {
            System.err.println("Failed to load custom font: " + e.getMessage());
            return new Font(Font.SERIF, Font.PLAIN, 12);
        }
    }
}