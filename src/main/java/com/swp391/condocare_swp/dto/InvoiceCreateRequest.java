package com.swp391.condocare_swp.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Request tạo hóa đơn theo Mô hình B.
 * Hệ thống tự tính phí từ FeeTemplate — Staff chỉ cần cung cấp căn hộ + tháng/năm.
 */
@Data
public class InvoiceCreateRequest {

    @NotBlank(message = "Căn hộ không được để trống.")
    private String apartmentId;

    @NotNull(message = "Tháng không được để trống.")
    @Min(value = 1, message = "Tháng phải từ 1 đến 12.")
    @Max(value = 12, message = "Tháng phải từ 1 đến 12.")
    private Integer month;

    @NotNull(message = "Năm không được để trống.")
    @Min(value = 2020, message = "Năm không hợp lệ.")
    private Integer year;
}