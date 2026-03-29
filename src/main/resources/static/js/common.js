/**
 * common.js — Apartment Management System
 * Các hàm dùng chung cho toàn bộ ứng dụng
 */

// API Base URL —
const API_BASE_URL = '/api';

/* ─── Loading spinner ─────────────────────────────────────── */
function showLoading() {
    document.getElementById('loading')?.classList.add('show');
}
function hideLoading() {
    document.getElementById('loading')?.classList.remove('show');
}

/* ─── Alert ──────────────────────────────────────────────── */
function showAlert(message, type = 'info') {
    const alertDiv = document.createElement('div');
    alertDiv.className = `alert alert-${type} alert-dismissible fade show`;
    alertDiv.role = 'alert';
    alertDiv.innerHTML = `${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>`;
    const container = document.getElementById('alertContainer')
        || document.querySelector('.container')
        || document.querySelector('.container-fluid');
    if (container) {
        container.insertBefore(alertDiv, container.firstChild);
        setTimeout(() => alertDiv.remove(), 5000);
    }
}

/* ─── Token storage ──────────────────────────────────────── */
function saveToken(token, remember = false) {
    if (remember) {
        localStorage.setItem('token', token);
        sessionStorage.removeItem('token');
    } else {
        sessionStorage.setItem('token', token);
        localStorage.removeItem('token');
    }
}

function getToken() {
    return sessionStorage.getItem('token') || localStorage.getItem('token');
}

function removeToken() {
    sessionStorage.removeItem('token');
    localStorage.removeItem('token');
}

/* ─── User info storage ──────────────────────────────────── */
function saveUserInfo(userInfo, remember = false) {
    const s = JSON.stringify(userInfo);
    if (remember) {
        localStorage.setItem('userInfo', s);
        sessionStorage.removeItem('userInfo');
    } else {
        sessionStorage.setItem('userInfo', s);
        localStorage.removeItem('userInfo');
    }
}

function getUserInfo() {
    const raw = sessionStorage.getItem('userInfo') || localStorage.getItem('userInfo');
    try { return raw ? JSON.parse(raw) : null; } catch { return null; }
}

function removeUserInfo() {
    sessionStorage.removeItem('userInfo');
    localStorage.removeItem('userInfo');
}

/* ─── Auth state ─────────────────────────────────────────── */
function isLoggedIn() {
    return getToken() !== null;
}

function logout() {
    removeToken();
    removeUserInfo();
    localStorage.removeItem('unreadCount');
    window.location.href = '/login';
}

/**
 * Redirect nếu đã đăng nhập — dùng ở trang login/register.
 * Staff   → /dashboard
 * Resident → /resident
 */
function redirectIfLoggedIn() {
    if (!isLoggedIn()) return;
    const info = getUserInfo();
    if (!info) return;
    if (info.userType === 'staff') {
        window.location.href = '/dashboard';
    } else if (info.userType === 'resident') {
        window.location.href = '/resident';
    }
}

/** Redirect về login nếu chưa đăng nhập */
function redirectIfNotLoggedIn() {
    if (!isLoggedIn()) window.location.href = '/login';
}

/* ─── Login session ──────────────────────────────────────── */
function saveLoginSession(token, userInfo, remember = false) {
    saveToken(token, remember);
    saveUserInfo(userInfo, remember);
}

/* ─── Role helpers ───────────────────────────────────────── */
function getRole() {
    return getUserInfo()?.role || '';
}

function isRole(...roles) {
    return roles.includes(getRole());
}

/* ─── API request với JWT ────────────────────────────────── */
async function apiRequest(url, options = {}) {
    const token = getToken();
    const merged = {
        ...options,
        headers: {
            'Content-Type': 'application/json',
            ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
            ...options.headers,
        }
    };
    try {
        const response = await fetch(url, merged);
        if (response.status === 401) { logout(); return; }
        return response;
    } catch (error) {
        console.error('API Request Error:', error);
        throw error;
    }
}

/* ─── Validation helpers ─────────────────────────────────── */
function isValidEmail(email) {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

function isValidPhone(phone) {
    return /^[0-9]{10,11}$/.test(phone);
}

/* ─── Formatting ─────────────────────────────────────────── */
function formatCurrency(amount) {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
}

function formatDate(dateString) {
    if (!dateString) return '—';
    return new Intl.DateTimeFormat('vi-VN').format(new Date(dateString));
}

function formatDateTime(dateString) {
    if (!dateString) return '—';
    return new Date(dateString).toLocaleString('vi-VN');
}

/* ─── HTML escaping (dùng chung toàn app) ────────────────── */
function escHtml(str) {
    if (str == null) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

/* ─── Sinh mật khẩu ngẫu nhiên (dùng chung) ─────────────── */
function generateLocalPassword(len = 12) {
    const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789@#$%';
    let p = '';
    for (let i = 0; i < len; i++) p += chars[Math.floor(Math.random() * chars.length)];
    return p;
}

/* ─── Staff Sidebar — role-aware (dùng cho tất cả staff pages) ── */

/**
 * Định nghĩa menu sidebar theo từng role.
 * Gọi renderStaffSidebar(activePage) trong inline script của mỗi trang staff.
 *
 * activePage: 'dashboard' | 'buildings' | 'apartments' | 'resident' |
 *             'staff' | 'invoices' | 'service-requests'
 *
 * Role redirect mặc định (gọi khi trang không phù hợp):
 *   TECHNICIAN  → /dashboard/service-requests
 *   ACCOUNTANT  → /dashboard/invoices
 *   RECEPTIONIST→ /dashboard/service-requests (hoặc resident)
 */
const _STAFF_MENU = [
    {
        key:   'dashboard',
        href:  '/dashboard',
        icon:  'fa-tachometer-alt',
        label: 'Tổng quan',
        roles: ['ADMIN', 'MANAGER', 'RECEPTIONIST', 'ACCOUNTANT'],
    },
    {
        key:   'buildings',
        href:  '/dashboard/buildings',
        icon:  'fa-building',
        label: 'Tòa nhà',
        roles: ['ADMIN', 'MANAGER'],
    },
    {
        key:   'apartments',
        href:  '/dashboard/apartments',
        icon:  'fa-home',
        label: 'Căn hộ & Phí',
        roles: ['ADMIN', 'MANAGER', 'ACCOUNTANT'],
    },
    {
        key:   'resident',
        href:  '/dashboard/resident',
        icon:  'fa-users',
        label: 'Quản lý cư dân',
        roles: ['ADMIN', 'MANAGER', 'RECEPTIONIST'],
    },
    {
        key:   'staff',
        href:  '/dashboard/staff',
        icon:  'fa-users-cog',
        label: 'Quản lý nhân viên',
        roles: ['ADMIN', 'MANAGER'],
    },
    {
        key:   'invoices',
        href:  '/dashboard/invoices',
        icon:  'fa-receipt',
        label: 'Hóa đơn',
        // ACCOUNTANT chỉ thấy mục này
        roles: ['ADMIN', 'MANAGER', 'ACCOUNTANT'],
    },
    {
        key:   'vehicles',
        href:  '/dashboard/vehicles',
        icon:  'fa-car',
        label: 'Quản lý xe',
        // RECEPTIONIST duyệt đăng ký xe từ resident
        roles: ['ADMIN', 'MANAGER', 'RECEPTIONIST'],
    },
    {
        key:   'notifications',
        href:  '/dashboard/notifications',
        icon:  'fa-bell',
        label: 'Thông báo',
        // RECEPTIONIST gửi thông báo đến cư dân
        roles: ['ADMIN', 'MANAGER', 'RECEPTIONIST'],
    },
    {
        key:   'service-requests',
        href:  '/dashboard/service-requests',
        icon:  'fa-tools',
        label: 'Yêu cầu hỗ trợ',
        // TECHNICIAN và RECEPTIONIST chỉ thấy mục này
        roles: ['ADMIN', 'MANAGER', 'TECHNICIAN', 'RECEPTIONIST'],
    },
];

/** Trang mặc định mỗi role được redirect về */
const _ROLE_HOME = {
    ADMIN:        '/dashboard',
    MANAGER:      '/dashboard',
    RECEPTIONIST: '/dashboard/vehicles',
    ACCOUNTANT:   '/dashboard/invoices',
    TECHNICIAN:   '/dashboard/service-requests',
};

/**
 * Render sidebar staff theo role, và redirect nếu trang hiện tại
 * không thuộc quyền của role đó.
 *
 * @param {string} activePage - key của trang hiện tại (xem _STAFF_MENU)
 * @param {boolean} [redirectIfForbidden=true] - redirect nếu role không được phép xem trang này
 */
function renderStaffSidebar(activePage, redirectIfForbidden = true) {
    const info = getUserInfo();
    if (!info || info.userType !== 'staff') {
        window.location.href = '/login';
        return;
    }

    const role = (info.role || '').toUpperCase();

    // ── Redirect nếu role không được phép vào trang này ──────────────────────
    if (redirectIfForbidden) {
        const pageItem = _STAFF_MENU.find(m => m.key === activePage);
        if (pageItem && !pageItem.roles.includes(role)) {
            const home = _ROLE_HOME[role] || '/dashboard';
            window.location.href = home;
            return;
        }
    }

    // ── Cập nhật tên user trên sidebar ───────────────────────────────────────
    const nameEl = document.getElementById('sidebarUser');
    if (nameEl) nameEl.textContent = info.fullName || info.username || '';

    // ── Render <ul> items theo role ──────────────────────────────────────────
    const navEl = document.getElementById('sidebarNav');
    if (!navEl) return;

    const ul = navEl.querySelector('ul.nav.flex-column');
    if (!ul) return;

    const items = _STAFF_MENU
        .filter(m => m.roles.includes(role))
        .map(m => `
            <li class="nav-item">
                <a class="nav-link ${m.key === activePage ? 'active' : ''}"
                   href="${m.href}" title="${m.label}">
                    <i class="fas ${m.icon} me-2"></i>${m.label}
                </a>
            </li>`).join('');

    ul.innerHTML = items + `
        <li class="nav-item mt-4">
            <a class="nav-link text-danger-emphasis" href="#" id="logoutBtn">
                <i class="fas fa-sign-out-alt me-2"></i>Đăng xuất
            </a>
        </li>`;

    // Re-bind logout (innerHTML xóa event listener cũ)
    document.getElementById('logoutBtn')?.addEventListener('click', e => {
        e.preventDefault();
        if (confirm('Đăng xuất?')) logout();
    });
}

/* ─── Debounce ───────────────────────────────────────────── */
function debounce(func, wait) {
    let timeout;
    return function(...args) {
        clearTimeout(timeout);
        timeout = setTimeout(() => func(...args), wait);
    };
}

/* ─── DOM ready listeners ────────────────────────────────── */
document.addEventListener('DOMContentLoaded', function() {
    // Logout button handler
    document.getElementById('logoutBtn')?.addEventListener('click', e => {
        e.preventDefault(); logout();
    });

    // Staff sidebar mobile toggle (tự inject)
    const staffSidebar = document.querySelector('nav.sidebar');
    if (!staffSidebar) return;

    staffSidebar.querySelectorAll('.nav-link').forEach(link => {
        const span = link.querySelector('span');
        const text = span ? span.textContent.trim() : link.textContent.trim();
        if (text && !link.getAttribute('title')) link.setAttribute('title', text);
    });

    if (!document.getElementById('staffMobileToggle')) {
        const btn = document.createElement('button');
        btn.id = 'staffMobileToggle';
        btn.className = 'staff-mobile-toggle';
        btn.innerHTML = '<i class="fas fa-bars"></i>';
        btn.setAttribute('aria-label', 'Mở menu');
        document.body.appendChild(btn);

        btn.addEventListener('click', () => {
            staffSidebar.classList.toggle('open');
            btn.querySelector('i').className =
                staffSidebar.classList.contains('open') ? 'fas fa-times' : 'fas fa-bars';
        });

        document.addEventListener('click', e => {
            if (window.innerWidth <= 768
                && staffSidebar.classList.contains('open')
                && !staffSidebar.contains(e.target)
                && !btn.contains(e.target)) {
                staffSidebar.classList.remove('open');
                btn.querySelector('i').className = 'fas fa-bars';
            }
        });
    }
});