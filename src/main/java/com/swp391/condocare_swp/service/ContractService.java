package com.swp391.condocare_swp.service;

import com.swp391.condocare_swp.entity.Contract;
import com.swp391.condocare_swp.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContractService {

    private final ContractRepository contractRepository;

    public Page<Contract> getContractsForManager(String managerId, String apartmentId, int pageNo, int pageSize) {
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("createdAt").descending());
        return contractRepository.findContractsForManager(managerId, apartmentId, pageable);
    }

    public Page<Contract> getContractsForResident(String residentId, int pageNo, int pageSize) {
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("createdAt").descending());
        return contractRepository.findContractsForResident(residentId, pageable);
    }

    public Contract getContractById(String id) {
        return contractRepository.findById(id).orElse(null);
    }

    public void createContract(Contract contract) {
        contract.setStatus(Contract.ContractStatus.DRAFT);
        contractRepository.save(contract);
    }

    public void updateContract(Contract updatedContract) throws Exception {
        Contract existing = contractRepository.findById(updatedContract.getId()).orElseThrow();
        if (existing.getStatus() != Contract.ContractStatus.DRAFT) {
            throw new Exception("Hợp đồng đã gửi, không thể chỉnh sửa!");
        }
        contractRepository.save(updatedContract);
    }

    public void sendContract(String id) throws Exception {
        Contract contract = contractRepository.findById(id).orElseThrow();
        if (contract.getStatus() == Contract.ContractStatus.DRAFT) {
            contract.setStatus(Contract.ContractStatus.PENDING);
            contractRepository.save(contract);
        } else {
            throw new Exception("Chỉ bản nháp mới có thể gửi đi!");
        }
    }

    public boolean respondContract(String contractId, String residentId, boolean isAccepted) {
        Contract contract = contractRepository.findById(contractId).orElse(null);
        if (contract != null && contract.getResident().getId().equals(residentId)
                && contract.getStatus() == Contract.ContractStatus.PENDING) {

            contract.setStatus(isAccepted ? Contract.ContractStatus.ACTIVE : Contract.ContractStatus.REJECT);
            contractRepository.save(contract);
            return true;
        }
        return false;
    }
}
