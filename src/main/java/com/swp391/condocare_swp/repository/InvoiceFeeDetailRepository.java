package com.swp391.condocare_swp.repository;

import com.swp391.condocare_swp.entity.InvoiceFeeDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceFeeDetailRepository extends JpaRepository<InvoiceFeeDetail, String> {

    List<InvoiceFeeDetail> findByInvoiceId(String invoiceId);
}