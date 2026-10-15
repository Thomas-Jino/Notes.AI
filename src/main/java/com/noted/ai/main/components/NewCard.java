// components/NewCard.java
package com.noted.ai.main.components;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.noted.ai.main.Scale;
import com.noted.ai.main.Theme;

/**
 * One row in the documents list: a bordered card showing a document's name,
 * a pencil button that turns the name into an editable field, and an "Open"
 * button.
 *
 * Purely a view - it reports what the user did through Listener and never
 * decides what that action means. Main.java implements Listener.
 */
public class NewCard extends JPanel {

    /** Reported back to whoever built this card. */
    public interface Listener {
        void onOpen(NewCard source);
        void onRename(NewCard source, String newName);
        void onDelete(NewCard source);
    }

    private static final String VIEW_LABEL = "label";
    private static final String VIEW_EDIT  = "edit";
    private static final double DESIGN_ARC = 16;

    private final Listener listener;
    private final CardLayout nameSwap = new CardLayout();
    private final JPanel nameHolder = new JPanel(nameSwap);
    private final JLabel nameLabel = new JLabel();
    private final JTextField nameField = new JTextField();

    private String documentName;
    private int documentId = -1;

    public NewCard(String documentName, Listener listener) {
        this(documentName, -1, listener);
    }

    public NewCard(String documentName, int documentId, Listener listener) {
        super(new BorderLayout());
        this.documentName = documentName;
        this.documentId = documentId;
        this.listener = listener;

        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(Scale.s(14), Scale.s(18), Scale.s(14), Scale.s(14)));

        buildContent();
    }

    public String getDocumentName() {
        return documentName;
    }

    public int getDocumentId() {
        return documentId;
    }

    public void setDocumentId(int documentId) {
        this.documentId = documentId;
    }

    /* ------------------------------------------------------------------
     * Layout
     * ------------------------------------------------------------------ */
    private void buildContent() {
        nameLabel.setText(documentName);
        nameLabel.setForeground(Theme.TEXT_DARK);
        Scale.font(nameLabel, 15);

        nameField.setText(documentName);
        nameField.setBorder(BorderFactory.createEmptyBorder());
        Scale.font(nameField, 15);
        nameField.addActionListener(e -> commitRename());
        nameField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                commitRename();
            }
        });

        PillButton editButton = new PillButton("Edit");
        editButton.addActionListener(e -> beginRename());

        PillButton open = new PillButton("Open");
        open.addActionListener(e -> {
            if (listener != null) {
                listener.onOpen(this);
            }
        });

        PillButton deleteButton = new PillButton("Delete");
        deleteButton.addActionListener(e -> {
            if (listener != null) {
                listener.onDelete(this);
            }
        });

        JPanel labelRow = new JPanel();
        labelRow.setOpaque(false);
        labelRow.setLayout(new BoxLayout(labelRow, BoxLayout.X_AXIS));
        labelRow.add(nameLabel);

        nameHolder.setOpaque(false);
        nameHolder.add(labelRow, VIEW_LABEL);
        nameHolder.add(nameField, VIEW_EDIT);
        nameSwap.show(nameHolder, VIEW_LABEL);

        JPanel actions = new JPanel();
        actions.setOpaque(false);
        actions.setLayout(new BoxLayout(actions, BoxLayout.X_AXIS));
        actions.add(editButton);
        actions.add(Box.createHorizontalStrut(Scale.s(8)));
        actions.add(open);
        actions.add(Box.createHorizontalStrut(Scale.s(8)));
        actions.add(deleteButton);

        add(nameHolder, BorderLayout.CENTER);
        add(actions, BorderLayout.EAST);
    }

    private void beginRename() {
        nameField.setText(documentName);
        nameSwap.show(nameHolder, VIEW_EDIT);
        SwingUtilities.invokeLater(() -> {
            nameField.requestFocusInWindow();
            nameField.selectAll();
        });
    }

    private void commitRename() {
        String candidate = nameField.getText().trim();
        nameSwap.show(nameHolder, VIEW_LABEL);

        if (candidate.isEmpty() || candidate.equals(documentName)) {
            return;
        }
        documentName = candidate;
        nameLabel.setText(documentName);
        if (listener != null) {
            listener.onRename(this, documentName);
        }
    }

    /* ------------------------------------------------------------------
     * Painting
     * ------------------------------------------------------------------ */
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = smooth(g);
        int arc = Scale.s(DESIGN_ARC);
        g2.setColor(Theme.CARD_BG);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
        g2.setColor(Theme.FIELD_BORDER);
        g2.setStroke(new BasicStroke(Math.max(1f, Scale.sf(1.0))));
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
        g2.dispose();
        super.paintComponent(g);
    }

    private static Graphics2D smooth(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return g2;
    }

    /* ------------------------------------------------------------------
     * Small local widgets. App.java keeps its own copies of a card look and
     * a pill button so this file has no compile-time dependency on App.
     * ------------------------------------------------------------------ */

    /** Small dark pill button, matching the "Add New" button's look in App.java. */
    private static final class PillButton extends JButton {
        PillButton(String text) {
            super(text);
            setForeground(Color.WHITE);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            boolean isPrimaryAction = "Open".equals(text) || "Delete".equals(text);
            setFont(Theme.font(isPrimaryAction ? 16f : 13f));
            setBorder(BorderFactory.createEmptyBorder(
                    Scale.s(isPrimaryAction ? 10 : 7),
                    Scale.s(isPrimaryAction ? 24 : 18),
                    Scale.s(isPrimaryAction ? 10 : 7),
                    Scale.s(isPrimaryAction ? 24 : 18)));
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

}