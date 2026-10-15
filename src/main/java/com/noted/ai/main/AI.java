package com.noted.ai.main;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.output.Response;

public class AI {
    public static String response;
    private static final ExecutorService AI_EXECUTOR = Executors.newFixedThreadPool(4);
    private static final String OLLAMA_BASE_URL = "http://localhost:11434";
    private static final String OLLAMA_MODEL_NAME = "qwen2.5:3b";
    private static final Duration OLLAMA_TIMEOUT = Duration.ofMinutes(2);

    private static final ChatLanguageModel CHAT_MODEL = createChatModel();
    private static final StreamingChatLanguageModel STREAMING_CHAT_MODEL = createStreamingChatModel();

    public static final class EmotionAnalysis {
        private final String emotion;
        private final int intensity;

        public EmotionAnalysis(String emotion, int intensity) {
            this.emotion = emotion;
            this.intensity = intensity;
        }

        public String getEmotion() {
            return emotion;
        }

        public int getIntensity() {
            return intensity;
        }
    }

    private static ChatLanguageModel createChatModel() {
        return OllamaChatModel.builder()
                .baseUrl(OLLAMA_BASE_URL)
                .modelName(OLLAMA_MODEL_NAME)
                .timeout(OLLAMA_TIMEOUT)
                .build();
    }

    private static StreamingChatLanguageModel createStreamingChatModel() {
        return OllamaStreamingChatModel.builder()
                .baseUrl(OLLAMA_BASE_URL)
                .modelName(OLLAMA_MODEL_NAME)
                .timeout(OLLAMA_TIMEOUT)
                .build();
    }

    public static CompletableFuture<String> sendMessageAsync(String text) {
        return CompletableFuture.supplyAsync(() -> {
            String reply = sendMessage(text);
            AI.response = reply;
            return reply;
        }, AI_EXECUTOR);
    }

    public static EmotionAnalysis analyzeEmotions(String text) {
        String prompt = "Analyze the emotion in this text. Return JSON only with keys \"Emotion\" and \"Intensity\". "
                + "Use one of these labels: Happy, Sad, Angry, Tense, Calm, Excited, Fearful, Disgusted, "
                + "Surprised, Neutral. Intensity must be an integer from 1 to 10. Text: " + text;
        String reply = CHAT_MODEL.generate(prompt);
        return parseEmotionAnalysis(reply);
    }

    public static EmotionAnalysis parseEmotionAnalysis(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            throw new IllegalArgumentException("Emotion analysis response was empty.");
        }

        String json = rawJson.trim();
        if (json.startsWith("```")) {
            json = json.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
        }

        Matcher emotionMatcher = Pattern.compile("\\\"Emotion\\\"\\s*:\\s*\\\"(.*?)\\\"",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(json);
        Matcher intensityMatcher = Pattern.compile("\\\"Intensity\\\"\\s*:\\s*(\\d+)",
                Pattern.CASE_INSENSITIVE).matcher(json);

        if (!emotionMatcher.find() || !intensityMatcher.find()) {
            throw new IllegalArgumentException("Could not parse emotion JSON: " + rawJson);
        }

        String emotion = emotionMatcher.group(1).trim();
        int intensity = Integer.parseInt(intensityMatcher.group(1).trim());
        if (intensity < 1 || intensity > 10) {
            throw new IllegalArgumentException("Intensity must be between 1 and 10: " + intensity);
        }

        return new EmotionAnalysis(emotion, intensity);
    }

    public static void streamMessage(String text, Consumer<String> onToken, Consumer<Throwable> onError) {
        streamMessage(text, onToken, () -> {
        }, onError);
    }

    public static void streamMessage(String text, Consumer<String> onToken, Runnable onComplete,
            Consumer<Throwable> onError) {
        StringBuilder fullReply = new StringBuilder();
        STREAMING_CHAT_MODEL.generate(UserMessage.from(text), new StreamingResponseHandler<AiMessage>() {
            @Override
            public void onNext(String token) {
                fullReply.append(token);
                onToken.accept(token);
            }

            @Override
            public void onComplete(Response<AiMessage> response) {
                AI.response = fullReply.toString();
                System.out.println("AI Response:\n" + AI.response);
                onComplete.run();
            }

            @Override
            public void onError(Throwable error) {
                onError.accept(error);
            }
        });
    }

    public static String sendMessage(String text) {
        String reply = CHAT_MODEL.generate(text);
        AI.response = reply;
        System.out.println("AI Response:\n" + reply);
        return reply;
    }
}