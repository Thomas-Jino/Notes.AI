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
        view.setOnSaveDocument(this::handleSaveDocument);
        view.setOnCancelDocument(view::showDocuments);
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
        view.showDocumentEditor(source.getDocumentName(), "");
    }

    private void handleRenameDocument(NewCard source, String newName) {
        System.out.println("Renamed document to: " + newName);
    }

    private void handleSaveDocument(App.DocumentSave document) {
        try {
            AI.EmotionAnalysis analysis = AI.analyzeEmotions(document.getContent());
            int documentId = Database.saveDocument(document.getTitle(), document.getContent());
            Database.saveEmotion(documentId, analysis.getEmotion(), analysis.getIntensity());

            System.out.println("Saved document: " + document.getTitle());
            System.out.println("Emotion: " + analysis.getEmotion() + " | Intensity: " + analysis.getIntensity());
        } catch (Exception ex) {
            System.err.println("Unable to save document with emotion analysis: " + ex.getMessage());
            ex.printStackTrace();
        }

        view.showDocuments();
    }

    /* ------------------------------------------------------------------
     * Assistant chat
     * ------------------------------------------------------------------ */
    private void handleSendMessage(String text) {
        view.appendUserMessage(text);
        view.beginAssistantThinking();

        AI.streamMessage(
                text,
                token -> SwingUtilities.invokeLater(() -> view.appendAssistantChunk(token)),
                () -> SwingUtilities.invokeLater(() -> view.finishAssistantResponse()),
                error -> SwingUtilities.invokeLater(() -> {
                    view.finishAssistantResponse();
                    view.appendAssistantMessage("Sorry, I couldn't respond: " + error.getMessage());
                })
        );
    }
}