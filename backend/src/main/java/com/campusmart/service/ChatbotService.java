package com.campusmart.service;

import com.campusmart.dto.ChatRequestDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class ChatbotService {

    private static final Logger log = LoggerFactory.getLogger(ChatbotService.class);

    @Value("${chatbot.api-key}")
    private String apiKey;

    @Value("${chatbot.base-url}")
    private String baseUrl;

    @Value("${chatbot.model}")
    private String model;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private WebSearchService webSearchService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SYSTEM_PROMPT = """
        你是CampusMart校园闲置交易平台的智能客服助手，名叫MiMo。你知识渊博、乐于助人。
        你的能力包括：
        1. 回答用户关于平台功能、使用方法的问题
        2. 帮助用户了解交易流程、发布商品、下单等操作
        3. 解决用户遇到的常见问题
        4. 回答各种通用问题，包括学习、生活、常识等
        5. 提供平台规则和安全提示
        请用友好、专业的语气回答。回答可以详细一些，不要过于简短。
        """;

    // Keywords that suggest the question needs internet search
    private static final Pattern SEARCH_PATTERN = Pattern.compile(
        "(\\d{4}年|最新|最近|今天|昨天|明天|现在|目前|当前|新闻|热搜|" +
        "高考|考研|四六级|期末|放假|开学|天气|房价|股票|" +
        "小米|华为|苹果|特斯拉|chatgpt|" +
        "谁是|什么是.{0,4}(总统|主席|CEO|冠军|金牌)|" +
        "最新消息|实时|今天是|现在是)"
    );

    private boolean needsSearch(String message) {
        return SEARCH_PATTERN.matcher(message).find();
    }

    public String chat(String userMessage, List<ChatRequestDTO.ChatMessage> history) throws Exception {
        // Step 1: Check if web search is needed
        String searchContext = "";
        if (needsSearch(userMessage)) {
            log.info("Question may need web search: {}", userMessage);
            List<WebSearchService.SearchResult> searchResults = webSearchService.search(userMessage, 3);
            if (!searchResults.isEmpty()) {
                StringBuilder sb = new StringBuilder("\n\n[互联网搜索结果]\n");
                for (WebSearchService.SearchResult r : searchResults) {
                    sb.append("- ").append(r.toText()).append("\n");
                }
                searchContext = sb.toString();
                log.info("Found {} search results", searchResults.size());
            }
        }

        // Step 2: Build request
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", model);
        requestBody.put("max_tokens", 2048);

        String systemPrompt = SYSTEM_PROMPT;
        if (!searchContext.isEmpty()) {
            systemPrompt += "\n\n以下是关于用户问题的互联网搜索结果，请参考这些信息来回答。如果搜索结果与问题无关，可以忽略。" + searchContext;
        }
        requestBody.put("system", systemPrompt);

        ArrayNode messages = objectMapper.createArrayNode();

        if (history != null) {
            for (ChatRequestDTO.ChatMessage msg : history) {
                if ("user".equals(msg.getRole()) || "assistant".equals(msg.getRole())) {
                    ObjectNode historyMsg = objectMapper.createObjectNode();
                    historyMsg.put("role", msg.getRole());
                    historyMsg.put("content", msg.getContent());
                    messages.add(historyMsg);
                }
            }
        }

        ObjectNode userMsg = objectMapper.createObjectNode();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);

        requestBody.set("messages", messages);

        // Step 3: Call LLM API
        String url = baseUrl.replaceAll("/+$", "") + "/v1/messages";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");

        log.info("Calling MiMo API: {} | model: {}", url, model);

        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);

        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        } catch (HttpClientErrorException e) {
            log.error("API HTTP error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new Exception("API返回错误 " + e.getStatusCode() + "：" + e.getResponseBodyAsString());
        }

        String respBody = response.getBody();
        log.info("API response: {}", respBody != null ? respBody.substring(0, Math.min(respBody.length(), 500)) : "null");

        // Step 4: Parse response
        JsonNode responseJson = objectMapper.readTree(respBody);

        // Format 1: Anthropic format - { "content": [{ "type": "text", "text": "..." }] }
        JsonNode contentArray = responseJson.get("content");
        if (contentArray != null && contentArray.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode contentItem : contentArray) {
                String type = contentItem.path("type").asText("");
                if ("text".equals(type) && contentItem.has("text")) {
                    sb.append(contentItem.get("text").asText());
                }
            }
            if (sb.length() > 0) return sb.toString();
        }

        // Format 2: OpenAI format - { "choices": [{ "message": { "content": "..." } }] }
        JsonNode choices = responseJson.get("choices");
        if (choices != null && choices.isArray() && choices.size() > 0) {
            JsonNode messageNode = choices.get(0).get("message");
            if (messageNode != null && messageNode.has("content")) {
                return messageNode.get("content").asText();
            }
        }

        // Format 3: Simple { "text": "..." } or { "response": "..." }
        if (responseJson.has("text")) return responseJson.get("text").asText();
        if (responseJson.has("response")) return responseJson.get("response").asText();
        if (responseJson.has("result")) return responseJson.get("result").asText();

        // Format 4: Error response
        if (responseJson.has("error")) {
            String errMsg = responseJson.get("error").toString();
            throw new Exception("API返回错误：" + errMsg);
        }

        log.warn("Unrecognized response format: {}", respBody);
        throw new Exception("无法解析API响应格式，请查看后端日志");
    }
}
