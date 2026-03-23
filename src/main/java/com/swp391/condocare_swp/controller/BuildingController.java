package com.swp391.condocare_swp.controller;

import com.swp391.condocare_swp.entity.Building;
import com.swp391.condocare_swp.service.BuildingService;
import com.swp391.condocare_swp.service.StaffService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class BuildingController {

    private final BuildingService buildingService;
    private final StaffService staffService;

    @GetMapping("/buildings")
    public String showBuildingList (
    // @RequestParam bắt giá trị name="keyword" từ thẻ input HTML
    // required = false nghĩa là lúc mới vào trang không có keyword cũng không báo lỗi
    @RequestParam(value = "keyword", required = false) String keyword,
    @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
    @RequestParam(value = "pagesize", defaultValue = "5") int pageSizeFe,
    Model model) {

        int pageSize= pageSizeFe;

        Page<Building> page =buildingService.getBuildingsPaginated(keyword, pageNo, pageSize);
//        List<Building> buildings = buildingService.getBuildings(keyword);

        model.addAttribute("buildings", page.getContent());
        model.addAttribute("currentPage", pageNo);
        model.addAttribute("totalPages", page.getTotalPages());
        model.addAttribute("totalItems", page.getTotalElements());
        model.addAttribute("keyword", keyword);
        model.addAttribute("pageSize",pageSize );
        return "building-list";
    }
    @GetMapping("/buildings/view/{id}")
    public String viewBuildingDetail(@PathVariable("id") String id, Model model) {

        // Gọi Service lấy thông tin tòa nhà theo ID
        Building building = buildingService.getBuildingById(id);
        if (building == null) return "redirect:/dashboard/buildings";

        // Gửi data tòa nhà sang giao diện HTML
        model.addAttribute("building", building);

        // Trả về file giao diện building-detail.html
        return "building-detail";
    }


    @GetMapping("/buildings/edit/{id}")
    public String showEditForm(@PathVariable("id") String id, Model model) {
        Building building = buildingService.getBuildingById(id);
        if (building == null) return "redirect:/dashboard/buildings";

        model.addAttribute("building", building);
        model.addAttribute("managers", staffService.getAllStaffs());
        model.addAttribute("isEdit", true); // Đánh dấu đây là CHỈNH SỬA
        return "building-form";
    }

    @GetMapping("/buildings/add")
    public String showAddForm(Model model) {
        model.addAttribute("building", new Building());
        model.addAttribute("managers", staffService.getAllStaffs());
        model.addAttribute("isEdit", false); // Đánh dấu đây là THÊM MỚI
        return "building-form";
    }

    @PostMapping("/buildings/save")
    public String saveBuilding(@ModelAttribute("building") Building building,
                               @RequestParam(value = "isEdit", defaultValue = "false") boolean isEdit,
                               Model model, // Thêm Model để đẩy lỗi ra form
                               RedirectAttributes redirectAttributes) {
        try {
            if (!isEdit) {
                // Nếu an toàn, tiến hành lưu
                buildingService.saveBuilding(building);
                redirectAttributes.addFlashAttribute("successMessage", "Thêm mới Tòa nhà thành công!");
            } else {
                // NẾU LÀ CHỈNH SỬA: Cho phép ghi đè bình thường
                buildingService.saveBuilding(building);
                redirectAttributes.addFlashAttribute("successMessage", "Cập nhật Tòa nhà thành công!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lưu thất bại! Vui lòng kiểm tra lại dữ liệu.");
        }
        return "redirect:/dashboard/buildings";
    }

    // 6. Chuyển đổi trạng thái (Khóa <-> Kích hoạt)
    @GetMapping("/buildings/toggle-status/{id}")
    public String toggleBuildingStatus(@PathVariable("id") String id,
                                       RedirectAttributes redirectAttributes) {
        try {
            buildingService.toggleBuildingStatus(id);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái Tòa nhà thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Cập nhật trạng thái thất bại!");
        }
        return "redirect:/dashboard/buildings";
    }

}
