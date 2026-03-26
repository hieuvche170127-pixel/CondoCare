package com.swp391.condocare_swp.service;

import com.swp391.condocare_swp.entity.ServiceRequest;
import com.swp391.condocare_swp.entity.Staff;
import com.swp391.condocare_swp.repository.ServiceRequestRepository;
import com.swp391.condocare_swp.repository.StaffRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * StaffServiceRequestService — Xử lý yêu cầu hỗ trợ phía Staff.
 *
 * Thay đổi so với phiên bản cũ:
 * - assignAndStart(), markDone(), reject() đều gọi NotificationService.sendRequestStatusNotification()
 *   để resident nhận thông báo trong hệ thống khi trạng thái yêu cầu thay đổi.
 */
@Service
public class StaffServiceRequestService {

    private static final Logger logger = LoggerFactory.getLogger(StaffServiceRequestService.class);

    @Autowired private ServiceRequestRepository srRepo;
    @Autowired private StaffRepository          staffRepo;
    @Autowired private NotificationService      notificationService;

    // ─── HELPER ───────────────────────────────────────────────────────────────

    private Staff currentStaff() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return staffRepo.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin nhân viên"));
    }

    // ─── LIST + FILTER ────────────────────────────────────────────────────────

    public Map<String, Object> listRequests(
            String status, String priority, String assignedToId,
            String keyword, int page, int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ServiceRequest> pageResult;

        if (keyword != null && !keyword.isBlank()) {
            pageResult = srRepo.searchAllByKeyword(keyword.trim(), pageable);
        } else {
            ServiceRequest.RequestStatus statusEnum = null;
            if (status != null && !status.isBlank() && !status.equalsIgnoreCase("ALL"))
                statusEnum = ServiceRequest.RequestStatus.valueOf(status.toUpperCase());

            ServiceRequest.Priority priorityEnum = null;
            if (priority != null && !priority.isBlank() && !priority.equalsIgnoreCase("ALL"))
                priorityEnum = ServiceRequest.Priority.valueOf(priority.toUpperCase());

            String resolvedAssignedToId = null;
            if ("me".equalsIgnoreCase(assignedToId)) {
                resolvedAssignedToId = currentStaff().getId();
            } else if (assignedToId != null && !assignedToId.isBlank()
                    && !assignedToId.equalsIgnoreCase("ALL")) {
                resolvedAssignedToId = assignedToId;
            }

            pageResult = srRepo.filterStaff(statusEnum, priorityEnum, resolvedAssignedToId, pageable);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content",       pageResult.getContent().stream().map(this::toMap).toList());
        result.put("totalElements", pageResult.getTotalElements());
        result.put("totalPages",    pageResult.getTotalPages());
        result.put("currentPage",   pageResult.getNumber());
        result.put("size",          pageResult.getSize());
        return result;
    }

    public Map<String, Object> getDetail(String id) {
        return toMapFull(findById(id));
    }

    public Map<String, Object> getStats() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total",      srRepo.count());
        m.put("pending",    srRepo.countByStatus(ServiceRequest.RequestStatus.PENDING));
        m.put("inProgress", srRepo.countByStatus(ServiceRequest.RequestStatus.IN_PROGRESS));
        m.put("done",       srRepo.countByStatus(ServiceRequest.RequestStatus.DONE));
        m.put("rejected",   srRepo.countByStatus(ServiceRequest.RequestStatus.REJECTED));
        m.put("awaitingConfirm",
                srRepo.findByStatusAndResidentConfirmedFalse(ServiceRequest.RequestStatus.DONE).size());
        return m;
    }

    // ─── ACTIONS ──────────────────────────────────────────────────────────────

    /**
     * Phân công nhân viên và chuyển PENDING → IN_PROGRESS.
     * Resident nhận thông báo hệ thống ngay khi được giao.
     */
    @Transactional
    public Map<String, Object> assignAndStart(String requestId, String assigneeId, String note) {
        ServiceRequest sr = findById(requestId);

        if (sr.getStatus() != ServiceRequest.RequestStatus.PENDING)
            throw new RuntimeException("Chỉ có thể phân công yêu cầu PENDING. Trạng thái hiện tại: " + sr.getStatus());

        Staff assignee = staffRepo.findById(assigneeId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên: " + assigneeId));

        sr.setAssignedTo(assignee);
        sr.setStatus(ServiceRequest.RequestStatus.IN_PROGRESS);
        if (note != null && !note.isBlank()) sr.setNote(note.trim());
        srRepo.save(sr);

        logger.info("SR [{}] assigned to staff [{}] by [{}]",
                requestId, assigneeId, currentStaff().getUsername());

        // Thông báo cho resident
        if (sr.getResident() != null) {
            notificationService.sendRequestStatusNotification(
                    sr.getResident(), requestId, sr.getTitle(),
                    "IN_PROGRESS", note, assignee);
        }

        return toMap(sr);
    }

    /**
     * Từ chối yêu cầu.
     * Resident nhận thông báo kèm lý do từ chối.
     */
    @Transactional
    public Map<String, Object> reject(String requestId, String rejectReason) {
        if (rejectReason == null || rejectReason.isBlank())
            throw new IllegalArgumentException("Phải nhập lý do từ chối");

        ServiceRequest sr = findById(requestId);

        if (sr.getStatus() == ServiceRequest.RequestStatus.DONE ||
                sr.getStatus() == ServiceRequest.RequestStatus.REJECTED)
            throw new RuntimeException("Không thể từ chối yêu cầu đã " + sr.getStatus().name());

        Staff staff = currentStaff();
        sr.setStatus(ServiceRequest.RequestStatus.REJECTED);
        sr.setRejectReason(rejectReason.trim());
        srRepo.save(sr);

        logger.info("SR [{}] REJECTED by [{}]: {}", requestId, staff.getUsername(), rejectReason);

        // Thông báo cho resident kèm lý do
        if (sr.getResident() != null) {
            notificationService.sendRequestStatusNotification(
                    sr.getResident(), requestId, sr.getTitle(),
                    "REJECTED", rejectReason, staff);
        }

        return toMap(sr);
    }

    /**
     * Cập nhật ghi chú trong khi xử lý.
     */
    @Transactional
    public Map<String, Object> updateNote(String requestId, String note) {
        ServiceRequest sr = findById(requestId);
        sr.setNote(note != null ? note.trim() : null);
        srRepo.save(sr);
        return toMap(sr);
    }

    /**
     * Hoàn thành yêu cầu — Staff upload ảnh xác nhận.
     * Resident nhận thông báo và được yêu cầu xác nhận.
     */
    @Transactional
    public Map<String, Object> markDone(String requestId, String completionImage, String note) {
        ServiceRequest sr = findById(requestId);

        if (sr.getStatus() != ServiceRequest.RequestStatus.IN_PROGRESS)
            throw new RuntimeException("Chỉ có thể hoàn thành yêu cầu IN_PROGRESS. Trạng thái hiện tại: " + sr.getStatus());

        if (completionImage == null || completionImage.isBlank())
            throw new IllegalArgumentException("Phải upload ảnh xác nhận hoàn thành");

        if (!completionImage.startsWith("data:image/"))
            throw new IllegalArgumentException("Ảnh không đúng định dạng (phải là Data URL)");

        if (completionImage.length() > 2_800_000)
            throw new IllegalArgumentException("Ảnh quá lớn. Tối đa 2MB.");

        Staff staff = currentStaff();
        sr.setStatus(ServiceRequest.RequestStatus.DONE);
        sr.setCompletionImage(completionImage);
        if (note != null && !note.isBlank()) sr.setNote(note.trim());
        srRepo.save(sr);

        logger.info("SR [{}] marked DONE by [{}]", requestId, staff.getUsername());

        // Thông báo cho resident: hoàn thành, cần xác nhận
        if (sr.getResident() != null) {
            notificationService.sendRequestStatusNotification(
                    sr.getResident(), requestId, sr.getTitle(),
                    "DONE", note, staff);
        }

        return toMap(sr);
    }

    /**
     * Danh sách nhân viên có thể được phân công (cho dropdown).
     */
    public List<Map<String, Object>> getAssignableStaff() {
        return staffRepo.findAll().stream()
                .filter(s -> s.getStatus() == Staff.StaffStatus.ACTIVE)
                .map(s -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id",         s.getId());
                    m.put("fullName",   s.getFullName());
                    m.put("username",   s.getUsername());
                    m.put("position",   s.getPosition());
                    m.put("department", s.getDepartment());
                    m.put("roleId",     s.getRole().getId());
                    m.put("roleName",   s.getRole().getName());
                    return m;
                })
                .toList();
    }

    // ─── HELPERS ──────────────────────────────────────────────────────────────

    private ServiceRequest findById(String id) {
        return srRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu: " + id));
    }

    private Map<String, Object> toMap(ServiceRequest sr) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",               sr.getId());
        m.put("title",            sr.getTitle());
        m.put("description",      sr.getDescription());
        m.put("category",         sr.getCategory().name());
        m.put("status",           sr.getStatus().name());
        m.put("priority",         sr.getPriority().name());
        m.put("note",             sr.getNote());
        m.put("rejectReason",     sr.getRejectReason());
        m.put("residentConfirmed", sr.getResidentConfirmed());
        m.put("confirmedAt",      sr.getConfirmedAt() != null ? sr.getConfirmedAt().toString() : null);
        m.put("hasImage",         sr.getCompletionImage() != null && !sr.getCompletionImage().isBlank());
        m.put("createdAt",        sr.getCreatedAt().toString());
        m.put("updatedAt",        sr.getUpdatedAt() != null ? sr.getUpdatedAt().toString() : null);

        if (sr.getResident() != null) {
            m.put("residentId",    sr.getResident().getId());
            m.put("residentName",  sr.getResident().getFullName());
            m.put("residentPhone", sr.getResident().getPhone());
        }
        if (sr.getApartment() != null) {
            m.put("apartmentId",     sr.getApartment().getId());
            m.put("apartmentNumber", sr.getApartment().getNumber());
            m.put("buildingName",    sr.getApartment().getBuilding() != null
                    ? sr.getApartment().getBuilding().getName() : "");
        } else {
            m.put("apartmentId",     null);
            m.put("apartmentNumber", null);
            m.put("buildingName",    null);
        }
        if (sr.getAssignedTo() != null) {
            m.put("assignedToId",   sr.getAssignedTo().getId());
            m.put("assignedToName", sr.getAssignedTo().getFullName());
        } else {
            m.put("assignedToId",   null);
            m.put("assignedToName", null);
        }
        return m;
    }

    private Map<String, Object> toMapFull(ServiceRequest sr) {
        Map<String, Object> m = toMap(sr);
        m.put("completionImage", sr.getCompletionImage());
        return m;
    }
}