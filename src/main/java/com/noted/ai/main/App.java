// App.java
package com.noted.ai.main;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.Path2D;
import java.util.function.Consumer;

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
 * Noted.AI - main window.
 *
 * This class is presentation only: layout, painting, state switching that is
 * purely visual (empty vs. populated, collapsed vs. expanded), and resize
 * behaviour. It holds no document data and makes no decisions about what a
 * click means - it only reports that the click happened, through the small
 * callback API below. Main.java owns every one of those decisions.
 *
 * Build it with {@link #createAndShow()}, not a constructor - App has no
 * public constructor and no main() of its own.
 */
public final class App {

    private static final double SPLIT_RATIO = 0.46;

    private final JFrame frame;
    private final JButton addNewButton;
    private final DocumentsPanel documentsPanel;
    private final ChatPanel chatPanel;

    private App() {
        installLookAndFeel();

        frame = new JFrame("Noted.AI");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(700, 540));
        frame.setSize(980, 760);
        frame.setLocationRelativeTo(null);

        documentsPanel = new DocumentsPanel();
        chatPanel = new ChatPanel();

        addNewButton = new PillButton("Add New");

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
                Scale.recompute(split.getWidth(), split.getHeight());
                split.setDividerLocation(SPLIT_RATIO);
                split.revalidate();
                split.repaint();
            }
        });

        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);

        SwingUtilities.invokeLater(() -> {
            Scale.recompute(split.getWidth(), split.getHeight());
            split.setDividerLocation(SPLIT_RATIO);
        });
    }

    private void configureUI() {
        Scale.font(addNewButton, 13);
        Scale.padding(addNewButton, 8, 18, 8, 18);
        documentsPanel.installScaleHooks();
        documentsPanel.initializeState();
        chatPanel.installScaleHooks();
        Scale.recompute(frame.getWidth(), frame.getHeight());
    }

    /** Builds the window and shows it. This is the only way to obtain an App. */
    public static App createAndShow() {
        App app = new App();
        app.configureUI();
        app.frame.setVisible(true);
        return app;
    }

    /* ------------------------------------------------------------------
     * Public API for Main.java - every method below either registers a
     * callback for something the user did, or renders something Main told
     * it to render. Nothing here decides what a document or a message is.
     * ------------------------------------------------------------------ */

    public void setOnAddNewDocument(Runnable handler) {
        addNewButton.addActionListener(e -> handler.run());
    }

    public void setOnSendMessage(Consumer<String> handler) {
        chatPanel.setOnSend(handler);
    }

    public void addDocumentCard(JComponent card) {
        documentsPanel.addCard(card);
    }

    public void removeDocumentCard(JComponent card) {
        documentsPanel.removeCard(card);
    }

    public void clearDocumentCards() {
        documentsPanel.clear();
    }

    public int getDocumentCardCount() {
        return documentsPanel.getCardCount();
    }

    public void appendUserMessage(String text) {
        chatPanel.appendMessage("You", text, true);
    }

    public void appendAssistantMessage(String text) {
        chatPanel.appendMessage("Assistant", text, false);
    }

    /* ------------------------------------------------------------------
     * Look and feel
     * ------------------------------------------------------------------ */
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
    private JPanel buildLeftPanel() {
        JPanel left = new JPanel(new BorderLayout());
        left.setBackground(Theme.CARD_BG);
        Scale.padding(left, 80, 45, 40, 45);

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel appName = new JLabel("Noted.AI");
        appName.setForeground(Theme.TEXT_DARK);
        appName.setAlignmentX(Component.LEFT_ALIGNMENT);
        Scale.font(appName, 34);

        JPanel docRow = new JPanel(new BorderLayout());
        docRow.setOpaque(false);
        docRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel documentsHeading = new JLabel("Documents");
        documentsHeading.setForeground(Theme.TEXT_DARK);
        Scale.font(documentsHeading, 34);
        docRow.add(documentsHeading, BorderLayout.WEST);

        JPanel buttonHolder = new JPanel(new GridBagLayout()); // vertically centres the pill
        buttonHolder.setOpaque(false);
        buttonHolder.add(addNewButton);
        docRow.add(buttonHolder, BorderLayout.EAST);

        header.add(appName);
        header.add(new Gap(46, true));
        header.add(docRow);

        left.add(header, BorderLayout.NORTH);
        left.add(documentsPanel, BorderLayout.CENTER);
        return left;
    }

    /* ------------------------------------------------------------------
     * Right panel : mood analysis + assistant
     * ------------------------------------------------------------------ */
    private JPanel buildRightPanel() {
        JPanel right = new JPanel(new BorderLayout());
        right.setBackground(Theme.PANEL_DARK);
        Scale.padding(right, 62, 40, 40, 40);

        JLabel title = new JLabel("Mood Analysis");
        title.setForeground(Color.WHITE);
        Scale.font(title, 32);

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.add(title, BorderLayout.WEST);
        Scale.padding(titleRow, 0, 4, 26, 0);
        right.add(titleRow, BorderLayout.NORTH);

        JPanel cards = new JPanel(new BorderLayout());
        cards.setOpaque(false);
        Scale.vgap(cards, 22);
        cards.add(buildMoodCard(), BorderLayout.NORTH);
        cards.add(chatPanel, BorderLayout.CENTER);

        right.add(cards, BorderLayout.CENTER);
        return right;
    }

    private JComponent buildMoodCard() {
        RoundedPanel card = new RoundedPanel(26, null);
        card.setBackground(Theme.CARD_BG);
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
            line.setForeground(Theme.TEXT_MUTED);
            line.setAlignmentX(Component.CENTER_ALIGNMENT);
            Scale.font(line, designSize);
            stack.add(line);
        }
        return stack;
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
     * ------------------------------------------------------------------ */

    /** Scale-aware spacer, the equivalent of Box.createStrut for this layout. */
    private static final class Gap extends JComponent {
        private final double design;
        private final boolean vertical;

        Gap(double design, boolean vertical) {
            this.design = design;
            this.vertical = vertical;
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredSize() {
            return vertical ? new Dimension(0, Scale.s(design)) : new Dimension(Scale.s(design), 0);
        }

        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        @Override
        public Dimension getMaximumSize() {
            return vertical
                    ? new Dimension(Short.MAX_VALUE, Scale.s(design))
                    : new Dimension(Scale.s(design), Short.MAX_VALUE);
        }
    }

    /** White card with rounded corners and an optional hairline border. Not final - ChatPanel extends it. */
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
            return designHeight > 0 ? new Dimension(pref.width, Scale.s(designHeight)) : pref;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            int arc = Scale.s(designArc);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            if (borderColor != null) {
                g2.setColor(borderColor);
                g2.setStroke(new BasicStroke(Math.max(1f, Scale.sf(1.0))));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Small dark pill button used for "Add New". */
    private static final class PillButton extends JButton {
        PillButton(String text) {
            super(text);
            setForeground(Color.WHITE);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFont(Theme.font(13f));
            setBorder(BorderFactory.createEmptyBorder(Scale.s(8), Scale.s(18), Scale.s(8), Scale.s(18)));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            Color fill = Theme.PANEL_DARK;
            if (getModel().isPressed()) {
                fill = fill.darker();
            } else if (getModel().isRollover()) {
                fill = Theme.PANEL_HOVER;
            }
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Circular avatar with initials. */
    private static final class Avatar extends JComponent {
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
            int d = Scale.s(designDiameter);
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
            g2.setFont(Theme.font(Scale.sf(13)));
            FontMetrics fm = g2.getFontMetrics();
            int x = (d - fm.stringWidth(initials)) / 2;
            int y = (d - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(initials, x, y);
            g2.dispose();
        }
    }

    /** Thin "+" glyph for the empty mood card. */
    private static final class PlusIcon extends JComponent {
        private final double designSize;

        PlusIcon(double designSize) {
            this.designSize = designSize;
        }

        @Override
        public Dimension getPreferredSize() {
            int d = Scale.s(designSize);
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
            g2.setColor(Theme.ICON_COLOR);
            g2.setStroke(new BasicStroke(Math.max(1.2f, Scale.sf(2.2)),
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
    private static final class SendButton extends JButton {
        SendButton() {
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        public Dimension getPreferredSize() {
            int d = Scale.s(30);
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
            g2.setColor(getModel().isRollover() ? Theme.PANEL_DARK : Theme.ICON_COLOR);
            g2.setStroke(new BasicStroke(Math.max(1f, Scale.sf(1.6)),
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

    /** Borderless text field that paints its own placeholder. */
    private static final class PlaceholderField extends JTextField {
        private final String placeholder;

        PlaceholderField(String placeholder) {
            this.placeholder = placeholder;
            setForeground(Theme.TEXT_DARK);
            setCaretColor(Theme.TEXT_DARK);
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
            g2.setColor(Theme.TEXT_MUTED);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(placeholder, 2, y);
            g2.dispose();
        }
    }

    /** Message wrapper that never grows taller than its content inside a BoxLayout. */
    private static final class MessageBlock extends JPanel {
        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }
    }

    /** Vertical column that follows the viewport width; used by both the chat and the documents list. */
    private static final class ScrollableColumn extends JPanel implements Scrollable {
        ScrollableColumn() {
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Scale.s(18);
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Scale.s(72);
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
    private static final class ThinScrollBarUI extends BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            thumbColor = Theme.SCROLL_THUMB;
            trackColor = Theme.CARD_BG;
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
            g2.setColor(Theme.SCROLL_THUMB);
            g2.fillRoundRect(thumbBounds.x, thumbBounds.y + 2,
                    thumbBounds.width, thumbBounds.height - 4,
                    thumbBounds.width, thumbBounds.width);
            g2.dispose();
        }
    }

    /** Small chevron toggle, pointing down when collapsed and up when expanded. */
    private static final class ChevronButton extends JButton {
        private boolean expanded;

        ChevronButton() {
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        void setExpanded(boolean expanded) {
            this.expanded = expanded;
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            int d = Scale.s(22);
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
            g2.setColor(getModel().isRollover() ? Theme.PANEL_DARK : Theme.TEXT_MUTED);
            g2.setStroke(new BasicStroke(Math.max(1.2f, Scale.sf(1.8)),
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            float w = getWidth();
            float h = getHeight();
            float midX = w / 2f;

            Path2D.Float chevron = new Path2D.Float();
            if (expanded) {
                chevron.moveTo(w * 0.22f, h * 0.62f);
                chevron.lineTo(midX, h * 0.36f);
                chevron.lineTo(w * 0.78f, h * 0.62f);
            } else {
                chevron.moveTo(w * 0.22f, h * 0.38f);
                chevron.lineTo(midX, h * 0.64f);
                chevron.lineTo(w * 0.78f, h * 0.38f);
            }
            g2.draw(chevron);
            g2.dispose();
        }
    }

    /**
     * Left-hand documents area. Shows a muted placeholder while there are no
     * cards, then switches to a scrollable list once any are added - entirely
     * on its own. Main only ever calls addCard / removeCard / clear.
     */
    private static final class DocumentsPanel extends JPanel {

        private static final String EMPTY_CARD = "empty";
        private static final String LIST_CARD  = "list";
        private static final int COMPACT_ROWS = 3;
        private static final double ROW_HEIGHT = 76;
        private static final double EXPANDED_HEIGHT = 420;

        private final CardLayout layout = new CardLayout();
        private final ScrollableColumn cardColumn = new ScrollableColumn();
        private final JScrollPane scrollPane;
        private final ChevronButton chevron = new ChevronButton();
        private boolean expanded;

        DocumentsPanel() {
            setLayout(layout);
            setOpaque(false);

            cardColumn.setLayout(new BoxLayout(cardColumn, BoxLayout.Y_AXIS));

            scrollPane = new JScrollPane(cardColumn);
            scrollPane.setOpaque(false);
            scrollPane.getViewport().setOpaque(false);
            scrollPane.setBorder(null);
            scrollPane.setViewportBorder(null);
            scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

            JScrollBar bar = scrollPane.getVerticalScrollBar();
            bar.setUI(new ThinScrollBarUI());
            bar.setOpaque(false);
            Scale.onScale(() -> {
                bar.setPreferredSize(new Dimension(Scale.s(6), 0));
                bar.setUnitIncrement(Scale.s(18));
            });

            chevron.setVisible(false);
            chevron.addActionListener(e -> {
                expanded = !expanded;
                chevron.setExpanded(expanded);
                updateScrollHeight();
            });

            JPanel chevronRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            chevronRow.setOpaque(false);
            chevronRow.add(chevron);
            Scale.padding(chevronRow, 8, 0, 0, 0);

            JPanel listPanel = new JPanel(new BorderLayout());
            listPanel.setOpaque(false);
            listPanel.add(scrollPane, BorderLayout.CENTER);
            listPanel.add(chevronRow, BorderLayout.SOUTH);

            add(buildEmptyState(), EMPTY_CARD);
            add(listPanel, LIST_CARD);
        }

        private void installScaleHooks() {
            Scale.onScale(this::updateScrollHeight);
        }

        private void initializeState() {
            layout.show(this, EMPTY_CARD);
        }

        private JPanel buildEmptyState() {
            JPanel centered = new JPanel(new GridBagLayout());
            centered.setOpaque(false);
            centered.add(centeredLines(15,
                    "Documents are empty right now,",
                    "click Add new to start"));
            return centered;
        }

        void addCard(JComponent card) {
            if (cardColumn.getComponentCount() > 0) {
                cardColumn.add(new Gap(12, true));
            }
            cardColumn.add(card);
            refresh();
        }

        void removeCard(JComponent card) {
            int index = indexOf(card);
            if (index < 0) {
                return;
            }
            cardColumn.remove(index);
            if (index < cardColumn.getComponentCount() && cardColumn.getComponent(index) instanceof Gap) {
                cardColumn.remove(index);
            } else if (index > 0 && cardColumn.getComponent(index - 1) instanceof Gap) {
                cardColumn.remove(index - 1);
            }
            refresh();
        }

        void clear() {
            cardColumn.removeAll();
            refresh();
        }

        int getCardCount() {
            int total = 0;
            for (Component child : cardColumn.getComponents()) {
                if (!(child instanceof Gap)) {
                    total++;
                }
            }
            return total;
        }

        private int indexOf(Component target) {
            Component[] children = cardColumn.getComponents();
            for (int i = 0; i < children.length; i++) {
                if (children[i] == target) {
                    return i;
                }
            }
            return -1;
        }

        private void refresh() {
            int count = getCardCount();
            chevron.setVisible(count > COMPACT_ROWS);
            if (count == 0) {
                expanded = false;
                chevron.setExpanded(false);
            }
            updateScrollHeight();
            layout.show(this, count == 0 ? EMPTY_CARD : LIST_CARD);
            revalidate();
            repaint();
        }

        private void updateScrollHeight() {
            int visibleRows = Math.max(1, Math.min(getCardCount(), COMPACT_ROWS));
            double compact = visibleRows * ROW_HEIGHT;
            double target = expanded ? EXPANDED_HEIGHT : compact;
            scrollPane.setPreferredSize(new Dimension(10, Scale.s(target)));
        }
    }

    /**
     * Right-hand assistant card: message history + input row. Reports typed
     * text through setOnSend; appendMessage only ever renders what it's told.
     */
    private static final class ChatPanel extends RoundedPanel {

        private static final String GREETING =
                "Hi, I'm your AI Assistant. I can help you to analyze your notes "
              + "and be a second brain of yours in problem solving journey!";

        private final ScrollableColumn messages = new ScrollableColumn();
        private final JScrollPane scrollPane;
        private final PlaceholderField input;
        private Consumer<String> onSend;

        ChatPanel() {
            super(26, null);
            setBackground(Theme.CARD_BG);
            setLayout(new BorderLayout());

            messages.setLayout(new BoxLayout(messages, BoxLayout.Y_AXIS));
            messages.add(buildMessageBlock("Assistant here!", GREETING, Theme.ICON_COLOR, "AI"));

            scrollPane = new JScrollPane(messages);
            scrollPane.setOpaque(false);
            scrollPane.getViewport().setOpaque(false);
            scrollPane.setBorder(null);
            scrollPane.setViewportBorder(null);
            scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

            JScrollBar bar = scrollPane.getVerticalScrollBar();
            bar.setUI(new ThinScrollBarUI());
            bar.setOpaque(false);
            Scale.onScale(() -> {
                bar.setPreferredSize(new Dimension(Scale.s(6), 0));
                bar.setUnitIncrement(Scale.s(18));
            });

            scrollPane.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    messages.revalidate();
                }
            });

            RoundedPanel inputRow = new RoundedPanel(18, Theme.FIELD_BORDER);
            inputRow.setBackground(Theme.CARD_BG);
            inputRow.setLayout(new BorderLayout());
            Scale.hgap(inputRow, 10);
            Scale.padding(inputRow, 12, 16, 12, 12);

            input = new PlaceholderField("Type your message ...");
            Scale.font(input, 14);

            SendButton send = new SendButton();
            send.addActionListener(e -> submitInput());
            input.addActionListener(e -> submitInput());

            inputRow.add(input, BorderLayout.CENTER);
            inputRow.add(send, BorderLayout.EAST);

            add(scrollPane, BorderLayout.CENTER);
            add(inputRow, BorderLayout.SOUTH);
        }

        private void installScaleHooks() {
            Scale.vgap(this, 14);
            Scale.padding(this, 22, 22, 18, 22);
        }

        void setOnSend(Consumer<String> handler) {
            this.onSend = handler;
        }

        void appendMessage(String name, String text, boolean isUser) {
            Color avatarColor = isUser ? Theme.USER_AVATAR : Theme.ICON_COLOR;
            String initials = isUser ? "You" : "AI";
            messages.add(buildMessageBlock(name, text, avatarColor, initials));
            messages.revalidate();
            messages.repaint();
            scrollToBottom();
        }

        private void submitInput() {
            String text = input.getText().trim();
            if (text.isEmpty() || onSend == null) {
                return;
            }
            input.setText("");
            onSend.accept(text);
        }

        private void scrollToBottom() {
            SwingUtilities.invokeLater(() -> {
                JScrollBar bar = scrollPane.getVerticalScrollBar();
                bar.setValue(bar.getMaximum());
            });
        }

        private static JComponent buildMessageBlock(String name, String body, Color avatarColor, String initials) {
            MessageBlock block = new MessageBlock();
            block.setLayout(new BorderLayout());
            block.setOpaque(false);
            Scale.vgap(block, 12);
            Scale.padding(block, 0, 0, 20, 0);

            JPanel head = new JPanel();
            head.setOpaque(false);
            head.setLayout(new BoxLayout(head, BoxLayout.X_AXIS));
            head.add(new Avatar(initials, avatarColor, 46));
            head.add(new Gap(16, false));

            JLabel who = new JLabel(name);
            who.setForeground(Theme.TEXT_DARK);
            Scale.font(who, 15);
            head.add(who);
            head.add(Box.createHorizontalGlue());

            JTextArea text = new JTextArea(body);
            text.setForeground(Theme.TEXT_DARK);
            text.setLineWrap(true);
            text.setWrapStyleWord(true);
            text.setEditable(false);
            text.setFocusable(false);
            text.setOpaque(false);
            Scale.font(text, 14);
            Scale.padding(text, 0, 16, 0, 6);

            block.add(head, BorderLayout.NORTH);
            block.add(text, BorderLayout.CENTER);
            return block;
        }
    }
}