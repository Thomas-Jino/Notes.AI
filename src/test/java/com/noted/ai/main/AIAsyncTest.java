package com.noted.ai.main;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class AIAsyncTest {

    @Test
    void sendMessageAsync_returnsFuture() {
        CompletableFuture<String> future = AI.sendMessageAsync("hello");
        assertNotNull(future);
    }

    @Test
    void streamMessage_usesStreamingCallback() {
        assertDoesNotThrow(() -> AI.streamMessage("hello", token -> {
        }, error -> {
        }));
    }

    @Test
    void parseEmotionAnalysis_extractsEmotionAndIntensity() {
        AI.EmotionAnalysis analysis = AI.parseEmotionAnalysis("{\"Emotion\":\"Happy\",\"Intensity\":10}");

        assertNotNull(analysis);
        assertEquals("Happy", analysis.getEmotion());
        assertEquals(10, analysis.getIntensity());
    }

    @Test
    void buildPromptWithDocuments_includesSavedDocumentContext() throws SQLException {
        Database.initialize();
        Database.saveDocument("Project Plan", "Ship in 2 weeks.");
        Database.saveDocument("Meeting Notes", "Need more tests.");

        String prompt = AI.buildPromptWithDocuments("Summarize my notes");

        assertTrue(prompt.contains("Project Plan"));
        assertTrue(prompt.contains("Ship in 2 weeks."));
        assertTrue(prompt.contains("Meeting Notes"));
        assertTrue(prompt.contains("User question: Summarize my notes"));
    }

    @Test
    void tryCreateNoteFromUserRequest_createsANewDocumentWithTitleAndContent() throws SQLException {
        Database.initialize();

        boolean created = Main.tryCreateNoteFromUserRequest(
                "Create a new note named \"Trip Ideas\": Plan the weekend trip and bring snacks.");

        assertTrue(created);
        Database.Document document = Database.findDocumentByTitle("Trip Ideas");
        assertNotNull(document);
        assertEquals("Plan the weekend trip and bring snacks.", document.getContent());
    }

    @Test
    void tryDeleteNoteFromUserRequest_deletesANoteByTitle() throws SQLException {
        Database.initialize();
        Database.saveDocument("Delete Me", "content");

        boolean deleted = Main.tryDeleteNoteFromUserRequest("Delete the note named \"Delete Me\".");

        assertTrue(deleted);
        assertEquals(null, Database.findDocumentByTitle("Delete Me"));
    }

    @Test
    void findAllDocuments_keepsOnlyLatestDuplicateTitle() throws SQLException {
        Database.initialize();
        Database.saveDocument("Duplicate Title", "Old content");

        Database.Document first = Database.findDocumentByTitle("Duplicate Title");
        assertNotNull(first);

        Database.saveDocument("Duplicate Title", "New content");
        List<Database.Document> documents = Database.findAllDocuments();

        boolean seenLatestOnly = documents.stream()
                .filter(doc -> "Duplicate Title".equals(doc.getTitle()))
                .count() == 1;

        Database.Document latest = Database.findDocumentByTitle("Duplicate Title");
        assertTrue(seenLatestOnly);
        assertEquals("New content", latest.getContent());
    }

    @Test
    void deleteDocument_removesExistingRecord() throws SQLException {
        Database.initialize();
        int id = Database.saveDocument("Delete Me", "content");

        Database.deleteDocument(id);

        assertEquals(null, Database.findDocumentById(id));
    }
}
