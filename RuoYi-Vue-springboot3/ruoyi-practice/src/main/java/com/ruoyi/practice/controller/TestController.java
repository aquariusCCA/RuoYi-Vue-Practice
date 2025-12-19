package com.ruoyi.practice.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
//@CrossOrigin
public class TestController {
    @PostMapping("/api/public/test")
    public Map<String, String> publicTest() {
        return Map.of("result", "OK");
    }
}