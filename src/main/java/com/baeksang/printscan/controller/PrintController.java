package com.baeksang.printscan.controller;

import com.baeksang.printscan.dto.PrintRequestDTO;
import com.baeksang.printscan.entity.PrintJob;
import com.baeksang.printscan.entity.PrintJobPriority;
import com.baeksang.printscan.service.PrintService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/print")
@RequiredArgsConstructor
public class PrintController {

    private final PrintService printService;

    @PostMapping("/request")
    public ResponseEntity<PrintJob> requestPrint(
            @RequestParam("title") String title,
            @RequestParam("copies") Integer copies,
            @RequestParam("priority") String priority,
            @RequestParam("doubleSided") Boolean doubleSided,
            @RequestParam("color") Boolean color,
            @RequestParam("stapled") Boolean stapled,
            @RequestParam(value = "notes", required = false) String notes,
            @RequestParam("files") List<MultipartFile> files,
            @AuthenticationPrincipal UserDetails userDetails) throws IOException {

        PrintRequestDTO request = PrintRequestDTO.builder()
                .username(userDetails.getUsername())
                .title(title)
                .copies(copies)
                .priority(PrintJobPriority.valueOf(priority))
                .doubleSided(doubleSided)
                .color(color)
                .stapled(stapled)
                .notes(notes)
                .build();

        PrintJob printJob = printService.createPrintJob(request, files);
        return ResponseEntity.ok(printJob);
    }

    @GetMapping("/status")
    public ResponseEntity<List<PrintJob>> getPrintJobs(@AuthenticationPrincipal UserDetails userDetails) {
        List<PrintJob> jobs = printService.getPrintJobsByUser(userDetails.getUsername());
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/status/{id}")
    public ResponseEntity<PrintJob> getPrintJob(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        PrintJob job = printService.getPrintJob(id, userDetails.getUsername());
        return ResponseEntity.ok(job);
    }

    @DeleteMapping("/cancel/{id}")
    public ResponseEntity<Void> cancelPrintJob(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        printService.cancelPrintJob(id, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }
} 