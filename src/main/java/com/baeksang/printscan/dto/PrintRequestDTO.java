package com.baeksang.printscan.dto;

import com.baeksang.printscan.entity.PrintJobPriority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrintRequestDTO {
    private String username;
    private String title;
    private Integer copies;
    private PrintJobPriority priority;
    private Boolean doubleSided;
    private Boolean color;
    private Boolean stapled;
    private String notes;
} 