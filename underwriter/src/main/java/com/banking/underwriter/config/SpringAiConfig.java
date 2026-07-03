package com.banking.underwriter.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Slf4j
@Configuration
public class SpringAiConfig {
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        log.info("Initializing system ChatClient bean with system parameters.");
        return builder
                .defaultSystem("You are a highly precise financial data extraction subsystem. " +
                        "Your sole responsibility is to scan document segments and extract numeric data points. " +
                        "Never make independent credit approvals, guess missing values, or extrapolate data.")
                .build();
    }
}