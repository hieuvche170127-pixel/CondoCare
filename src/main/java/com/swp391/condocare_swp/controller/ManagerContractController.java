package com.swp391.condocare_swp.controller;

import com.swp391.condocare_swp.entity.Apartment;
import com.swp391.condocare_swp.entity.Contract;
import com.swp391.condocare_swp.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/dashboard/contracts")
@RequiredArgsConstructor
public class ManagerContractController {

    private final ContractService contractService;
    private final ApartmentService apartmentService;
    private final ResidentService residentService;
    private final StaffService staffService;
    private final BuildingService buildingService;

    private String getCurrentManagerId() { return "S08"; } // Giả lập ID

    @GetMapping("/api/apartments")
    @ResponseBody
    public List<Apartment> getApartmentsByBuilding(@RequestParam String buildingId) {
        // Đảm bảo ApartmentService của bạn có hàm này: apartmentRepository.findByBuildingId(buildingId)
        return apartmentService.getApartmentsByBuilding(buildingId);
    }

    @GetMapping
    public String listContracts(@RequestParam(value = "apartmentId", required = false) String apartmentId,
                                @RequestParam(defaultValue = "1") int pageNo, Model model) {
        Page<Contract> page = contractService.getContractsForManager(getCurrentManagerId(), apartmentId, pageNo - 1, 5);

        model.addAttribute("contracts", page.getContent());
        model.addAttribute("currentPage", pageNo);
        model.addAttribute("totalPages", page.getTotalPages());
        model.addAttribute("apartmentId", apartmentId);
        return "contract-list";
    }

    @GetMapping("/view/{id}")
    public String viewContract(@PathVariable("id") String id, Model model) {
        Contract contract = contractService.getContractById(id);
        if (contract == null) return "redirect:/dashboard/contracts";
        model.addAttribute("contract", contract);
        return "contract-view";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        Contract contract = new Contract();
        contract.setApartment(new Apartment());
        model.addAttribute("contract", new Contract());
        model.addAttribute("isEdit", false);
        loadFormData(model);
        return "contract-form";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") String id, Model model, RedirectAttributes redirectAttributes) {
        Contract contract = contractService.getContractById(id);
        if (contract == null || contract.getStatus() != Contract.ContractStatus.DRAFT) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể sửa hợp đồng này!");
            return "redirect:/dashboard/contracts";
        }
        model.addAttribute("contract", contract);
        model.addAttribute("isEdit", true);
        loadFormData(model);
        return "contract-form";
    }

    @PostMapping("/save")
    public String saveContract(@ModelAttribute("contract") Contract contract,
                               @RequestParam("isEdit") boolean isEdit, RedirectAttributes redirectAttributes) {
        try {
            if (!isEdit) {
                contractService.createContract(contract);
                redirectAttributes.addFlashAttribute("successMessage", "Tạo bản Nháp thành công!");
            } else {
                contractService.updateContract(contract);
                redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thành công!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/dashboard/contracts";
    }

    @GetMapping("/send/{id}")
    public String sendContract(@PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        try {
            contractService.sendContract(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã gửi hợp đồng cho cư dân!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/dashboard/contracts";
    }

    // Hàm phụ trợ load danh sách cho form (Tòa nhà, Căn hộ, Cư dân, NV)
    // Hàm phụ trợ load danh sách cho form (Tòa nhà, Căn hộ, Cư dân, NV)
    private void loadFormData(Model model) {
        model.addAttribute("buildings", buildingService.getBuildings(null));

        // SỬA LẠI DÒNG NÀY: Truyền đúng 3 tham số (từ khóa, trang, số lượng)
        model.addAttribute("allApartments", apartmentService.getApartmentsPaginated(null, 0, 1000).getContent());

        model.addAttribute("residents", residentService.getAllResidents());
        model.addAttribute("staffs", staffService.getAllStaffs());
    }
}
