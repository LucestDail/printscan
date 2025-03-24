package com.baeksang.printscan.model;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class Product {
    private String id;
    private String producerId;
    private String qrCode;
    private LocalDateTime producedAt;
    private String status;
}
