package com.printscan.edge.web;

import com.printscan.edge.config.PrinterProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** 서버렌더 페이지 (Thymeleaf, Apple 디자인). */
@Controller
@RequiredArgsConstructor
public class PageController {

    private final PrinterProperties printer;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("printerMode", printer.getMode());
        return "index";
    }

    @GetMapping("/designer")
    public String designer(Model model) {
        model.addAttribute("printerMode", printer.getMode());
        return "designer";
    }

    @GetMapping("/scan")
    public String scan(Model model) {
        model.addAttribute("printerMode", printer.getMode());
        return "scan";
    }
}
