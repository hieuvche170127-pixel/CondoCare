package com.swp391.condocare_swp.repository;

import com.swp391.condocare_swp.entity.Apartment;
import com.swp391.condocare_swp.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository
        extends JpaRepository<Invoice, String>, JpaSpecificationExecutor<Invoice> {

    List<Invoice> findByApartmentOrderByYearDescMonthDesc(Apartment apartment);

    Optional<Invoice> findByApartmentAndMonthAndYear(
            Apartment apartment, Integer month, Integer year);

    List<Invoice> findByApartmentAndStatus(
            Apartment apartment, Invoice.InvoiceStatus status);

    /** Dùng cho scheduler: tìm UNPAID đã quá hạn */
    List<Invoice> findByStatusAndDueDateBefore(
            Invoice.InvoiceStatus status, LocalDate date);

    List<Invoice> findAllByOrderByYearDescMonthDesc();

    long countByStatus(Invoice.InvoiceStatus status);
}