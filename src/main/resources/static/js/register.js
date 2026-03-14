// Register page logic
console.log('Register page loaded');

redirectIfLoggedIn();

document.getElementById('registerForm').addEventListener('submit', async function(e) {
    e.preventDefault();
    console.log('Form submitted');

    document.getElementById('alertContainer').innerHTML = '';

    const formData = {
        username:        document.getElementById('username').value.trim(),
        password:        document.getElementById('password').value,
        confirmPassword: document.getElementById('confirmPassword').value,
        fullName:        document.getElementById('fullName').value.trim(),
        email:           document.getElementById('email').value.trim(),
        phone:           document.getElementById('phone').value.trim()
    };

    console.log('Form data:', formData);

    // Validation
    if (!formData.username || !formData.password || !formData.fullName || !formData.email || !formData.phone) {
        showNotification('Vui lòng điền đầy đủ thông tin bắt buộc!', 'danger');
        return;
    }

    if (formData.password !== formData.confirmPassword) {
        showNotification('Mật khẩu và xác nhận mật khẩu không khớp!', 'danger');
        return;
    }

    showLoading();

    try {
        const response = await fetch(`${API_BASE_URL}/auth/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(formData)
        });

        console.log('Response status:', response.status);
        const data = await response.text();
        console.log('Response:', data);

        if (response.ok) {
            showNotification(data, 'success');
            document.getElementById('registerForm').reset();
            setTimeout(() => { window.location.href = '/login'; }, 2000);
        } else {
            showNotification(data, 'danger');
        }
    } catch (error) {
        console.error('Error:', error);
        showNotification('Có lỗi xảy ra. Vui lòng thử lại.', 'danger');
    } finally {
        hideLoading();
    }
});

function showNotification(message, type) {
    const icon = type === 'success' ? 'fa-check-circle' : 'fa-exclamation-circle';
    const div = document.createElement('div');
    div.className = `alert alert-${type} alert-dismissible fade show`;
    div.innerHTML = `
        <i class="fas ${icon}"></i> ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;
    document.getElementById('alertContainer').appendChild(div);
}

