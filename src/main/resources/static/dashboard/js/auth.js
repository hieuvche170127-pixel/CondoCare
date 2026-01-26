let currentUser = JSON.parse(sessionStorage.getItem('currentUser') || 'null');
let isLoading = false; // Giả sử load nhanh

function login(userData) {
  currentUser = userData;
  sessionStorage.setItem('currentUser', JSON.stringify(userData));
  window.location.href = 'dashboard/dashboard.html';
}

function logout() {
  currentUser = null;
  sessionStorage.removeItem('currentUser');
  window.location.href = '../login.html';
}