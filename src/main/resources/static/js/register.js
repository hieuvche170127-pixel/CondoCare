// Register page logic
console.log('Register page loaded');

// Redirect nếu đã đăng nhập
redirectIfLoggedIn();

document.getElementById('registerForm').addEventListener('submit', async function(e) {
    e.preventDefault();
    console.log('Form submitted');

    // Clear previous alerts
    document.getElementById('alertContainer').innerHTML = '';

    // Get form data
    const formData = {
        username: document.getElementById('username').value.trim(),
        password: document.getElementById('password').value,
        confirmPassword: document.getElementById('confirmPassword').value,
        fullName: document.getElementById('fullName').value.trim(),
        email: document.getElementById('email').value.trim(),
        phone: document.getElementById('phone').value.trim(),
        idNumber: document.getElementById('idNumber').value.trim(),
        apartmentId: document.getElementById('apartmentId').value.trim(),
        residentType: document.getElementById('residentType').value,
        gender: document.getElementById('gender').value
    };

    console.log('Form data:', formData);

    // Validation
    if (!formData.username || !formData.password || !formData.fullName || !formData.email) {
        showAlert('Vui lòng điền đầy đủ thông tin bắt buộc!', 'danger');
        return;
    }

    if (formData.password !== formData.confirmPassword) {
        showAlert('Password và Confirm Password không khớp!', 'danger');
        return;
    }

    if (!formData.residentType) {
        showAlert('Vui lòng chọn loại cư dân!', 'danger');
        return;
    }

    if (!formData.gender) {
        showAlert('Vui lòng chọn giới tính!', 'danger');
        return;
    }

    showLoading();
    console.log('Calling API...');

    try {
        const response = await fetch(`${API_BASE_URL}/auth/register`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(formData)
        });

        console.log('Response status:', response.status);
        console.log('Response ok:', response.ok);

        const contentType = response.headers.get('content-type');
        let data;

        if (contentType && contentType.includes('application/json')) {
            data = await response.json();
            console.log('Response data (JSON):', data);
        } else {
            data = await response.text();
            console.log('Response data (TEXT):', data);
        }

        if (response.ok) {
            // Hiển thị thông báo thành công
            const alertDiv = document.createElement('div');
            alertDiv.className = 'alert alert-success alert-dismissible fade show';
            alertDiv.innerHTML = `
                <i class="fas fa-check-circle"></i> <strong>Thành công!</strong> ${data}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            `;
            document.getElementById('alertContainer').appendChild(alertDiv);

            // Reset form
            document.getElementById('registerForm').reset();

            // Redirect sau 2 giây
            console.log('Redirecting to login...');
            setTimeout(() => {
                window.location.href = '/login';
            }, 2000);
        } else {
            // Hiển thị lỗi
            const alertDiv = document.createElement('div');
            alertDiv.className = 'alert alert-danger alert-dismissible fade show';
            alertDiv.innerHTML = `
                <i class="fas fa-exclamation-circle"></i> <strong>Lỗi!</strong> ${data}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            `;
            document.getElementById('alertContainer').appendChild(alertDiv);
        }
    } catch (error) {
        console.error('Error:', error);
        const alertDiv = document.createElement('div');
        alertDiv.className = 'alert alert-danger alert-dismissible fade show';
        alertDiv.innerHTML = `
            <i class="fas fa-exclamation-triangle"></i> <strong>Lỗi!</strong> Có lỗi xảy ra. Vui lòng thử lại sau.
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        `;
        document.getElementById('alertContainer').appendChild(alertDiv);
    } finally {
        hideLoading();
    }
});

