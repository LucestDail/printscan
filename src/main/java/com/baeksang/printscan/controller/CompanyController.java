package com.baeksang.printscan.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/company", produces = MediaType.APPLICATION_JSON_VALUE)
public class CompanyController {

    @Value("${company.name}")
    private String companyName;

    @Value("${company.system.name}")
    private String systemName;

    @GetMapping("/info")
    public ResponseEntity<Map<String, String>> getCompanyInfo() {
        Map<String, String> info = new HashMap<>();
        info.put("companyName", new String(companyName.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));
        info.put("systemName", new String(systemName.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));
        
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
            .body(info);
    }
} 