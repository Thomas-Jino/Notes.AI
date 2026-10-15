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
    private int activeDocumentId = -1;

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
        controller.loadExistingDocuments();
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
        NewCard card = new NewCard(name, -1, new NewCard.Listener() {
            @Override
            public void onOpen(NewCard source) {
                handleOpenDocument(source);
            }

            @Override
            public void onRename(NewCard source, String newName) {
                handleRenameDocument(source, newName);
            }

            @Override
            public void onDelete(NewCard source) {
                handleDeleteDocument(source);
            }
        });

        view.addDocumentCard(card);
    }

    private void handleOpenDocument(NewCard source) {
        try {
            Database.Document document = source.getDocumentId() > -1
                    ? Database.findDocumentById(source.getDocumentId())
                    : Database.findDocumentByTitle(source.getDocumentName());

            if (document != null) {
                activeDocumentId = document.getId();
                view.showDocumentEditor(document.getTitle(), document.getContent());
                return;
            }
        } catch (Exception ex) {
            System.err.println("Unable to load document content for '" + source.getDocumentName() + "': " + ex.getMessage());
        }

        activeDocumentId = -1;
        view.showDocumentEditor(source.getDocumentName(), "");
    }

    private void loadExistingDocuments() {
        try {
            view.clearDocumentCards();
            for (Database.Document document : Database.findAllDocuments()) {
                NewCard card = new NewCard(document.getTitle(), document.getId(), new NewCard.Listener() {
                    @Override
                    public void onOpen(NewCard source) {
                        handleOpenDocument(source);
                    }

                    @Override
                    public void onRename(NewCard source, String newName) {
                        handleRenameDocument(source, newName);
                    }

                    @Override
                    public void onDelete(NewCard source) {
                        handleDeleteDocument(source);
                    }
                });
                view.addDocumentCard(card);
            }
        } catch (Exception ex) {
            System.err.println("Unable to load existing documents: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void handleRenameDocument(NewCard source, String newName) {
        if (source.getDocumentId() <= -1) {
            return;
        }

        try {
            Database.Document current = Database.findDocumentById(source.getDocumentId());
            if (current == null) {
                return;
            }
            Database.updateDocument(source.getDocumentId(), newName, current.getContent());
        } catch (Exception ex) {
            System.err.println("Unable to rename document: " + ex.getMessage());
        }
    }

    private void handleDeleteDocument(NewCard source) {
        if (source.getDocumentId() > -1) {
            try {
                Database.deleteDocument(source.getDocumentId());
            } catch (Exception ex) {
                System.err.println("Unable to delete document: " + ex.getMessage());
                return;
            }
        }

        view.removeDocumentCard(source);
        if (activeDocumentId == source.getDocumentId()) {
            activeDocumentId = -1;
        }
        view.showDocuments();
    }

    private void handleSaveDocument(App.DocumentSave document) {
        try {
            AI.EmotionAnalysis analysis = AI.analyzeEmotions(document.getContent());

            int documentId = activeDocumentId;
            if (documentId > -1) {
                Database.updateDocument(documentId, document.getTitle(), document.getContent());
            } else {
                Database.Document existing = Database.findDocumentByTitle(document.getTitle());
                if (existing != null) {
                    documentId = existing.getId();
                    Database.updateDocument(documentId, document.getTitle(), document.getContent());
                } else {
                    documentId = Database.saveDocument(document.getTitle(), document.getContent());
                }
            }

            Database.saveEmotion(documentId, analysis.getEmotion(), analysis.getIntensity());
            activeDocumentId = documentId;

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