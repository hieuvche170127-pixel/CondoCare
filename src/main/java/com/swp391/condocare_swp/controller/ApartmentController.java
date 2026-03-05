package com.swp391.condocare_swp.controller;


import com.swp391.condocare_swp.entity.Apartment;
import com.swp391.condocare_swp.entity.Building;
import com.swp391.condocare_swp.service.ApartmentService;
import com.swp391.condocare_swp.service.BuildingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class ApartmentController {

    private final ApartmentService apartmentService;
    private final BuildingService buildingService; // Dùng để load danh sách Tòa nhà vào form

    // 1. DANH SÁCH CĂN HỘ
    // Sửa lại hàm số 1 (DANH SÁCH CĂN HỘ) như sau:
    @GetMapping("/apartments")
    public String listApartments(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "buildingId", required = false) String buildingId,
            @RequestParam(value = "status", required = false) Apartment.ApartmentStatus status,
            @RequestParam(value = "rentalStatus", required = false) Apartment.RentalStatus rentalStatus,
            @RequestParam(defaultValue = "1") int pageNo,
            Model model) {

        int pageSize = 5; // Hiển thị 5 record/trang

        // Truyền tất cả filter xuống Service
        Page<Apartment> page = apartmentService.filterApartments(keyword, buildingId, status, rentalStatus, pageNo - 1, pageSize);

        model.addAttribute("apartments", page.getContent());
        model.addAttribute("currentPage", pageNo);
        model.addAttribute("totalPages", page.getTotalPages());
        model.addAttribute("totalItems", page.getTotalElements());

        // Đẩy danh sách tòa nhà ra để làm Filter Dropdown
        model.addAttribute("buildings", buildingService.getBuildings(null));

        // Trả lại các giá trị đang lọc để giao diện giữ trạng thái
        model.addAttribute("keyword", keyword);
        model.addAttribute("buildingId", buildingId);
        model.addAttribute("status", status);
        model.addAttribute("rentalStatus", rentalStatus);

        return "apartment-list";
    }

    // 2. XEM CHI TIẾT
    @GetMapping("/apartments/view/{id}")
    public String viewApartment(@PathVariable("id") String id, Model model) {
        Apartment apt = apartmentService.getApartmentById(id);
        if (apt == null) return "redirect:/dashboard/apartments";
        model.addAttribute("apartment", apt);
        return "apartment-detail";
    }

    // 3. FORM THÊM MỚI
    @GetMapping("/apartments/add")
    public String showAddForm(Model model) {
        model.addAttribute("apartment", new Apartment());
        model.addAttribute("buildings", buildingService.getBuildings(null)); // Load list tòa nhà
        model.addAttribute("isEdit", false);
        return "apartment-form";
    }

    // 4. FORM SỬA
    @GetMapping("/apartments/edit/{id}")
    public String showEditForm(@PathVariable("id") String id, Model model) {
        Apartment apt = apartmentService.getApartmentById(id);
        if (apt == null) return "redirect:/dashboard/apartments";

        model.addAttribute("apartment", apt);
        model.addAttribute("buildings", buildingService.getBuildings(null));
        model.addAttribute("isEdit", true);
        return "apartment-form";
    }

    // 5. LƯU (CREATE & UPDATE) - CHỐNG TRÙNG LẶP
    @PostMapping("/apartments/save")
    public String saveApartment(@ModelAttribute("apartment") Apartment apartment,
                                @RequestParam(value = "isEdit", defaultValue = "false") boolean isEdit,
                                Model model, // Phải có model để trả lỗi về form
                                RedirectAttributes redirectAttributes) {
        try {
            if (!isEdit) {
                // KIỂM TRA TRÙNG LẶP KHI THÊM MỚI
                boolean isDuplicate = apartmentService.checkDuplicateApartment(
                        apartment.getBuilding().getId(),
                        apartment.getFloor(),
                        apartment.getNumber()
                );

                if (isDuplicate) {
                    // Nếu trùng -> Giữ lại data, hiện thông báo lỗi
                    model.addAttribute("errorMessage", "Thất bại! Căn hộ số " + apartment.getNumber() +
                            " tại Tầng " + apartment.getFloor() + " của Tòa nhà này đã tồn tại.");
                    model.addAttribute("buildings", buildingService.getBuildings(null));
                    model.addAttribute("isEdit", false);
                    return "apartment-form"; // Quay lại form ngay lập tức
                }

                // Không trùng thì lưu bình thường
                apartmentService.saveApartment(apartment);
                redirectAttributes.addFlashAttribute("successMessage", "Thêm mới căn hộ thành công!");
            } else {
                // KHI CHỈNH SỬA
                apartmentService.saveApartment(apartment);
                redirectAttributes.addFlashAttribute("successMessage", "Cập nhật căn hộ thành công!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi hệ thống! Vui lòng kiểm tra lại dữ liệu.");
        }
        return "redirect:/dashboard/apartments";
    }

    // 6. XÓA CĂN HỘ
    @GetMapping("/apartments/delete/{id}")
    public String deleteApartment(@PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        try {
            apartmentService.deleteApartment(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa căn hộ thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa do căn hộ đang chứa dữ liệu cư dân/hợp đồng!");
        }
        return "redirect:/dashboard/apartments";
    }
}