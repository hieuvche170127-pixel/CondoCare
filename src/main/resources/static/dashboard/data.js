const mockBuildings = [
  { id: "B001", name: "Sunrise Apartment", address: "123 Nguyen Van Linh, HCM", totalFloors: 5, totalApartments: 15, createdAt: new Date("2023-12-01") },
  { id: "B002", name: "Moonlight Tower", address: "456 Le Van Luong, Ha Noi", totalFloors: 10, totalApartments: 50, createdAt: new Date("2024-01-15") }
];

let apartments = loadData('apartments', [
  { id: "A101", buildingId: "B001", unitNumber: "101", floor: 1, area: 70, bedrooms: 2, bathrooms: 1, rentPrice: 8000000, status: "OCCUPIED", description: "Floor 1 - Room 1", createdAt: "2024-01-01", updatedAt: "2024-01-01" },
  { id: "A102", buildingId: "B001", unitNumber: "102", floor: 1, area: 72, bedrooms: 2, bathrooms: 1, rentPrice: 8200000, status: "OCCUPIED", description: "Floor 1 - Room 2", createdAt: "2024-01-01", updatedAt: "2024-01-01" },
  { id: "A103", buildingId: "B001", unitNumber: "103", floor: 1, area: 75, bedrooms: 3, bathrooms: 2, rentPrice: 9500000, status: "AVAILABLE", description: "Floor 1 - Room 3", createdAt: "2024-01-01", updatedAt: "2024-01-01" },
  { id: "A201", buildingId: "B001", unitNumber: "201", floor: 2, area: 70, bedrooms: 2, bathrooms: 1, rentPrice: 8000000, status: "OCCUPIED", description: "Floor 2 - Room 1", createdAt: "2024-01-01", updatedAt: "2024-01-01" },
  { id: "A202", buildingId: "B001", unitNumber: "202", floor: 2, area: 72, bedrooms: 2, bathrooms: 1, rentPrice: 8200000, status: "AVAILABLE", description: "Floor 2 - Room 2", createdAt: "2024-01-01", updatedAt: "2024-01-01" },
  { id: "A203", buildingId: "B001", unitNumber: "203", floor: 2, area: 75, bedrooms: 3, bathrooms: 2, rentPrice: 9500000, status: "AVAILABLE", description: "Floor 2 - Room 3", createdAt: "2024-01-01", updatedAt: "2024-01-01" },
  { id: "C101", buildingId: "B002", unitNumber: "101", floor: 1, area: 80, bedrooms: 2, bathrooms: 1, rentPrice: 10000000, status: "OCCUPIED", description: "Moonlight F1-01", createdAt: "2024-01-01", updatedAt: "2024-01-01" },
  { id: "C102", buildingId: "B002", unitNumber: "102", floor: 1, area: 85, bedrooms: 3, bathrooms: 2, rentPrice: 12000000, status: "OCCUPIED", description: "Moonlight F1-02", createdAt: "2024-01-01", updatedAt: "2024-01-01" },
  { id: "C103", buildingId: "B002", unitNumber: "103", floor: 1, area: 90, bedrooms: 3, bathrooms: 2, rentPrice: 14000000, status: "AVAILABLE", description: "Moonlight F1-03", createdAt: "2024-01-01", updatedAt: "2024-01-01" },
  { id: "C201", buildingId: "B002", unitNumber: "201", floor: 2, area: 80, bedrooms: 2, bathrooms: 1, rentPrice: 10000000, status: "OCCUPIED", description: "Moonlight F2-01", createdAt: "2024-01-01", updatedAt: "2024-01-01" }
]);

function getApartmentById(id) {
  return apartments.find(a => a.id === id);
}

function getBuildingById(id) {
  return buildings.find(b => b.id === id);
}

function getApartmentsByBuildingId(buildingId) {
  return apartments.filter(a => a.buildingId === buildingId);
}

function formatCurrency(amount) {
  return new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(amount);
}

function getStatusBadge(status) {
  let cls = "badge ";
  if (status === "OCCUPIED") cls += "bg-success-subtle text-success-emphasis";
  else if (status === "AVAILABLE") cls += "bg-primary-subtle text-primary-emphasis";
  else if (status === "MAINTENANCE") cls += "bg-warning-subtle text-warning-emphasis";
  else cls += "bg-secondary-subtle text-secondary-emphasis";
  return `<span class="${cls}">${status}</span>`;
}

function loadData(key, defaultValue) {
  const data = localStorage.getItem(key);
  return data ? JSON.parse(data) : defaultValue;
}

function saveData(key, value) {
  localStorage.setItem(key, JSON.stringify(value));
}

function toggleSidebar() {
  const sidebar = document.getElementById('sidebar');
  const isCollapsed = sidebar.style.width === '80px';
  sidebar.style.width = isCollapsed ? '260px' : '80px';
  // Cập nhật main padding
  document.querySelector('main').style.paddingLeft = isCollapsed ? '260px' : '80px';
  // Hide/show text
  const texts = sidebar.querySelectorAll('.nav-text');
  texts.forEach(t => t.style.display = isCollapsed ? 'inline' : 'none');
}

function renderSidebar() {
  const nav = document.getElementById('navItems');
  nav.innerHTML = '';
  const currentUser = JSON.parse(sessionStorage.getItem('currentUser') || 'null');
  if (!currentUser) return;

  const items = [
    { title: "Dashboard", href: "../dashboard/dashboard.html", icon: "layout-dashboard" },
    { title: "Buildings", href: "../buildings/index.html", icon: "building" },
    { title: "Apartments", href: "../apartments/index.html", icon: "home" },
    { title: "Residents", href: "../residents/index.html", icon: "users", roles: ["Admin", "Manager", "Staff"] },
    { title: "Vehicles", href: "../vehicles/index.html", icon: "car", roles: ["Admin", "Manager", "Staff"] },
    { title: "Finance", href: "../finance/index.html", icon: "dollar-sign", roles: ["Admin", "Manager", "Staff"] },
    { title: "Maintenance", href: "../maintenance/index.html", icon: "wrench" },
    { title: "Security", href: "../security/index.html", icon: "shield", roles: ["Admin", "Manager", "Staff"] },
    { title: "Notifications", href: "../notifications/index.html", icon: "bell" },
    { title: "Facilities", href: "../facilities/index.html", icon: "calendar-days" },
    { title: "Staff", href: "../staff/index.html", icon: "user-cog", roles: ["Admin"] },
    { title: "Reports", href: "../reports/index.html", icon: "bar-chart-3", roles: ["Admin", "Manager"] },
    { title: "Settings", href: "../settings/index.html", icon: "settings", roles: ["Admin"] },
  ];

  items.filter(item => !item.roles || item.roles.includes(currentUser.role)).forEach(item => {
    nav.innerHTML += `
      <li>
        <a href="${item.href}" class="text-white d-block py-2 px-3">
          <i data-lucide="${item.icon}" class="me-2"></i>
          <span class="nav-text">${item.title}</span>
        </a>
      </li>
    `;
  });

  const userSec = document.getElementById('userSection');
  userSec.innerHTML = `
    <div class="d-flex align-items-center mb-3">
      <div class="bg-primary-subtle rounded-circle p-2 me-3">
        <span>${currentUser.fullName.split(' ').map(n => n[0]).join('').toUpperCase().slice(0,2)}</span>
      </div>
      <div class="nav-text">
        <p>${currentUser.fullName}</p>
        <small>${currentUser.role}</small>
      </div>
    </div>
    <button class="btn btn-link text-white w-100 text-start" onclick="logout()">
      <i data-lucide="log-out"></i>
      <span class="nav-text ms-2">Logout</span>
    </button>
  `;
}

function logout() {
  sessionStorage.removeItem('currentUser');
  window.location.href = '../../login.html';
}

// Xuất cho các page khác
window.buildings = buildings;
window.apartments = apartments;
window.getApartmentById = getApartmentById;
window.getBuildingById = getBuildingById;
window.getApartmentsByBuildingId = getApartmentsByBuildingId;
window.formatCurrency = formatCurrency;
window.getStatusBadge = getStatusBadge;
window.saveData = saveData;
window.mockHistory = mockHistory;
window.mockEquipment = mockEquipment;