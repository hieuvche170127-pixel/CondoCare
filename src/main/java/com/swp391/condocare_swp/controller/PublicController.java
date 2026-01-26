package com.swp391.condocare_swp.controller;

import com.swp391.condocare_swp.entity.Building;
import com.swp391.condocare_swp.service.ApartmentService;
import com.swp391.condocare_swp.service.BuildingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public")
public class PublicController {

    @Autowired
    private BuildingService buildingService;

    @Autowired
    private ApartmentService apartmentService;

    @GetMapping("/buildings")
    public ResponseEntity<List<Building>> getAllBuildings() {
        return ResponseEntity.ok(buildingService.getAllBuildings());
    }

    @GetMapping("/apartments")
    public ResponseEntity<List<ApartmentDto>> getAllApartments() {
        return ResponseEntity.ok(apartmentService.getAllApartmentsDto());
    }
}