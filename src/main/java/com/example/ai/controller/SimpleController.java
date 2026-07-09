package com.example.ai.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SimpleController {
    @GetMapping("/test-scan")
    public String testScan() {
        return "controller 包扫描成功";
    }
}
