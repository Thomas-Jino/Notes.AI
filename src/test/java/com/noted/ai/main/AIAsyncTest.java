package com.noted.ai.main;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
}
