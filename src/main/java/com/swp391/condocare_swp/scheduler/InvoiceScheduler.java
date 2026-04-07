package com.swp391.condocare_swp.scheduler;

import com.swp391.condocare_swp.entity.Invoice;
import com.swp391.condocare_swp.entity.Residents;
import com.swp391.condocare_swp.repository.InvoiceRepository;
import com.swp391.condocare_swp.repository.ResidentsRepository;
import com.swp391.condocare_swp.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/**
 * Scheduler tự động cập nhật hóa đơn UNPAID → OVERDUE khi quá hạn.
 * Chạy mỗi ngày lúc 00:30.
 *
 * [FIX #6] Thêm gửi notification cho cư dân khi hóa đơn chuyển sang OVERDUE.
 * Trước đây Scheduler chỉ update DB, cư dân không nhận được cảnh báo nào.
 */
@Component
public class InvoiceScheduler {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceScheduler.class);
    private static final NumberFormat VND_FORMAT =
            NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    @Autowired private InvoiceRepository    invoiceRepo;
    @Autowired private ResidentsRepository  residentRepo;
    @Autowired private NotificationService  notificationService;

    @Scheduled(cron = "0 30 0 * * *")
    @Transactional
    public void updateOverdueInvoices() {
        LocalDate today = LocalDate.now();
        List<Invoice> overdueList = invoiceRepo
                .findByStatusAndDueDateBefore(Invoice.InvoiceStatus.UNPAID, today);

        int notified = 0;
        for (Invoice inv : overdueList) {
            inv.setStatus(Invoice.InvoiceStatus.OVERDUE);
            invoiceRepo.save(inv);

            // [FIX #6] Gửi notification cho từng cư dân ACTIVE trong căn hộ
            if (inv.getApartment() != null) {
                List<Residents> residents = residentRepo.findByApartmentIdAndStatus(
                        inv.getApartment().getId(), Residents.ResidentStatus.ACTIVE);

                String amountFormatted = formatVnd(inv.getTotalAmount());

                for (Residents r : residents) {
                    try {
                        notificationService.sendPaymentReminder(
                                r,
                                inv.getId(),
                                amountFormatted,
                                null   // null = hệ thống gửi tự động, không phải staff cụ thể
                        );
                        notified++;
                    } catch (Exception e) {
                        logger.warn("Could not send overdue notification to resident {} for invoice {}: {}",
                                r.getId(), inv.getId(), e.getMessage());
                    }
                }
            }
        }

        if (!overdueList.isEmpty())
            logger.info("Scheduler: {} invoice(s) → OVERDUE, {} notification(s) sent",
                    overdueList.size(), notified);
    }

    private String formatVnd(BigDecimal amount) {
        if (amount == null) return "0 đ";
        return VND_FORMAT.format(amount) + " đ";
    }
}