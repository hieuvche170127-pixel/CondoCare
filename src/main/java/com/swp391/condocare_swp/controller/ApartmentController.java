package com.swp391.condocare_swp.controller;

import com.swp391.condocare_swp.service.ApartmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller quản lý Tòa nhà (Building) và Căn hộ (Apartment).
 *
 * Building endpoints: /api/buildings/**
 * Apartment endpoints: /api/apartments/**
 */
@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
public class ApartmentController {

    private static final Logger logger = LoggerFactory.getLogger(ApartmentController.class);
    @Autowired private ApartmentService service;

    // ── STATS ─────────────────────────────────────────────────────────────────

    /** GET /api/apartments/stats */
    @GetMapping("/api/apartments/stats")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    public ResponseEntity<?> getStats() {
        try { return ResponseEntity.ok(service.getStats()); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    // ── BUILDING ──────────────────────────────────────────────────────────────

    /** GET /api/buildings */
    @GetMapping("/api/buildings")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    public ResponseEntity<?> getAllBuildings() {
        try { return ResponseEntity.ok(service.getAllBuildings()); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    /** GET /api/buildings/{id} */
    @GetMapping("/api/buildings/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    public ResponseEntity<?> getBuildingDetail(@PathVariable String id) {
        try { return ResponseEntity.ok(service.getBuildingDetail(id)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    /**
     * POST /api/buildings
     * Body: { name, address, totalFloors, totalApartments, managerId }
     */
    @PostMapping("/api/buildings")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> createBuilding(@RequestBody Map<String, String> body) {
        try { return ResponseEntity.ok(service.createBuilding(body)); }
        catch (Exception e) {
            logger.error("Error creating building", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * PUT /api/buildings/{id}
     * Body: { name?, address?, totalFloors?, totalApartments?, managerId? }
     */
    @PutMapping("/api/buildings/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> updateBuilding(@PathVariable String id,
                                            @RequestBody Map<String, String> body) {
        try { return ResponseEntity.ok(service.updateBuilding(id, body)); }
        catch (Exception e) {
            logger.error("Error updating building {}", id, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ── APARTMENT ─────────────────────────────────────────────────────────────

    /**
     * GET /api/apartments
     * Params: buildingId (optional), status (optional)
     */
    @GetMapping("/api/apartments")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    public ResponseEntity<?> getAllApartments(
            @RequestParam(required = false) String buildingId,
            @RequestParam(required = false) String status) {
        try { return ResponseEntity.ok(service.getAllApartments(buildingId, status)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    /** GET /api/apartments/{id} */
    @GetMapping("/api/apartments/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    public ResponseEntity<?> getApartmentDetail(@PathVariable String id) {
        try { return ResponseEntity.ok(service.getApartmentDetail(id)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    /**
     * POST /api/apartments
     * Body: { buildingId, number, floor, area, description? }
     */
    @PostMapping("/api/apartments")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> createApartment(@RequestBody Map<String, Object> body) {
        try { return ResponseEntity.ok(service.createApartment(body)); }
        catch (Exception e) {
            logger.error("Error creating apartment", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * PUT /api/apartments/{id}
     * Body: { floor?, area?, status?, rentalStatus?, description? }
     */
    @PutMapping("/api/apartments/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> updateApartment(@PathVariable String id,
                                             @RequestBody Map<String, Object> body) {
        try { return ResponseEntity.ok(service.updateApartment(id, body)); }
        catch (Exception e) {
            logger.error("Error updating apartment {}", id, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}