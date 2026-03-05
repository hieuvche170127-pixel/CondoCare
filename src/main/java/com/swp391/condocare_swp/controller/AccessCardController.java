package com.swp391.condocare_swp.controller;

import com.swp391.condocare_swp.entity.AccessCard;
import com.swp391.condocare_swp.service.AccessCardService;
import com.swp391.condocare_swp.service.BuildingService;
import com.swp391.condocare_swp.service.ResidentService;
import com.swp391.condocare_swp.service.StaffService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/dashboard/access-cards")
@RequiredArgsConstructor
public class AccessCardController {

    private final AccessCardService accessCardService;
    private final ResidentService residentService;
    private final StaffService staffService;

    // Thêm BuildingService vào controller
    private final BuildingService buildingService;

    // Sửa lại hàm listAccessCards
    @GetMapping
    public String listAccessCards(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "buildingId", required = false) String buildingId,
            @RequestParam(value = "status", required = false) AccessCard.CardStatus status,
            @RequestParam(defaultValue = "1") int pageNo,
            Model model) {

        int pageSize = 5; // Cố định 5 bản ghi / trang theo yêu cầu
        Page<AccessCard> page = accessCardService.filterAccessCards(keyword, status, buildingId, pageNo - 1, pageSize);

        model.addAttribute("accessCards", page.getContent());
        model.addAttribute("currentPage", pageNo);
        model.addAttribute("totalPages", page.getTotalPages());
        model.addAttribute("totalItems", page.getTotalElements());

        // Truyền list Tòa nhà ra view để làm Filter
        model.addAttribute("buildings", buildingService.getBuildings(null));

        // Trả lại các giá trị lọc để giữ trạng thái trên form
        model.addAttribute("keyword", keyword);
        model.addAttribute("buildingId", buildingId);
        model.addAttribute("status", status);

        return "accesscard-list";
    }

    // 2. XEM CHI TIẾT
    @GetMapping("/view/{id}")
    public String viewAccessCard(@PathVariable("id") String id, Model model) {
        AccessCard card = accessCardService.getAccessCardById(id);
        if (card == null) return "redirect:/dashboard/access-cards";
        model.addAttribute("accessCard", card);
        return "accesscard-view";
    }

    // 3. FORM THÊM MỚI
    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("accessCard", new AccessCard());
        loadFormData(model);
        model.addAttribute("isEdit", false);
        return "accesscard-form";
    }

    // 4. FORM SỬA
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") String id, Model model) {
        AccessCard card = accessCardService.getAccessCardById(id);
        if (card == null) return "redirect:/dashboard/access-cards";

        model.addAttribute("accessCard", card);
        loadFormData(model);
        model.addAttribute("isEdit", true);
        return "accesscard-form";
    }

    // 5. LƯU & CHỐNG TRÙNG LẶP CARD NUMBER
    @PostMapping("/save")
    public String saveAccessCard(@ModelAttribute("accessCard") AccessCard accessCard,
                                 @RequestParam(value = "isEdit", defaultValue = "false") boolean isEdit,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        try {
            if (!isEdit) {
                // Kiểm tra trùng mã thẻ vật lý
                if (accessCardService.checkDuplicateCardNumber(accessCard.getCardNumber())) {
                    model.addAttribute("errorMessage", "Thất bại! Mã thẻ vật lý '" + accessCard.getCardNumber() + "' đã tồn tại trên hệ thống.");
                    loadFormData(model);
                    model.addAttribute("isEdit", false);
                    return "accesscard-form";
                }
                accessCardService.saveAccessCard(accessCard);
                redirectAttributes.addFlashAttribute("successMessage", "Cấp thẻ mới thành công!");
            } else {
                // Nếu là edit, có thể cần check xem cardNumber có bị đổi sang một mã đã tồn tại không (tùy nghiệp vụ)
                accessCardService.saveAccessCard(accessCard);
                redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin thẻ thành công!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi hệ thống! Vui lòng kiểm tra lại dữ liệu.");
        }
        return "redirect:/dashboard/access-cards";
    }

    // 6. XÓA THẺ
    @GetMapping("/delete/{id}")
    public String deleteAccessCard(@PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        try {
            accessCardService.deleteAccessCard(id);
            redirectAttributes.addFlashAttribute("successMessage", "Thu hồi/Xóa thẻ thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa do thẻ đang bị ràng buộc dữ liệu!");
        }
        return "redirect:/dashboard/access-cards";
    }

    // 6. KHÓA THẺ (Thay vì xóa vật lý)
    @GetMapping("/lock/{id}")
    public String lockAccessCard(@PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        try {
            accessCardService.lockAccessCard(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã khóa thẻ thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi hệ thống! Không thể khóa thẻ.");
        }
        return "redirect:/dashboard/access-cards";
    }

    // Hàm tiện ích load dữ liệu cho dropdowns
    private void loadFormData(Model model) {
        model.addAttribute("residents", residentService.getAllResidents());
        model.addAttribute("staffs", staffService.getAllStaffs());
    }
}