package com.printscan.cloud.web;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final OrgContext orgContext;

    @GetMapping("/")
    public String dashboard(HttpServletRequest req) {
        try {
            orgContext.resolve(req);          // 세션/헤더/단일org 폴백으로 확정되면 대시보드
            return "dashboard";
        } catch (IllegalArgumentException e) {
            return "redirect:/login";         // 멀티테넌트인데 미로그인 → 로그인
        }
    }

    @GetMapping("/login")
    public String login() { return "login"; }
}
