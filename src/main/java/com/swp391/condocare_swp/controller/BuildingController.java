package com.swp391.condocare_swp.controller;

import com.swp391.condocare_swp.entity.Building;
import com.swp391.condocare_swp.service.BuildingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class BuildingController {

    private final BuildingService buildingService;

    @GetMapping("/buildings")
    public String showBuildingList (
    // @RequestParam bắt giá trị name="keyword" từ thẻ input HTML
    // required = false nghĩa là lúc mới vào trang không có keyword cũng không báo lỗi
    @RequestParam(value = "keyword", required = false) String keyword,
    Model model) {

        List<Building> buildings = buildingService.getBuildings(keyword);

//        List<Building> buildings = buildingService.getAllBuildingList();

        model.addAttribute("buildings", buildings);
        model.addAttribute("keyword", keyword);
        return "building-list";
    }

}
