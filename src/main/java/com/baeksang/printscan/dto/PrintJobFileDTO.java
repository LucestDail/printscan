package com.baeksang.printscan.dto;

import com.baeksang.printscan.entity.PrintJobFile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrintJobFileDTO {
    private Long id;
    private String originalFilename;
    private String storedFilename;
    private Long fileSize;

    public static PrintJobFileDTO fromEntity(PrintJobFile entity) {
        return PrintJobFileDTO.builder()
                .id(entity.getId())
                .originalFilename(entity.getOriginalFilename())
                .storedFilename(entity.getStoredFilename())
                .fileSize(entity.getFileSize())
                .build();
    }
} 