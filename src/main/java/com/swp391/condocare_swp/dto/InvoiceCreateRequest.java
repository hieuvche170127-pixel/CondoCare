package com.swp391.condocare_swp.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class InvoiceCreateRequest {

    @NotBlank(message = "Căn hộ không được để trống")
    private String apartmentId;

    @NotNull(message = "Tháng không được để trống")
    @Min(value = 1, message = "Tháng phải từ 1-12")
    @Max(value = 12, message = "Tháng phải từ 1-12")
    private Integer month;

    @NotNull(message = "Năm không được để trống")
    @Min(value = 2020, message = "Năm không hợp lệ")
    private Integer year;

    // Optional - có thể null nếu không có điện/nước/fee
    private String electricReadingId;
    private String waterReadingId;
    private String serviceFeeId;

    /**
     * Danh sách ID các phí gửi xe (xe máy, ô tô, xe đạp điện...).
     * Thay thế parkingFeeId đơn lẻ — Service sẽ cộng gộp thành 1 tổng parkingAmount
     * và lưu FK đại diện là phần tử đầu tiên (nếu có).
     */
    private List<String> parkingFeeIds;
}