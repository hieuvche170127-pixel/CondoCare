package com.swp391.condocare_swp.service;

import com.swp391.condocare_swp.entity.Contract;
import com.swp391.condocare_swp.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ContractScheduler {

    private final ContractRepository contractRepository;

    @Scheduled(cron = "0 0 0 * * ?") // Chạy lúc 00:00 mỗi ngày
    @Transactional
    public void autoTerminateExpiredContracts() {
        LocalDate today = LocalDate.now();
        List<Contract> expiredContracts = contractRepository
                .findByStatusAndEndDateBefore(Contract.ContractStatus.ACTIVE, today);

        if (!expiredContracts.isEmpty()) {
            for (Contract contract : expiredContracts) {
                contract.setStatus(Contract.ContractStatus.TERMINATED);
            }
            contractRepository.saveAll(expiredContracts);
        }
    }
}