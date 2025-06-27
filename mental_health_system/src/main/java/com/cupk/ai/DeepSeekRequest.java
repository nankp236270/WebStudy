package com.cupk.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record DeepSeekRequest(
        @JsonProperty("model") String model,
        @JsonProperty("messages") List<Message> messages,
        @JsonProperty("temperature") double temperature,
        @JsonProperty("system_prompt") String systemPrompt
) {
    public record Message(String role, String content) {}
}
