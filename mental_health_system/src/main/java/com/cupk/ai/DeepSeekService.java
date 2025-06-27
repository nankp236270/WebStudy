package com.cupk.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.*;
import java.io.InputStream;

@Service
public class DeepSeekService {

    @Value("${deepseek.api.key}")
    private String apiKey;

    private static final String API_URL = "https://api.deepseek.com/v1/chat/completions";
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 加载Prompt文本
    public String loadPrompt() throws Exception {
        ClassPathResource resource = new ClassPathResource("deepseek_prompt.txt");
        try (InputStream input = resource.getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    // 发送消息到DeepSeek
    public String chatWithAI(List<DeepSeekRequest.Message> messages) throws Exception {
        String systemPrompt = loadPrompt();
        DeepSeekRequest req = new DeepSeekRequest(
                "deepseek-chat",
                messages,
                0.3,
                systemPrompt
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        String body = objectMapper.writeValueAsString(req);
        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.postForEntity(API_URL, entity, String.class);

        // 返回格式 { choices: [{ message: { content: "...AI回复..." } }] }
        String aiContent = objectMapper.readTree(response.getBody())
                .path("choices").get(0)
                .path("message")
                .path("content")
                .asText();
        return aiContent;
    }

    // 情绪JSON解析
    public EmotionData parseEmotion(String aiResponse) throws Exception {
        Pattern pattern = Pattern.compile("🌡️\\{.*?\\}");
        Matcher matcher = pattern.matcher(aiResponse);
        if (matcher.find()) {
            String json = matcher.group().replace("🌡️", "");
            return objectMapper.readValue(json, EmotionData.class);
        }
        throw new IllegalStateException("未检测到情绪数据");
    }
}
