package com.example.ai.controller;

import com.example.ai.Result;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@RestController
@RequestMapping("/pdf")
public class PdfController {

    @PostMapping("/extract")
    public Result<String> extractText(@RequestParam("file") MultipartFile file) {
        // 1. 文件为空校验
        if (file.isEmpty()) {
            return Result.error("文件为空");
        }

        // 2. 校验文件类型
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase().endsWith(".pdf")) {
            return Result.error("只支持 PDF 文件");
        }

        try (InputStream is = file.getInputStream()) {
            // 3. 使用 Loader 加载 PDF
            PDDocument document = Loader.loadPDF(is.readAllBytes());

            // 4. 提取文本
            PDFTextStripper stripper = new PDFTextStripper();
            String fullText = stripper.getText(document);
            document.close(); // 记得关闭文档

            // 5. 检查是否为空
            if (fullText == null || fullText.trim().isEmpty()) {
                return Result.error("PDF 内容为空");
            }

            // 6. 截取前 500 字符作为预览
            String preview = fullText.length() > 500
                    ? fullText.substring(0, 500) + "...（共" + fullText.length() + "字）"
                    : fullText;

            return Result.success(preview);

        } catch (Exception e) {
            return Result.error("PDF 解析失败: " + e.getMessage());
        }
    }}