package com.noted.ai.main.components;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class NewCardTest {

    @Test
    void actionButtons_areOrderedEditOpenDelete() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            NewCard card = new NewCard("Example", null);
            JPanel actions = (JPanel) card.getComponent(1);

            List<String> texts = new ArrayList<>();
            for (java.awt.Component component : actions.getComponents()) {
                if (component instanceof JButton button) {
                    texts.add(button.getText());
                }
            }

            assertEquals(List.of("Edit", "Open", "Delete"), texts);
        });
    }
}
