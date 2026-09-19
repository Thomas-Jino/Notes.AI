// Scale.java
package com.noted.ai.main;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * Central scaling engine shared by every GUI class in this package and its
 * components sub-package, so the whole application grows and shrinks as one
 * piece no matter which class draws a given pixel.
 *
 * Holds no application state and makes no decisions about what the app
 * does - only how big things are drawn.
 */
public final class Scale {

    private static final float DESIGN_WIDTH  = 820f;
    private static final float DESIGN_HEIGHT = 640f;
    private static final float MIN_FACTOR    = 0.80f;
    private static final float MAX_FACTOR    = 2.60f;

    private static final List<Runnable> HOOKS = new ArrayList<>();
    private static float factor = 1f;

    private Scale() {
        // static utility, not meant to be instantiated
    }

    /** Recomputes the factor from the current window size; reruns every hook only if it changed. */
    public static void recompute(int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        float next = Math.min(width / DESIGN_WIDTH, height / DESIGN_HEIGHT);
        next = Math.max(MIN_FACTOR, Math.min(MAX_FACTOR, next));

        if (Math.abs(next - factor) > 0.005f) {
            factor = next;
            for (Runnable hook : HOOKS) {
                hook.run();
            }
        }
    }

    public static int s(double designValue) {
        return Math.max(1, Math.round((float) (designValue * factor)));
    }

    public static float sf(double designValue) {
        return (float) (designValue * factor);
    }

    /** Registers a restyle step and runs it once immediately so first layout is already correct. */
    public static void onScale(Runnable hook) {
        HOOKS.add(hook);
        hook.run();
    }

    public static void font(JComponent target, double designSize) {
        onScale(() -> target.setFont(Theme.font(sf(designSize))));
    }

    public static void padding(JComponent target, double top, double left, double bottom, double right) {
        onScale(() -> target.setBorder(
                BorderFactory.createEmptyBorder(s(top), s(left), s(bottom), s(right))));
    }

    public static void vgap(JPanel target, double designGap) {
        onScale(() -> ((BorderLayout) target.getLayout()).setVgap(s(designGap)));
    }

    public static void hgap(JPanel target, double designGap) {
        onScale(() -> ((BorderLayout) target.getLayout()).setHgap(s(designGap)));
    }
}