package com.example.ai.controller;

import com.example.ai.Result;
import com.example.ai.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import com.example.ai.entity.Book;
import java.util.List;

@RestController
@RequestMapping("/ai")
public class AiKnowledgeController {

    private static final String API_KEY = "sk-84c1acaf64e643f78f3b39e256422e7b";
    @Autowired
    private BookService bookService;
    // 测试方法
    @GetMapping("/test")
    public Result<String> test() {
        return Result.success("Controller 工作正常");
    }

    @GetMapping("/books/available")
    public Result<List<Book>> availableBooks() {
        return Result.success(bookService.listAvailableBooks());
    }

    @PostMapping("/ask")
    public Result<String> ask(@RequestPart("file") MultipartFile file,
                              @RequestPart("question") String question) {
        System.out.println("收到 question: " + question);
        System.out.println("文件大小: " + file.getSize());

        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            String prompt = "根据以下内容回答问题：\n\n" + content + "\n\n问题：" + question;

            // ---------- Redis 缓存逻辑 ----------
            redis.clients.jedis.Jedis jedis = null;
            String cacheKey = "ai:answer:" + (prompt.hashCode() & 0x7fffffff);
            String cachedAnswer = null;

            try {
                jedis = new redis.clients.jedis.Jedis("localhost", 6379);
                cachedAnswer = jedis.get(cacheKey);
            } catch (Exception e) {
                System.out.println("Redis 连接失败，降级到直接调用 API: " + e.getMessage());
            }

            // 如果缓存命中，直接返回
            if (cachedAnswer != null && !cachedAnswer.isEmpty()) {
                System.out.println("缓存命中，直接返回");
                if (jedis != null) jedis.close();
                return Result.success(cachedAnswer);
            }

            // ---------- 缓存未命中，调用 DeepSeek API ----------
            String response = callDeepSeek(prompt);
            String finalAnswer = response;

            // 如果 response 包含 tool_calls，执行工具调用
            if (response.contains("tool_calls")) {
                String booksJson = executeToolCall("listAvailableBooks");
                finalAnswer = "可借图书：" + booksJson;
            }

            // 将回答存入 Redis（过期时间 1 小时）
            if (jedis != null) {
                try {
                    jedis.setex(cacheKey, 3600, finalAnswer);
                    System.out.println("回答已缓存，key: " + cacheKey);
                } catch (Exception e) {
                    System.out.println("缓存写入失败: " + e.getMessage());
                }
            }
            if (jedis != null) jedis.close();

            return Result.success(finalAnswer);

        } catch (IOException e) {
            e.printStackTrace();
            return Result.error("读取文件失败: " + e.getMessage());
        }
    }


    private String callDeepSeek(String prompt) throws IOException {
        URL url = new URL("https://api.deepseek.com/chat/completions");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
        conn.setDoOutput(true);

        // 1. 构造包含工具描述的请求体
        String json = "{\n" +
                "  \"model\": \"deepseek-chat\",\n" +
                "  \"messages\": [{\"role\": \"user\", \"content\": \"" + escapeJson(prompt) + "\"}],\n" +
                "  \"tools\": [{\n" +
                "    \"type\": \"function\",\n" +
                "    \"function\": {\n" +
                "      \"name\": \"listAvailableBooks\",\n" +
                "      \"description\": \"查询所有可借的图书列表\",\n" +
                "      \"parameters\": {\n" +
                "        \"type\": \"object\",\n" +
                "        \"properties\": {},\n" +
                "        \"required\": []\n" +
                "      }\n" +
                "    }\n" +
                "  }]\n" +
                "}";

        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes());
        }

        int code = conn.getResponseCode();
        if (code == 200) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                return sb.toString();
            }
        } else {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                return "API调用失败: " + sb.toString();
            }
        }
    }

    private String executeToolCall(String toolName) {
        if ("listAvailableBooks".equals(toolName)) {
            List<Book> books = bookService.listAvailableBooks();
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < books.size(); i++) {
                Book b = books.get(i);
                if (i > 0) sb.append(",");
                sb.append("{")
                        .append("\"id\":").append(b.getId()).append(",")
                        .append("\"title\":\"").append(escapeJson(b.getTitle())).append("\",")
                        .append("\"author\":\"").append(escapeJson(b.getAuthor())).append("\",")
                        .append("\"status\":").append(b.getStatus())
                        .append("}");
            }
            sb.append("]");
            return sb.toString();
        }
        return "[]";
    }


        private String escapeJson(String s) {
            if (s == null) return "";
            StringBuilder sb = new StringBuilder();
            for (char c : s.toCharArray()) {
                switch (c) {
                    case '"': sb.append("\\\""); break;
                    case '\\': sb.append("\\\\"); break;
                    case '/': sb.append("\\/"); break;
                    case '\b': sb.append("\\b"); break;
                    case '\f': sb.append("\\f"); break;
                    case '\n': sb.append("\\n"); break;
                    case '\r': sb.append("\\r"); break;
                    case '\t': sb.append("\\t"); break;
                    default:
                        if (c < 0x20) {
                            sb.append(String.format("\\u%04x", (int)c));
                        } else {
                            sb.append(c);
                        }
                        break;
                }
            }
            return sb.toString();
        }
    }
