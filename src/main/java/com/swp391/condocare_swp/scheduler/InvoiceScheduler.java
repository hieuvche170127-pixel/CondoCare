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

@Component
public class InvoiceScheduler {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceScheduler.class);

    @Autowired
    private InvoiceRepository invoiceRepository;

    /**
     * Chạy mỗi ngày lúc 00:30 AM
     * Cập nhật status UNPAID → OVERDUE nếu quá hạn
     */
    @Scheduled(cron = "0 30 0 * * *")
    @Transactional
    public void updateOverdueInvoices() {
        logger.info("Running scheduled job: Update overdue invoices");

        LocalDate today = LocalDate.now();

        // Tìm tất cả invoice UNPAID và quá due_date
        List<Invoice> unpaidInvoices = invoiceRepository
                .findByStatusAndDueDateBefore(Invoice.InvoiceStatus.UNPAID, today);

        logger.info("Found {} overdue invoices", unpaidInvoices.size());

        for (Invoice invoice : unpaidInvoices) {
            invoice.setStatus(Invoice.InvoiceStatus.OVERDUE);
            invoiceRepository.save(invoice);
            logger.info("Updated invoice {} to OVERDUE", invoice.getId());
        }

        logger.info("Completed updating overdue invoices");
    }
}