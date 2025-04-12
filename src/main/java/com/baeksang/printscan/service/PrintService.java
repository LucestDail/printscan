package com.baeksang.printscan.service;

import com.baeksang.printscan.dto.PrintRequestDTO;
import com.baeksang.printscan.entity.PrintJob;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface PrintService {
    PrintJob createPrintJob(PrintRequestDTO request, List<MultipartFile> files) throws IOException;
    List<PrintJob> getPrintJobsByUser(String username);
    PrintJob getPrintJob(Long id, String username);
    void cancelPrintJob(Long id, String username);
} 