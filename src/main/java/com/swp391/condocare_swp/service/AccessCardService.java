package com.swp391.condocare_swp.service;

import com.swp391.condocare_swp.entity.AccessCard;
import com.swp391.condocare_swp.repository.AccessCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccessCardService {

    private final AccessCardRepository accessCardRepository;

    // Thay đổi tham số truyền vào của hàm filter
    public Page<AccessCard> filterAccessCards(String keyword, AccessCard.CardStatus status, String buildingId, int pageNo, int pageSize) {
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("issuedAt").descending());
        return accessCardRepository.filterAccessCards(keyword, status, buildingId, pageable);
    }

    public AccessCard getAccessCardById(String id) {
        return accessCardRepository.findById(id).orElse(null);
    }

    public void saveAccessCard(AccessCard accessCard) {
        accessCardRepository.save(accessCard);
    }

    public void deleteAccessCard(String id) {
        accessCardRepository.deleteById(id);
    }

    public boolean checkDuplicateCardNumber(String cardNumber) {
        return accessCardRepository.existsByCardNumber(cardNumber);
    }

    // Thay thế hàm deleteAccessCard cũ bằng hàm này
    public void lockAccessCard(String id) {
        AccessCard card = accessCardRepository.findById(id).orElse(null);
        if (card != null) {
            card.setStatus(AccessCard.CardStatus.BLOCKED);
            accessCardRepository.save(card);
        }
    }
}