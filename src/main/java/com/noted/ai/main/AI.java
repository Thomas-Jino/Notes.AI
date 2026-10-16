package com.noted.ai.main;

import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
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

    public static final class CreateNoteIntent {
        private final String action;
        private final String title;
        private final String content;

        public CreateNoteIntent(String action, String title, String content) {
            this.action = action;
            this.title = title;
            this.content = content;
        }

        public String getAction() {
            return action;
        }

        public String getTitle() {
            return title;
        }

        public String getContent() {
            return content;
        }
    }

    public static final class DeleteNoteIntent {
        private final String action;
        private final String title;

        public DeleteNoteIntent(String action, String title) {
            this.action = action;
            this.title = title;
        }

        public String getAction() {
            return action;
        }

        public String getTitle() {
            return title;
        }
    }

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

    public static String buildPromptWithDocuments(String userText) {
        if (userText == null || userText.isBlank()) {
            return userText == null ? "" : userText;
        }

        try {
            Database.initialize();
            List<Database.Document> documents = Database.findAllDocuments();
            if (documents == null || documents.isEmpty()) {
                return userText;
            }

            StringBuilder prompt = new StringBuilder();
            prompt.append("Use the saved documents below as context for your answer. "
                    + "Answer the user's question using the documents when relevant.\n\n");

            for (Database.Document document : documents) {
                prompt.append("Document: ").append(document.getTitle()).append("\n");
                String content = document.getContent() == null ? "" : document.getContent().trim();
                if (!content.isEmpty()) {
                    prompt.append(content).append("\n\n");
                }
            }

            prompt.append("User question: ").append(userText).append("\n");
            return prompt.toString();
        } catch (SQLException ex) {
            System.err.println("Unable to load saved documents for AI context: " + ex.getMessage());
            return userText;
        }
    }

    public static CreateNoteIntent inferCreateNoteIntent(String userText) {
        if (userText == null || userText.isBlank()) {
            return null;
        }

        String normalized = userText.trim();
        String lower = normalized.toLowerCase(Locale.ROOT);

        String storedTitle = extractStoredTitle(normalized);
        if (storedTitle != null) {
            String content = normalized;
            if (lower.contains("poem") || lower.contains("write a poem") || lower.contains("poetry")) {
                String poem = CHAT_MODEL.generate("Write a short poem about cats. Return only the poem text and nothing else.");
                content = "Poem about cats:\n" + poem;
            }
            return new CreateNoteIntent("create_note", storedTitle, content);
        }

        if ((lower.contains("store it as app idea")
                || lower.contains("save it as app idea")
                || lower.contains("save as app idea")
                || lower.contains("store as app idea")
                || lower.contains("write the code") && lower.contains("app idea")
                || lower.contains("save this as") && lower.contains("app idea")
                || lower.contains("store this as") && lower.contains("app idea"))) {
            return new CreateNoteIntent("create_note", "App Idea", normalized);
        }

        if ((lower.contains("store it as") || lower.contains("save it as")
                || lower.contains("store as") || lower.contains("save as"))
                && (lower.contains("note") || lower.contains("document") || lower.contains("idea") || lower.contains("app"))) {
            String title = "Document";
            if (lower.contains("idea") || lower.contains("app idea")) {
                title = "App Idea";
            } else if (lower.contains("note")) {
                title = "Note";
            }
            return new CreateNoteIntent("create_note", title, normalized);
        }

        String prompt = "Decide whether the user is asking to create a new note. "
                + "If yes, return JSON only in this exact form and nothing else: "
                + "{\"action\":\"create_note\",\"title\":\"note title\",\"content\":\"note text\"}. "
                + "When the user says to save or store an app idea or code snippet, treat it as a create_note action with title \"App Idea\". "
                + "If the user is not asking to create a note, return: {\"action\":\"chat\"}. "
                + "Do not explain yourself. Use the user's exact note title and content when supplied. "
                + "User request: " + userText;

        String rawResponse = CHAT_MODEL.generate(prompt);
        return parseCreateNoteIntent(rawResponse);
    }

    private static String extractStoredTitle(String userText) {
        if (userText == null || userText.isBlank()) {
            return null;
        }

        Matcher matcher = Pattern.compile("(?is)(?:store|save)(?: it)?(?: as| as the)?\s+(?:the\s+)?(.+)$")
                .matcher(userText);
        if (!matcher.find()) {
            return null;
        }

        String title = matcher.group(1).trim();
        title = title.replaceFirst("(?is)^\s*the\s+", "").trim();
        title = title.replaceFirst("(?is)\s+[.-]$", "").trim();
        if (title.isBlank() || title.contains(" and ") && title.split(" and ").length > 1 && !title.contains("poem")) {
            return null;
        }
        return title;
    }

    public static DeleteNoteIntent inferDeleteNoteIntent(String userText) {
        if (userText == null || userText.isBlank()) {
            return null;
        }

        String prompt = "Decide whether the user is asking to delete an existing note. "
                + "If yes, return JSON only in this exact form and nothing else: "
                + "{\"action\":\"delete_note\",\"title\":\"note title\"}. "
                + "If the user is asking to delete all notes or all documents, return: "
                + "{\"action\":\"delete_note\",\"title\":\"all notes\"}. "
                + "If the user is not asking to delete a note, return: {\"action\":\"chat\"}. "
                + "Use the exact note title the user mentions, or use \"all notes\" when they ask to delete everything. "
                + "User request: " + userText;

        String rawResponse = CHAT_MODEL.generate(prompt);
        return parseDeleteNoteIntent(rawResponse);
    }

    public static CreateNoteIntent parseCreateNoteIntent(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return null;
        }

        String json = rawJson.trim();
        if (json.startsWith("```")) {
            json = json.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
        }

        Matcher actionMatcher = Pattern.compile("\\\"action\\\"\\s*:\\s*\\\"(.*?)\\\"",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(json);
        Matcher titleMatcher = Pattern.compile("\\\"title\\\"\\s*:\\s*\\\"(.*?)\\\"",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(json);
        Matcher contentMatcher = Pattern.compile("\\\"content\\\"\\s*:\\s*\\\"(.*?)\\\"",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(json);

        if (!actionMatcher.find()) {
            return null;
        }

        String action = actionMatcher.group(1).trim();
        if (!"create_note".equalsIgnoreCase(action)) {
            return null;
        }

        if (!titleMatcher.find() || !contentMatcher.find()) {
            return null;
        }

        String title = titleMatcher.group(1).replace("\\n", " ").replace("\\\"", "\"").trim();
        String content = contentMatcher.group(1).replace("\\n", " ").replace("\\\"", "\"").trim();
        if (title.isBlank() || content.isBlank()) {
            return null;
        }

        return new CreateNoteIntent(action, title, content);
    }

    public static DeleteNoteIntent parseDeleteNoteIntent(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return null;
        }

        String json = rawJson.trim();
        if (json.startsWith("```")) {
            json = json.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
        }

        Matcher actionMatcher = Pattern.compile("\\\"action\\\"\\s*:\\s*\\\"(.*?)\\\"",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(json);
        Matcher titleMatcher = Pattern.compile("\\\"title\\\"\\s*:\\s*\\\"(.*?)\\\"",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(json);

        if (!actionMatcher.find()) {
            return null;
        }

        String action = actionMatcher.group(1).trim();
        if (!"delete_note".equalsIgnoreCase(action)) {
            return null;
        }

        if (!titleMatcher.find()) {
            return null;
        }

        String title = titleMatcher.group(1).replace("\\n", " ").replace("\\\"", "\"").trim();
        if (title.isBlank()) {
            return null;
        }

        return new DeleteNoteIntent(action, title);
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