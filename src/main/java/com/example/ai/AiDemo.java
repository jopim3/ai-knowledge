package com.example.ai;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class AiDemo {
    public static void main(String[] args) throws Exception {
        String apiKey = "sk-84c1acaf64e643f78f3b39e256422e7b";  // 替换成你自己的

        URL url = new URL("https://api.deepseek.com/chat/completions");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setDoOutput(true);

        String json = "{\n" +
                "  \"model\": \"deepseek-chat\",\n" +
                "  \"messages\": [{\"role\": \"user\", \"content\": \"用一句话解释什么是Java\"}]\n" +
                "}";

        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes());
        }

        int code = conn.getResponseCode();
        if (code == 200) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }
        } else {
            System.out.println("请求失败，状态码: " + code);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }
        }
        conn.disconnect();
    }
}