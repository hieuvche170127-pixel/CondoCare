package com.swp391.condocare_swp.controller;


import com.swp391.condocare_swp.entity.Apartment;
import com.swp391.condocare_swp.entity.Building;
import com.swp391.condocare_swp.service.ApartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class ApartmentController {

    private final ApartmentService apartmentService;

    @GetMapping("/apartments")
    public String listApartments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        String hardcodedBuildingId = "B001";

        Page<Apartment> apartmentPage = apartmentService.getApartmentsByBuilding(hardcodedBuildingId, page, size);
        Building dummyBuilding = new Building();
        dummyBuilding.setId(hardcodedBuildingId);
        dummyBuilding.setName("Tòa nhà CondoCare 001");

        model.addAttribute("building", dummyBuilding);
        model.addAttribute("buildingId", hardcodedBuildingId);
        model.addAttribute("apartments", apartmentPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", apartmentPage.getTotalPages());

        return "apartment-list";
    }
}
