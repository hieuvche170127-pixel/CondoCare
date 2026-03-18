/**
 * staff-management.js
 * JS dùng chung cho Staff Management và Resident Management pages.
 * Require: common.js được load trước
 */

const API_STAFF    = `${API_BASE_URL}/staff-management`;

/* ── Auth check cho Staff pages (dùng common.js) ── */
function requireLogin() {
    if (!isLoggedIn()) {
        window.location.href = '/login';
        return false;
    }
    const info = getUserInfo();
    if (!info || info.userType !== 'staff') {
        alert('Trang này chỉ dành cho nhân viên!');
        window.location.href = '/login';
        return false;
    }
    return true;
}
const API_RESIDENT = `${API_BASE_URL}/resident-management`;

/* ══════════════════════════════════════════════════
   HELPERS CHUNG
══════════════════════════════════════════════════ */
function showMgmtAlert(containerId, message, type = 'success') {
    const icons = { success: 'fa-check-circle', danger: 'fa-exclamation-circle', warning: 'fa-exclamation-triangle', info: 'fa-info-circle' };
    document.getElementById(containerId).innerHTML = `
        <div class="alert alert-${type} alert-dismissible fade show">
            <i class="fas ${icons[type] || icons.info} me-2"></i>${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>`;
    // Auto-dismiss after 4s for success
    if (type === 'success') setTimeout(() => {
        document.getElementById(containerId).innerHTML = '';
    }, 4000);
}

function formatDate(val) {
    if (!val) return '—';
    return new Date(val).toLocaleDateString('vi-VN');
}

function formatDateTime(val) {
    if (!val) return '—';
    return new Date(val).toLocaleString('vi-VN');
}

function escHtml(str) {
    if (!str) return '';
    return String(str).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
}

function renderPagination(pageData, containerId, onPageClick) {
    if (!pageData || pageData.totalPages <= 1) {
        document.getElementById(containerId).innerHTML = '';
        return;
    }
    const cur   = pageData.number;
    const total = pageData.totalPages;
    let html = `<nav><ul class="pagination pagination-sm mb-0 justify-content-center">`;
    html += `<li class="page-item ${cur === 0 ? 'disabled' : ''}">
        <a class="page-link" href="#" onclick="(${onPageClick})(${cur-1});return false">‹</a></li>`;
    for (let i = Math.max(0, cur-2); i <= Math.min(total-1, cur+2); i++) {
        html += `<li class="page-item ${i===cur ? 'active' : ''}">
            <a class="page-link" href="#" onclick="(${onPageClick})(${i});return false">${i+1}</a></li>`;
    }
    html += `<li class="page-item ${cur === total-1 ? 'disabled' : ''}">
        <a class="page-link" href="#" onclick="(${onPageClick})(${cur+1});return false">›</a></li>`;
    html += `</ul></nav>`;
    document.getElementById(containerId).innerHTML = html;
}

/* ══════════════════════════════════════════════════
   STAFF MANAGEMENT
══════════════════════════════════════════════════ */
const StaffMgmt = {
    currentPage: 0,
    pageSize: 10,
    roles: [],

    async init() {
        await this.loadRoles();
        await this.loadStats();
        await this.loadList();
        this.bindEvents();
    },

    async loadRoles() {
        try {
            const res = await apiRequest(`${API_STAFF}/roles`);
            this.roles = await res.json();
            // Populate role dropdowns
            ['filterRole', 'formRole', 'editFormRole'].forEach(id => {
                const el = document.getElementById(id);
                if (!el) return;
                const isFilter = id === 'filterRole';
                el.innerHTML = isFilter ? '<option value="">Tất cả role</option>' : '<option value="">-- Chọn role --</option>';
                this.roles.forEach(r => {
                    el.innerHTML += `<option value="${r.id}">${r.name}</option>`;
                });
            });
        } catch(e) { console.error('Load roles error', e); }
    },

    async loadStats() {
        try {
            const res  = await apiRequest(`${API_STAFF}/stats`);
            const data = await res.json();
            if (document.getElementById('statTotal'))   document.getElementById('statTotal').textContent   = data.total   || 0;
            if (document.getElementById('statActive'))  document.getElementById('statActive').textContent  = data.active  || 0;
            if (document.getElementById('statResigned'))document.getElementById('statResigned').textContent= data.resigned|| 0;
            if (document.getElementById('statOnLeave')) document.getElementById('statOnLeave').textContent = data.onLeave || 0;
        } catch(e) { console.error('Load stats error', e); }
    },

    async loadList(page = 0) {
        this.currentPage = page;
        showLoading();
        try {
            const params = new URLSearchParams({
                page, size: this.pageSize,
                sort: document.getElementById('sortField')?.value || 'fullName',
                direction: document.getElementById('sortDir')?.value || 'asc',
            });
            const search = document.getElementById('searchInput')?.value?.trim();
            const role   = document.getElementById('filterRole')?.value;
            const status = document.getElementById('filterStatus')?.value;
            if (search) params.set('search', search);
            if (role)   params.set('roleId', role);
            if (status) params.set('status', status);

            const res  = await apiRequest(`${API_STAFF}?${params}`);
            const data = await res.json();
            this.renderTable(data.content || []);
            this.renderInfo(data);
            renderPagination(data, 'paginationContainer', `StaffMgmt.loadList`);
        } catch(e) {
            showMgmtAlert('alertContainer', 'Không thể tải danh sách nhân viên: ' + e.message, 'danger');
        } finally { hideLoading(); }
    },

    renderTable(list) {
        const tbody = document.getElementById('staffTableBody');
        if (!list.length) {
            tbody.innerHTML = `<tr><td colspan="8" class="text-center py-4 text-muted">
                <i class="fas fa-users fa-2x d-block mb-2 opacity-25"></i>Không có dữ liệu</td></tr>`;
            return;
        }
        const statusBadge = { ACTIVE:'bg-success', RESIGNED:'bg-secondary', ON_LEAVE:'bg-warning text-dark' };
        const statusLabel = { ACTIVE:'Đang làm', RESIGNED:'Đã nghỉ', ON_LEAVE:'Nghỉ phép' };
        const currentUser = getUserInfo();
        const currentUsername = currentUser?.username || '';
        const currentRole = currentUser?.roleName || currentUser?.role || '';
        tbody.innerHTML = list.map(s => {
            const isSelf    = s.username === currentUsername;
            const isAdmin   = s.roleName?.toUpperCase() === 'ADMIN';
            const canDelete = !isSelf && !(isAdmin && currentRole?.toUpperCase() !== 'ADMIN');
            const deleteBtn = canDelete
                ? `<button class="btn btn-sm btn-outline-danger" title="Vô hiệu hóa"
                       onclick="StaffMgmt.confirmDelete('${s.id}','${escHtml(s.fullName)}')">
                       <i class="fas fa-user-slash"></i>
                   </button>`
                : `<button class="btn btn-sm btn-outline-secondary" disabled
                       title="${isSelf ? 'Không thể tự vô hiệu hóa' : 'Không có quyền'}">
                       <i class="fas fa-lock"></i>
                   </button>`;
            return `
            <tr ${isSelf ? 'class="table-active"' : ''}>
                <td><span class="fw-bold text-primary">${escHtml(s.id)}</span></td>
                <td>
                    <div class="fw-semibold">${escHtml(s.fullName)}${isSelf ? ' <span class="badge bg-info text-dark ms-1">Bạn</span>' : ''}</div>
                    <small class="text-muted">${escHtml(s.username)}</small>
                </td>
                <td>${escHtml(s.email || '—')}</td>
                <td>${escHtml(s.phone || '—')}</td>
                <td>
                    <div>${escHtml(s.position || '—')}</div>
                    <small class="text-muted">${escHtml(s.department || '')}</small>
                </td>
                <td><span class="badge bg-primary bg-opacity-75">${escHtml(s.roleName)}</span></td>
                <td><span class="badge ${statusBadge[s.status] || 'bg-secondary'}">${statusLabel[s.status] || s.status}</span></td>
                <td>
                    <div class="d-flex gap-1">
                        <button class="btn btn-sm btn-outline-primary" title="Chỉnh sửa"
                            onclick="StaffMgmt.openEdit('${s.id}')">
                            <i class="fas fa-edit"></i>
                        </button>
                        ${deleteBtn}
                    </div>
                </td>
            </tr>`;
        }).join('');
    },

    renderInfo(data) {
        const el = document.getElementById('tableInfo');
        if (el) el.textContent =
            `Hiển thị ${(data.number * data.size) + 1}–${Math.min((data.number+1)*data.size, data.totalElements)} / ${data.totalElements} nhân viên`;
    },

    bindEvents() {
        document.getElementById('searchInput')?.addEventListener('keydown', e => {
            if (e.key === 'Enter') this.loadList(0);
        });
        document.getElementById('btnSearch')?.addEventListener('click', () => this.loadList(0));
        document.getElementById('btnReset')?.addEventListener('click', () => {
            ['searchInput','filterRole','filterStatus'].forEach(id => {
                const el = document.getElementById(id);
                if (el) el.value = '';
            });
            this.loadList(0);
        });

        // Create form submit
        document.getElementById('btnCreateSubmit')?.addEventListener('click', () => this.submitCreate());
        document.getElementById('btnEditSubmit')?.addEventListener('click',   () => this.submitEdit());
    },

    async submitCreate() {
        const btn = document.getElementById('btnCreateSubmit');
        btn.disabled = true;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin me-1"></i>Đang lưu...';
        try {
            const body = {
                username:   document.getElementById('formUsername').value.trim(),
                password:   document.getElementById('formPassword').value,
                fullName:   document.getElementById('formFullName').value.trim(),
                email:      document.getElementById('formEmail').value.trim(),
                phone:      document.getElementById('formPhone').value.trim(),
                position:   document.getElementById('formPosition').value.trim(),
                department: document.getElementById('formDepartment').value.trim(),
                dob:        document.getElementById('formDob').value || null,
                gender:     document.getElementById('formGender').value,
                roleId:     document.getElementById('formRole').value,
            };
            if (!body.username || !body.password || !body.fullName || !body.roleId || !body.gender) {
                showMgmtAlert('createFormAlert', 'Vui lòng điền đầy đủ các trường bắt buộc (*)', 'warning');
                return;
            }
            const res = await apiRequest(API_STAFF, { method: 'POST', body: JSON.stringify(body) });
            if (res.ok) {
                bootstrap.Modal.getInstance(document.getElementById('createModal'))?.hide();
                document.getElementById('createForm')?.reset();
                showMgmtAlert('alertContainer', 'Tạo nhân viên thành công!', 'success');
                this.loadList(0); this.loadStats();
            } else {
                const err = await res.text();
                showMgmtAlert('createFormAlert', err, 'danger');
            }
        } catch(e) {
            showMgmtAlert('createFormAlert', 'Lỗi: ' + e.message, 'danger');
        } finally {
            btn.disabled = false;
            btn.innerHTML = '<i class="fas fa-save me-1"></i>Lưu';
        }
    },

    currentEditId: null,
    async openEdit(id) {
        this.currentEditId = id;
        showLoading();
        try {
            const res  = await apiRequest(`${API_STAFF}/${id}`);
            const data = await res.json();
            // Populate edit form
            const fields = { editFullName: data.fullName, editEmail: data.email,
                editPhone: data.phone, editPosition: data.position,
                editDepartment: data.department, editDob: data.dob,
                editGender: data.gender, editStatus: data.status };
            Object.entries(fields).forEach(([id, val]) => {
                const el = document.getElementById(id);
                if (el) el.value = val || '';
            });
            const roleEl = document.getElementById('editFormRole');
            if (roleEl) roleEl.value = data.roleId;
            document.getElementById('editStaffId').textContent = data.id;
            document.getElementById('editNewPassword').value = '';
            new bootstrap.Modal(document.getElementById('editModal')).show();
        } catch(e) {
            showMgmtAlert('alertContainer', 'Không thể tải thông tin nhân viên', 'danger');
        } finally { hideLoading(); }
    },

    async submitEdit() {
        if (!this.currentEditId) return;
        const btn = document.getElementById('btnEditSubmit');
        btn.disabled = true;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin me-1"></i>Đang lưu...';
        try {
            const body = {
                fullName:    document.getElementById('editFullName').value.trim(),
                email:       document.getElementById('editEmail').value.trim(),
                phone:       document.getElementById('editPhone').value.trim(),
                position:    document.getElementById('editPosition').value.trim(),
                department:  document.getElementById('editDepartment').value.trim(),
                dob:         document.getElementById('editDob').value || null,
                gender:      document.getElementById('editGender').value,
                roleId:      document.getElementById('editFormRole').value,
                status:      document.getElementById('editStatus').value,
                newPassword: document.getElementById('editNewPassword').value || null,
            };
            const res = await apiRequest(`${API_STAFF}/${this.currentEditId}`, { method: 'PUT', body: JSON.stringify(body) });
            if (res.ok) {
                bootstrap.Modal.getInstance(document.getElementById('editModal'))?.hide();
                showMgmtAlert('alertContainer', 'Cập nhật thành công!', 'success');
                this.loadList(this.currentPage); this.loadStats();
            } else {
                showMgmtAlert('editFormAlert', await res.text(), 'danger');
            }
        } catch(e) {
            showMgmtAlert('editFormAlert', 'Lỗi: ' + e.message, 'danger');
        } finally {
            btn.disabled = false;
            btn.innerHTML = '<i class="fas fa-save me-1"></i>Lưu thay đổi';
        }
    },

    confirmDelete(id, name) {
        if (!confirm(`Vô hiệu hóa nhân viên "${name}"?\nNhân viên sẽ không thể đăng nhập.`)) return;
        this.deleteStaff(id);
    },

    async deleteStaff(id) {
        showLoading();
        try {
            const res = await apiRequest(`${API_STAFF}/${id}`, { method: 'DELETE' });
            if (res.ok) {
                showMgmtAlert('alertContainer', 'Đã vô hiệu hóa nhân viên!', 'success');
                this.loadList(this.currentPage); this.loadStats();
            } else {
                showMgmtAlert('alertContainer', await res.text(), 'danger');
            }
        } catch(e) {
            showMgmtAlert('alertContainer', 'Lỗi: ' + e.message, 'danger');
        } finally { hideLoading(); }
    }
};

/* ══════════════════════════════════════════════════
   RESIDENT MANAGEMENT
══════════════════════════════════════════════════ */
const ResidentMgmt = {
    currentPage: 0,
    pageSize: 10,

    async init() {
        await this.loadStats();
        await this.loadList();
        this.bindEvents();
    },

    async loadStats() {
        try {
            const res  = await apiRequest(`${API_RESIDENT}/stats`);
            const data = await res.json();
            if (document.getElementById('rStatTotal'))    document.getElementById('rStatTotal').textContent    = data.total    || 0;
            if (document.getElementById('rStatActive'))   document.getElementById('rStatActive').textContent   = data.active   || 0;
            if (document.getElementById('rStatInactive')) document.getElementById('rStatInactive').textContent = data.inactive || 0;
            if (document.getElementById('rStatOwners'))   document.getElementById('rStatOwners').textContent   = data.owners   || 0;
            if (document.getElementById('rStatTenants'))  document.getElementById('rStatTenants').textContent  = data.tenants  || 0;
        } catch(e) { console.error('Load resident stats error', e); }
    },

    async loadList(page = 0) {
        this.currentPage = page;
        showLoading();
        try {
            const params = new URLSearchParams({ page, size: this.pageSize,
                sort: document.getElementById('rSortField')?.value || 'fullName',
                direction: document.getElementById('rSortDir')?.value || 'asc' });
            const search = document.getElementById('rSearchInput')?.value?.trim();
            const type   = document.getElementById('rFilterType')?.value;
            const status = document.getElementById('rFilterStatus')?.value;
            if (search) params.set('search', search);
            if (type)   params.set('type', type);
            if (status) params.set('status', status);

            const res  = await apiRequest(`${API_RESIDENT}?${params}`);
            const data = await res.json();
            this.renderTable(data.content || []);
            this.renderInfo(data);
            renderPagination(data, 'rPaginationContainer', `ResidentMgmt.loadList`);
        } catch(e) {
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
        const typeBadge  = { OWNER:'bg-primary', TENANT:'bg-info', GUEST:'bg-secondary' };
        const typeLabel  = { OWNER:'Chủ nhà', TENANT:'Thuê', GUEST:'Khách' };
        const statusBadge= { ACTIVE:'bg-success', INACTIVE:'bg-danger' };
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
                           <small class="d-block text-muted">${escHtml(r.buildingName||'')}</small>`
                        : `<span class="text-muted">—</span>`}
                </td>
                <td><span class="badge ${typeBadge[r.type]||'bg-secondary'}">${typeLabel[r.type]||r.type}</span></td>
                <td><span class="badge ${statusBadge[r.status]||'bg-secondary'}">${r.status==='ACTIVE'?'Hoạt động':'Không HĐ'}</span></td>
                <td>
                    <div class="d-flex gap-1">
                        <button class="btn btn-sm btn-outline-primary" title="Chỉnh sửa"
                            onclick="ResidentMgmt.openEdit('${r.id}')">
                            <i class="fas fa-edit"></i>
                        </button>
                        <button class="btn btn-sm btn-outline-danger" title="Vô hiệu hóa"
                            onclick="ResidentMgmt.confirmDeactivate('${r.id}','${escHtml(r.fullName)}')">
                            <i class="fas fa-ban"></i>
                        </button>
                    </div>
                </td>
            </tr>`).join('');
    },

    renderInfo(data) {
        const el = document.getElementById('rTableInfo');
        if (el) el.textContent =
            `Hiển thị ${(data.number*data.size)+1}–${Math.min((data.number+1)*data.size, data.totalElements)} / ${data.totalElements} cư dân`;
    },

    bindEvents() {
        document.getElementById('rSearchInput')?.addEventListener('keydown', e => {
            if (e.key === 'Enter') this.loadList(0);
        });
        document.getElementById('rBtnSearch')?.addEventListener('click', () => this.loadList(0));
        document.getElementById('rBtnReset')?.addEventListener('click', () => {
            ['rSearchInput','rFilterType','rFilterStatus'].forEach(id => {
                const el = document.getElementById(id); if (el) el.value = '';
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
            const body = {
                username:    document.getElementById('rFormUsername').value.trim(),
                password:    document.getElementById('rFormPassword').value,
                fullName:    document.getElementById('rFormFullName').value.trim(),
                type:        document.getElementById('rFormType').value,
                dob:         document.getElementById('rFormDob').value || null,
                gender:      document.getElementById('rFormGender').value,
                idNumber:    document.getElementById('rFormIdNumber').value.trim(),
                phone:       document.getElementById('rFormPhone').value.trim(),
                email:       document.getElementById('rFormEmail').value.trim(),
                apartmentId: document.getElementById('rFormApartment').value.trim() || null,
            };
            if (!body.username || !body.password || !body.fullName || !body.gender || !body.type) {
                showMgmtAlert('rCreateFormAlert', 'Vui lòng điền đầy đủ các trường bắt buộc (*)', 'warning');
                return;
            }
            const res = await apiRequest(API_RESIDENT, { method: 'POST', body: JSON.stringify(body) });
            if (res.ok) {
                bootstrap.Modal.getInstance(document.getElementById('rCreateModal'))?.hide();
                document.getElementById('rCreateForm')?.reset();
                showMgmtAlert('rAlertContainer', 'Tạo cư dân thành công!', 'success');
                this.loadList(0); this.loadStats();
            } else {
                showMgmtAlert('rCreateFormAlert', await res.text(), 'danger');
            }
        } catch(e) {
            showMgmtAlert('rCreateFormAlert', 'Lỗi: ' + e.message, 'danger');
        } finally {
            btn.disabled = false;
            btn.innerHTML = '<i class="fas fa-save me-1"></i>Lưu';
        }
    },

    currentEditId: null,
    async openEdit(id) {
        this.currentEditId = id;
        showLoading();
        try {
            const res  = await apiRequest(`${API_RESIDENT}/${id}`);
            const data = await res.json();
            const fields = { rEditFullName: data.fullName, rEditEmail: data.email,
                rEditPhone: data.phone, rEditIdNumber: data.idNumber,
                rEditDob: data.dob, rEditGender: data.gender,
                rEditType: data.type, rEditStatus: data.status,
                rEditApartment: data.apartmentId || '',
                rEditTempResidence: data.tempResidence || '',
                rEditTempAbsence: data.tempAbsence || '' };
            Object.entries(fields).forEach(([id, val]) => {
                const el = document.getElementById(id); if (el) el.value = val || '';
            });
            document.getElementById('rEditResidentId').textContent = data.id;
            document.getElementById('rEditNewPassword').value = '';
            new bootstrap.Modal(document.getElementById('rEditModal')).show();
        } catch(e) {
            showMgmtAlert('rAlertContainer', 'Không thể tải thông tin cư dân', 'danger');
        } finally { hideLoading(); }
    },

    async submitEdit() {
        if (!this.currentEditId) return;
        const btn = document.getElementById('rBtnEditSubmit');
        btn.disabled = true;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin me-1"></i>Đang lưu...';
        try {
            const body = {
                fullName:      document.getElementById('rEditFullName').value.trim(),
                email:         document.getElementById('rEditEmail').value.trim(),
                phone:         document.getElementById('rEditPhone').value.trim(),
                idNumber:      document.getElementById('rEditIdNumber').value.trim(),
                dob:           document.getElementById('rEditDob').value || null,
                gender:        document.getElementById('rEditGender').value,
                type:          document.getElementById('rEditType').value,
                status:        document.getElementById('rEditStatus').value,
                apartmentId:   document.getElementById('rEditApartment').value.trim(),
                tempResidence: document.getElementById('rEditTempResidence').value.trim(),
                tempAbsence:   document.getElementById('rEditTempAbsence').value.trim(),
                newPassword:   document.getElementById('rEditNewPassword').value || null,
            };
            const res = await apiRequest(`${API_RESIDENT}/${this.currentEditId}`, { method: 'PUT', body: JSON.stringify(body) });
            if (res.ok) {
                bootstrap.Modal.getInstance(document.getElementById('rEditModal'))?.hide();
                showMgmtAlert('rAlertContainer', 'Cập nhật cư dân thành công!', 'success');
                this.loadList(this.currentPage); this.loadStats();
            } else {
                showMgmtAlert('rEditFormAlert', await res.text(), 'danger');
            }
        } catch(e) {
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
                this.loadList(this.currentPage); this.loadStats();
            } else {
                showMgmtAlert('rAlertContainer', await res.text(), 'danger');
            }
        } catch(e) {
            showMgmtAlert('rAlertContainer', 'Lỗi: ' + e.message, 'danger');
        } finally { hideLoading(); }
    }
};