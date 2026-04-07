/**
 * resident-management.js
 * [FIX #3] Tách từ staff-management.js — chỉ xử lý trang Quản lý Cư dân (/dashboard/resident).
 *
 * Require: common.js được load trước.
 * Template dùng: staff/residents.html
 * API base: /api/resident-management
 */

const API_RESIDENT = `${API_BASE_URL}/resident-management`;

/* ══════════════════════════════════════════════════
   HELPERS CHUNG (giống staff-management.js)
══════════════════════════════════════════════════ */
const ALERT_TIMEOUT_R = {
    success: 4000,
    info:    6000,
    warning: 8000,
    danger:  0,
};

function showMgmtAlert(containerId, message, type = 'success') {
    const icons = {
        success: 'fa-check-circle',
        danger:  'fa-exclamation-circle',
        warning: 'fa-exclamation-triangle',
        info:    'fa-info-circle',
    };
    const container = document.getElementById(containerId);
    if (!container) return;

    container.innerHTML = `
        <div class="alert alert-${type} alert-dismissible fade show">
            <i class="fas ${icons[type] || icons.info} me-2"></i>${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>`;

    const delay = ALERT_TIMEOUT_R[type] ?? 5000;
    if (delay > 0) {
        setTimeout(() => {
            const alert = container.querySelector('.alert');
            if (alert) {
                alert.classList.remove('show');
                setTimeout(() => alert.remove(), 300);
            }
        }, delay);
    }
}

function formatDateR(val) {
    if (!val) return '—';
    return new Date(val).toLocaleDateString('vi-VN');
}

function renderPaginationR(pageData, containerId, onPageClick) {
    if (!pageData || pageData.totalPages <= 1) {
        const el = document.getElementById(containerId);
        if (el) el.innerHTML = '';
        return;
    }
    const cur   = pageData.number;
    const total = pageData.totalPages;
    let html = `<nav><ul class="pagination pagination-sm mb-0 justify-content-center">`;
    html += `<li class="page-item ${cur === 0 ? 'disabled' : ''}">
        <a class="page-link" href="#" onclick="(${onPageClick})(${cur - 1});return false">‹</a></li>`;
    for (let i = Math.max(0, cur - 2); i <= Math.min(total - 1, cur + 2); i++) {
        html += `<li class="page-item ${i === cur ? 'active' : ''}">
            <a class="page-link" href="#" onclick="(${onPageClick})(${i});return false">${i + 1}</a></li>`;
    }
    html += `<li class="page-item ${cur === total - 1 ? 'disabled' : ''}">
        <a class="page-link" href="#" onclick="(${onPageClick})(${cur + 1});return false">›</a></li>`;
    html += `</ul></nav>`;
    const el = document.getElementById(containerId);
    if (el) el.innerHTML = html;
}

/* ══════════════════════════════════════════════════
   RESIDENT MANAGEMENT
══════════════════════════════════════════════════ */
const ResidentMgmt = {
    currentPage: 0,
    pageSize: 10,
    _readOnly: false,

    async init() {
        await this.loadStats();
        await this.loadList();
        this.bindEvents();

        // Ẩn nút Thêm/Sửa/Xóa cư dân khi role không phải ADMIN/MANAGER
        const role = (getUserInfo()?.role || '').toUpperCase();
        if (!['ADMIN', 'MANAGER'].includes(role)) {
            document.querySelectorAll('[data-bs-target="#rCreateModal"]').forEach(el => el.remove());
            this._readOnly = true;
        }
    },

    async loadStats() {
        try {
            const res  = await apiRequest(`${API_RESIDENT}/stats`);
            const data = await res.json();
            const set  = (id, val) => { const el = document.getElementById(id); if (el) el.textContent = val || 0; };
            set('rStatTotal',    data.total);
            set('rStatActive',   data.active);
            set('rStatPending',  data.pending);
            set('rStatInactive', data.inactive);
            set('rStatOwners',   data.owners);
            set('rStatTenants',  data.tenants);
            set('rStatGuests',   data.guests);

            const badge = document.getElementById('pendingBadge');
            if (badge) {
                const n = data.pending || 0;
                badge.textContent    = n;
                badge.style.display  = n > 0 ? '' : 'none';
            }
        } catch (e) { console.error('Load resident stats error', e); }
    },

    async loadList(page = 0) {
        this.currentPage = page;
        showLoading();
        try {
            const params = new URLSearchParams({
                page, size: this.pageSize,
                sort:      document.getElementById('rSortField')?.value  || 'fullName',
                direction: document.getElementById('rSortDir')?.value    || 'asc',
            });
            const search = document.getElementById('rSearchInput')?.value?.trim();
            const type   = document.getElementById('rFilterType')?.value;
            const status = document.getElementById('rFilterStatus')?.value;
            if (search) params.set('search', search);
            if (type)   params.set('type',   type);
            if (status) params.set('status', status);

            const res  = await apiRequest(`${API_RESIDENT}?${params}`);
            const data = await res.json();
            this.renderTable(data.content || []);
            this.renderInfo(data);
            renderPaginationR(data, 'rPaginationContainer', `ResidentMgmt.loadList`);
        } catch (e) {
            showMgmtAlert('rAlertContainer', 'Không thể tải danh sách: ' + e.message, 'danger');
        } finally { hideLoading(); }
    },

    renderTable(list) {
        const tbody = document.getElementById('residentTableBody');
        if (!list.length) {
            tbody.innerHTML = `<tr><td colspan="8" class="text-center py-4 text-muted">
                <i class="fas fa-users fa-2x d-block mb-2 opacity-25"></i>Không có dữ liệu</td></tr>`;
            return;
        }
        const typeBadge   = { OWNER: 'bg-primary', TENANT: 'bg-info', GUEST: 'bg-secondary' };
        const typeLabel   = { OWNER: 'Chủ nhà', TENANT: 'Thuê', GUEST: 'Khách' };
        const statusBadge = { ACTIVE: 'bg-success', INACTIVE: 'bg-danger', PENDING: 'bg-warning text-dark' };
        const statusLabel = { ACTIVE: 'Hoạt động', INACTIVE: 'Không HĐ', PENDING: 'Chờ duyệt' };

        tbody.innerHTML = list.map(r => `
            <tr>
                <td><span class="fw-bold text-primary">${escHtml(r.id)}</span></td>
                <td>
                    <div class="fw-semibold">${escHtml(r.fullName)}</div>
                    <small class="text-muted">${escHtml(r.username)}</small>
                </td>
                <td>${escHtml(r.email || '—')}</td>
                <td>${escHtml(r.phone || '—')}</td>
                <td>
                    ${r.apartmentNumber
                        ? `<span class="badge bg-light text-dark border">${escHtml(r.apartmentNumber)}</span>
                           <small class="d-block text-muted">${escHtml(r.buildingName || '')}</small>`
                        : `<span class="text-muted">—</span>`}
                </td>
                <td><span class="badge ${typeBadge[r.type] || 'bg-secondary'}">${typeLabel[r.type] || r.type}</span></td>
                <td><span class="badge ${statusBadge[r.status] || 'bg-secondary'}">${statusLabel[r.status] || r.status}</span></td>
                <td>
                    <div class="d-flex gap-1">
                        ${this._readOnly ? '' : `
                        <button class="btn btn-sm btn-outline-primary" title="Chỉnh sửa"
                            onclick="ResidentMgmt.openEdit('${r.id}')">
                            <i class="fas fa-edit"></i>
                        </button>
                        <button class="btn btn-sm btn-outline-danger" title="Vô hiệu hóa"
                            onclick="ResidentMgmt.confirmDeactivate('${r.id}','${escHtml(r.fullName)}')">
                            <i class="fas fa-ban"></i>
                        </button>`}
                        <button class="btn btn-sm btn-outline-secondary" title="Xem chi tiết"
                            onclick="ResidentMgmt.openDetail('${r.id}')">
                            <i class="fas fa-eye"></i>
                        </button>
                    </div>
                </td>
            </tr>`).join('');
    },

    renderInfo(data) {
        const el = document.getElementById('rTableInfo');
        if (el) el.textContent =
            `Hiển thị ${(data.number * data.size) + 1}–${Math.min((data.number + 1) * data.size, data.totalElements)} / ${data.totalElements} cư dân`;
    },

    bindEvents() {
        document.getElementById('rSearchInput')?.addEventListener('keydown', e => {
            if (e.key === 'Enter') this.loadList(0);
        });
        document.getElementById('rBtnSearch')?.addEventListener('click',  () => this.loadList(0));
        document.getElementById('rBtnReset')?.addEventListener('click', () => {
            ['rSearchInput', 'rFilterType', 'rFilterStatus'].forEach(id => {
                const el = document.getElementById(id);
                if (el) el.value = '';
            });
            this.loadList(0);
        });
        document.getElementById('rBtnCreateSubmit')?.addEventListener('click', () => this.submitCreate());
        document.getElementById('rBtnEditSubmit')?.addEventListener('click',   () => this.submitEdit());
    },

    async submitCreate() {
        const btn = document.getElementById('rBtnCreateSubmit');
        btn.disabled = true;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin me-1"></i>Đang lưu...';
        try {
            const email = document.getElementById('rFormEmail').value.trim();
            const body = {
                username:    document.getElementById('rFormUsername').value.trim(),
                password:    document.getElementById('rFormPassword')?.value?.trim() || null,
                fullName:    document.getElementById('rFormFullName').value.trim(),
                type:        document.getElementById('rFormType').value,
                dob:         document.getElementById('rFormDob').value || null,
                gender:      document.getElementById('rFormGender').value,
                idNumber:    document.getElementById('rFormIdNumber').value.trim(),
                phone:       document.getElementById('rFormPhone').value.trim(),
                email,
                apartmentId: document.getElementById('rFormApartment').value.trim() || null,
            };
            if (!body.username || !body.fullName || !body.gender || !body.type) {
                showMgmtAlert('rCreateFormAlert', 'Vui lòng điền đầy đủ các trường bắt buộc (*)', 'warning');
                return;
            }
            if (!email) {
                showMgmtAlert('rCreateFormAlert', 'Vui lòng nhập email để hệ thống gửi mật khẩu cho cư dân.', 'warning');
                return;
            }
            const res = await apiRequest(API_RESIDENT, { method: 'POST', body: JSON.stringify(body) });
            if (res.ok) {
                bootstrap.Modal.getInstance(document.getElementById('rCreateModal'))?.hide();
                document.getElementById('rCreateForm')?.reset();
                const msg = await res.text();
                showMgmtAlert('rAlertContainer', msg, 'success');
                this.loadList(0);
                this.loadStats();
            } else {
                showMgmtAlert('rCreateFormAlert', await res.text(), 'danger');
            }
        } catch (e) {
            showMgmtAlert('rCreateFormAlert', 'Lỗi: ' + e.message, 'danger');
        } finally {
            btn.disabled = false;
            btn.innerHTML = '<i class="fas fa-save me-1"></i>Lưu';
        }
    },

    async openDetail(id) {
        showLoading();
        try {
            const res  = await apiRequest(`${API_RESIDENT}/${id}`);
            const data = await res.json();
            hideLoading();
            const typeLabel   = { OWNER: 'Chủ sở hữu', TENANT: 'Người thuê', GUEST: 'Khách' };
            const statusLabel = { ACTIVE: 'Hoạt động', INACTIVE: 'Không hoạt động', PENDING: 'Chờ duyệt' };
            const apt = data.apartmentId
                ? `${data.apartmentNumber} — ${data.buildingName || ''}`
                : 'Chưa có căn hộ';
            showMgmtAlert('rAlertContainer',
                `<strong>${data.fullName}</strong> &nbsp;|&nbsp; `
                + `${typeLabel[data.type] || data.type} &nbsp;|&nbsp; `
                + `Căn hộ: ${apt} &nbsp;|&nbsp; `
                + `SĐT: ${data.phone || '—'} &nbsp;|&nbsp; `
                + `Email: ${data.email || '—'} &nbsp;|&nbsp; `
                + `Trạng thái: ${statusLabel[data.status] || data.status}`,
                'info');
        } catch (e) {
            hideLoading();
            showMgmtAlert('rAlertContainer', 'Không thể tải chi tiết: ' + e.message, 'danger');
        }
    },

    currentEditId: null,

    async openEdit(id) {
        this.currentEditId = id;
        showLoading();
        try {
            const res  = await apiRequest(`${API_RESIDENT}/${id}`);
            const data = await res.json();
            const fields = {
                rEditFullName:      data.fullName,
                rEditEmail:         data.email,
                rEditPhone:         data.phone,
                rEditIdNumber:      data.idNumber,
                rEditDob:           data.dob,
                rEditGender:        data.gender,
                rEditType:          data.type,
                rEditStatus:        data.status,
                rEditApartment:     data.apartmentId || '',
                rEditTempResidence: data.tempResidence || '',
                rEditTempAbsence:   data.tempAbsence   || '',
            };
            Object.entries(fields).forEach(([elId, val]) => {
                const el = document.getElementById(elId);
                if (el) el.value = val || '';
            });
            const idEl = document.getElementById('rEditResidentId');
            if (idEl) idEl.textContent = data.id;
            const pwdEl = document.getElementById('rEditNewPassword');
            if (pwdEl) pwdEl.value = '';
            ['rEditFormAlert', 'rResetPwdAlert'].forEach(aid => {
                const a = document.getElementById(aid);
                if (a) a.innerHTML = '';
            });
            new bootstrap.Modal(document.getElementById('rEditModal')).show();
        } catch (e) {
            showMgmtAlert('rAlertContainer', 'Không thể tải thông tin cư dân', 'danger');
        } finally { hideLoading(); }
    },

    async submitEdit() {
        if (!this.currentEditId) return;
        const btn = document.getElementById('rBtnEditSubmit');
        btn.disabled = true;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin me-1"></i>Đang lưu...';
        try {
            const nb  = v => (v == null || String(v).trim() === '') ? null : String(v).trim();
            const body = {
                fullName:      nb(document.getElementById('rEditFullName').value),
                email:         nb(document.getElementById('rEditEmail').value),
                phone:         nb(document.getElementById('rEditPhone').value),
                idNumber:      nb(document.getElementById('rEditIdNumber').value),
                dob:           nb(document.getElementById('rEditDob').value),
                gender:        document.getElementById('rEditGender').value,
                type:          document.getElementById('rEditType').value,
                status:        document.getElementById('rEditStatus').value,
                apartmentId:   nb(document.getElementById('rEditApartment').value),
                tempResidence: nb(document.getElementById('rEditTempResidence')?.value),
                tempAbsence:   nb(document.getElementById('rEditTempAbsence')?.value),
                newPassword:   nb(document.getElementById('rEditNewPassword')?.value),
            };
            if (!body.fullName) {
                showMgmtAlert('rEditFormAlert', 'Họ và tên không được để trống', 'warning');
                return;
            }
            if (!body.phone) {
                showMgmtAlert('rEditFormAlert', 'Số điện thoại không được để trống', 'warning');
                return;
            }
            const res = await apiRequest(`${API_RESIDENT}/${this.currentEditId}`, { method: 'PUT', body: JSON.stringify(body) });
            if (res.ok) {
                bootstrap.Modal.getInstance(document.getElementById('rEditModal'))?.hide();
                showMgmtAlert('rAlertContainer', 'Cập nhật cư dân thành công!', 'success');
                this.loadList(this.currentPage);
                this.loadStats();
            } else {
                showMgmtAlert('rEditFormAlert', await res.text(), 'danger');
            }
        } catch (e) {
            showMgmtAlert('rEditFormAlert', 'Lỗi: ' + e.message, 'danger');
        } finally {
            btn.disabled = false;
            btn.innerHTML = '<i class="fas fa-save me-1"></i>Lưu thay đổi';
        }
    },

    confirmDeactivate(id, name) {
        if (!confirm(`Vô hiệu hóa tài khoản cư dân "${name}"?`)) return;
        this.deactivate(id);
    },

    async deactivate(id) {
        showLoading();
        try {
            const res = await apiRequest(`${API_RESIDENT}/${id}`, { method: 'DELETE' });
            if (res.ok) {
                showMgmtAlert('rAlertContainer', 'Đã vô hiệu hóa tài khoản!', 'success');
                this.loadList(this.currentPage);
                this.loadStats();
            } else {
                showMgmtAlert('rAlertContainer', await res.text(), 'danger');
            }
        } catch (e) {
            showMgmtAlert('rAlertContainer', 'Lỗi: ' + e.message, 'danger');
        } finally { hideLoading(); }
    },
};

/* ──────────────────────────────────────────────────────────────────
   AUTO-GEN PASSWORD KHI NHẬP EMAIL — RESIDENT CREATE FORM
────────────────────────────────────────────────────────────────── */
document.addEventListener('DOMContentLoaded', () => {
    const rEmailEl = document.getElementById('rFormEmail');
    const rPwdEl   = document.getElementById('rFormPassword');
    const rPwdDisp = document.getElementById('rFormPasswordDisplay');

    if (rEmailEl && rPwdEl) {
        rEmailEl.addEventListener('blur', () => {
            if (rEmailEl.value.trim() && !rPwdEl.value.trim()) {
                const pwd = generateLocalPassword();
                rPwdEl.value = pwd;
                rPwdEl.type  = 'text';
                if (rPwdDisp) {
                    rPwdDisp.innerHTML = `
                      <div class="d-flex align-items-center gap-2 mt-1 flex-wrap">
                        <span class="badge bg-success">
                          <i class="fas fa-envelope me-1"></i>Sẽ gửi tới email cư dân
                        </span>
                        <button type="button" class="btn btn-sm btn-outline-secondary py-0"
                          onclick="navigator.clipboard.writeText('${pwd}');this.textContent='✓ Đã copy'">
                          <i class="fas fa-copy"></i> Copy
                        </button>
                        <button type="button" class="btn btn-sm btn-outline-primary py-0"
                          onclick="
                            const np=generateLocalPassword();
                            document.getElementById('rFormPassword').value=np;
                            document.getElementById('rFormPassword').type='text';
                          ">
                          <i class="fas fa-redo"></i> Sinh lại
                        </button>
                      </div>`;
                    rPwdDisp.classList.remove('d-none');
                }
            }
        });
        rPwdEl.addEventListener('input', () => {
            if (rPwdDisp && rPwdEl.value.trim())
                rPwdDisp.classList.add('d-none');
        });
    }
});
