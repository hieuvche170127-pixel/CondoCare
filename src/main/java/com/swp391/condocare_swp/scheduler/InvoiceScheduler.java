package com.swp391.condocare_swp.scheduler;

import com.swp391.condocare_swp.entity.Invoice;
import com.swp391.condocare_swp.repository.InvoiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Scheduler tự động cập nhật hóa đơn UNPAID → OVERDUE khi quá hạn.
 * Chạy mỗi ngày lúc 00:30.
 */
@Component
public class InvoiceScheduler {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceScheduler.class);

    @Autowired private InvoiceRepository invoiceRepo;

    @Scheduled(cron = "0 30 0 * * *")
    @Transactional
    public void updateOverdueInvoices() {
        LocalDate today = LocalDate.now();
        List<Invoice> overdue = invoiceRepo
                .findByStatusAndDueDateBefore(Invoice.InvoiceStatus.UNPAID, today);

        overdue.forEach(inv -> {
            inv.setStatus(Invoice.InvoiceStatus.OVERDUE);
            invoiceRepo.save(inv);
        });

        if (!overdue.isEmpty())
            logger.info("Scheduler: {} invoice(s) → OVERDUE", overdue.size());
    }
}