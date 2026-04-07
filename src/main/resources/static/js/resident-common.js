/**
 * resident-common.js
 * Các hàm dùng chung cho TOÀN BỘ trang Resident Dashboard.
 * File này phải được load SAU common.js
 */

/* ─── Kiểm tra login & quyền ───────────────────────────────── */
function requireResidentLogin() {
    if (!isLoggedIn()) {
        window.location.href = '/login';
        return false;
    }
    const info = getUserInfo();
    if (!info || info.userType !== 'resident') {
        alert('Trang này chỉ dành cho cư dân!');
        window.location.href = '/login';
        return false;
    }
    return true;
}

/* ─── Sidebar ───────────────────────────────────────────────── */
/**
 * Render sidebar vào element có id="residentSidebar"
 * @param {string} activePage - tên trang đang active: 'home'|'notifications'|'invoices'|'apartment'|'requests'|'profile'
 */
function renderResidentSidebar(activePage) {
    const info = getUserInfo() || {};
    const unread = parseInt(localStorage.getItem('unreadCount') || '0');

    const menu = [
        { key: 'home',          href: '/resident',                icon: 'fa-home',             label: 'Trang chủ' },
        { key: 'notifications', href: '/resident/notifications',   icon: 'fa-bell',             label: 'Thông báo',
          badge: unread > 0 ? `<span class="badge bg-danger ms-auto">${unread}</span>` : '' },
        { key: 'invoices',      href: '/resident/invoices',        icon: 'fa-file-invoice-dollar', label: 'Hóa đơn' },
        { key: 'apartment',     href: '/resident/apartment',       icon: 'fa-building',         label: 'Thông tin căn hộ' },
        { key: 'requests',      href: '/resident/requests',        icon: 'fa-tools',            label: 'Yêu cầu hỗ trợ' },
        { key: 'vehicles',      href: '/resident/vehicles',        icon: 'fa-motorcycle',       label: 'Phương tiện' },
        { key: 'profile',       href: '/profile',                  icon: 'fa-user-circle',      label: 'Thông tin cá nhân' },
    ];

    const items = menu.map(m => `
        <li class="nav-item">
            <a class="nav-link d-flex align-items-center gap-2 ${m.key === activePage ? 'active' : ''}"
               href="${m.href}">
                <i class="fas ${m.icon}"></i>
                <span>${m.label}</span>
                ${m.badge || ''}
            </a>
        </li>`).join('');

    document.getElementById('residentSidebar').innerHTML = `
        <div class="position-sticky pt-3 d-flex flex-column h-100">
            <!-- Logo + tên user -->
            <div class="text-center mb-4 px-3">
                <div class="resident-avatar mb-2">
                    <i class="fas fa-user-circle fa-3x text-white-50"></i>
                </div>
                <h6 class="text-white mb-0">${info.fullName || 'Cư dân'}</h6>
                <small class="text-white-50">Cư dân</small>
            </div>

            <!-- Menu chính -->
            <ul class="nav flex-column px-2 flex-grow-1">${items}</ul>

            <!-- Phần dưới cùng -->
            <div class="px-2 pb-3">
                <hr class="border-secondary">
                <ul class="nav flex-column">
                    <li class="nav-item">
                        <a class="nav-link d-flex align-items-center gap-2" href="/">
                            <i class="fas fa-arrow-left"></i> <span>Về trang chủ</span>
                        </a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link d-flex align-items-center gap-2 text-danger-emphasis"
                           href="#" id="sidebarLogoutBtn">
                            <i class="fas fa-sign-out-alt"></i> <span>Đăng xuất</span>
                        </a>
                    </li>
                </ul>
            </div>
        </div>`;

    // Gán sự kiện logout
    document.getElementById('sidebarLogoutBtn').addEventListener('click', e => {
        e.preventDefault();
        if (confirm('Bạn có chắc muốn đăng xuất?')) logout();
    });
}

/* ─── Alert helper ──────────────────────────────────────────── */
function showResidentAlert(containerId, message, type = 'danger') {
    const icon = type === 'success' ? 'fa-check-circle' : type === 'warning' ? 'fa-exclamation-triangle' : 'fa-exclamation-circle';
    document.getElementById(containerId).innerHTML = `
        <div class="alert alert-${type} alert-dismissible fade show">
            <i class="fas ${icon}"></i> ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>`;
}

/* ─── Badge loại thông báo ──────────────────────────────────── */
const NOTIF_TYPE = {
    INFO:        { cls: 'bg-info',     icon: 'fa-info-circle',    label: 'Thông tin' },
    WARNING:     { cls: 'bg-warning text-dark', icon: 'fa-exclamation-triangle', label: 'Cảnh báo' },
    URGENT:      { cls: 'bg-danger',   icon: 'fa-bell',           label: 'Khẩn cấp' },
    MAINTENANCE: { cls: 'bg-secondary',icon: 'fa-wrench',         label: 'Bảo trì' },
    PAYMENT:     { cls: 'bg-primary',  icon: 'fa-money-bill',     label: 'Thanh toán' },
};

/* ─── Badge trạng thái yêu cầu ─────────────────────────────── */
const SR_STATUS = {
    PENDING:     { cls: 'bg-warning text-dark', label: 'Chờ xử lý' },
    IN_PROGRESS: { cls: 'bg-primary',           label: 'Đang xử lý' },
    DONE:        { cls: 'bg-success',            label: 'Hoàn thành' },
    REJECTED:    { cls: 'bg-danger',             label: 'Từ chối' },
};

/* ─── Badge danh mục yêu cầu ───────────────────────────────── */
const SR_CATEGORY = {
    ELECTRIC:  { icon: 'fa-bolt',        label: 'Điện' },
    WATER:     { icon: 'fa-tint',        label: 'Nước' },
    INTERNET:  { icon: 'fa-wifi',        label: 'Internet' },
    HVAC:      { icon: 'fa-snowflake',   label: 'Điều hòa' },
    STRUCTURE: { icon: 'fa-home',        label: 'Kết cấu' },
    OTHER:     { icon: 'fa-tools',       label: 'Khác' },
};

/* ─── Badge trạng thái hóa đơn ─────────────────────────────── */
const INV_STATUS = {
    UNPAID:  { cls: 'bg-danger',  label: 'Chưa thanh toán' },
    PAID:    { cls: 'bg-success', label: 'Đã thanh toán' },
    OVERDUE: { cls: 'bg-dark',    label: 'Quá hạn' },
};

/* ─── Format số tiền VND ───────────────────────────────────── */
function formatVND(amount) {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
}

/* ─── Format datetime ───────────────────────────────────────── */
function formatDate(dateStr) {
    if (!dateStr) return '—';
    return new Date(dateStr).toLocaleDateString('vi-VN');
}
function formatDateTime(dateStr) {
    if (!dateStr) return '—';
    return new Date(dateStr).toLocaleString('vi-VN');
}