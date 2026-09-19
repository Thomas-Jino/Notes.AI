package com.noted.ai.main;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.Path2D;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.basic.BasicScrollBarUI;

/**
 * Noted.AI - main window, empty state.
 *
 * This class is presentation only: layout, painting and resize behaviour.
 * It holds no application state and talks to nothing else in the project.
 *
 * Every dimension below is expressed at design scale (1.0) and passed through
 * s()/sf() before use, so the whole window grows and shrinks as one piece.
 *
 * Styling of a component is always applied by the builder that creates it,
 * never from inside the component's own constructor, so no partially built
 * instance is ever published to the scale registry.
 */
public class App {

    /* ------------------------------------------------------------------
     * Palette
     * ------------------------------------------------------------------ */
    private static final Color PANEL_DARK   = Color.decode("#383F4D");
    private static final Color PANEL_HOVER  = Color.decode("#464E5F");
    private static final Color CARD_BG      = Color.WHITE;
    private static final Color TEXT_DARK    = Color.decode("#1B2029");
    private static final Color TEXT_MUTED   = Color.decode("#6C7480");
    private static final Color FIELD_BORDER = Color.decode("#E4E7ED");
    private static final Color ICON_COLOR   = Color.decode("#3A4150");
    private static final Color SCROLL_THUMB = Color.decode("#D5D9E1");

    /* ------------------------------------------------------------------
     * Scaling
     *
     * DESIGN_WIDTH / DESIGN_HEIGHT describe the window size at which the
     * layout is drawn 1:1. A larger window scales everything up, a smaller
     * one scales it down. Lower these two numbers to make the UI bigger
     * everywhere; raise them to make it smaller.
     * ------------------------------------------------------------------ */
    private static final float DESIGN_WIDTH  = 820f;
    private static final float DESIGN_HEIGHT = 640f;
    private static final float MIN_SCALE     = 0.80f;
    private static final float MAX_SCALE     = 2.60f;
    private static final double SPLIT_RATIO  = 0.46;

    private static final List<Runnable> SCALE_HOOKS = new ArrayList<>();
    private static float scale = 1f;

    private static final String FONT_PATH = "/fonts/static/Kalnia-Regular.ttf";
    private static Font baseFont;

    private App() {
        // GUI definition only; not meant to be instantiated.
    }

    /* ------------------------------------------------------------------
     * Entry point
     * ------------------------------------------------------------------ */
    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        SwingUtilities.invokeLater(App::buildAndShow);
    }

    private static void buildAndShow() {
        installLookAndFeel();

        JFrame frame = new JFrame("Java Noted.AI");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(700, 540));
        frame.setSize(980, 760);
        frame.setLocationRelativeTo(null);

        final JSplitPane split = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT, buildLeftPanel(), buildRightPanel());
        split.setResizeWeight(SPLIT_RATIO);
        split.setContinuousLayout(true);
        split.setDividerSize(0);
        split.setBorder(null);

        frame.setContentPane(split);
        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                applyScale(split);
            }
        });

        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setVisible(true);

        SwingUtilities.invokeLater(() -> applyScale(split));
    }

    /** Recomputes the scale factor from the current window size and re-lays everything out. */
    private static void applyScale(JSplitPane split) {
        int width = split.getWidth();
        int height = split.getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        float next = Math.min(width / DESIGN_WIDTH, height / DESIGN_HEIGHT);
        next = Math.max(MIN_SCALE, Math.min(MAX_SCALE, next));

        if (Math.abs(next - scale) > 0.005f) {
            scale = next;
            for (Runnable hook : SCALE_HOOKS) {
                hook.run();
            }
        }

        split.setDividerLocation(SPLIT_RATIO);
        split.revalidate();
        split.repaint();
    }

    private static int s(double designValue) {
        return Math.max(1, Math.round((float) (designValue * scale)));
    }

    private static float sf(double designValue) {
        return (float) (designValue * scale);
    }

    /** Registers a restyle step and runs it once so the first layout is already correct. */
    private static void onScale(Runnable hook) {
        SCALE_HOOKS.add(hook);
        hook.run();
    }

    /**
     * Applies a scale-aware font. Call this from a builder on a fully constructed
     * component, never from that component's own constructor.
     */
    private static void scaledFont(JComponent target, double designSize) {
        onScale(() -> target.setFont(font(sf(designSize))));
    }

    /** Applies scale-aware empty-border padding. Same construction rule as scaledFont. */
    private static void scaledPadding(JComponent target, double top, double left,
                                      double bottom, double right) {
        onScale(() -> target.setBorder(
                BorderFactory.createEmptyBorder(s(top), s(left), s(bottom), s(right))));
    }

    /** Applies a scale-aware vertical gap to a BorderLayout container. */
    private static void scaledVgap(JPanel target, double designGap) {
        onScale(() -> ((BorderLayout) target.getLayout()).setVgap(s(designGap)));
    }

    /** Applies a scale-aware horizontal gap to a BorderLayout container. */
    private static void scaledHgap(JPanel target, double designGap) {
        onScale(() -> ((BorderLayout) target.getLayout()).setHgap(s(designGap)));
    }

    private static void installLookAndFeel() {
        try {
            // referenced by name so the file still compiles without the dependency resolved
            UIManager.setLookAndFeel("com.formdev.flatlaf.FlatLightLaf");
        } catch (ReflectiveOperationException | UnsupportedLookAndFeelException ex) {
            System.err.println("FlatLaf unavailable (" + ex.getMessage() + ") - falling back.");
            installSystemLookAndFeel();
        }
    }

    private static void installSystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ReflectiveOperationException | UnsupportedLookAndFeelException ex) {
            System.err.println("System look and feel unavailable (" + ex.getMessage()
                    + ") - keeping the default.");
        }
    }

    /* ------------------------------------------------------------------
     * Left panel : documents
     * ------------------------------------------------------------------ */
    private static JPanel buildLeftPanel() {
        JPanel left = new JPanel(new BorderLayout());
        left.setBackground(CARD_BG);
        scaledPadding(left, 80, 45, 40, 45);

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel appName = new JLabel("Java Noted.AI");
        appName.setForeground(TEXT_DARK);
        appName.setAlignmentX(Component.LEFT_ALIGNMENT);
        scaledFont(appName, 34);

        JPanel docRow = new JPanel(new BorderLayout());
        docRow.setOpaque(false);
        docRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel documents = new JLabel("Documents");
        documents.setForeground(TEXT_DARK);
        scaledFont(documents, 34);
        docRow.add(documents, BorderLayout.WEST);

        PillButton addNew = new PillButton("Add New");
        scaledFont(addNew, 13);
        scaledPadding(addNew, 8, 18, 8, 18);

        JPanel buttonHolder = new JPanel(new GridBagLayout()); // vertically centres the pill
        buttonHolder.setOpaque(false);
        buttonHolder.add(addNew);
        docRow.add(buttonHolder, BorderLayout.EAST);

        header.add(appName);
        header.add(new Gap(46, true));
        header.add(docRow);

        left.add(header, BorderLayout.NORTH);
        left.add(buildEmptyDocumentsState(), BorderLayout.CENTER);
        return left;
    }

    private static JPanel buildEmptyDocumentsState() {
        JPanel centered = new JPanel(new GridBagLayout());
        centered.setOpaque(false);
        centered.add(centeredLines(15,
                "Documents are empty right now,",
                "click Add new to start"));
        return centered;
    }

    /* ------------------------------------------------------------------
     * Right panel : mood analysis + assistant
     * ------------------------------------------------------------------ */
    private static JPanel buildRightPanel() {
        JPanel right = new JPanel(new BorderLayout());
        right.setBackground(PANEL_DARK);
        scaledPadding(right, 62, 40, 40, 40);

        JLabel title = new JLabel("Mood Analysis");
        title.setForeground(Color.WHITE);
        scaledFont(title, 32);

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.add(title, BorderLayout.WEST);
        scaledPadding(titleRow, 0, 4, 26, 0);
        right.add(titleRow, BorderLayout.NORTH);

        JPanel cards = new JPanel(new BorderLayout());
        cards.setOpaque(false);
        scaledVgap(cards, 22);
        cards.add(buildMoodCard(), BorderLayout.NORTH);
        cards.add(buildChatCard(), BorderLayout.CENTER);

        right.add(cards, BorderLayout.CENTER);
        return right;
    }

    private static JComponent buildMoodCard() {
        RoundedPanel card = new RoundedPanel(26, null);
        card.setBackground(CARD_BG);
        card.setLayout(new GridBagLayout());
        card.setDesignHeight(210);

        JPanel stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));

        PlusIcon plus = new PlusIcon(46);
        plus.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel caption = centeredLines(14,
                "Add Documents to start mood",
                "analysis.");
        caption.setAlignmentX(Component.CENTER_ALIGNMENT);

        stack.add(plus);
        stack.add(new Gap(18, true));
        stack.add(caption);

        card.add(stack);
        return card;
    }

    private static JComponent buildChatCard() {
        RoundedPanel card = new RoundedPanel(26, null);
        card.setBackground(CARD_BG);
        card.setLayout(new BorderLayout());
        scaledVgap(card, 14);
        scaledPadding(card, 22, 22, 18, 22);

        card.add(buildMessageArea(), BorderLayout.CENTER);
        card.add(buildInputRow(), BorderLayout.SOUTH);
        return card;
    }

    private static JScrollPane buildMessageArea() {
        final ScrollableColumn messages = new ScrollableColumn();
        messages.setLayout(new BoxLayout(messages, BoxLayout.Y_AXIS));
        messages.add(buildAssistantMessage());

        JScrollPane scroll = new JScrollPane(messages);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.setViewportBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        final JScrollBar bar = scroll.getVerticalScrollBar();
        bar.setUI(new ThinScrollBarUI());
        bar.setOpaque(false);
        onScale(() -> {
            bar.setPreferredSize(new Dimension(s(6), 0));
            bar.setUnitIncrement(s(18));
        });

        // wrapped text needs a second pass once the real width is known
        scroll.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                messages.revalidate();
            }
        });
        return scroll;
    }

    private static JComponent buildInputRow() {
        RoundedPanel inputRow = new RoundedPanel(18, FIELD_BORDER);
        inputRow.setBackground(CARD_BG);
        inputRow.setLayout(new BorderLayout());
        scaledHgap(inputRow, 10);
        scaledPadding(inputRow, 12, 16, 12, 12);

        PlaceholderField input = new PlaceholderField("Type your message ...");
        scaledFont(input, 14);

        inputRow.add(input, BorderLayout.CENTER);
        inputRow.add(new SendButton(), BorderLayout.EAST);
        return inputRow;
    }

    private static JComponent buildAssistantMessage() {
        MessageBlock block = new MessageBlock();
        block.setLayout(new BorderLayout());
        block.setOpaque(false);
        scaledVgap(block, 12);
        scaledPadding(block, 0, 0, 20, 0);

        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.X_AXIS));
        head.add(new Avatar("AI", ICON_COLOR, 46));
        head.add(new Gap(16, false));

        JLabel who = new JLabel("Assistant here!");
        who.setForeground(TEXT_DARK);
        scaledFont(who, 15);
        head.add(who);
        head.add(Box.createHorizontalGlue());

        JTextArea text = new JTextArea(
                "Hi, I'm your AI Assistant. I can help you to analyze your notes "
              + "and be a second brain of yours in problem solving journey!");
        text.setForeground(TEXT_DARK);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setEditable(false);
        text.setFocusable(false);
        text.setOpaque(false);
        scaledFont(text, 14);
        scaledPadding(text, 0, 16, 0, 6);

        block.add(head, BorderLayout.NORTH);
        block.add(text, BorderLayout.CENTER);
        return block;
    }

    /** Vertical stack of centred muted lines, used by both empty states. */
    private static JPanel centeredLines(double designSize, String... lines) {
        JPanel stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));

        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                Gap gap = new Gap(4, true);
                gap.setAlignmentX(Component.CENTER_ALIGNMENT);
                stack.add(gap);
            }
            JLabel line = new JLabel(lines[i]);
            line.setForeground(TEXT_MUTED);
            line.setAlignmentX(Component.CENTER_ALIGNMENT);
            scaledFont(line, designSize);
            stack.add(line);
        }
        return stack;
    }

    /* ------------------------------------------------------------------
     * Fonts and painting helpers
     * ------------------------------------------------------------------ */
    private static Font font(float size) {
        if (baseFont == null) {
            baseFont = loadBaseFont();
        }
        return baseFont.deriveFont(size);
    }

    private static Font loadBaseFont() {
        try (InputStream in = App.class.getResourceAsStream(FONT_PATH)) {
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

    private static Graphics2D smooth(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return g2;
    }

    /* ------------------------------------------------------------------
     * Custom components
     *
     * None of these register themselves with the scale registry; they either
     * read the scale at paint / layout time or are styled by their builder.
     * ------------------------------------------------------------------ */

    /** Scale-aware spacer, the equivalent of Box.createStrut for this layout. */
    private static class Gap extends JComponent {
        private final double design;
        private final boolean vertical;

        Gap(double design, boolean vertical) {
            this.design = design;
            this.vertical = vertical;
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredSize() {
            return vertical ? new Dimension(0, s(design)) : new Dimension(s(design), 0);
        }

        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        @Override
        public Dimension getMaximumSize() {
            return vertical
                    ? new Dimension(Short.MAX_VALUE, s(design))
                    : new Dimension(s(design), Short.MAX_VALUE);
        }
    }

    /** White card with rounded corners and an optional hairline border. */
    private static class RoundedPanel extends JPanel {
        private final double designArc;
        private final transient Color borderColor;
        private double designHeight = -1;

        RoundedPanel(double designArc, Color borderColor) {
            this.designArc = designArc;
            this.borderColor = borderColor;
            setOpaque(false);
        }

        void setDesignHeight(double designHeight) {
            this.designHeight = designHeight;
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension pref = super.getPreferredSize();
            return designHeight > 0 ? new Dimension(pref.width, s(designHeight)) : pref;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            int arc = s(designArc);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            if (borderColor != null) {
                g2.setColor(borderColor);
                g2.setStroke(new BasicStroke(Math.max(1f, sf(1.0))));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Small dark pill button used for "Add New". Font and padding come from the builder. */
    private static class PillButton extends JButton {
        PillButton(String text) {
            super(text);
            setForeground(Color.WHITE);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            Color fill = PANEL_DARK;
            if (getModel().isPressed()) {
                fill = fill.darker();
            } else if (getModel().isRollover()) {
                fill = PANEL_HOVER;
            }
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Circular avatar with initials. */
    private static class Avatar extends JComponent {
        private final String initials;
        private final transient Color background;
        private final double designDiameter;

        Avatar(String initials, Color background, double designDiameter) {
            this.initials = initials;
            this.background = background;
            this.designDiameter = designDiameter;
        }

        @Override
        public Dimension getPreferredSize() {
            int d = s(designDiameter);
            return new Dimension(d, d);
        }

        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            int d = Math.min(getWidth(), getHeight());
            g2.setColor(background);
            g2.fillOval(0, 0, d, d);

            g2.setColor(Color.WHITE);
            g2.setFont(font(sf(13)));
            FontMetrics fm = g2.getFontMetrics();
            int x = (d - fm.stringWidth(initials)) / 2;
            int y = (d - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(initials, x, y);
            g2.dispose();
        }
    }

    /** Thin "+" glyph for the empty mood card. */
    private static class PlusIcon extends JComponent {
        private final double designSize;

        PlusIcon(double designSize) {
            this.designSize = designSize;
        }

        @Override
        public Dimension getPreferredSize() {
            int d = s(designSize);
            return new Dimension(d, d);
        }

        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            g2.setColor(ICON_COLOR);
            g2.setStroke(new BasicStroke(Math.max(1.2f, sf(2.2)),
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int size = Math.min(getWidth(), getHeight());
            int pad = Math.round(size * 0.13f);
            int mid = size / 2;
            g2.drawLine(pad, mid, size - pad, mid);
            g2.drawLine(mid, pad, mid, size - pad);
            g2.dispose();
        }
    }

    /** Outlined triangle send glyph. */
    private static class SendButton extends JButton {
        SendButton() {
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        public Dimension getPreferredSize() {
            int d = s(30);
            return new Dimension(d, d);
        }

        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            g2.setColor(getModel().isRollover() ? PANEL_DARK : ICON_COLOR);
            g2.setStroke(new BasicStroke(Math.max(1f, sf(1.6)),
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            float w = getWidth();
            float h = getHeight();
            Path2D.Float arrow = new Path2D.Float();
            arrow.moveTo(w * 0.30f, h * 0.23f);
            arrow.lineTo(w * 0.74f, h * 0.50f);
            arrow.lineTo(w * 0.30f, h * 0.77f);
            arrow.closePath();
            g2.draw(arrow);
            g2.dispose();
        }
    }

    /** Borderless text field that paints its own placeholder. Font comes from the builder. */
    private static class PlaceholderField extends JTextField {
        private final String placeholder;

        PlaceholderField(String placeholder) {
            this.placeholder = placeholder;
            setForeground(TEXT_DARK);
            setCaretColor(TEXT_DARK);
            setOpaque(false);
            setBorder(null);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (!getText().isEmpty()) {
                return;
            }
            Graphics2D g2 = smooth(g);
            g2.setColor(TEXT_MUTED);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(placeholder, 2, y);
            g2.dispose();
        }
    }

    /** Message wrapper that never grows taller than its content inside a BoxLayout. */
    private static class MessageBlock extends JPanel {
        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }
    }

    /**
     * Vertical message column that follows the viewport width so text wraps correctly.
     * Its BoxLayout is installed by the builder, not by this constructor.
     */
    private static class ScrollableColumn extends JPanel implements Scrollable {
        ScrollableColumn() {
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return s(18);
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return s(72);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    /** Minimal scrollbar: no arrows, no track, rounded thumb. */
    private static class ThinScrollBarUI extends BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            thumbColor = SCROLL_THUMB;
            trackColor = CARD_BG;
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return hiddenButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return hiddenButton();
        }

        private JButton hiddenButton() {
            JButton button = new JButton();
            Dimension zero = new Dimension(0, 0);
            button.setPreferredSize(zero);
            button.setMinimumSize(zero);
            button.setMaximumSize(zero);
            return button;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
            // intentionally empty
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
            if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) {
                return;
            }
            Graphics2D g2 = smooth(g);
            g2.setColor(SCROLL_THUMB);
            g2.fillRoundRect(thumbBounds.x, thumbBounds.y + 2,
                    thumbBounds.width, thumbBounds.height - 4,
                    thumbBounds.width, thumbBounds.width);
            g2.dispose();
        }
    }
}