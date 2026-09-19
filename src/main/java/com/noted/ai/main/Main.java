// Main.java
package com.noted.ai.main;

import javax.swing.SwingUtilities;

import com.noted.ai.main.components.NewCard;

/**
 * Application entry point and controller.
 *
 * Main owns every decision App.java doesn't make for itself: what a new
 * document is named, what happens when it's opened or renamed, and what the
 * assistant replies. App only ever renders what Main tells it to render.
 */
public class Main {

    private final App view;
    private int nextDocumentNumber = 1;

    private Main(App view) {
        this.view = view;
        wireView();
    }

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        SwingUtilities.invokeLater(Main::start);
    }

    private static void start() {
        Main controller = new Main(App.createAndShow());
        if (controller.view == null) {
            throw new IllegalStateException("Main controller failed to initialize");
        }
    }

    private void wireView() {
        view.setOnAddNewDocument(this::handleAddNewDocument);
        view.setOnSendMessage(this::handleSendMessage);
    }

    /* ------------------------------------------------------------------
     * Documents
     * ------------------------------------------------------------------ */
    private void handleAddNewDocument() {
        String name = "Untitled Document " + nextDocumentNumber++;
        NewCard card = new NewCard(name, new NewCard.Listener() {
            @Override
            public void onOpen(NewCard source) {
                handleOpenDocument(source);
            }

            @Override
            public void onRename(NewCard source, String newName) {
                handleRenameDocument(source, newName);
            }
        });

        view.addDocumentCard(card);

        // TODO: create the row in sample.db via Database.java once that wiring exists.
    }

    private void handleOpenDocument(NewCard source) {
        // TODO: open the real document editor for source.getDocumentName().
        System.out.println("Open requested: " + source.getDocumentName());
    }

    private void handleRenameDocument(NewCard source, String newName) {
        // TODO: persist the rename via Database.java once that wiring exists.
        System.out.println("Renamed " + source.getDocumentName() + " to: " + newName);
    }

    /* ------------------------------------------------------------------
     * Assistant chat
     * ------------------------------------------------------------------ */
    private void handleSendMessage(String text) {
        view.appendUserMessage(text);

        // TODO: replace with a real call into AI.java / Ollama.
        view.appendAssistantMessage("I can't analyze anything yet - connect me to Ollama in AI.java!");
    }
}