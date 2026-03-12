/**
 * profile.js — Xử lý toàn bộ logic trang Thông tin cá nhân
 * Hỗ trợ cả Staff và Resident, bao gồm chọn dịch vụ gửi xe.
 */

/* ══════════════════════════════════════════════════════
   CONSTANTS & HELPERS
══════════════════════════════════════════════════════ */
const API_PROFILE  = `${API_BASE_URL}/profile`;
const fmtVND = n => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(n || 0);

function showAlert(msg, type = 'danger') {
    const icon = type === 'success' ? 'fa-check-circle'
               : type === 'warning' ? 'fa-exclamation-triangle' : 'fa-exclamation-circle';
    document.getElementById('alertContainer').innerHTML = `
        <div class="alert alert-${type} alert-dismissible fade show py-2">
            <i class="fas ${icon} me-1"></i>${msg}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>`;
    document.getElementById('alertContainer').scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

/* ══════════════════════════════════════════════════════
   LOAD PROFILE
══════════════════════════════════════════════════════ */
async function loadProfile() {
    showLoading();
    try {
        const res = await apiRequest(`${API_PROFILE}/me`);
        if (!res.ok) throw new Error(await res.text());
        const p = await res.json();
        window._profileData = p;   // cache để submitServices dùng lại
        renderProfile(p);
    } catch (e) {
        document.getElementById('profileContent').innerHTML =
            `<div class="alert alert-danger"><i class="fas fa-exclamation-circle me-2"></i>Không tải được thông tin: ${e.message}</div>`;
    } finally {
        hideLoading();
    }
}

/* ══════════════════════════════════════════════════════
   RENDER TOÀN BỘ PROFILE
══════════════════════════════════════════════════════ */
function renderProfile(p) {
    const initials = (p.fullName || '?').split(' ').map(w => w[0]).slice(-2).join('').toUpperCase();
    const genderTxt = p.gender === 'M' ? 'Nam' : p.gender === 'F' ? 'Nữ' : '—';

    /* ── Quick info items (sidebar card) ── */
    const quickItems = p.userType === 'staff' ? `
        <div class="qi-item"><div class="qi-label">Vị trí</div><div class="qi-value">${p.position || '—'}</div></div>
        <div class="qi-item"><div class="qi-label">Phòng ban</div><div class="qi-value">${p.department || '—'}</div></div>
        <div class="qi-item"><div class="qi-label">Vai trò</div><div class="qi-value">${p.roleName || '—'}</div></div>
        <div class="qi-item"><div class="qi-label">Trạng thái</div>
            <div class="qi-value"><span class="badge ${p.staffStatus === 'ACTIVE' ? 'bg-success' : 'bg-secondary'}">${p.staffStatus || '—'}</span></div></div>
    ` : `
        <div class="qi-item"><div class="qi-label">CCCD / CMND</div><div class="qi-value font-monospace">${p.idNumber || '—'}</div></div>
        <div class="qi-item"><div class="qi-label">Loại cư dân</div>
            <div class="qi-value">${p.residentType === 'OWNER' ? '🏠 Chủ sở hữu' : p.residentType === 'TENANT' ? '🔑 Người thuê' : '👤 Khách'}</div></div>
        <div class="qi-item"><div class="qi-label">Trạng thái</div>
            <div class="qi-value"><span class="badge ${p.residentStatus === 'ACTIVE' ? 'bg-success' : 'bg-secondary'}">${p.residentStatus || '—'}</span></div></div>
        ${p.apartmentId ? `<div class="qi-item"><div class="qi-label">Căn hộ</div>
            <div class="qi-value fw-bold">${p.apartmentNumber || p.apartmentId}
            <small class="text-muted d-block">${p.buildingName || ''}</small></div></div>` : ''}
    `;

    /* ── Tab dịch vụ xe (chỉ resident) ── */
    const servicesTab = p.userType !== 'resident' ? '' : `
        <li class="nav-item">
            <button class="nav-link" data-bs-toggle="tab" data-bs-target="#tabServices">
                <i class="fas fa-car me-1"></i>Dịch vụ xe
            </button>
        </li>`;

    const servicesPane = p.userType !== 'resident' ? '' : `
        <div class="tab-pane fade" id="tabServices">
            ${renderServicesPane(p)}
        </div>`;

    /* ── Resident-only fields ── */
    const residentExtraFields = p.userType !== 'resident' ? '' : `
        <div class="col-md-6">
            <label class="form-label fw-semibold">CCCD / CMND</label>
            <input type="text" class="form-control" id="idNumber" value="${p.idNumber || ''}" maxlength="12" placeholder="12 chữ số">
        </div>
        <div class="col-md-6">
            <label class="form-label fw-semibold">Loại cư dân</label>
            <input type="text" class="form-control bg-light" value="${
                p.residentType === 'OWNER' ? 'Chủ sở hữu' : p.residentType === 'TENANT' ? 'Người thuê' : 'Khách'
            }" readonly>
        </div>
        <div class="col-md-6">
            <label class="form-label fw-semibold">Địa chỉ tạm trú</label>
            <input type="text" class="form-control" id="tempResidence" value="${p.tempResidence || ''}" placeholder="Nhập địa chỉ...">
        </div>
        <div class="col-md-6">
            <label class="form-label fw-semibold">Thông tin tạm vắng</label>
            <input type="text" class="form-control" id="tempAbsence" value="${p.tempAbsence || ''}" placeholder="VD: Đi công tác...">
        </div>`;

    /* ── Staff-only fields ── */
    const staffExtraFields = p.userType !== 'staff' ? '' : `
        <div class="col-md-6">
            <label class="form-label fw-semibold">Vị trí</label>
            <input type="text" class="form-control bg-light" value="${p.position || ''}" readonly>
        </div>
        <div class="col-md-6">
            <label class="form-label fw-semibold">Phòng ban</label>
            <input type="text" class="form-control bg-light" value="${p.department || ''}" readonly>
        </div>`;

    document.getElementById('profileContent').innerHTML = `
    <div class="row g-3">
        <!-- LEFT: Avatar + quick info -->
        <div class="col-lg-3">
            <div class="avatar-card mb-3">
                <div class="avatar-circle">${initials}</div>
                <div class="avatar-name">${p.fullName || '—'}</div>
                <div class="avatar-sub">@${p.username}</div>
                <div class="avatar-badge">${p.userType === 'resident'
                    ? (p.residentType === 'OWNER' ? '🏠 Chủ sở hữu' : p.residentType === 'TENANT' ? '🔑 Người thuê' : '👤 Khách')
                    : (p.roleName || p.position || 'Staff')}</div>
            </div>
            <div class="card border-0 shadow-sm">
                <div class="card-body p-3">
                    <div class="qi-item">
                        <div class="qi-label">Mã số</div>
                        <div class="qi-value font-monospace small">${p.id}</div>
                    </div>
                    <div class="qi-item">
                        <div class="qi-label">Giới tính</div>
                        <div class="qi-value">${genderTxt}</div>
                    </div>
                    <div class="qi-item">
                        <div class="qi-label">Ngày sinh</div>
                        <div class="qi-value">${p.dob ? new Date(p.dob).toLocaleDateString('vi-VN') : '—'}</div>
                    </div>
                    ${quickItems}
                </div>
            </div>
        </div>

        <!-- RIGHT: Tabs -->
        <div class="col-lg-9">
            <div class="card border-0 shadow-sm h-100">
                <div class="card-header bg-white border-bottom-0 pb-0">
                    <ul class="nav ptabs" id="profileTab">
                        <li class="nav-item">
                            <button class="nav-link active" data-bs-toggle="tab" data-bs-target="#tabInfo">
                                <i class="fas fa-edit me-1"></i>Thông tin cá nhân
                            </button>
                        </li>
                        ${servicesTab}
                        <li class="nav-item">
                            <button class="nav-link" data-bs-toggle="tab" data-bs-target="#tabPwd">
                                <i class="fas fa-key me-1"></i>Đổi mật khẩu
                            </button>
                        </li>
                    </ul>
                </div>
                <div class="card-body tab-content pt-3">

                    <!-- TAB: Thông tin ── -->
                    <div class="tab-pane fade show active" id="tabInfo">
                        <form id="profileForm" onsubmit="submitProfile(event)">
                            <div class="row g-3">
                                <div class="col-md-6">
                                    <label class="form-label fw-semibold">Họ và tên <span class="text-danger">*</span></label>
                                    <input type="text" class="form-control" id="fullName" value="${p.fullName || ''}" required>
                                </div>
                                <div class="col-md-6">
                                    <label class="form-label fw-semibold">Username</label>
                                    <input type="text" class="form-control bg-light" value="${p.username}" readonly>
                                </div>
                                <div class="col-md-6">
                                    <label class="form-label fw-semibold">Email <span class="text-danger">*</span></label>
                                    <input type="email" class="form-control" id="email" value="${p.email || ''}" required>
                                </div>
                                <div class="col-md-6">
                                    <label class="form-label fw-semibold">Số điện thoại <span class="text-danger">*</span></label>
                                    <input type="text" class="form-control" id="phone" value="${p.phone || ''}" required
                                        pattern="[0-9]{10,11}" title="10-11 chữ số">
                                </div>
                                <div class="col-md-6">
                                    <label class="form-label fw-semibold">Ngày sinh</label>
                                    <input type="date" class="form-control" id="dob" value="${p.dob || ''}">
                                </div>
                                <div class="col-md-6">
                                    <label class="form-label fw-semibold">Giới tính</label>
                                    <select class="form-select" id="gender">
                                        <option value="">— Chọn —</option>
                                        <option value="M" ${p.gender === 'M' ? 'selected' : ''}>Nam</option>
                                        <option value="F" ${p.gender === 'F' ? 'selected' : ''}>Nữ</option>
                                    </select>
                                </div>
                                ${residentExtraFields}
                                ${staffExtraFields}
                                <div class="col-12 pt-1">
                                    <button type="submit" class="btn btn-primary px-4">
                                        <i class="fas fa-save me-1"></i>Lưu thay đổi
                                    </button>
                                </div>
                            </div>
                        </form>
                    </div>

                    <!-- TAB: Dịch vụ xe (resident) ── -->
                    ${servicesPane}

                    <!-- TAB: Đổi mật khẩu ── -->
                    <div class="tab-pane fade" id="tabPwd">
                        <form id="pwdForm" onsubmit="submitPassword(event)" style="max-width:420px">
                            <div class="mb-3">
                                <label class="form-label fw-semibold">Mật khẩu hiện tại <span class="text-danger">*</span></label>
                                <input type="password" class="form-control" id="currentPassword" required>
                            </div>
                            <div class="mb-3">
                                <label class="form-label fw-semibold">Mật khẩu mới <span class="text-danger">*</span></label>
                                <input type="password" class="form-control" id="newPassword" required minlength="6"
                                    oninput="checkPwdStrength(this.value)">
                                <div id="pwdStrengthBar" class="mt-1" style="height:4px;border-radius:2px;background:#e5e7eb;overflow:hidden">
                                    <div id="pwdStrengthFill" style="height:100%;width:0;transition:all .3s"></div>
                                </div>
                                <small id="pwdStrengthTxt" class="text-muted"></small>
                            </div>
                            <div class="mb-3">
                                <label class="form-label fw-semibold">Xác nhận mật khẩu <span class="text-danger">*</span></label>
                                <input type="password" class="form-control" id="confirmPassword" required>
                            </div>
                            <button type="submit" class="btn btn-warning px-4">
                                <i class="fas fa-key me-1"></i>Đổi mật khẩu
                            </button>
                        </form>
                    </div>

                </div><!-- tab-content -->
            </div>
        </div>
    </div>`;
}

/* ══════════════════════════════════════════════════════
   RENDER SERVICES PANE
══════════════════════════════════════════════════════ */
function renderServicesPane(p) {
    const fees  = p.fees || [];
    const parking = fees.filter(f => f.type === 'PARKING');
    const fixed   = fees.filter(f => f.type !== 'PARKING');

    const vehicleIcon = t => t === 'car' ? '🚗' : t === 'ebike' ? '🛴' : t === 'motorbike' ? '🏍️' : '🅿️';

    /* Apartment banner */
    const aptBanner = p.apartmentId ? `
        <div class="apt-banner mb-3">
            <i class="fas fa-building fa-2x text-primary opacity-75"></i>
            <div>
                <div class="fw-bold">${p.apartmentNumber || p.apartmentId}
                    ${p.buildingName ? `<span class="text-muted fw-normal">— ${p.buildingName}</span>` : ''}
                </div>
                <div class="small text-muted">${p.apartmentArea ? p.apartmentArea + ' m²' : ''}</div>
            </div>
        </div>` : '';

    /* Parking cards */
    let parkingSection = '';
    if (parking.length === 0) {
        parkingSection = `<p class="text-muted small mb-0"><i class="fas fa-info-circle me-1"></i>
            Căn hộ chưa có phí gửi xe nào được cấu hình bởi ban quản lý.</p>`;
    } else {
        const cards = parking.map(f => `
            <div class="col-6 col-sm-4 col-md-3">
                <div class="vcard ${f.active ? 'on' : ''}" onclick="toggleVCard(this, '${f.id}')">
                    <input type="checkbox" value="${f.id}" ${f.active ? 'checked' : ''} style="display:none">
                    <span class="vc-icon">${vehicleIcon(f.vehicleType)}</span>
                    <div class="vc-name">${f.name}</div>
                    <div class="vc-price">${fmtVND(f.amount)}/tháng</div>
                    <i class="fas fa-check-circle vc-tick"></i>
                </div>
            </div>`).join('');
        parkingSection = `<div class="row g-2">${cards}</div>`;
    }

    /* Fixed fees (SERVICE / MANAGEMENT) */
    const fixedSection = fixed.length === 0 ? '' : `
        <div class="sec-divider mt-3"><i class="fas fa-building me-1"></i>Phí cố định</div>
        ${fixed.map(f => `
        <div class="d-flex justify-content-between align-items-center py-2 border-bottom">
            <div>
                <div class="fw-semibold small">${f.name}</div>
                <div class="text-muted" style="font-size:.75rem">${f.type}</div>
            </div>
            <span class="badge bg-success-subtle text-success border border-success-subtle">${fmtVND(f.amount)}/tháng</span>
        </div>`).join('')}`;

    return `
        ${aptBanner}
        <div class="sec-divider"><i class="fas fa-car me-1"></i>Dịch vụ gửi xe</div>
        <p class="text-muted small mb-2">
            Chọn loại xe bạn đang sử dụng dịch vụ gửi xe tại tòa nhà.
            Thay đổi sẽ được áp dụng từ tháng hóa đơn tiếp theo.
        </p>
        ${parkingSection}
        ${fixedSection}
        <div class="mt-3">
            <button class="btn btn-primary px-4" onclick="submitServices()">
                <i class="fas fa-save me-1"></i>Lưu thay đổi dịch vụ
            </button>
        </div>`;
}

/* ══════════════════════════════════════════════════════
   VEHICLE CARD TOGGLE
══════════════════════════════════════════════════════ */
function toggleVCard(el, feeId) {
    el.classList.toggle('on');
    const cb = el.querySelector('input[type=checkbox]');
    if (cb) cb.checked = el.classList.contains('on');
}

/* ══════════════════════════════════════════════════════
   SUBMIT: CẬP NHẬT THÔNG TIN
══════════════════════════════════════════════════════ */
async function submitProfile(e) {
    e.preventDefault();
    const p = window._profileData || {};
    const payload = {
        fullName : document.getElementById('fullName').value.trim(),
        email    : document.getElementById('email').value.trim(),
        phone    : document.getElementById('phone').value.trim(),
        dob      : document.getElementById('dob')?.value    || null,
        gender   : document.getElementById('gender')?.value || null,
    };
    // Resident extra fields
    if (p.userType === 'resident') {
        payload.idNumber      = document.getElementById('idNumber')?.value.trim()      || null;
        payload.tempResidence = document.getElementById('tempResidence')?.value.trim() || null;
        payload.tempAbsence   = document.getElementById('tempAbsence')?.value.trim()   || null;
    }

    showLoading();
    try {
        const res = await apiRequest(`${API_PROFILE}/update`, {
            method: 'PUT', body: JSON.stringify(payload)
        });
        const msg = await res.text();
        if (!res.ok) { showAlert('Lỗi: ' + msg); return; }

        // Đồng bộ tên trong localStorage
        const stored = getUserInfo();
        if (stored) { stored.fullName = payload.fullName; saveUserInfo(stored); }

        showAlert('✅ ' + msg, 'success');
        loadProfile(); // reload để cập nhật quick-info
    } catch (err) {
        showAlert('Lỗi kết nối: ' + err.message);
    } finally {
        hideLoading();
    }
}

/* ══════════════════════════════════════════════════════
   SUBMIT: LƯU DỊCH VỤ XE
══════════════════════════════════════════════════════ */
async function submitServices() {
    const p = window._profileData || {};
    // Thu thập tất cả checkbox được chọn trong vehicle cards
    const selectedFeeIds = Array.from(
        document.querySelectorAll('#tabServices .vcard.on input[type=checkbox]')
    ).map(cb => cb.value);

    const payload = {
        fullName : p.fullName,
        email    : p.email,
        phone    : p.phone,
        selectedFeeIds,
    };

    showLoading();
    try {
        const res = await apiRequest(`${API_PROFILE}/update`, {
            method: 'PUT', body: JSON.stringify(payload)
        });
        const msg = await res.text();
        if (!res.ok) { showAlert('Lỗi: ' + msg); return; }
        showAlert('✅ Cập nhật dịch vụ xe thành công!', 'success');
        loadProfile();
    } catch (err) {
        showAlert('Lỗi kết nối: ' + err.message);
    } finally {
        hideLoading();
    }
}

/* ══════════════════════════════════════════════════════
   SUBMIT: ĐỔI MẬT KHẨU
══════════════════════════════════════════════════════ */
async function submitPassword(e) {
    e.preventDefault();
    const newPwd  = document.getElementById('newPassword').value;
    const confPwd = document.getElementById('confirmPassword').value;
    if (newPwd !== confPwd) { showAlert('Mật khẩu xác nhận không khớp!', 'warning'); return; }

    showLoading();
    try {
        const res = await apiRequest(`${API_PROFILE}/change-password`, {
            method: 'PUT',
            body: JSON.stringify({
                currentPassword : document.getElementById('currentPassword').value,
                newPassword     : newPwd,
                confirmPassword : confPwd,
            })
        });
        const msg = await res.text();
        if (!res.ok) { showAlert('Lỗi: ' + msg); return; }
        showAlert('✅ ' + msg, 'success');
        document.getElementById('pwdForm').reset();
        document.getElementById('pwdStrengthFill').style.width = '0';
        document.getElementById('pwdStrengthTxt').textContent  = '';
    } catch (err) {
        showAlert('Lỗi kết nối: ' + err.message);
    } finally {
        hideLoading();
    }
}

/* ══════════════════════════════════════════════════════
   PASSWORD STRENGTH
══════════════════════════════════════════════════════ */
function checkPwdStrength(v) {
    let score = 0;
    if (v.length >= 8)          score++;
    if (/[A-Z]/.test(v))        score++;
    if (/[0-9]/.test(v))        score++;
    if (/[^A-Za-z0-9]/.test(v)) score++;

    const fill = document.getElementById('pwdStrengthFill');
    const txt  = document.getElementById('pwdStrengthTxt');
    const cfg  = [
        null,
        { w: '25%', c: '#ef4444', t: 'Rất yếu' },
        { w: '50%', c: '#f97316', t: 'Yếu' },
        { w: '75%', c: '#eab308', t: 'Trung bình' },
        { w: '100%',c: '#22c55e', t: 'Mạnh' },
    ];
    const s = cfg[score] || cfg[1];
    fill.style.width      = s.w;
    fill.style.background = s.c;
    txt.textContent       = s.t;
    txt.style.color       = s.c;
}

/* ══════════════════════════════════════════════════════
   INIT
══════════════════════════════════════════════════════ */
loadProfile();