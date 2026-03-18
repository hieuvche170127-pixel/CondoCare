package com.swp391.condocare_swp.controller;

import com.swp391.condocare_swp.dto.ResidentCreateRequest;
import com.swp391.condocare_swp.dto.ResidentUpdateRequest;
import com.swp391.condocare_swp.service.ResidentManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/resident-management")
public class ResidentManagementController {

    private static final Logger logger = LoggerFactory.getLogger(ResidentManagementController.class);
    @Autowired private ResidentManagementService service;

    /** GET /api/resident-management/stats */
    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        try { return ResponseEntity.ok(service.getStats()); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    /** GET /api/resident-management?page=0&size=10&search=&type=&status=&apartmentId= */
    @GetMapping
    public ResponseEntity<?> listResidents(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String apartmentId,
            @RequestParam(defaultValue = "fullName") String sort,
            @RequestParam(defaultValue = "asc") String direction) {
        try {
            Sort.Direction dir = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
            PageRequest pageable = PageRequest.of(page, size, Sort.by(dir, sort));
            return ResponseEntity.ok(service.listResidents(search, type, status, apartmentId, pageable));
        } catch (Exception e) {
            logger.error("Error listing residents", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** GET /api/resident-management/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<?> getResident(@PathVariable String id) {
        try { return ResponseEntity.ok(service.getResidentDetail(id)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    /** POST /api/resident-management */
    @PostMapping
    public ResponseEntity<?> createResident(@Valid @RequestBody ResidentCreateRequest request) {
        try { return ResponseEntity.ok(service.createResident(request)); }
        catch (Exception e) {
            logger.error("Error creating resident", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** PUT /api/resident-management/{id} */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateResident(@PathVariable String id,
                                            @Valid @RequestBody ResidentUpdateRequest request) {
        try { return ResponseEntity.ok(service.updateResident(id, request)); }
        catch (Exception e) {
            logger.error("Error updating resident {}", id, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** DELETE /api/resident-management/{id} → soft delete (INACTIVE) */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deactivateResident(@PathVariable String id) {
        try { return ResponseEntity.ok(service.deactivateResident(id)); }
        catch (Exception e) {
            logger.error("Error deactivating resident {}", id, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}