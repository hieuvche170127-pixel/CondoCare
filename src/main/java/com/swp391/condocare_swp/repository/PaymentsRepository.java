package com.swp391.condocare_swp.repository;

import com.swp391.condocare_swp.entity.Payments;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentsRepository extends JpaRepository<Payments, String> {

    /** Kiểm tra invoice đã có payment record chưa (tránh lưu trùng) */
    boolean existsByInvoiceId(String invoiceId);

    /** Tìm payment theo invoiceId */
    Optional<Payments> findByInvoiceId(String invoiceId);

    /** Tìm theo MoMo transId — dùng để đối soát */
    Optional<Payments> findByMomoTransId(String momoTransId);
}