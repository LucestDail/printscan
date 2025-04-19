package com.baeksang.printscan.controller;

import com.baeksang.printscan.service.PrinterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import javax.print.PrintService;
import javax.print.PrintException;
import java.util.Map;

@RestController
@RequestMapping("/api/print")
@RequiredArgsConstructor
public class PrinterController {

    private final PrinterService printerService;

    @PostMapping("/usb/qr")
    public ResponseEntity<String> printQrCode(@RequestBody Map<String, String> request) {
        try {
            String data = request.getOrDefault("data", "");
            String text = request.getOrDefault("text", "");
            
            if (data.isEmpty()) {
                return ResponseEntity.badRequest().body("데이터가 비어있습니다.");
            }

            PrintService printer = printerService.findPrinter("zebra", "zd421")
                .orElse(null);

            if (printer == null) {
                return ResponseEntity.notFound().build();
            }

            String zpl = text.isEmpty() ? 
                printerService.generateQrZpl(data, 100, 10, 2, 4) :
                printerService.generateQrWithTextZpl(data, text);

            printerService.printZpl(printer, zpl);
            return ResponseEntity.ok("출력이 완료되었습니다.");

        } catch (PrintException e) {
            return ResponseEntity.internalServerError()
                .body("프린터 오류: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("시스템 오류: " + e.getMessage());
        }
    }

    @PostMapping("/usb/text")
    public ResponseEntity<String> printText(@RequestBody Map<String, String> request) {
        try {
            String text = request.getOrDefault("text", "");
            
            if (text.isEmpty()) {
                return ResponseEntity.badRequest().body("텍스트가 비어있습니다.");
            }

            PrintService printer = printerService.findPrinter("zebra", "zd421")
                .orElse(null);

            if (printer == null) {
                return ResponseEntity.notFound().build();
            }

            String zpl = printerService.generateTextZpl(text, 100, 100, 30);
            printerService.printZpl(printer, zpl);
            
            return ResponseEntity.ok("출력이 완료되었습니다.");

        } catch (PrintException e) {
            return ResponseEntity.internalServerError()
                .body("프린터 오류: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("시스템 오류: " + e.getMessage());
        }
    }
} 