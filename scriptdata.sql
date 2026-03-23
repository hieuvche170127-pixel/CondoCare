INSERT INTO Role (ID, name, description) VALUES
                                             ('R001', 'ADMIN', 'Quản trị hệ thống'),
                                             ('R002', 'MANAGER', 'Quản lý tòa nhà'),
                                             ('R003', 'STAFF', 'Nhân viên');

Select * from Staff;

INSERT INTO Staff (ID, username, password, full_name, email, phone,
                   position, department, dob, gender, status, role_id,
                   hired_at, create_at) VALUES
-- ADMIN
('S001', 'admin',
 '$2a$12$/o2PVhApFyYq8bnrAqughuJw1CMhDvKafBW/oohDwRIq.QeWg3SR.',
 'Nguyễn Hoàng Anh', 'admin@condocare.vn', '0901234567',
 'System Administrator', 'IT', '1990-01-15', 'M', 'ACTIVE', 'R001',
 '2023-01-01 08:00:00', '2023-01-01 08:00:00'),
-- MANAGER
('S002', 'manager1',
 '$2a$12$52WQXil57KI12HcA6SpCCeEuKJ72yqErC2iH8j3Xi4iVnoyH8IBNW',
 'Trần Thị Minh Châu', 'chau.tran@condocare.vn', '0902345678',
 'Building Manager', 'Management', '1985-05-20', 'F', 'ACTIVE', 'R002',
 '2023-02-01 08:00:00', '2023-02-01 08:00:00'),
('S003', 'manager2',
 '$2a$12$52WQXil57KI12HcA6SpCCeEuKJ72yqErC2iH8j3Xi4iVnoyH8IBNW',
 'Lê Quang Hải', 'hai.le@condocare.vn', '0903456789',
 'Assistant Manager', 'Management', '1988-09-10', 'M', 'ACTIVE', 'R002',
 '2023-03-01 08:00:00', '2023-03-01 08:00:00'),
-- STAFF
('S004', 'staff1',
 '$2a$12$3X5u2ueEV9YOC6n2uKRf9uJeuqEWaysXzF1YBxz.W48o4ASZS8E6.',
 'Phạm Minh Đức', 'duc.pham@condocare.vn', '0904567890',
 'Technical Staff', 'Maintenance', '1995-08-20', 'M', 'ACTIVE', 'R003',
 '2023-04-01 08:00:00', '2023-04-01 08:00:00'),
('S005', 'staff2',
 '$2a$12$3X5u2ueEV9YOC6n2uKRf9uJeuqEWaysXzF1YBxz.W48o4ASZS8E6.',
 'Nguyễn Thị Lan', 'lan.nguyen@condocare.vn', '0905678901',
 'Customer Service', 'Customer Service', '1997-03-12', 'F', 'ACTIVE', 'R003',
 '2023-05-01 08:00:00', '2023-05-01 08:00:00'),
('S006', 'staff3',
 '$2a$12$3X5u2ueEV9YOC6n2uKRf9uJeuqEWaysXzF1YBxz.W48o4ASZS8E6.',
 'Võ Văn Bình', 'binh.vo@condocare.vn', '0906789012',
 'Security Guard', 'Security', '1993-11-05', 'M', 'ON_LEAVE', 'R003',
 '2023-06-01 08:00:00', '2023-06-01 08:00:00');

-- ============================================================
-- 3. BUILDING
-- ============================================================
INSERT INTO Building (ID, name, address, total_floors, total_apartments, manager_id) VALUES
                                                                                         ('B001', 'Tòa A', '123 Đường Nguyễn Huệ, Quận 1, TP.HCM',    20, 80,  'S002'),
                                                                                         ('B002', 'Tòa B', '456 Đường Lê Lợi, Quận 1, TP.HCM',        15, 60,  'S003'),
                                                                                         ('B003', 'Tòa C', '789 Đường Trần Hưng Đạo, Quận 5, TP.HCM', 25, 100, 'S002');

-- ============================================================
-- 4. APARTMENT
-- ============================================================
-- Tòa A — Tầng 1
INSERT INTO Apartment (ID, number, floor, area, building_id, status, rental_status, total_resident, total_vehicle) VALUES
                                                                                                                       ('A101', 'A101', 1, 80,  'B001', 'EMPTY',       'AVAILABLE', 0, 0),
                                                                                                                       ('A102', 'A102', 1, 80,  'B001', 'OCCUPIED',    'RENTED',    2, 2),
                                                                                                                       ('A103', 'A103', 1, 85,  'B001', 'OCCUPIED',    'OWNER',     4, 3),
                                                                                                                       ('A104', 'A104', 1, 85,  'B001', 'MAINTENANCE', 'AVAILABLE', 0, 0);

-- Tòa A — Tầng 2
INSERT INTO Apartment (ID, number, floor, area, building_id, status, rental_status, total_resident, total_vehicle) VALUES
                                                                                                                       ('A201', 'A201', 2, 90,  'B001', 'OCCUPIED',    'OWNER',     2, 1),
                                                                                                                       ('A202', 'A202', 2, 90,  'B001', 'EMPTY',       'AVAILABLE', 0, 0),
                                                                                                                       ('A203', 'A203', 2, 95,  'B001', 'OCCUPIED',    'RENTED',    3, 2),
                                                                                                                       ('A204', 'A204', 2, 95,  'B001', 'OCCUPIED',    'OWNER',     5, 4);

-- Tòa B — Tầng 1-2
INSERT INTO Apartment (ID, number, floor, area, building_id, status, rental_status, total_resident, total_vehicle) VALUES
                                                                                                                       ('B101', 'B101', 1, 75, 'B002', 'EMPTY',    'AVAILABLE', 0, 0),
                                                                                                                       ('B102', 'B102', 1, 75, 'B002', 'OCCUPIED', 'RENTED',    2, 1),
                                                                                                                       ('B201', 'B201', 2, 80, 'B002', 'OCCUPIED', 'OWNER',     3, 2),
                                                                                                                       ('B202', 'B202', 2, 80, 'B002', 'EMPTY',    'AVAILABLE', 0, 0);

-- Tòa C — Tầng 1
INSERT INTO Apartment (ID, number, floor, area, building_id, status, rental_status, total_resident, total_vehicle) VALUES
                                                                                                                       ('C101', 'C101', 1, 100, 'B003', 'OCCUPIED', 'OWNER',  3, 2),
                                                                                                                       ('C102', 'C102', 1, 100, 'B003', 'EMPTY',    'AVAILABLE', 0, 0);

-- ============================================================
-- 5. RESIDENTS
-- Mật khẩu tất cả: Resident@123
-- ============================================================
INSERT INTO Residents (ID, username, password, full_name, type, dob, gender,
                       id_number, phone, email, status, apartment_id, create_at) VALUES
-- A102 — thuê (2 người)
('RES001', 'hoang.thi.b',
 '$2a$12$mlm3QmRVPk0UIOmyg920W.qsB5Ur496xhTsnmUotDgHhMWDZuwM.a',
 'Hoàng Thị B', 'TENANT', '1992-07-20', 'F',
 '001234567891', '0912345679', 'hoang.b@gmail.com', 'ACTIVE', 'A102', NOW()),
('RES002', 'tran.van.c',
 '$2a$12$mlm3QmRVPk0UIOmyg920W.qsB5Ur496xhTsnmUotDgHhMWDZuwM.a',
 'Trần Văn C', 'TENANT', '1990-03-15', 'M',
 '001234567892', '0912345680', 'tran.c@gmail.com', 'ACTIVE', 'A102', NOW()),
-- A103 — chủ sở hữu (4 người)
('RES003', 'pham.van.a',
 '$2a$12$mlm3QmRVPk0UIOmyg920W.qsB5Ur496xhTsnmUotDgHhMWDZuwM.a',
 'Phạm Văn A', 'OWNER', '1980-03-15', 'M',
 '001234567890', '0912345678', 'pham.a@gmail.com', 'ACTIVE', 'A103', NOW()),
('RES004', 'pham.thi.d',
 '$2a$12$mlm3QmRVPk0UIOmyg920W.qsB5Ur496xhTsnmUotDgHhMWDZuwM.a',
 'Phạm Thị D', 'OWNER', '1982-06-20', 'F',
 '001234567893', '0912345681', NULL, 'ACTIVE', 'A103', NOW()),
('RES005', 'pham.van.e',
 '$2a$12$mlm3QmRVPk0UIOmyg920W.qsB5Ur496xhTsnmUotDgHhMWDZuwM.a',
 'Phạm Văn E (con)', 'GUEST', '2010-01-10', 'M',
 NULL, '0912345682', NULL, 'ACTIVE', 'A103', NOW()),
-- A201 — chủ sở hữu (2 người)
('RES006', 'nguyen.van.f',
 '$2a$12$mlm3QmRVPk0UIOmyg920W.qsB5Ur496xhTsnmUotDgHhMWDZuwM.a',
 'Nguyễn Văn F', 'OWNER', '1975-11-30', 'M',
 '001234567894', '0912345683', 'nguyen.f@gmail.com', 'ACTIVE', 'A201', NOW()),
('RES007', 'nguyen.thi.g',
 '$2a$12$mlm3QmRVPk0UIOmyg920W.qsB5Ur496xhTsnmUotDgHhMWDZuwM.a',
 'Nguyễn Thị G', 'OWNER', '1978-04-05', 'F',
 '001234567895', '0912345684', NULL, 'ACTIVE', 'A201', NOW()),
-- A203 — thuê (3 người)
('RES008', 'le.van.h',
 '$2a$12$mlm3QmRVPk0UIOmyg920W.qsB5Ur496xhTsnmUotDgHhMWDZuwM.a',
 'Lê Văn H', 'TENANT', '1995-09-18', 'M',
 '001234567896', '0912345685', 'le.h@gmail.com', 'ACTIVE', 'A203', NOW()),
('RES009', 'le.thi.i',
 '$2a$12$mlm3QmRVPk0UIOmyg920W.qsB5Ur496xhTsnmUotDgHhMWDZuwM.a',
 'Lê Thị I', 'TENANT', '1997-12-25', 'F',
 '001234567897', '0912345686', NULL, 'ACTIVE', 'A203', NOW()),
-- A204 — chủ sở hữu (5 người)
('RES010', 'vo.van.k',
 '$2a$12$mlm3QmRVPk0UIOmyg920W.qsB5Ur496xhTsnmUotDgHhMWDZuwM.a',
 'Võ Văn K', 'OWNER', '1970-02-14', 'M',
 '001234567898', '0912345687', 'vo.k@gmail.com', 'ACTIVE', 'A204', NOW()),
-- Resident không có căn hộ
('RES011', 'nguyen.thi.l',
 '$2a$12$mlm3QmRVPk0UIOmyg920W.qsB5Ur496xhTsnmUotDgHhMWDZuwM.a',
 'Nguyễn Thị L', 'TENANT', '1998-08-08', 'F',
 '001234567899', '0912345688', 'nguyen.l@gmail.com', 'ACTIVE', NULL, NOW()),
-- Resident INACTIVE
('RES012', 'tran.van.m',
 '$2a$12$mlm3QmRVPk0UIOmyg920W.qsB5Ur496xhTsnmUotDgHhMWDZuwM.a',
 'Trần Văn M', 'TENANT', '1988-05-22', 'M',
 '001234567800', '0912345689', 'tran.m@gmail.com', 'INACTIVE', NULL, NOW());

-- ============================================================
-- 6. VEHICLE
-- ============================================================
INSERT INTO Vehicle (ID, type, license_plate, registered_at, status, resident_id) VALUES
-- A102: xe máy + xe đạp điện
('VH001', 'Xe máy',       '59-B1-12345', NOW(), 'ACTIVE', 'RES001'),
('VH002', 'Xe đạp điện',  NULL,           NOW(), 'ACTIVE', 'RES002'),
-- A103: 2 xe máy + 1 ô tô
('VH003', 'Xe máy',       '51-A2-23456', NOW(), 'ACTIVE', 'RES003'),
('VH004', 'Xe máy',       '51-A2-34567', NOW(), 'ACTIVE', 'RES004'),
('VH005', 'Ô tô',         '51K-123.45',  NOW(), 'ACTIVE', 'RES003'),
-- A201: xe máy
('VH006', 'Xe máy',       '59-C3-45678', NOW(), 'ACTIVE', 'RES006'),
-- A203: xe máy + xe đạp điện
('VH007', 'Xe máy',       '59-D4-56789', NOW(), 'ACTIVE', 'RES008'),
('VH008', 'Xe đạp điện',  NULL,           NOW(), 'ACTIVE', 'RES009'),
-- A204: 2 ô tô + 2 xe máy
('VH009', 'Ô tô',         '51L-678.90',  NOW(), 'ACTIVE', 'RES010'),
('VH010', 'Ô tô',         '51L-789.01',  NOW(), 'ACTIVE', 'RES010'),
('VH011', 'Xe máy',       '59-E5-67890', NOW(), 'ACTIVE', 'RES010'),
('VH012', 'Xe máy',       '59-E5-78901', NOW(), 'ACTIVE', 'RES010');

-- ============================================================
-- 8. FEES — phí thường xuyên của từng căn hộ
-- ============================================================
INSERT INTO Fees (ID, apartment_id, name, description, type, amount,
                  effective_from, effective_to, create_by) VALUES

-- Phí quản lý (SERVICE) — 7.000đ/m²
('F001', 'A102', 'Phí quản lý', 'Phí quản lý 80m² × 7.000đ',  'SERVICE', 560000,  '2026-01-01', NULL, 'S002'),
('F002', 'A103', 'Phí quản lý', 'Phí quản lý 85m² × 7.000đ',  'SERVICE', 595000,  '2026-01-01', NULL, 'S002'),
('F003', 'A201', 'Phí quản lý', 'Phí quản lý 90m² × 7.000đ',  'SERVICE', 630000,  '2026-01-01', NULL, 'S002'),
('F004', 'A203', 'Phí quản lý', 'Phí quản lý 95m² × 7.000đ',  'SERVICE', 665000,  '2026-01-01', NULL, 'S002'),
('F005', 'A204', 'Phí quản lý', 'Phí quản lý 95m² × 7.000đ',  'SERVICE', 665000,  '2026-01-01', NULL, 'S002'),

-- Phí xe máy (PARKING)
('F010', 'A102', 'Phí xe máy',       'Xe máy biển 59-B1-12345', 'PARKING', 100000, '2026-01-01', NULL, 'S002'),
('F011', 'A103', 'Phí xe máy (1)',   'Xe máy biển 51-A2-23456', 'PARKING', 100000, '2026-01-01', NULL, 'S002'),
('F012', 'A103', 'Phí xe máy (2)',   'Xe máy biển 51-A2-34567', 'PARKING', 100000, '2026-01-01', NULL, 'S002'),
('F013', 'A201', 'Phí xe máy',       'Xe máy biển 59-C3-45678', 'PARKING', 100000, '2026-01-01', NULL, 'S002'),
('F014', 'A203', 'Phí xe máy',       'Xe máy biển 59-D4-56789', 'PARKING', 100000, '2026-01-01', NULL, 'S002'),
('F015', 'A204', 'Phí xe máy (1)',   'Xe máy biển 59-E5-67890', 'PARKING', 100000, '2026-01-01', NULL, 'S002'),
('F016', 'A204', 'Phí xe máy (2)',   'Xe máy biển 59-E5-78901', 'PARKING', 100000, '2026-01-01', NULL, 'S002'),

-- Phí ô tô (PARKING)
('F020', 'A103', 'Phí ô tô',         'Ô tô biển 51K-123.45',   'PARKING', 1300000, '2026-01-01', NULL, 'S002'),
('F021', 'A204', 'Phí ô tô (1)',     'Ô tô biển 51L-678.90',   'PARKING', 1300000, '2026-01-01', NULL, 'S002'),
('F022', 'A204', 'Phí ô tô (2)',     'Ô tô biển 51L-789.01',   'PARKING', 1300000, '2026-01-01', NULL, 'S002'),

-- Phí xe đạp điện (PARKING)
('F030', 'A102', 'Phí xe đạp điện', 'Xe đạp điện không biển', 'PARKING', 75000,   '2026-01-01', NULL, 'S002'),
('F031', 'A203', 'Phí xe đạp điện', 'Xe đạp điện không biển', 'PARKING', 75000,   '2026-01-01', NULL, 'S002');

-- ============================================================
-- 9. METER_READING — điện + nước tháng 1 & 2/2026
-- consumption = current - previous, total_amount = consumption × unit_price
-- Điện: 2.800đ/kWh | Nước: 15.929đ/m³
-- ============================================================

-- ── Tháng 1/2026 ───────────────────────────────────────────
INSERT INTO meter_reading (ID, apartment_id, meter_type, month, year,
                           previous_index, current_index, consumption, unit_price, total_amount,
                           recorded_by, recorded_at) VALUES

                                                         ('MR0126A102E', 'A102', 'ELECTRIC', 1, 2026, 1250, 1400, 150, 2800,  420000, 'S004', '2026-01-31 09:00:00'),
                                                         ('MR0126A102W', 'A102', 'WATER',    1, 2026,   88,   96,   8, 15929, 127432, 'S004', '2026-01-31 09:10:00'),

                                                         ('MR0126A103E', 'A103', 'ELECTRIC', 1, 2026, 3100, 3300, 200, 2800,  560000, 'S004', '2026-01-31 09:20:00'),
                                                         ('MR0126A103W', 'A103', 'WATER',    1, 2026,  210,  222,  12, 15929, 191148, 'S004', '2026-01-31 09:30:00'),

                                                         ('MR0126A201E', 'A201', 'ELECTRIC', 1, 2026, 2400, 2580, 180, 2800,  504000, 'S004', '2026-01-31 09:40:00'),
                                                         ('MR0126A201W', 'A201', 'WATER',    1, 2026,  155,  165,  10, 15929, 159290, 'S004', '2026-01-31 09:50:00'),

                                                         ('MR0126A203E', 'A203', 'ELECTRIC', 1, 2026, 1800, 2020, 220, 2800,  616000, 'S004', '2026-01-31 10:00:00'),
                                                         ('MR0126A203W', 'A203', 'WATER',    1, 2026,  320,  334,  14, 15929, 223006, 'S004', '2026-01-31 10:10:00'),

                                                         ('MR0126A204E', 'A204', 'ELECTRIC', 1, 2026, 4500, 4760, 260, 2800,  728000, 'S004', '2026-01-31 10:20:00'),
                                                         ('MR0126A204W', 'A204', 'WATER',    1, 2026,  400,  418,  18, 15929, 286722, 'S004', '2026-01-31 10:30:00');

-- ── Tháng 2/2026 ───────────────────────────────────────────
INSERT INTO meter_reading (ID, apartment_id, meter_type, month, year,
                           previous_index, current_index, consumption, unit_price, total_amount,
                           recorded_by, recorded_at) VALUES

                                                         ('MR0226A102E', 'A102', 'ELECTRIC', 2, 2026, 1400, 1545, 145, 2800,  406000, 'S004', '2026-02-28 09:00:00'),
                                                         ('MR0226A102W', 'A102', 'WATER',    2, 2026,   96,  103,   7, 15929, 111503, 'S004', '2026-02-28 09:10:00'),

                                                         ('MR0226A103E', 'A103', 'ELECTRIC', 2, 2026, 3300, 3490, 190, 2800,  532000, 'S004', '2026-02-28 09:20:00'),
                                                         ('MR0226A103W', 'A103', 'WATER',    2, 2026,  222,  233,  11, 15929, 175219, 'S004', '2026-02-28 09:30:00'),

                                                         ('MR0226A201E', 'A201', 'ELECTRIC', 2, 2026, 2580, 2755, 175, 2800,  490000, 'S004', '2026-02-28 09:40:00'),
                                                         ('MR0226A201W', 'A201', 'WATER',    2, 2026,  165,  174,   9, 15929, 143361, 'S004', '2026-02-28 09:50:00'),

                                                         ('MR0226A203E', 'A203', 'ELECTRIC', 2, 2026, 2020, 2230, 210, 2800,  588000, 'S004', '2026-02-28 10:00:00'),
                                                         ('MR0226A203W', 'A203', 'WATER',    2, 2026,  334,  347,  13, 15929, 207077, 'S004', '2026-02-28 10:10:00'),

                                                         ('MR0226A204E', 'A204', 'ELECTRIC', 2, 2026, 4760, 5010, 250, 2800,  700000, 'S004', '2026-02-28 10:20:00'),
                                                         ('MR0226A204W', 'A204', 'WATER',    2, 2026,  418,  435,  17, 15929, 270793, 'S004', '2026-02-28 10:30:00');

-- ── Tháng 3/2026 (mới ghi, chưa có hóa đơn) ───────────────
INSERT INTO meter_reading (ID, apartment_id, meter_type, month, year,
                           previous_index, current_index, consumption, unit_price, total_amount,
                           recorded_by, recorded_at) VALUES

                                                         ('MR0326A102E', 'A102', 'ELECTRIC', 3, 2026, 1545, 1688, 143, 2800,  400400, 'S004', '2026-03-18 09:00:00'),
                                                         ('MR0326A102W', 'A102', 'WATER',    3, 2026,  103,  110,   7, 15929, 111503, 'S004', '2026-03-18 09:10:00'),

                                                         ('MR0326A103E', 'A103', 'ELECTRIC', 3, 2026, 3490, 3680, 190, 2800,  532000, 'S004', '2026-03-18 09:20:00'),
                                                         ('MR0326A103W', 'A103', 'WATER',    3, 2026,  233,  244,  11, 15929, 175219, 'S004', '2026-03-18 09:30:00');

-- ============================================================
-- 10. INVOICE
-- Cấu trúc:
--   electric_amount, water_amount  = từ meter_reading
--   service_amount                 = từ Fees loại SERVICE
--   parking_amount                 = tổng tất cả Fees loại PARKING
--   parking_fee_id                 = FK đại diện (1 fee đầu tiên)
--   total_amount                   = tổng 4 khoản
-- ============================================================

-- ── Tháng 1/2026 — tất cả PAID ────────────────────────────
INSERT INTO Invoice (ID, apartment_id, month, year,
                     electric_reading_id, water_reading_id, service_fee_id, parking_fee_id,
                     electric_amount, water_amount, service_amount, parking_amount, total_amount,
                     status, issued_at, due_date, paid_at, create_by) VALUES

-- A102: điện 420k + nước 127k + quản lý 560k + (xe máy 100k + xe đạp 75k = 175k) = 1.282.432đ
('INV202601A102', 'A102', 1, 2026,
 'MR0126A102E', 'MR0126A102W', 'F001', 'F010',
 420000, 127432, 560000, 175000, 1282432,
 'PAID', '2026-01-31 08:00:00', '2026-02-15', '2026-02-10 10:00:00', 'S001'),

-- A103: điện 560k + nước 191k + quản lý 595k + (xe máy×2 200k + ô tô 1.300k = 1.500k) = 2.846.148đ
('INV202601A103', 'A103', 1, 2026,
 'MR0126A103E', 'MR0126A103W', 'F002', 'F011',
 560000, 191148, 595000, 1500000, 2846148,
 'PAID', '2026-01-31 08:00:00', '2026-02-15', '2026-02-08 09:00:00', 'S001'),

-- A201: điện 504k + nước 159k + quản lý 630k + xe máy 100k = 1.393.290đ
('INV202601A201', 'A201', 1, 2026,
 'MR0126A201E', 'MR0126A201W', 'F003', 'F013',
 504000, 159290, 630000, 100000, 1393290,
 'PAID', '2026-01-31 08:00:00', '2026-02-15', '2026-02-12 14:00:00', 'S001'),

-- A203: điện 616k + nước 223k + quản lý 665k + (xe máy 100k + xe đạp 75k = 175k) = 1.679.006đ
('INV202601A203', 'A203', 1, 2026,
 'MR0126A203E', 'MR0126A203W', 'F004', 'F014',
 616000, 223006, 665000, 175000, 1679006,
 'PAID', '2026-01-31 08:00:00', '2026-02-15', '2026-02-09 11:00:00', 'S001'),

-- A204: điện 728k + nước 287k + quản lý 665k + (xe máy×2 200k + ô tô×2 2.600k = 2.800k) = 4.479.722đ
('INV202601A204', 'A204', 1, 2026,
 'MR0126A204E', 'MR0126A204W', 'F005', 'F021',
 728000, 286722, 665000, 2800000, 4479722,
 'PAID', '2026-01-31 08:00:00', '2026-02-15', '2026-02-11 16:00:00', 'S001');

-- ── Tháng 2/2026 — mix PAID / UNPAID / OVERDUE ────────────
INSERT INTO Invoice (ID, apartment_id, month, year,
                     electric_reading_id, water_reading_id, service_fee_id, parking_fee_id,
                     electric_amount, water_amount, service_amount, parking_amount, total_amount,
                     status, issued_at, due_date, paid_at, create_by) VALUES

-- A102: OVERDUE (quá hạn 15/3 chưa trả)
('INV202602A102', 'A102', 2, 2026,
 'MR0226A102E', 'MR0226A102W', 'F001', 'F010',
 406000, 111503, 560000, 175000, 1252503,
 'OVERDUE', '2026-02-28 08:00:00', '2026-03-15', NULL, 'S001'),

-- A103: PAID
('INV202602A103', 'A103', 2, 2026,
 'MR0226A103E', 'MR0226A103W', 'F002', 'F011',
 532000, 175219, 595000, 1500000, 2802219,
 'PAID', '2026-02-28 08:00:00', '2026-03-15', '2026-03-05 10:00:00', 'S001'),

-- A201: UNPAID (còn hạn)
('INV202602A201', 'A201', 2, 2026,
 'MR0226A201E', 'MR0226A201W', 'F003', 'F013',
 490000, 143361, 630000, 100000, 1363361,
 'UNPAID', '2026-02-28 08:00:00', '2026-03-15', NULL, 'S001'),

-- A203: UNPAID
('INV202602A203', 'A203', 2, 2026,
 'MR0226A203E', 'MR0226A203W', 'F004', 'F014',
 588000, 207077, 665000, 175000, 1635077,
 'UNPAID', '2026-02-28 08:00:00', '2026-03-15', NULL, 'S001'),

-- A204: OVERDUE
('INV202602A204', 'A204', 2, 2026,
 'MR0226A204E', 'MR0226A204W', 'F005', 'F021',
 700000, 270793, 665000, 2800000, 4435793,
 'OVERDUE', '2026-02-28 08:00:00', '2026-03-15', NULL, 'S001');

-- ============================================================
-- 11. NOTIFICATION
-- ============================================================
INSERT INTO Notification (ID, title, content, type, resident_id, is_read, created_by, created_at) VALUES

-- Thông báo toàn khu (resident_id = NULL)
('N001', 'Bảo trì thang máy Tòa A',
 'Ban quản lý thông báo: thang máy Tòa A sẽ bảo trì ngày 20/03/2026 từ 8h–12h. Cư dân vui lòng sử dụng cầu thang bộ trong thời gian này.',
 'MAINTENANCE', NULL, 0, 'S001', DATE_SUB(NOW(), INTERVAL 5 DAY)),

('N002', 'Nhắc thanh toán phí tháng 2/2026',
 'Hạn thanh toán phí tháng 2/2026 là ngày 15/03/2026. Quý cư dân chưa thanh toán vui lòng hoàn tất sớm để tránh phát sinh phí trễ hạn.',
 'PAYMENT', NULL, 0, 'S001', DATE_SUB(NOW(), INTERVAL 3 DAY)),

('N003', 'Quy định mới thẻ từ bãi đỗ xe',
 'Từ ngày 01/04/2026, bãi đỗ xe áp dụng thẻ từ thông minh. Cư dân vui lòng đến văn phòng ban quản lý đăng ký trước ngày 28/03/2026.',
 'INFO', NULL, 0, 'S002', DATE_SUB(NOW(), INTERVAL 2 DAY)),

('N004', 'Cúp điện khu vực ngày 25/03/2026',
 'Điện lực TP.HCM thông báo cúp điện từ 7h–17h ngày 25/03/2026 để nâng cấp hạ tầng. Ban quản lý đã chuẩn bị máy phát điện dự phòng.',
 'WARNING', NULL, 0, 'S001', DATE_SUB(NOW(), INTERVAL 1 DAY)),

('N005', 'Tổng vệ sinh chung cư quý 1/2026',
 'Ban quản lý tổ chức tổng vệ sinh hành lang, sân thượng và khu vực công cộng vào Chủ nhật 23/03/2026. Mong cư dân hợp tác và không để xe/đồ vật cản lối.',
 'INFO', NULL, 0, 'S003', NOW()),

-- Thông báo cá nhân
('N006', 'Chào mừng cư dân mới!',
 'Ban quản lý xin chào mừng bạn đến với cộng đồng CondoCare! Mọi thắc mắc vui lòng liên hệ văn phòng tầng 1 Tòa A, giờ hành chính 8h–17h.',
 'INFO', 'RES001', 0, 'S002', DATE_SUB(NOW(), INTERVAL 7 DAY)),

('N007', 'Nhắc nhở: Hóa đơn tháng 2 quá hạn',
 'Hóa đơn tháng 2/2026 của căn hộ A102 đã quá hạn thanh toán (15/03/2026). Vui lòng đến văn phòng thanh toán hoặc chuyển khoản để tránh phí phạt.',
 'PAYMENT', 'RES001', 0, 'S001', DATE_SUB(NOW(), INTERVAL 1 DAY)),

('N008', 'Nhắc nhở: Hóa đơn tháng 2 quá hạn',
 'Hóa đơn tháng 2/2026 của căn hộ A204 đã quá hạn thanh toán (15/03/2026). Vui lòng liên hệ ban quản lý để xử lý.',
 'PAYMENT', 'RES010', 0, 'S001', DATE_SUB(NOW(), INTERVAL 1 DAY));

-- ============================================================
-- 12. SERVICE_REQUEST
-- Đủ các trạng thái: PENDING, IN_PROGRESS, DONE, REJECTED
-- ============================================================
INSERT INTO service_request (ID, title, description, category, status, priority,
                             resident_id, apartment_id, assigned_to,
                             note, completion_image, resident_confirmed, confirmed_at, reject_reason,
                             created_at, updated_at) VALUES

-- DONE + resident đã confirm
('SR001', 'Bóng đèn hành lang bị hỏng',
 'Bóng đèn phía trước cửa phòng bị hỏng từ 3 ngày nay, nhờ ban quản lý cho người thay.',
 'ELECTRIC', 'DONE', 'LOW', 'RES003', 'A103', 'S004',
 'Đã thay bóng LED 9W, kiểm tra hoạt động bình thường.',
 NULL, 1, DATE_SUB(NOW(), INTERVAL 5 DAY), NULL,
 DATE_SUB(NOW(), INTERVAL 12 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),

-- DONE nhưng resident chưa confirm
('SR002', 'Vòi nước bồn rửa bị rỉ',
 'Vòi nước tại bồn rửa nhà bếp bị rỉ liên tục, gây lãng phí nước. Mong sửa chữa sớm.',
 'WATER', 'DONE', 'MEDIUM', 'RES001', 'A102', 'S004',
 'Đã thay ron vòi và xiết lại đầu nối. Test không còn rỉ.',
 NULL, 0, NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),

-- IN_PROGRESS
('SR003', 'Điều hòa không mát',
 'Điều hòa phòng khách hoạt động nhưng không làm mát được. Có thể cần nạp gas hoặc vệ sinh.',
 'HVAC', 'IN_PROGRESS', 'MEDIUM', 'RES001', 'A102', 'S004',
 'Đã kiểm tra, cần đặt gas R32. Dự kiến hoàn thành ngày mai.',
 NULL, 0, NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()),

-- PENDING — chờ phân công
('SR004', 'Cửa thoát hiểm bị kẹt',
 'Cửa thoát hiểm tầng 2 không mở được, rất nguy hiểm trong trường hợp khẩn cấp.',
 'STRUCTURE', 'PENDING', 'HIGH', 'RES006', 'A201', NULL,
 NULL, NULL, 0, NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 1 DAY), NULL),

-- PENDING — ưu tiên thấp
('SR005', 'Vết nứt nhỏ trên trần nhà',
 'Phát hiện vết nứt dài khoảng 30cm trên trần phòng ngủ. Chưa có dấu hiệu thấm nước.',
 'STRUCTURE', 'PENDING', 'LOW', 'RES008', 'A203', NULL,
 NULL, NULL, 0, NULL, NULL,
 NOW(), NULL),

-- REJECTED
('SR006', 'Yêu cầu lắp thêm điều hòa phòng ngủ',
 'Nhờ ban quản lý lắp thêm 1 điều hòa cho phòng ngủ thứ 2.',
 'HVAC', 'REJECTED', 'LOW', 'RES003', 'A103', NULL,
 NULL, NULL, 0, NULL,
 'Việc lắp thêm thiết bị điện lớn thuộc trách nhiệm cư dân, không nằm trong phạm vi dịch vụ ban quản lý. Quý cư dân vui lòng tự liên hệ đơn vị lắp đặt có phép.',
 DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),

-- DONE không có ảnh (edge case)
('SR007', 'Đèn hành lang tầng 2 Tòa A chập chờn',
 'Đèn hành lang tầng 2 bật tắt liên tục từ chiều qua, gây khó chịu.',
 'ELECTRIC', 'DONE', 'MEDIUM', 'RES006', 'A201', 'S005',
 'Đã thay bộ tắc-te đèn huỳnh quang, hoạt động ổn định.',
 NULL, 1, DATE_SUB(NOW(), INTERVAL 2 DAY), NULL,
 DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY));

