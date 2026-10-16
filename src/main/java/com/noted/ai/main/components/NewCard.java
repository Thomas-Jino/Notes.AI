// components/NewCard.java
package com.noted.ai.main.components;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

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
    private static final int ACTION_WIDTH = 112;
    private static final int ACTION_HEIGHT = 38;
    

    private final Listener listener;
    private final CardLayout nameSwap = new CardLayout();
    private final JPanel nameHolder = new JPanel(nameSwap);
    private final JPanel actionPanel = createActionsPanel();
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
        setBorder(BorderFactory.createEmptyBorder(Scale.s(16), Scale.s(18), Scale.s(16), Scale.s(16)));

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

    public void resetToLabelView() {
        nameSwap.show(nameHolder, VIEW_LABEL);
        nameHolder.revalidate();
        nameHolder.repaint();
        NewCard.this.revalidate();
        NewCard.this.repaint();
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

        JPanel labelRow = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, Scale.s(12), 0));
        labelRow.setOpaque(false);
        labelRow.add(nameLabel);

        JPanel viewLabel = new JPanel(new GridBagLayout());
        viewLabel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, Scale.s(6), 0);
        viewLabel.add(labelRow, gbc);

        JPanel viewEdit = new JPanel(new GridBagLayout());
        viewEdit.setOpaque(false);
        GridBagConstraints gbc2 = new GridBagConstraints();
        gbc2.gridx = 0; gbc2.gridy = 0; gbc2.weightx = 1.0; gbc2.fill = GridBagConstraints.HORIZONTAL;
        gbc2.insets = new Insets(0, 0, Scale.s(6), 0);
        viewEdit.add(nameField, gbc2);

        nameHolder.setOpaque(false);
        nameHolder.add(viewLabel, VIEW_LABEL);
        nameHolder.add(viewEdit, VIEW_EDIT);
        resetToLabelView();

        add(nameHolder, BorderLayout.CENTER);
        add(actionPanel, BorderLayout.SOUTH);
    }

    private JPanel createActionsPanel() {
        JPanel a = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, Scale.s(12), 0));
        a.setOpaque(false);
        PillButton editBtn = new PillButton("Edit");
        editBtn.addActionListener(e -> beginRename());
        PillButton openBtn = new PillButton("Open");
        openBtn.addActionListener(e -> { if (listener != null) listener.onOpen(this); });
        PillButton deleteBtn = new PillButton("Delete");
        deleteBtn.addActionListener(e -> { if (listener != null) listener.onDelete(this); });
        a.add(editBtn);
        a.add(openBtn);
        a.add(deleteBtn);
        a.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, Scale.s(ACTION_HEIGHT) + Scale.s(16)));
        return a;
    }

    private void beginRename() {
        nameField.setText(documentName);
        nameSwap.show(nameHolder, VIEW_EDIT);
        nameField.requestFocusInWindow();
        nameField.selectAll();
        nameHolder.revalidate();
        nameHolder.repaint();
        NewCard.this.revalidate();
        NewCard.this.repaint();
    }

    private void commitRename() {
        String candidate = nameField.getText().trim();
        nameSwap.show(nameHolder, VIEW_LABEL);

        if (candidate.isEmpty() || candidate.equals(documentName)) {
            nameHolder.revalidate();
            nameHolder.repaint();
            NewCard.this.revalidate();
            NewCard.this.repaint();
            return;
        }
        documentName = candidate;
        nameLabel.setText(documentName);
        if (listener != null) {
            listener.onRename(this, documentName);
        }
        nameHolder.revalidate();
        nameHolder.repaint();
        NewCard.this.revalidate();
        NewCard.this.repaint();
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
            setHorizontalAlignment(SwingConstants.CENTER);

                Scale.font(this, 13);

                Scale.onScale(() -> {
                    int width = Scale.s(ACTION_WIDTH);
                    int height = Scale.s(ACTION_HEIGHT);
                    setPreferredSize(new java.awt.Dimension(Math.min(width, Scale.s(90)), height));
                    setMinimumSize(new java.awt.Dimension(Math.min(width, Scale.s(70)), height));
                setBorder(BorderFactory.createEmptyBorder(
                    Scale.s(8),
                    Scale.s(18),
                    Scale.s(8),
                    Scale.s(18)));
                });
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