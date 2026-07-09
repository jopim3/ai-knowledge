package com.example.ai.controller;

import com.example.ai.Result;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/ai")
public class AiKnowledgeController {

    private static final String API_KEY = "sk-84c1acaf64e643f78f3b39e256422e7b";

    // 测试方法
    @GetMapping("/test")
    public Result<String> test() {
        return Result.success("Controller 工作正常");
    }

    @PostMapping("/ask")
    public Result<String> ask(@RequestPart("file") MultipartFile file,
                              @RequestPart("question") String question)  {
        System.out.println("收到 question: " + question);
        System.out.println("文件大小: " + file.getSize());
        try {
            // 1. 读取上传文件的内容
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);

            // 2. 构造 prompt：把文件内容作为上下文，让 AI 回答问题
            String prompt = "根据以下内容回答问题：\n\n" + content + "\n\n问题：" + question;

            // 3. 调用 DeepSeek API
            String answer = callDeepSeek(prompt);
            return Result.success(answer);
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

        String json = "{\n" +
                "  \"model\": \"deepseek-chat\",\n" +
                "  \"messages\": [{\"role\": \"user\", \"content\": \"" + escapeJson(prompt) + "\"}]\n" +
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

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}