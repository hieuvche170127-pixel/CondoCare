package com.swp391.condocare_swp.repository;

import com.swp391.condocare_swp.entity.Apartment;
import com.swp391.condocare_swp.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, String> {

    /** Tất cả hóa đơn của 1 căn hộ, mới nhất trước */
    List<Invoice> findByApartmentOrderByYearDescMonthDesc(Apartment apartment);

    /** Hóa đơn của 1 tháng cụ thể */
    Optional<Invoice> findByApartmentAndMonthAndYear(Apartment apartment, Integer month, Integer year);

    /** Hóa đơn chưa thanh toán của 1 căn hộ */
    List<Invoice> findByApartmentAndStatus(Apartment apartment, Invoice.InvoiceStatus status);
}
