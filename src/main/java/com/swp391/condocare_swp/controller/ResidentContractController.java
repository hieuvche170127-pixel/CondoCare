package com.swp391.condocare_swp.controller;

import com.swp391.condocare_swp.entity.Contract;
import com.swp391.condocare_swp.service.ContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/resident-dashboard/contracts")
@RequiredArgsConstructor
public class ResidentContractController {

    private final ContractService contractService;

    private String getCurrentResidentId() { return "RES02"; } // Giả lập ID

    @GetMapping
    public String listResidentContracts(@RequestParam(defaultValue = "1") int pageNo, Model model) {
        Page<Contract> page = contractService.getContractsForResident(getCurrentResidentId(), pageNo - 1, 5);
        model.addAttribute("contracts", page.getContent());
        model.addAttribute("currentPage", pageNo);
        model.addAttribute("totalPages", page.getTotalPages());
        return "resident-contract-list";
    }

    @GetMapping("/view/{id}")
    public String viewResidentContract(@PathVariable("id") String id, Model model) {
        Contract contract = contractService.getContractById(id);
        if (contract == null || !contract.getResident().getId().equals(getCurrentResidentId())) {
            return "redirect:/resident-dashboard/contracts";
        }
        model.addAttribute("contract", contract);
        return "resident-contract-view";
    }

    @PostMapping("/respond")
    public String respondContract(@RequestParam("contractId") String contractId,
                                  @RequestParam("action") String action,
                                  RedirectAttributes redirectAttributes) {
        boolean isAccepted = "CONFIRM".equalsIgnoreCase(action);
        boolean success = contractService.respondContract(contractId, getCurrentResidentId(), isAccepted);

        if (success) {
            redirectAttributes.addFlashAttribute("successMessage", isAccepted ? "Đã Ký hợp đồng!" : "Đã Từ chối hợp đồng!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Thao tác thất bại!");
        }
        return "redirect:/resident-dashboard/contracts";
    }
}
