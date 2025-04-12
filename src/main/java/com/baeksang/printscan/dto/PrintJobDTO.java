package com.baeksang.printscan.dto;

import com.baeksang.printscan.entity.PrintJob;
import com.baeksang.printscan.entity.PrintJobPriority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrintJobDTO {
    private Long id;
    private String title;
    private String department;
    private Integer copies;
    private PrintJobPriority priority;
    private String status;
    private Boolean doubleSided;
    private Boolean color;
    private Boolean stapled;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private List<PrintJobFileDTO> files;

    public static PrintJobDTO fromEntity(PrintJob entity) {
        return PrintJobDTO.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .department(entity.getUser().getDepartment())
                .copies(entity.getCopies())
                .priority(entity.getPriority())
                .status(entity.getStatus().name())
                .doubleSided(entity.getDoubleSided())
                .color(entity.getColor())
                .stapled(entity.getStapled())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .startedAt(entity.getStartedAt())
                .completedAt(entity.getCompletedAt())
                .files(entity.getFiles().stream()
                        .map(PrintJobFileDTO::fromEntity)
                        .collect(Collectors.toList()))
                .build();
    }
}