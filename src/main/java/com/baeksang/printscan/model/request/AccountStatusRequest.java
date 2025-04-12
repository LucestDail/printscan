package com.baeksang.printscan.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountStatusRequest {
    
    @NotBlank(message = "상태는 필수입니다")
    private String status;  // ACTIVE, INACTIVE, LOCKED, DELETED
    
    private String reason;  // 상태 변경 사유 (선택사항)
} 