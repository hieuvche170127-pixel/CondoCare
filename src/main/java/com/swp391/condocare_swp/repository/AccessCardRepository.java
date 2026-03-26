package com.swp391.condocare_swp.repository;

import com.swp391.condocare_swp.entity.AccessCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccessCardRepository extends JpaRepository<AccessCard, String> {

    List<AccessCard> findByResidentId(String residentId);

    Optional<AccessCard> findByCardNumber(String cardNumber);

    boolean existsByCardNumber(String cardNumber);

    long countByResidentIdAndStatus(String residentId, AccessCard.CardStatus status);
}