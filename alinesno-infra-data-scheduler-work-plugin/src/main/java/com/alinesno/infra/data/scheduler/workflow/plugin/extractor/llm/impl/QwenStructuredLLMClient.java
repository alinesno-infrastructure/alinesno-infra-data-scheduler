// QwenStructuredLLMClient.java
package com.alinesno.infra.data.scheduler.workflow.plugin.extractor.llm.impl;

import com.alinesno.infra.data.scheduler.workflow.plugin.extractor.dto.LLMStructuredExtraction;
import com.alinesno.infra.data.scheduler.workflow.plugin.extractor.llm.GenericLLMClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 基于 Qwen（DashScope）的结构化 LLM 客户端
 * <p>
 * 利用 response_format=json_object 强制模型输出合法 JSON。
 * 不依赖行业知识，适用于任意文本抽取任务。
 * </p>
 */
@Component
public class QwenStructuredLLMClient implements GenericLLMClient {

    @Value("${llm.api.key}")
    private String apiKey;

    @Value("${llm.model:qwen-max}")
    private String model;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public LLMStructuredExtraction extractStructured(String prompt) throws Exception {
        // 构造请求体（启用 JSON 模式）
        String requestBody = """
        {
          "model": "%s",
          "input": {
            "messages": [
              {"role": "user", "content": "%s"}
            ]
          },
          "parameters": {
            "response_format": {"type": "json_object"}
          }
        }
        """.formatted(model, escapeJson(prompt));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofSeconds(60))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("LLM call failed: " + response.body());
        }

        // 解析 DashScope 响应
        JsonNode root = objectMapper.readTree(response.body());
        String outputText = root.path("output").path("choices").get(0).path("message").asText();

        // 直接反序列化为结构化对象
        return objectMapper.readValue(outputText, LLMStructuredExtraction.class);
    }

    private static String escapeJson(String input) {
        return input.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}