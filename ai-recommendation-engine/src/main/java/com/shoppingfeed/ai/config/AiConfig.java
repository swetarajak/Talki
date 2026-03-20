package com.shoppingfeed.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Bean
    public ChatClient chatClient(OpenAiChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem("""
                    You are a personalized shopping assistant for an
                    e-commerce platform. Your job is to rank products
                    for users based on their preferences and behaviour.
                    Always respond with valid JSON only. No extra text.
                    """)
                .build();
    }

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        // M1 version — direct constructor, no builder
        return new SimpleVectorStore(embeddingModel);
    }
}