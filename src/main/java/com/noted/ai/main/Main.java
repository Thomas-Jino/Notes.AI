// Main.java
package com.noted.ai.main;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public static final class NoteCreateRequest {
        private final String title;
        private final String content;

        public NoteCreateRequest(String title, String content) {
            this.title = title;
            this.content = content;
        }

        public String getTitle() {
            return title;
        }

        public String getContent() {
            return content;
        }
    }

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
        controller.view.refreshMoodSummary();
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
            view.refreshMoodSummary();
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
        view.refreshMoodSummary();
    }

    /* ------------------------------------------------------------------
     * Assistant chat
     * ------------------------------------------------------------------ */
    public static NoteCreateRequest parseCreateNoteRequest(String userText) {
        AI.CreateNoteIntent intent = AI.inferCreateNoteIntent(userText);
        if (intent != null) {
            return new NoteCreateRequest(cleanNoteValue(intent.getTitle()), cleanNoteValue(intent.getContent()));
        }

        return legacyParseCreateNoteRequest(userText);
    }

    private static NoteCreateRequest legacyParseCreateNoteRequest(String userText) {
        if (userText == null || userText.isBlank()) {
            return null;
        }

        String rawText = userText.trim();
        Matcher commandMatcher = Pattern.compile("(?is)^(?:can\\s+you\\s+|please\\s+)?(?:create|add|make|save)\\b")
                .matcher(rawText);
        if (!commandMatcher.find()) {
            return null;
        }

        String remainder = rawText.substring(commandMatcher.end()).trim();
        if (remainder.isBlank()) {
            return null;
        }

        String[] separators = {
                " with content:", " with text:", " and content:", " and text:",
                " with content ", " with text ", " and content ", " and text ",
                ":", " - "
        };

        int bestIndex = -1;
        String bestSeparator = null;
        String lowered = remainder.toLowerCase(Locale.ROOT);
        for (String separator : separators) {
            int index = lowered.indexOf(separator.toLowerCase(Locale.ROOT));
            if (index >= 0 && (bestIndex < 0 || index < bestIndex)) {
                bestIndex = index;
                bestSeparator = separator;
            }
        }

        if (bestIndex < 0 || bestSeparator == null) {
            return null;
        }

        String titleCandidate = remainder.substring(0, bestIndex).trim();
        String contentCandidate = remainder.substring(bestIndex + bestSeparator.length()).trim();
        if (titleCandidate.isBlank() || contentCandidate.isBlank()) {
            return null;
        }

        String title = titleCandidate;
        while (true) {
            String nextTitle = title.replaceFirst("(?is)^(?:a|an|new|note|document|called|named|titled)\\s+", "").trim();
            if (nextTitle.equals(title)) {
                break;
            }
            title = nextTitle;
        }

        title = title.replaceFirst("(?is)^[\"']([^\"']+)[\"']$", "$1").trim();
        title = title.replaceFirst("(?is)^[\"']", "").replaceFirst("(?is)[\"']$", "").trim();
        if (title.isBlank()) {
            return null;
        }

        String content = contentCandidate
                .replaceFirst("(?is)^\\s*(?:with\\s+(?:content|text)|and\\s+(?:content|text))\\s*[:\\-]?\\s*", "")
                .trim();
        if (content.isBlank()) {
            return null;
        }

        return new NoteCreateRequest(cleanNoteValue(title), cleanNoteValue(content));
    }

    public static boolean tryCreateNoteFromUserRequest(String userText) {
        AI.CreateNoteIntent intent = AI.inferCreateNoteIntent(userText);
        if (intent == null || !"create_note".equalsIgnoreCase(intent.getAction())) {
            return false;
        }

        try {
            Database.initialize();
            Database.saveDocument(intent.getTitle(), intent.getContent());
            return true;
        } catch (Exception ex) {
            System.err.println("Unable to create note from user request: " + ex.getMessage());
            return false;
        }
    }

    public static boolean tryDeleteNoteFromUserRequest(String userText) {
        AI.DeleteNoteIntent intent = AI.inferDeleteNoteIntent(userText);
        if (intent == null || !"delete_note".equalsIgnoreCase(intent.getAction())) {
            return false;
        }

        try {
            Database.initialize();
            if (isDeleteAllRequest(intent.getTitle())) {
                Database.deleteAllDocuments();
                return true;
            }

            Database.Document document = Database.findDocumentByTitle(intent.getTitle());
            if (document == null) {
                return false;
            }
            Database.deleteDocument(document.getId());
            return true;
        } catch (Exception ex) {
            System.err.println("Unable to delete note from user request: " + ex.getMessage());
            return false;
        }
    }

    private static boolean isDeleteAllRequest(String title) {
        if (title == null) {
            return false;
        }

        String normalized = title.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("all")
                || normalized.equals("all notes")
                || normalized.equals("all documents")
                || normalized.equals("all my notes")
                || normalized.equals("all my documents")
                || normalized.equals("everything")
                || normalized.endsWith(" all notes")
                || normalized.endsWith(" all documents")
                || normalized.contains("delete all")
                || normalized.contains("all notes")
                || normalized.contains("all documents");
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }

        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }

        return null;
    }

    private static String cleanNoteValue(String value) {
        if (value == null) {
            return "";
        }

        String cleaned = value.replaceAll("[\\r\\n]+", " ").trim();
        return cleaned.replaceAll("\\s+", " ");
    }

    private void handleSendMessage(String text) {
        view.appendUserMessage(text);

        NoteCreateRequest noteRequest = parseCreateNoteRequest(text);
        if (noteRequest != null && tryCreateNoteFromUserRequest(text)) {
            Database.Document created = null;
            try {
                created = Database.findDocumentByTitle(noteRequest.getTitle());
            } catch (Exception ex) {
                System.err.println("Unable to reload created note: " + ex.getMessage());
            }

            if (created != null) {
                activeDocumentId = created.getId();
            }

            loadExistingDocuments();
            view.showDocumentEditor(noteRequest.getTitle(), noteRequest.getContent());
            view.appendAssistantMessage("Created note \"" + noteRequest.getTitle() + "\".");
            return;
        }

        AI.DeleteNoteIntent deleteIntent = AI.inferDeleteNoteIntent(text);
        if (deleteIntent != null && "delete_note".equalsIgnoreCase(deleteIntent.getAction())
                && tryDeleteNoteFromUserRequest(text)) {
            String title = deleteIntent.getTitle();
            int previousActiveDocumentId = activeDocumentId;

            if (isDeleteAllRequest(title)) {
                activeDocumentId = -1;
            } else if (previousActiveDocumentId > -1) {
                try {
                    Database.Document active = Database.findDocumentById(previousActiveDocumentId);
                    if (active != null && title.equalsIgnoreCase(active.getTitle())) {
                        activeDocumentId = -1;
                    }
                } catch (Exception ex) {
                    System.err.println("Unable to clear active note on delete: " + ex.getMessage());
                }
            }

            loadExistingDocuments();
            view.showDocuments();
            if (isDeleteAllRequest(title)) {
                view.appendAssistantMessage("Deleted all notes.");
            } else {
                view.appendAssistantMessage("Deleted note \"" + title + "\".");
            }
            return;
        }

        view.beginAssistantThinking();

        String contextualPrompt = AI.buildPromptWithDocuments(text);

        AI.streamMessage(
                contextualPrompt,
                token -> SwingUtilities.invokeLater(() -> view.appendAssistantChunk(token)),
                () -> SwingUtilities.invokeLater(() -> view.finishAssistantResponse()),
                error -> SwingUtilities.invokeLater(() -> {
                    view.finishAssistantResponse();
                    view.appendAssistantMessage("Sorry, I couldn't respond: " + error.getMessage());
                })
        );
    }
}