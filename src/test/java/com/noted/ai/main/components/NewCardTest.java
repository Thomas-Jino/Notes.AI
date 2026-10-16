package com.noted.ai.main.components;

import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class NewCardTest {

    private static List<String> visibleButtonTexts(JComponent root) {
        List<String> texts = new ArrayList<>();
        for (java.awt.Component component : root.getComponents()) {
            if (component instanceof JButton button && button.isVisible()) {
                texts.add(button.getText());
            } else if (component instanceof JComponent nested) {
                if (nested.isVisible()) {
                    texts.addAll(visibleButtonTexts(nested));
                }
            }
        }
        return texts;
    }

    @Test
    void actionButtons_areOrderedEditOpenDelete() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            NewCard card = new NewCard("Example", null);
            List<String> texts = visibleButtonTexts(card);
            assertEquals(List.of("Edit", "Open", "Delete"), texts);
        });
    }

    @Test
    void resetToLabelView_keepsActionButtonsVisible() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            NewCard card = new NewCard("Example", null);
            card.resetToLabelView();
            List<String> texts = visibleButtonTexts(card);
            assertEquals(List.of("Edit", "Open", "Delete"), texts);
        });
    }

    @Test
    void afterClearingContainerAndAddingCard_buttonsStayVisible() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JPanel container = new JPanel();
            container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));

            NewCard first = new NewCard("First", null);
            container.add(first);
            container.removeAll();

            NewCard second = new NewCard("Second", null);
            container.add(second);

            List<String> texts = visibleButtonTexts(container);
            assertEquals(List.of("Edit", "Open", "Delete"), texts);
        });
    }

    @Test
    void repeatedDeleteSequence_keepsButtonsVisible() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JPanel container = new JPanel();
            container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
            List<NewCard> cards = new ArrayList<>();

            for (int i = 0; i < 4; i++) {
                NewCard card = new NewCard("Doc " + i, null);
                container.add(card);
                cards.add(card);
            }

            for (int i = 0; i < 3; i++) {
                container.remove(cards.get(i));
            }

            List<String> texts = visibleButtonTexts(container);
            assertEquals(List.of("Edit", "Open", "Delete"), texts);
        });
    }
}
