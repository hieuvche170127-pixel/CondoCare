/**
 * momo-payment.js
 * [FIX - low priority] Tách logic thanh toán MoMo ra khỏi resident/invoices.html.
 *
 * Require: common.js, resident-common.js được load trước.
 * Dùng cho: resident/invoices.html
 *
 * Globals cần từ invoices.html:
 *   - _allInvoices  : mảng hóa đơn hiện đang load (để lấy totalAmount)
 *   - loadInvoices  : hàm reload danh sách hóa đơn
 */

/* ── State ──────────────────────────────────────────────────────── */
let _momoInvoiceId      = null;
let _momoPollingTimer   = null;
let _momoCountdownTimer = null;
const MOMO_TIMEOUT_SEC  = 15 * 60; // 15 phút

/* ── Entry point: gọi từ nút "Thanh toán MoMo" ─────────────────── */
async function openMoMo(invoiceId, fromDetail = false) {
    _momoInvoiceId = invoiceId;

    // Reset trạng thái modal
    _setVisible('momoLoading', true);
    _setVisible('momoError',   false);
    _setVisible('momoContent', false);
    _setVisible('momoSuccess', false);

    // Nếu gọi từ modal chi tiết → đóng modal đó trước
    const detailModal = bootstrap.Modal.getInstance(document.getElementById('detailModal'));
    if (detailModal && fromDetail) detailModal.hide();
    const delay = (detailModal && fromDetail) ? 320 : 0;

    setTimeout(async () => {
        new bootstrap.Modal(document.getElementById('momoModal')).show();
        await _createPayment(invoiceId);
    }, delay);
}

/* ── Gọi API tạo thanh toán ─────────────────────────────────────── */
async function _createPayment(invoiceId) {
    try {
        const res = await apiRequest(`${API_BASE_URL}/momo/create-payment`, {
            method: 'POST',
            body:   JSON.stringify({ invoiceId }),
        });
        if (!res.ok) { momoShowError(await res.text()); return; }

        const data = await res.json();

        // Lấy số tiền từ danh sách hóa đơn đang load (nếu có) hoặc fallback từ response
        const inv   = (typeof _allInvoices !== 'undefined' ? _allInvoices : []).find(i => i.id === invoiceId);
        const total = inv ? (inv.totalAmount ?? 0) : (data.amount ?? 0);

        document.getElementById('momoInvoiceLabel').textContent =
            `Hóa đơn ${invoiceId}` + (inv ? ` — Tháng ${inv.month}/${inv.year}` : '');
        document.getElementById('momoAmountDisplay').textContent =
            new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(total);

        const qrSrc = data.qrCodeUrl ||
            `https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=${encodeURIComponent(data.payUrl || '')}`;
        document.getElementById('momoQrImg').src        = qrSrc;
        document.getElementById('momoDeeplinkBtn').href = data.deeplink || data.payUrl || '#';
        document.getElementById('momoPayUrlBtn').href   = data.payUrl  || '#';

        _setVisible('momoLoading', false);
        _setVisible('momoContent', true);
        momoSwitchTab('qr');
        _startCountdown(MOMO_TIMEOUT_SEC);
        _startPolling(invoiceId);
    } catch (e) {
        momoShowError('Lỗi kết nối: ' + e.message);
    }
}

/* ── Tab QR / Link ──────────────────────────────────────────────── */
function momoSwitchTab(tab) {
    _setVisible('tabQr',   tab === 'qr');
    _setVisible('tabLink', tab === 'link');
    document.getElementById('tabQrBtn')?.classList.toggle('active',   tab === 'qr');
    document.getElementById('tabLinkBtn')?.classList.toggle('active', tab === 'link');
}

/* ── Hiển thị lỗi ───────────────────────────────────────────────── */
function momoShowError(msg) {
    _setVisible('momoLoading', false);
    _setVisible('momoContent', false);
    _setVisible('momoError',   true);
    const el = document.getElementById('momoErrorMsg');
    if (el) el.textContent = msg;
    momoCancelPolling();
}

/* ── Polling kiểm tra trạng thái thanh toán ─────────────────────── */
function _startPolling(invoiceId) {
    momoCancelPolling();
    _momoPollingTimer = setInterval(async () => {
        try {
            const res  = await apiRequest(`${API_BASE_URL}/momo/status/${invoiceId}`);
            if (!res) return;
            const data = await res.json();
            if (data.paid === true || data.status === 'PAID') {
                momoCancelPolling();
                _showSuccess();
            }
        } catch (_) { /* bỏ qua lỗi mạng trong polling */ }
    }, 3000);
}

/* ── Hủy polling và countdown ───────────────────────────────────── */
function momoCancelPolling() {
    if (_momoPollingTimer)   { clearInterval(_momoPollingTimer);   _momoPollingTimer   = null; }
    if (_momoCountdownTimer) { clearInterval(_momoCountdownTimer); _momoCountdownTimer = null; }
}

/* ── Đếm ngược hết hạn QR ──────────────────────────────────────── */
function _startCountdown(seconds) {
    let remaining = seconds;
    const el = document.getElementById('momoCountdown');
    if (_momoCountdownTimer) clearInterval(_momoCountdownTimer);

    _momoCountdownTimer = setInterval(() => {
        remaining--;
        const m = Math.floor(remaining / 60).toString().padStart(2, '0');
        const s = (remaining % 60).toString().padStart(2, '0');
        if (el) el.textContent = `${m}:${s}`;
        if (remaining <= 0) {
            momoCancelPolling();
            momoShowError('Mã QR đã hết hạn. Vui lòng thử lại.');
        }
    }, 1000);
}

/* ── Thanh toán thành công ──────────────────────────────────────── */
function _showSuccess() {
    _setVisible('momoContent', false);
    _setVisible('momoSuccess', true);
    setTimeout(() => {
        bootstrap.Modal.getInstance(document.getElementById('momoModal'))?.hide();
        if (typeof showResidentAlert === 'function') {
            showResidentAlert('alertContainer', '✅ Thanh toán MoMo thành công! Hóa đơn đã được cập nhật.', 'success');
        }
        if (typeof loadInvoices === 'function') loadInvoices(0);
    }, 2500);
}

/* ── Xử lý redirect từ MoMo return URL ─────────────────────────── */
(function handleMomoReturn() {
    const params     = new URLSearchParams(window.location.search);
    const momoResult = params.get('momoResult');
    if (momoResult === null) return;

    document.addEventListener('DOMContentLoaded', () => {
        if (typeof showResidentAlert !== 'function') return;
        if (momoResult === '0') {
            showResidentAlert('alertContainer', '✅ Thanh toán MoMo thành công!', 'success');
        } else {
            showResidentAlert('alertContainer',
                `Thanh toán MoMo thất bại (mã lỗi: ${momoResult})`, 'warning');
        }
        window.history.replaceState({}, '', '/resident/invoices');
    });
})();

/* ── Helper ─────────────────────────────────────────────────────── */
function _setVisible(id, visible) {
    const el = document.getElementById(id);
    if (!el) return;
    el.classList.toggle('d-none', !visible);
}
