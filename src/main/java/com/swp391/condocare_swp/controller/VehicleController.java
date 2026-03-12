package com.swp391.condocare_swp.controller;

import com.swp391.condocare_swp.entity.Vehicle;
import com.swp391.condocare_swp.service.ResidentService;
import com.swp391.condocare_swp.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/dashboard/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;
    private final ResidentService residentService;

    @GetMapping
    public String listVehicles(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) Vehicle.VehicleStatus status,
            @RequestParam(defaultValue = "1") int pageNo,
            Model model) {

        int pageSize = 5;
        Page<Vehicle> page = vehicleService.filterVehicles(keyword, status, pageNo - 1, pageSize);

        model.addAttribute("vehicles", page.getContent());
        model.addAttribute("currentPage", pageNo);
        model.addAttribute("totalPages", page.getTotalPages());
        model.addAttribute("totalItems", page.getTotalElements());

        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);

        return "vehicle-list";
    }

    @GetMapping("/view/{id}")
    public String viewVehicle(@PathVariable("id") String id, Model model) {
        Vehicle vehicle = vehicleService.getVehicleById(id);
        if (vehicle == null) return "redirect:/dashboard/vehicles";
        model.addAttribute("vehicle", vehicle);
        return "vehicle-view";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("vehicle", new Vehicle());
        model.addAttribute("residents", residentService.getAllResidents());
        model.addAttribute("isEdit", false);
        return "vehicle-form";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") String id, Model model) {
        Vehicle vehicle = vehicleService.getVehicleById(id);
        if (vehicle == null) return "redirect:/dashboard/vehicles";

        model.addAttribute("vehicle", vehicle);
        model.addAttribute("residents", residentService.getAllResidents());
        model.addAttribute("isEdit", true);
        return "vehicle-form";
    }

    @PostMapping("/save")
    public String saveVehicle(@ModelAttribute("vehicle") Vehicle vehicle,
                              @RequestParam(value = "isEdit", defaultValue = "false") boolean isEdit,
                              Model model, RedirectAttributes redirectAttributes) {
        try {
            // Check trùng biển số
            if (vehicleService.checkDuplicateLicensePlate(vehicle.getLicensePlate(), isEdit ? vehicle.getId() : null)) {
                model.addAttribute("errorMessage", "Thất bại! Biển số xe '" + vehicle.getLicensePlate() + "' đã được đăng ký.");
                model.addAttribute("residents", residentService.getAllResidents());
                model.addAttribute("isEdit", isEdit);
                return "vehicle-form";
            }

            if (isEdit) {
                // Giữ lại ngày đăng ký gốc nếu đang edit
                Vehicle existing = vehicleService.getVehicleById(vehicle.getId());
                if(existing != null) {
                    vehicle.setRegisteredAt(existing.getRegisteredAt());
                    // Nếu sửa lại thành ACTIVE thì reset revokedAt
                    if(vehicle.getStatus() == Vehicle.VehicleStatus.ACTIVE) {
                        vehicle.setRevokedAt(null);
                    } else if (existing.getStatus() == Vehicle.VehicleStatus.ACTIVE && vehicle.getStatus() == Vehicle.VehicleStatus.INACTIVE) {
                        vehicle.setRevokedAt(java.time.LocalDateTime.now());
                    }
                }
            }

            vehicleService.saveVehicle(vehicle);
            redirectAttributes.addFlashAttribute("successMessage", isEdit ? "Cập nhật phương tiện thành công!" : "Đăng ký phương tiện mới thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi hệ thống! Vui lòng kiểm tra lại dữ liệu.");
        }
        return "redirect:/dashboard/vehicles";
    }

    @GetMapping("/revoke/{id}")
    public String revokeVehicle(@PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        try {
            vehicleService.revokeVehicle(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy đăng ký phương tiện thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi hệ thống! Không thể hủy phương tiện.");
        }
        return "redirect:/dashboard/vehicles";
    }
}