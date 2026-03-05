package com.swp391.condocare_swp.repository;

import com.swp391.condocare_swp.entity.Apartment;
import com.swp391.condocare_swp.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
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

    /** Tìm invoice UNPAID quá hạn */
    List<Invoice> findByStatusAndDueDateBefore(
            Invoice.InvoiceStatus status,
            LocalDate date
    );

    // ── Filter theo thời gian ─────────────────────────────────────────────
    // Invoice dùng field month + year (Integer) thay vì createdAt
    // nên dùng derived query của Spring Data JPA thay vì @Query YEAR()/MONTH()

    /** Lọc theo tháng + năm — NOTE: trả về List vì có thể có nhiều loại phí trong cùng tháng */
    List<Invoice> findByApartmentAndMonthAndYearOrderByYearDescMonthDesc(
            Apartment apartment, Integer month, Integer year);

    /** Lọc theo năm */
    List<Invoice> findByApartmentAndYearOrderByYearDescMonthDesc(
            Apartment apartment, Integer year);

    /** Lọc theo quý + năm: tháng nằm trong khoảng [monthFrom, monthTo] */
    @Query("SELECT i FROM Invoice i WHERE i.apartment = :apartment " +
            "AND i.year = :year " +
            "AND i.month BETWEEN :monthFrom AND :monthTo " +
            "ORDER BY i.year DESC, i.month DESC")
    List<Invoice> findByApartmentAndQuarterYear(
            @Param("apartment") Apartment apartment,
            @Param("monthFrom") int monthFrom,
            @Param("monthTo")   int monthTo,
            @Param("year")      int year);

    /** Tìm kiếm theo keyword — tìm trong các fee liên quan nếu có trường name,
     *  hoặc fallback tìm theo status string. Tuỳ entity Invoice có field nào.
     *  Ở đây search theo note hoặc createdBy name nếu có. */
    @Query("SELECT i FROM Invoice i WHERE i.apartment = :apartment " +
            "AND (LOWER(i.createdBy.fullName) LIKE LOWER(CONCAT('%',:keyword,'%')) " +
            "OR LOWER(i.status) LIKE LOWER(CONCAT('%',:keyword,'%'))) " +
            "ORDER BY i.year DESC, i.month DESC")
    List<Invoice> searchByKeyword(
            @Param("apartment") Apartment apartment,
            @Param("keyword")   String keyword);
}