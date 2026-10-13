package com.noted.ai.main;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

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

    public static CompletableFuture<String> sendMessageAsync(String text) {
        return CompletableFuture.supplyAsync(() -> {
            String reply = sendMessage(text);
            AI.response = reply;
            return reply;
        }, AI_EXECUTOR);
    }

    public static void streamMessage(String text, Consumer<String> onToken, Consumer<Throwable> onError) {
        streamMessage(text, onToken, () -> {
        }, onError);
    }

    public static void streamMessage(String text, Consumer<String> onToken, Runnable onComplete,
            Consumer<Throwable> onError) {
        StreamingChatLanguageModel model = OllamaStreamingChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("sebdg/emotional_llama")
                .timeout(Duration.ofMinutes(2))
                .build();

        StringBuilder fullReply = new StringBuilder();
        model.generate(UserMessage.from(text), new StreamingResponseHandler<AiMessage>() {
            @Override
            public void onNext(String token) {
                fullReply.append(token);
                onToken.accept(token);
            }

            @Override
            public void onComplete(Response<AiMessage> response) {
                AI.response = fullReply.toString();
                System.out.println("Emotional Llama Response:\n" + AI.response);
                onComplete.run();
            }

            @Override
            public void onError(Throwable error) {
                onError.accept(error);
            }
        });
    }

    public static String sendMessage(String text) {
        ChatLanguageModel model = OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("sebdg/emotional_llama")
                .timeout(Duration.ofMinutes(2))
                .build();

        String reply = model.generate(text);
        AI.response = reply;
        System.out.println("Emotional Llama Response:\n" + reply);
        return reply;
    }
}