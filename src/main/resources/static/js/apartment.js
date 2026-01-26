function loadBuildings() {
    fetch('/api/public/buildings')
        .then(res => res.json())
        .then(buildings => {
            const list = document.getElementById('building-list');
            if (list) {
                buildings.forEach(b => {
                    const item = document.createElement('li');
                    item.className = 'list-group-item';
                    item.textContent = `${b.name} - ${b.address}`;
                    list.appendChild(item);
                });
            }
        })
        .catch(error => console.error('Error loading buildings:', error));
}

function loadApartments() {
    fetch('/api/public/apartments')
        .then(res => res.json())
        .then(apts => {
            const grid = document.getElementById('apartment-grid');
            if (grid) {
                const floors = {};
                apts.forEach(apt => {
                    if (!floors[apt.floor]) floors[apt.floor] = [];
                    floors[apt.floor].push(apt);
                });
                Object.keys(floors).sort((a, b) => a - b).forEach(floor => {
                    const floorDiv = document.createElement('div');
                    floorDiv.innerHTML = `<h3>Floor ${floor}</h3><div class="row">`;
                    floors[floor].forEach((apt, index) => {
                        const card = `
                            <div class="col-md-3 mb-3">  <!-- Adjust col-md-1.5 for 8 per row if needed, but Bootstrap uses 12 grid -->
                                <div class="card">
                                    <div class="card-body">
                                        <h5 class="card-title">${apt.number}</h5>
                                        <p>Area: ${apt.area} m²</p>
                                        <p>Status: ${apt.status}</p>
                                        <p>Rental: ${apt.rentalStatus}</p>
                                        <p>Residents: ${apt.totalResident}</p>
                                    </div>
                                </div>
                            </div>`;
                        floorDiv.querySelector('.row').innerHTML += card;
                        // For max 8 per row: Bootstrap row is 12 cols, col-md-1.5 not standard, use col-md-3 for 4, or custom CSS for 8
                    });
                    grid.appendChild(floorDiv);
                });
            }
        })
        .catch(error => console.error('Error loading apartments:', error));
}

function loadAdminApartments() {
    fetch('/api/admin/apartments')
        .then(res => {
            if (res.status === 401 || res.status === 403) {
                window.location.href = '/login.html';
                return;
            }
            return res.json();
        })
        .then(apts => {
            const grid = document.getElementById('apartment-grid');
            if (grid) {
                grid.innerHTML = '';  // Clear existing
                const floors = {};
                apts.forEach(apt => {
                    if (!floors[apt.floor]) floors[apt.floor] = [];
                    floors[apt.floor].push(apt);
                });
                Object.keys(floors).sort((a, b) => a - b).forEach(floor => {
                    const floorDiv = document.createElement('div');
                    floorDiv.innerHTML = `<h3>Floor ${floor}</h3><div class="row">`;
                    floors[floor].forEach((apt, index) => {
                        let actions = '';
                        // Check role from local (assume role stored in localStorage after login)
                        const userRole = localStorage.getItem('userRole');  // Set in auth.js after login
                        if (userRole === 'R_MGR') {
                            actions = `
                                <button class="btn btn-primary btn-sm" onclick="updateApartment('${apt.id}')">Update</button>
                                <button class="btn btn-danger btn-sm" onclick="deleteApartment('${apt.id}')">Delete</button>
                            `;
                        } else if (userRole.startsWith('R_S') || userRole === 'R_ACC') {
                            actions = `<button class="btn btn-primary btn-sm" onclick="updateApartmentStatus('${apt.id}')">Update Status</button>`;
                        }
                        const card = `
                            <div class="col-md-3 mb-3">
                                <div class="card">
                                    <div class="card-body">
                                        <h5 class="card-title">${apt.number}</h5>
                                        <p>Area: ${apt.area} m²</p>
                                        <p>Status: ${apt.status}</p>
                                        <p>Rental: ${apt.rentalStatus}</p>
                                        <p>Residents: ${apt.totalResident}</p>
                                        ${actions}
                                    </div>
                                </div>
                            </div>`;
                        floorDiv.querySelector('.row').innerHTML += card;
                    });
                    grid.appendChild(floorDiv);
                });
            }
        })
        .catch(error => console.error('Error loading admin apartments:', error));
}

// Thêm modal cho create/update (sử dụng Bootstrap modal)
document.body.innerHTML += `
    <div class="modal fade" id="apartmentModal" tabindex="-1" aria-labelledby="apartmentModalLabel" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title" id="apartmentModalLabel">Apartment Form</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body">
                    <form id="apartmentForm">
                        <input type="hidden" id="aptId">
                        <div class="mb-3">
                            <label for="number" class="form-label">Number</label>
                            <input type="text" class="form-control" id="number" required>
                        </div>
                        <div class="mb-3">
                            <label for="floor" class="form-label">Floor</label>
                            <input type="number" class="form-control" id="floor" required>
                        </div>
                        <div class="mb-3">
                            <label for="area" class="form-label">Area (m²)</label>
                            <input type="number" step="0.01" class="form-control" id="area" required>
                        </div>
                        <div class="mb-3">
                            <label for="buildingId" class="form-label">Building ID</label>
                            <input type="text" class="form-control" id="buildingId" required>
                        </div>
                        <div class="mb-3">
                            <label for="status" class="form-label">Status</label>
                            <select class="form-select" id="status" required>
                                <option value="EMPTY">EMPTY</option>
                                <option value="OCCUPIED">OCCUPIED</option>
                                <option value="MAINTENANCE">MAINTENANCE</option>
                            </select>
                        </div>
                        <div class="mb-3">
                            <label for="rentalStatus" class="form-label">Rental Status</label>
                            <select class="form-select" id="rentalStatus" required>
                                <option value="AVAILABLE">AVAILABLE</option>
                                <option value="RENTED">RENTED</option>
                                <option value="OWNER">OWNER</option>
                            </select>
                        </div>
                        <!-- Thêm fields khác nếu cần, ví dụ totalResident mặc định 0 -->
                    </form>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                    <button type="button" class="btn btn-primary" id="saveBtn">Save</button>
                </div>
            </div>
        </div>
    </div>
`;

function showCreateForm() {
    document.getElementById('apartmentModalLabel').textContent = 'Create Apartment';
    document.getElementById('aptId').value = '';
    document.getElementById('number').value = '';
    document.getElementById('floor').value = '';
    document.getElementById('area').value = '';
    document.getElementById('buildingId').value = '';
    document.getElementById('status').value = 'EMPTY';
    document.getElementById('rentalStatus').value = 'AVAILABLE';

    // Nếu là Manager, enable all fields
    const userRole = localStorage.getItem('userRole');
    if (userRole !== 'R_MGR') {
        // Không cho create nếu không phải Manager, nhưng button đã ẩn
        return;
    }

    const modal = new bootstrap.Modal(document.getElementById('apartmentModal'));
    modal.show();

    document.getElementById('saveBtn').onclick = saveApartment;
}

function updateApartment(id) {
    fetch(`/api/admin/apartments/${id}`)
        .then(res => res.json())
        .then(apt => {
            document.getElementById('apartmentModalLabel').textContent = 'Update Apartment';
            document.getElementById('aptId').value = apt.id;
            document.getElementById('number').value = apt.number;
            document.getElementById('floor').value = apt.floor;
            document.getElementById('area').value = apt.area;
            document.getElementById('buildingId').value = apt.buildingId;
            document.getElementById('status').value = apt.status;
            document.getElementById('rentalStatus').value = apt.rentalStatus;

            const userRole = localStorage.getItem('userRole');
            if (userRole !== 'R_MGR') {
                // Staff chỉ update status/rentalStatus, disable other fields
                document.getElementById('number').disabled = true;
                document.getElementById('floor').disabled = true;
                document.getElementById('area').disabled = true;
                document.getElementById('buildingId').disabled = true;
            } else {
                document.getElementById('number').disabled = false;
                document.getElementById('floor').disabled = false;
                document.getElementById('area').disabled = false;
                document.getElementById('buildingId').disabled = false;
            }

            const modal = new bootstrap.Modal(document.getElementById('apartmentModal'));
            modal.show();

            document.getElementById('saveBtn').onclick = saveApartment;
        })
        .catch(error => console.error('Error fetching apartment:', error));
}

// Alias for staff update status (same as updateApartment)
function updateApartmentStatus(id) {
    updateApartment(id);
}

function saveApartment() {
    const id = document.getElementById('aptId').value;
    const apartment = {
        number: document.getElementById('number').value,
        floor: parseInt(document.getElementById('floor').value),
        area: parseFloat(document.getElementById('area').value),
        building: { id: document.getElementById('buildingId').value },  // Assume building object
        status: document.getElementById('status').value,
        rentalStatus: document.getElementById('rentalStatus').value,
        totalResident: 0,  // Default
        // Add other fields if needed
    };

    const method = id ? 'PUT' : 'POST';
    const url = id ? `/api/admin/apartments/${id}` : '/api/admin/apartments';

    fetch(url, {
        method: method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(apartment)
    })
        .then(res => {
            if (res.ok) {
                bootstrap.Modal.getInstance(document.getElementById('apartmentModal')).hide();
                loadAdminApartments();
            } else {
                alert('Error saving apartment');
            }
        })
        .catch(error => console.error('Error saving apartment:', error));
}

function deleteApartment(id) {
    if (confirm('Are you sure to delete?')) {
        fetch(`/api/admin/apartments/${id}`, { method: 'DELETE' })
            .then(res => {
                if (res.ok) {
                    loadAdminApartments();
                } else {
                    alert('Error deleting apartment');
                }
            })
            .catch(error => console.error('Error deleting apartment:', error));
    }
}