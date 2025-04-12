package com.baeksang.printscan.controller;

import com.baeksang.printscan.dto.PrintJobDTO;
import com.baeksang.printscan.entity.PrintJob;
import com.baeksang.printscan.service.PrintService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/print-jobs")
@RequiredArgsConstructor
public class PrintJobController {

    private final PrintService printService;

    @GetMapping
    public ResponseEntity<List<PrintJobDTO>> getPrintJobs(@AuthenticationPrincipal UserDetails userDetails) {
        List<PrintJob> jobs = printService.getPrintJobsByUser(userDetails.getUsername());
        List<PrintJobDTO> jobDTOs = jobs.stream()
            .map(PrintJobDTO::fromEntity)
            .collect(Collectors.toList());
        return ResponseEntity.ok(jobDTOs);
    }

    @GetMapping("/status/{id}")
    public ResponseEntity<PrintJobDTO> getPrintJob(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        PrintJob job = printService.getPrintJob(id, userDetails.getUsername());
        return ResponseEntity.ok(PrintJobDTO.fromEntity(job));
    }

    @DeleteMapping("/cancel/{id}")
    public ResponseEntity<Void> cancelPrintJob(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        printService.cancelPrintJob(id, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }
} 