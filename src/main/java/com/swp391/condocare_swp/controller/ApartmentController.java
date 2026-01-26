package com.swp391.condocare_swp.controller;

import com.swp391.condocare_swp.entity.Apartment;
import com.swp391.condocare_swp.service.ApartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/apartments")
public class ApartmentController {

    @Autowired
    private ApartmentService apartmentService;

    @GetMapping
    @PreAuthorize("hasAnyRole('MGR', 'STAFF', 'SEC', 'ACC')")  // Staff groups
    public ResponseEntity<List<ApartmentDto>> getAdminApartments() {
        return ResponseEntity.ok(apartmentService.getAllApartmentsDto());
    }

    @PostMapping
    @PreAuthorize("hasRole('MGR')")  // Chỉ Manager create
    public ResponseEntity<Apartment> createApartment(@RequestBody Apartment apartment) {
        return ResponseEntity.ok(apartmentService.createApartment(apartment));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MGR', 'STAFF', 'SEC', 'ACC')")  // Staff update status, Manager full
    public ResponseEntity<Apartment> updateApartment(@PathVariable String id, @RequestBody Apartment apartment) {
        // Logic phân biệt: Nếu không phải MGR, chỉ update status/rentalStatus
        return ResponseEntity.ok(apartmentService.updateApartment(id, apartment));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MGR')")  // Chỉ Manager delete
    public ResponseEntity<Void> deleteApartment(@PathVariable String id) {
        apartmentService.deleteApartment(id);
        return ResponseEntity.ok().build();
    }
}