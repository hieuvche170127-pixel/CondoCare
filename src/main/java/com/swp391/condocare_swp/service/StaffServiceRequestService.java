package com.swp391.condocare_swp.service;

import com.swp391.condocare_swp.entity.ServiceRequest;
import com.swp391.condocare_swp.entity.Staff;
import com.swp391.condocare_swp.repository.ServiceRequestRepository;
import com.swp391.condocare_swp.repository.StaffRepository;
import com.swp391.condocare_swp.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/**
 * StaffServiceRequestService — Quan ly yeu cau ho tro phia Staff.
 *
 * THAY DOI (so voi phien ban cu):
 *   1. Bo private currentStaff() tu viet (SecurityContextHolder inline)
 *      -> Dung securityUtils.getCurrentStaff()
 *
 *   2. Them cac method *ForCurrentUser() nhan username lam tham so:
 *      - listRequestsForCurrentUser() -> thay cho listRequests() + isTechnician check trong Controller
 *      - getDetailForCurrentUser()    -> thay cho getDetail() + isTechnician check trong Controller
 *      - markDoneForCurrentUser()     -> thay cho markDone() + isTechnician check trong Controller
 *      - updateNoteForCurrentUser()   -> thay cho updateNote() + isTechnician check trong Controller
 *
 *      Logic phan quyen theo role TECHNICIAN duoc gom vao Service (khong de trong Controller).
 */
@Service
public class StaffServiceRequestService {

    private static final Logger logger = LoggerFactory.getLogger(StaffServiceRequestService.class);

    @Autowired private ServiceRequestRepository srRepo;
    @Autowired private StaffRepository          staffRepo;
    @Autowired private NotificationService      notificationService;
    @Autowired private EmailService             emailService;
    @Autowired private SecurityUtils            securityUtils;

    // HELPER - giu lai de backward compat voi code cu goi currentStaff()
    private Staff currentStaff() {
        return securityUtils.getCurrentStaff();
    }

    // === [NEW] ENTRY POINTS GOI TU CONTROLLER MOI =============================

    /** Thay cho listRequests() + isTechnician check trong Controller cu. */
    public Map<String, Object> listRequestsForCurrentUser(
            String currentUsername, String status, String priority,
            String assignedToId, String keyword, int page, int size) {
        Staff caller = staffRepo.findByUsername(currentUsername).orElse(null);
        boolean isTechnician = isTechnician(caller);
        String effectiveAssignedToId = isTechnician && caller != null ? caller.getId() : assignedToId;
        return listRequests(status, priority, effectiveAssignedToId, keyword, page, size);
    }

    /** Thay cho getDetail() + isTechnician assertAssignedTo trong Controller cu. */
    public Map<String, Object> getDetailForCurrentUser(String id, String currentUsername) {
        Staff caller = staffRepo.findByUsername(currentUsername).orElse(null);
        if (isTechnician(caller) && caller != null) assertAssignedTo(id, caller.getId());
        return getDetail(id);
    }

    /** Thay cho markDone() + isTechnician assertAssignedTo trong Controller cu. */
    @Transactional
    public Map<String, Object> markDoneForCurrentUser(
            String id, String currentUsername, String completionImage, String note) {
        Staff caller = staffRepo.findByUsername(currentUsername).orElse(null);
        if (isTechnician(caller) && caller != null) assertAssignedTo(id, caller.getId());
        return markDone(id, completionImage, note);
    }

    /** Thay cho updateNote() + isTechnician assertAssignedTo trong Controller cu. */
    @Transactional
    public Map<String, Object> updateNoteForCurrentUser(String id, String currentUsername, String note) {
        Staff caller = staffRepo.findByUsername(currentUsername).orElse(null);
        if (isTechnician(caller) && caller != null) assertAssignedTo(id, caller.getId());
        return updateNote(id, note);
    }

    // === LEGACY METHODS (giu nguyen de backward compat) =======================

    public Map<String, Object> listRequests(
            String status, String priority, String assignedToId,
            String keyword, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ServiceRequest> pageResult;
        if (assignedToId != null && !assignedToId.isBlank()) {
            Staff assignee = staffRepo.findById(assignedToId)
                    .orElseThrow(() -> new RuntimeException("Khong tim thay nhan vien: " + assignedToId));
            pageResult = filterRequests(status, priority, keyword, assignee, pageable);
        } else {
            pageResult = filterRequests(status, priority, keyword, null, pageable);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content",       pageResult.getContent().stream().map(this::mapToResponse).toList());
        result.put("totalElements", pageResult.getTotalElements());
        result.put("totalPages",    pageResult.getTotalPages());
        result.put("page",          page);
        result.put("size",          size);
        return result;
    }

    public Map<String, Object> getDetail(String id) {
        ServiceRequest sr = srRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Khong tim thay yeu cau: " + id));
        return mapToResponse(sr);
    }

    public Map<String, Object> getStats() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total",      srRepo.count());
        m.put("pending",    srRepo.countByStatus(ServiceRequest.RequestStatus.PENDING));
        m.put("inProgress", srRepo.countByStatus(ServiceRequest.RequestStatus.IN_PROGRESS));
        m.put("done",       srRepo.countByStatus(ServiceRequest.RequestStatus.DONE));
        m.put("rejected",   srRepo.countByStatus(ServiceRequest.RequestStatus.REJECTED));
        // [FIX] Thêm thống kê CANCELLED — cư dân tự hủy khi còn PENDING
        m.put("cancelled",  srRepo.countByStatus(ServiceRequest.RequestStatus.CANCELLED));
        return m;
    }

    @Transactional
    public Map<String, Object> assignAndStart(String requestId, String assigneeId, String note) {
        ServiceRequest sr = srRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay yeu cau: " + requestId));
        Staff assignee = staffRepo.findById(assigneeId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay nhan vien: " + assigneeId));
        Staff assigner = currentStaff();
        sr.setAssignedTo(assignee);
        sr.setStatus(ServiceRequest.RequestStatus.IN_PROGRESS);
        if (note != null && !note.isBlank()) sr.setNote(note);
        srRepo.save(sr);
        if (sr.getResident() != null)
            notificationService.sendRequestStatusNotification(sr.getResident(), requestId, sr.getTitle(), "IN_PROGRESS", note, assigner);
        logger.info("SR [{}] assigned to [{}] by [{}]", requestId, assigneeId, assigner.getUsername());
        return mapToResponse(sr);
    }

    @Transactional
    public Map<String, Object> reject(String requestId, String rejectReason) {
        if (rejectReason == null || rejectReason.isBlank())
            throw new IllegalArgumentException("Ly do tu choi khong duoc de trong.");
        ServiceRequest sr = srRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay yeu cau: " + requestId));
        Staff staff = currentStaff();
        sr.setStatus(ServiceRequest.RequestStatus.REJECTED);
        sr.setRejectReason(rejectReason.trim());
        srRepo.save(sr);
        logger.info("SR [{}] REJECTED by [{}]: {}", requestId, staff.getUsername(), rejectReason);
        if (sr.getResident() != null)
            notificationService.sendRequestStatusNotification(sr.getResident(), requestId, sr.getTitle(), "REJECTED", rejectReason, staff);
        return mapToResponse(sr);
    }

    @Transactional
    public Map<String, Object> updateNote(String requestId, String note) {
        ServiceRequest sr = srRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay yeu cau: " + requestId));
        sr.setNote(note);
        srRepo.save(sr);
        logger.info("SR [{}] note updated", requestId);
        return mapToResponse(sr);
    }

    @Transactional
    public Map<String, Object> markDone(String requestId, String completionImage, String note) {
        ServiceRequest sr = srRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay yeu cau: " + requestId));
        if (sr.getStatus() == ServiceRequest.RequestStatus.DONE)
            throw new IllegalArgumentException("Yeu cau nay da duoc danh dau hoan thanh roi.");
        if (sr.getStatus() == ServiceRequest.RequestStatus.REJECTED)
            throw new IllegalArgumentException("Yeu cau da bi tu choi, khong the danh dau hoan thanh.");
        Staff staff = currentStaff();
        sr.setStatus(ServiceRequest.RequestStatus.DONE);
        if (completionImage != null && !completionImage.isBlank()) sr.setCompletionImage(completionImage);
        if (note != null && !note.isBlank()) sr.setNote(note);
        srRepo.save(sr);
        logger.info("SR [{}] marked DONE by [{}]", requestId, staff.getUsername());
        if (sr.getResident() != null)
            notificationService.sendRequestStatusNotification(sr.getResident(), requestId, sr.getTitle(), "DONE", note, staff);
        return mapToResponse(sr);
    }

    public List<Map<String, Object>> getAssignableStaff() {
        return staffRepo.findAll().stream()
                .filter(s -> s.getRole() != null
                        && ("TECHNICIAN".equalsIgnoreCase(s.getRole().getName()) || "MANAGER".equalsIgnoreCase(s.getRole().getName()))
                        && s.getStatus() == Staff.StaffStatus.ACTIVE)
                .map(s -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", s.getId()); m.put("fullName", s.getFullName()); m.put("role", s.getRole().getName());
                    return m;
                }).toList();
    }

    public String getStaffIdByUsername(String username) {
        return staffRepo.findByUsername(username).map(Staff::getId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay nhan vien: " + username));
    }

    public void assertAssignedTo(String requestId, String staffId) {
        ServiceRequest sr = srRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay yeu cau: " + requestId));
        if (sr.getAssignedTo() == null || !sr.getAssignedTo().getId().equals(staffId))
            throw new RuntimeException("Ban khong co quyen thao tac voi yeu cau nay.");
    }

    // PRIVATE HELPERS
    private boolean isTechnician(Staff staff) {
        return staff != null && staff.getRole() != null
                && "TECHNICIAN".equalsIgnoreCase(staff.getRole().getName());
    }

    private Page<ServiceRequest> filterRequests(String status, String priority, String keyword, Staff assignee, PageRequest pageable) {
        boolean hasStatus   = status   != null && !status.isBlank()   && !status.equalsIgnoreCase("ALL");
        boolean hasPriority = priority != null && !priority.isBlank() && !priority.equalsIgnoreCase("ALL");
        boolean hasKeyword  = keyword  != null && !keyword.isBlank();
        boolean hasAssignee = assignee != null;
        if (!hasStatus && !hasPriority && !hasKeyword && !hasAssignee) return srRepo.findAll(pageable);
        ServiceRequest.RequestStatus statusEnum   = hasStatus   ? ServiceRequest.RequestStatus.valueOf(status.toUpperCase())   : null;
        ServiceRequest.Priority      priorityEnum = hasPriority ? ServiceRequest.Priority.valueOf(priority.toUpperCase()) : null;
        return srRepo.findWithFilters(statusEnum, priorityEnum, hasAssignee ? assignee : null, hasKeyword ? keyword.toLowerCase() : null, pageable);
    }

    private Map<String, Object> mapToResponse(ServiceRequest sr) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",              sr.getId());
        m.put("title",           sr.getTitle());
        m.put("description",     sr.getDescription());
        m.put("category",        sr.getCategory().name());
        m.put("status",          sr.getStatus().name());
        m.put("priority",        sr.getPriority().name());
        m.put("note",            sr.getNote());
        m.put("rejectReason",    sr.getRejectReason());
        m.put("completionImage", sr.getCompletionImage() != null ? "[base64]" : null);
        m.put("residentConfirmed", sr.getResidentConfirmed());
        m.put("confirmedAt",     sr.getConfirmedAt() != null ? sr.getConfirmedAt().toString() : null);
        m.put("createdAt",       sr.getCreatedAt().toString());
        m.put("updatedAt",       sr.getUpdatedAt() != null ? sr.getUpdatedAt().toString() : null);
        if (sr.getResident()   != null) { m.put("residentId",   sr.getResident().getId());   m.put("residentName", sr.getResident().getFullName()); }
        if (sr.getAssignedTo() != null) { m.put("assignedToId", sr.getAssignedTo().getId()); m.put("assignedToName", sr.getAssignedTo().getFullName()); }
        if (sr.getApartment()  != null) { m.put("apartmentId",  sr.getApartment().getId());  m.put("apartmentNumber", sr.getApartment().getNumber()); }
        return m;
    }
}