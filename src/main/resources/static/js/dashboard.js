// Dashboard logic
// Kiểm tra đăng nhập - redirect nếu chưa login
if (!isLoggedIn()) {
    alert('Vui lòng đăng nhập để truy cập Dashboard');
    window.location.href = '/login';
}

// Load user info
const userInfo = getUserInfo();
if (userInfo) {
    document.getElementById('userFullName').textContent = userInfo.fullName;

    // Kiểm tra user type
    if (userInfo.userType !== 'staff') {
        alert('Bạn không có quyền truy cập trang này');
        window.location.href = '/login';
    }
}

// Load dashboard stats
async function loadDashboardStats() {
    showLoading();
    
    try {
        const response = await apiRequest(`${API_BASE_URL}/dashboard/stats`);
        
        if (response.ok) {
            const stats = await response.json();
            
            // Update stats
            document.getElementById('totalApartments').textContent = stats.totalApartments || 0;
            document.getElementById('occupiedApartments').textContent = stats.occupiedApartments || 0;
            document.getElementById('emptyApartments').textContent = stats.emptyApartments || 0;
            document.getElementById('totalResidents').textContent = stats.totalResidents || 0;
            
            // Create chart
            createApartmentChart(stats);
        }
    } catch (error) {
        console.error('Error loading dashboard stats:', error);
        showAlert('Không thể tải dữ liệu. Vui lòng thử lại sau.', 'danger');
    } finally {
        hideLoading();
    }
}

// Create apartment status chart
function createApartmentChart(stats) {
    const ctx = document.getElementById('apartmentChart');
    if (!ctx) return;
    
    new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: ['Đang ở', 'Trống', 'Bảo trì'],
            datasets: [{
                data: [
                    stats.occupiedApartments || 0,
                    stats.emptyApartments || 0,
                    stats.maintenanceApartments || 0
                ],
                backgroundColor: ['#1cc88a', '#f6c23e', '#e74a3b']
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: true,
            plugins: {
                legend: {
                    position: 'bottom'
                }
            }
        }
    });
}

// Load stats on page load
loadDashboardStats();
