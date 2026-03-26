-- =============================================================
-- SWP391 - Apartment Management System
-- SCRIPT DỮ LIỆU TEST ĐẦY ĐỦ
-- Phiên bản: 2.0 — Mô hình B (EVN thu điện/nước trực tiếp)
-- Bao gồm: INSERT + kiểm tra constraint + edge cases
-- =============================================================

USE SWP391;
SET FOREIGN_KEY_CHECKS = 0;
 
-- =============================================================
-- PHẦN 1: PHÂN QUYỀN & NHÂN SỰ
-- =============================================================
 
-- -----------------------------------------------------
-- 1. Role
-- -----------------------------------------------------
INSERT INTO `Role` (`ID`, `name`, `description`) VALUES
                                                     ('R001', 'ADMIN',       'Quản trị hệ thống — toàn quyền'),
                                                     ('R002', 'MANAGER',     'Trưởng ban quản lý tòa nhà'),
                                                     ('R003', 'ACCOUNTANT',  'Kế toán — quản lý hóa đơn và thanh toán'),
                                                     ('R004', 'TECHNICIAN',  'Kỹ thuật viên — xử lý yêu cầu sửa chữa'),
                                                     ('R005', 'RECEPTIONIST','Lễ tân — tiếp nhận yêu cầu cư dân');

SELECT 'Role: OK' AS test_result, COUNT(*) AS rows_inserted FROM `Role`;

-- -----------------------------------------------------
-- 2. Staff
-- -----------------------------------------------------
INSERT INTO `Staff` (`ID`, `username`, `password`, `full_name`, `email`, `phone`, `position`, `department`, `dob`, `gender`, `status`, `role_id`, `hired_at`, `create_at`) VALUES
                                                                                                                                                                               ('ST001', 'admin.system',   '$2a$12$G6e4EaUmdIgS.pDzEm063ex3AlTeiwqNmEwOzfdgk06LnpIogrejC', 'Nguyễn Văn Khoa',    'admin@gmail.com',        '0901000001', 'Quản trị viên',       'IT',            '1985-03-15', 'M', 'ACTIVE',   'R001', '2020-01-01 08:00:00', '2020-01-01 08:00:00'),
                                                                                                                                                                               ('ST002', 'manager.toaA',   '$2a$12$G6e4EaUmdIgS.pDzEm063ex3AlTeiwqNmEwOzfdgk06LnpIogrejC', 'Trần Thị Lan',        'lan.tran@gmail.com',     '0901000002', 'Trưởng BQL Tòa A',    'Quản lý',       '1980-07-22', 'F', 'ACTIVE',   'R002', '2020-02-01 08:00:00', '2020-02-01 08:00:00'),
                                                                                                                                                                               ('ST003', 'manager.toaB',   '$2a$12$G6e4EaUmdIgS.pDzEm063ex3AlTeiwqNmEwOzfdgk06LnpIogrejC', 'Lê Văn Bình',         'binh.le@gmail.com',      '0901000003', 'Trưởng BQL Tòa B',    'Quản lý',       '1978-11-10', 'M', 'ACTIVE',   'R002', '2020-02-01 08:00:00', '2020-02-01 08:00:00'),
                                                                                                                                                                               ('ST004', 'ketoan.nguyen',  '$2a$12$G6e4EaUmdIgS.pDzEm063ex3AlTeiwqNmEwOzfdgk06LnpIogrejC', 'Phạm Thị Hoa',        'hoa.pham@gmail.com',     '0901000004', 'Kế toán viên',        'Kế toán',       '1990-05-18', 'F', 'ACTIVE',   'R003', '2021-01-15 08:00:00', '2021-01-15 08:00:00'),
                                                                                                                                                                               ('ST005', 'kythuatvien01',  '$2a$12$G6e4EaUmdIgS.pDzEm063ex3AlTeiwqNmEwOzfdgk06LnpIogrejC', 'Võ Văn Cường',        'cuong.vo@gmail.com',     '0901000005', 'Kỹ thuật viên điện',  'Kỹ thuật',      '1992-09-30', 'M', 'ACTIVE',   'R004', '2021-03-01 08:00:00', '2021-03-01 08:00:00'),
                                                                                                                                                                               ('ST006', 'kythuatvien02',  '$2a$12$G6e4EaUmdIgS.pDzEm063ex3AlTeiwqNmEwOzfdgk06LnpIogrejC', 'Đặng Thị Mỹ',         'my.dang@gmail.com',      '0901000006', 'Kỹ thuật viên nước',  'Kỹ thuật',      '1993-02-14', 'F', 'ACTIVE',   'R004', '2021-03-01 08:00:00', '2021-03-01 08:00:00'),
                                                                                                                                                                               ('ST007', 'letan.toa',      '$2a$12$G6e4EaUmdIgS.pDzEm063ex3AlTeiwqNmEwOzfdgk06LnpIogrejC', 'Bùi Văn Lễ',          'le.bui@gmail.com',       '0901000007', 'Lễ tân',              'Tiếp đón',      '1995-06-20', 'M', 'ON_LEAVE', 'R005', '2022-05-01 08:00:00', '2022-05-01 08:00:00'),
                                                                                                                                                                               ('ST008', 'staff.resigned', '$2a$12$G6e4EaUmdIgS.pDzEm063ex3AlTeiwqNmEwOzfdgk06LnpIogrejC', 'Hoàng Thị Xưa',       'xua.hoang@gmail.com',    '0901000008', 'Nhân viên cũ',        'Kế toán',       '1988-12-01', 'F', 'RESIGNED', 'R003', '2019-01-01 08:00:00', '2019-01-01 08:00:00');

SELECT 'Staff: OK' AS test_result, COUNT(*) AS rows_inserted FROM `Staff`;

-- =============================================================
-- PHẦN 2: TÒA NHÀ & CĂN HỘ
-- =============================================================

-- -----------------------------------------------------
-- 3. Building
-- -----------------------------------------------------
INSERT INTO `Building` (`ID`, `name`, `address`, `total_floors`, `total_apartments`, `manager_id`) VALUES
                                                                                                       ('BLD001', 'Tòa A - Sun City',    '123 Lê Văn Lương, Hà Nội',         25, 200, 'ST002'),
                                                                                                       ('BLD002', 'Tòa B - Moon Tower',  '456 Nguyễn Trãi, Hà Nội',          30, 240, 'ST003'),
                                                                                                       ('BLD003', 'Tòa C - Star Heights','789 Hoàng Quốc Việt, Hà Nội',       20, 160, 'ST002');

SELECT 'Building: OK' AS test_result, COUNT(*) AS rows_inserted FROM `Building`;

-- -----------------------------------------------------
-- 4. Apartment
-- -----------------------------------------------------
INSERT INTO `Apartment` (`ID`, `number`, `floor`, `area`, `building_id`, `status`, `rental_status`, `description`, `total_residents`, `total_vehicles`) VALUES
-- Tòa A
('APT001', 'A101', 1,  65.50, 'BLD001', 'OCCUPIED',    'RENTED',         'Căn 2PN view công viên', 3, 2),
('APT002', 'A102', 1,  72.00, 'BLD001', 'OCCUPIED',    'OWNER_OCCUPIED', 'Căn 3PN góc tầng 1',     2, 1),
('APT003', 'A201', 2,  65.50, 'BLD001', 'OCCUPIED',    'RENTED',         'Căn 2PN tầng 2',         1, 1),
('APT004', 'A202', 2,  90.00, 'BLD001', 'MAINTENANCE', 'AVAILABLE',      'Đang sửa chữa',          0, 0),
('APT005', 'A301', 3, 120.00, 'BLD001', 'EMPTY',       'AVAILABLE',      'Căn penthouse tầng 3',   0, 0),
-- Tòa B
('APT006', 'B101', 1,  55.00, 'BLD002', 'OCCUPIED',    'RENTED',         'Căn 2PN tòa B',          2, 1),
('APT007', 'B201', 2,  75.00, 'BLD002', 'OCCUPIED',    'OWNER_OCCUPIED', 'Căn 3PN tòa B',          4, 2),
('APT008', 'B301', 3,  60.00, 'BLD002', 'EMPTY',       'AVAILABLE',      'Căn đang trống',         0, 0),
-- Tòa C
('APT009', 'C101', 1,  85.00, 'BLD003', 'OCCUPIED',    'RENTED',         'Căn 3PN tòa C',          3, 2),
('APT010', 'C201', 2,  50.00, 'BLD003', 'EMPTY',       'AVAILABLE',      NULL,                     0, 0);

SELECT 'Apartment: OK' AS test_result, COUNT(*) AS rows_inserted FROM `Apartment`;

-- =============================================================
-- PHẦN 3: CƯ DÂN
-- =============================================================

-- -----------------------------------------------------
-- 5. Residents
-- -----------------------------------------------------
INSERT INTO `Residents` (`ID`, `username`, `password`, `full_name`, `type`, `dob`, `gender`, `id_number`, `phone`, `email`, `status`, `apartment_id`, `verified_by`, `verified_at`, `created_at`) VALUES
-- Cư dân đã ACTIVE trong APT001
('RES001', 'nguyen.a',     '$2a$12$VsH3AMnufjF0RHi3VaJKI.8dA.OWNzQwRWT/9eYw9AC3EzhMf8lbS', 'Nguyễn Văn A',      'OWNER',  '1985-04-10', 'M', '001085004010', '0912000001', 'a.nguyen@gmail.com',   'ACTIVE',   'APT001', 'ST002', '2023-01-10 09:00:00', '2023-01-08 10:00:00'),
('RES002', 'tran.b',       '$2a$12$VsH3AMnufjF0RHi3VaJKI.8dA.OWNzQwRWT/9eYw9AC3EzhMf8lbS', 'Trần Thị B',        'TENANT', '1990-08-25', 'F', '001090008025', '0912000002', 'b.tran@gmail.com',     'ACTIVE',   'APT001', 'ST002', '2023-01-10 09:30:00', '2023-01-09 08:30:00'),
('RES003', 'le.c',         '$2a$12$VsH3AMnufjF0RHi3VaJKI.8dA.OWNzQwRWT/9eYw9AC3EzhMf8lbS', 'Lê Văn C (Khách)',  'GUEST',  '2000-12-01', 'M', NULL,           '0912000003', NULL,                   'ACTIVE',   'APT001', 'ST002', '2023-06-01 10:00:00', '2023-05-30 14:00:00'),
-- Cư dân APT002
('RES004', 'pham.d',       '$2a$12$VsH3AMnufjF0RHi3VaJKI.8dA.OWNzQwRWT/9eYw9AC3EzhMf8lbS', 'Phạm Văn D',        'OWNER',  '1978-03-30', 'M', '001078003030', '0912000004', 'd.pham@gmail.com',     'ACTIVE',   'APT002', 'ST002', '2023-02-01 09:00:00', '2023-01-29 11:00:00'),
('RES005', 'vo.e',         '$2a$12$VsH3AMnufjF0RHi3VaJKI.8dA.OWNzQwRWT/9eYw9AC3EzhMf8lbS', 'Võ Thị E',          'TENANT', '1995-11-15', 'F', '001095011015', '0912000005', 'e.vo@gmail.com',       'ACTIVE',   'APT002', 'ST002', '2023-02-01 09:30:00', '2023-01-30 09:00:00'),
-- Cư dân APT003
('RES006', 'hoang.f',      '$2a$12$VsH3AMnufjF0RHi3VaJKI.8dA.OWNzQwRWT/9eYw9AC3EzhMf8lbS', 'Hoàng Văn F',       'TENANT', '1988-07-07', 'M', '001088007007', '0912000006', 'f.hoang@gmail.com',    'ACTIVE',   'APT003', 'ST002', '2023-03-15 09:00:00', '2023-03-13 15:00:00'),
-- Cư dân APT006 (Tòa B)
('RES007', 'dang.g',       '$2a$12$VsH3AMnufjF0RHi3VaJKI.8dA.OWNzQwRWT/9eYw9AC3EzhMf8lbS', 'Đặng Thị G',        'OWNER',  '1982-05-20', 'F', '001082005020', '0912000007', 'g.dang@gmail.com',     'ACTIVE',   'APT006', 'ST003', '2023-04-01 09:00:00', '2023-03-29 10:00:00'),
('RES008', 'bui.h',        '$2a$12$VsH3AMnufjF0RHi3VaJKI.8dA.OWNzQwRWT/9eYw9AC3EzhMf8lbS', 'Bùi Văn H',         'TENANT', '1993-01-25', 'M', '001093001025', '0912000008', 'h.bui@gmail.com',      'ACTIVE',   'APT006', 'ST003', '2023-04-01 09:30:00', '2023-03-30 08:00:00'),
-- Cư dân APT007 (Tòa B)
('RES009', 'do.i',         '$2a$12$VsH3AMnufjF0RHi3VaJKI.8dA.OWNzQwRWT/9eYw9AC3EzhMf8lbS', 'Đỗ Văn I',          'OWNER',  '1975-09-12', 'M', '001075009012', '0912000009', 'i.do@gmail.com',       'ACTIVE',   'APT007', 'ST003', '2022-12-01 09:00:00', '2022-11-28 09:00:00'),
('RES010', 'ngo.j',        '$2a$12$VsH3AMnufjF0RHi3VaJKI.8dA.OWNzQwRWT/9eYw9AC3EzhMf8lbS', 'Ngô Thị J',         'TENANT', '1998-02-28', 'F', '001098002028', '0912000010', 'j.ngo@gmail.com',      'ACTIVE',   'APT007', 'ST003', '2022-12-01 09:30:00', '2022-11-29 10:30:00'),
-- Cư dân APT009 (Tòa C)
('RES011', 'ly.k',         '$2a$12$VsH3AMnufjF0RHi3VaJKI.8dA.OWNzQwRWT/9eYw9AC3EzhMf8lbS','Lý Văn K',           'OWNER',  '1980-06-15', 'M', '001080006015', '0912000011', 'k.ly@gmail.com',       'ACTIVE',   'APT009', 'ST002', '2023-05-01 09:00:00', '2023-04-28 14:00:00'),
-- Cư dân đang PENDING (chờ duyệt)
('RES012', 'pending.user', '$2a$12$VsH3AMnufjF0RHi3VaJKI.8dA.OWNzQwRWT/9eYw9AC3EzhMf8lbS','Trương Thị Mới',     'TENANT', '1999-03-03', 'F', '001099003003', '0912000012', 'new.truong@gmail.com', 'PENDING',  NULL,     NULL,    NULL,                  '2024-04-10 08:00:00'),
-- Cư dân INACTIVE (đã rời tòa)
('RES013', 'old.resident', '$2a$12$VsH3AMnufjF0RHi3VaJKI.8dA.OWNzQwRWT/9eYw9AC3EzhMf8lbS','Vương Văn Cũ',       'TENANT', '1970-10-10', 'M', '001070010010', '0912000013', 'old.vuong@gmail.com',  'INACTIVE', NULL,     'ST002', '2021-01-01 09:00:00', '2020-12-28 11:00:00');

SELECT 'Residents: OK' AS test_result, COUNT(*) AS rows_inserted FROM `Residents`;

-- -----------------------------------------------------
-- 6. access_cards — Thẻ từ ra vào
-- -----------------------------------------------------
INSERT INTO `access_cards` (`ID`, `card_number`, `resident_id`, `issued_by`, `issued_at`, `expired_at`, `status`) VALUES
                                                                                                                      ('AC001', 'CARD-2024-0001', 'RES001', 'ST007', '2023-01-10 10:00:00', '2025-01-10 23:59:59', 'ACTIVE'),
                                                                                                                      ('AC002', 'CARD-2024-0002', 'RES002', 'ST007', '2023-01-10 10:15:00', '2025-01-10 23:59:59', 'ACTIVE'),
                                                                                                                      ('AC003', 'CARD-2024-0003', 'RES004', 'ST007', '2023-02-01 10:00:00', '2025-02-01 23:59:59', 'ACTIVE'),
                                                                                                                      ('AC004', 'CARD-2024-0004', 'RES005', 'ST007', '2023-02-01 10:15:00', '2025-02-01 23:59:59', 'ACTIVE'),
                                                                                                                      ('AC005', 'CARD-2024-0005', 'RES006', 'ST007', '2023-03-15 10:00:00', '2025-03-15 23:59:59', 'ACTIVE'),
                                                                                                                      ('AC006', 'CARD-2024-0006', 'RES007', 'ST007', '2023-04-01 10:00:00', '2025-04-01 23:59:59', 'ACTIVE'),
                                                                                                                      ('AC007', 'CARD-2024-0007', 'RES009', 'ST007', '2022-12-01 10:00:00', '2024-12-01 23:59:59', 'BLOCKED'), -- hết hạn / bị khóa
                                                                                                                      ('AC008', 'CARD-2024-0008', 'RES011', 'ST007', '2023-05-01 10:00:00', '2025-05-01 23:59:59', 'ACTIVE'),
                                                                                                                      ('AC009', 'CARD-2024-0009', 'RES001', 'ST007', '2023-06-01 11:00:00', '2025-06-01 23:59:59', 'LOST'),   -- mất thẻ
                                                                                                                      ('AC010', 'CARD-2024-0010', 'RES010', 'ST007', '2022-12-01 10:30:00', '2025-12-01 23:59:59', 'ACTIVE');

SELECT 'access_cards: OK' AS test_result, COUNT(*) AS rows_inserted FROM `access_cards`;

-- =============================================================
-- 7. Vehicle — Phương tiện
-- =============================================================
INSERT INTO `Vehicle` (`ID`, `type`, `license_plate`, `brand`, `model`, `color`, `resident_id`, `apartment_id`, `duration_type`, `registered_at`, `expired_at`, `pending_status`, `approved_by`, `approved_at`, `status`) VALUES
-- APT001 - Nguyễn Văn A (OWNER)
('VH001', 'MOTORBIKE',    '29B1-12345', 'Honda',   'Wave Alpha',   'Đen',   'RES001', 'APT001', 'MONTHLY',    '2024-01-01 00:00:00', '2024-12-31 23:59:59', 'APPROVED', 'ST002', '2024-01-01 09:00:00', 'ACTIVE'),
('VH002', 'CAR',          '29A-11111',  'Toyota',  'Fortuner',     'Trắng', 'RES001', 'APT001', 'YEARLY',     '2024-01-01 00:00:00', '2024-12-31 23:59:59', 'APPROVED', 'ST002', '2024-01-01 09:15:00', 'ACTIVE'),
-- APT001 - Trần Thị B (TENANT)
('VH003', 'ELECTRIC_BIKE','29X2-99887', 'VinFast', 'Feliz S',      'Xanh',  'RES002', 'APT001', 'MONTHLY',    '2024-01-15 00:00:00', '2024-12-31 23:59:59', 'APPROVED', 'ST002', '2024-01-15 10:00:00', 'ACTIVE'),
-- APT002 - Phạm Văn D
('VH004', 'MOTORBIKE',    '30B2-22222', 'Yamaha',  'Exciter',      'Đỏ',    'RES004', 'APT002', 'QUARTERLY',  '2024-01-01 00:00:00', '2024-03-31 23:59:59', 'APPROVED', 'ST002', '2024-01-01 09:00:00', 'ACTIVE'),
-- APT003 - Hoàng Văn F
('VH005', 'MOTORBIKE',    '29C3-33333', 'Honda',   'SH',           'Bạc',   'RES006', 'APT003', 'MONTHLY',    '2024-02-01 00:00:00', '2024-12-31 23:59:59', 'APPROVED', 'ST002', '2024-02-01 10:00:00', 'ACTIVE'),
-- APT006 - Tòa B
('VH006', 'CAR',          '51A-44444',  'Honda',   'CR-V',         'Xám',   'RES007', 'APT006', 'YEARLY',     '2024-01-01 00:00:00', '2024-12-31 23:59:59', 'APPROVED', 'ST003', '2024-01-01 09:00:00', 'ACTIVE'),
-- APT007 - Đỗ Văn I
('VH007', 'MOTORBIKE',    '29D4-55555', 'Suzuki',  'GSX',          'Đen',   'RES009', 'APT007', 'MONTHLY',    '2024-01-01 00:00:00', '2024-12-31 23:59:59', 'APPROVED', 'ST003', '2024-01-01 09:00:00', 'ACTIVE'),
('VH008', 'CAR',          '29D5-66666', 'Mazda',   'CX-5',         'Đỏ',    'RES009', 'APT007', 'YEARLY',     '2024-01-01 00:00:00', '2024-12-31 23:59:59', 'APPROVED', 'ST003', '2024-01-01 09:15:00', 'ACTIVE'),
-- APT009 - Tòa C
('VH009', 'BICYCLE',      NULL,         'Giant',   'ATX 810',      'Xanh',  'RES011', 'APT009', 'MONTHLY',    '2024-03-01 00:00:00', '2024-12-31 23:59:59', 'APPROVED', 'ST002', '2024-03-01 10:00:00', 'ACTIVE'),
('VH010', 'CAR',          '43A-77777',  'VinFast', 'VF 8',         'Trắng', 'RES011', 'APT009', 'YEARLY',     '2024-03-01 00:00:00', '2025-02-28 23:59:59', 'APPROVED', 'ST002', '2024-03-01 10:15:00', 'ACTIVE'),
-- Xe PENDING — chờ duyệt
('VH011', 'MOTORBIKE',    '29Z9-88888', 'Honda',   'Vision',       'Vàng',  'RES008', 'APT006', 'MONTHLY',    NULL,                  NULL,                  'PENDING',  NULL,    NULL,                  'ACTIVE'),
-- Xe REJECTED — bị từ chối (biển số vi phạm)
('VH012', 'MOTORBIKE',    '00X0-00000', 'Unknown', 'Unknown',      'Đen',   'RES010', 'APT007', 'MONTHLY',    NULL,                  NULL,                  'REJECTED', 'ST003', '2024-02-01 09:00:00', 'INACTIVE');

-- Cập nhật reject_reason cho xe bị từ chối
UPDATE `Vehicle` SET `reject_reason` = 'Biển số không hợp lệ, yêu cầu cung cấp lại' WHERE `ID` = 'VH012';

SELECT 'Vehicle: OK' AS test_result, COUNT(*) AS rows_inserted FROM `Vehicle`;

-- =============================================================
-- PHẦN 4: PHÍ DỊCH VỤ & HÓA ĐƠN
-- =============================================================

-- -----------------------------------------------------
-- 8. FeeTemplate — Mẫu phí
-- -----------------------------------------------------
INSERT INTO `FeeTemplate` (`ID`, `building_id`, `name`, `type`, `amount`, `unit`, `effective_from`, `effective_to`, `status`, `created_by`) VALUES
-- Tòa A
('FT001', 'BLD001', 'Phí quản lý chung',         'SERVICE', 10000.00, 'PER_M2', '2024-01-01', NULL,         'ACTIVE',   'ST002'),
('FT002', 'BLD001', 'Phí gửi xe máy',             'PARKING',  200000.00, 'FIXED', '2024-01-01', NULL,         'ACTIVE',   'ST002'),
('FT003', 'BLD001', 'Phí gửi ô tô',              'PARKING',  1200000.00,'FIXED', '2024-01-01', NULL,         'ACTIVE',   'ST002'),
('FT004', 'BLD001', 'Phí vệ sinh chung',          'SERVICE',  50000.00, 'PER_APT','2024-01-01', NULL,         'ACTIVE',   'ST002'),
('FT005', 'BLD001', 'Phí quản lý cũ (đã hết HH)','SERVICE',   8000.00, 'PER_M2', '2023-01-01', '2023-12-31', 'INACTIVE', 'ST002'),
-- Tòa B
('FT006', 'BLD002', 'Phí quản lý chung',          'SERVICE', 12000.00, 'PER_M2', '2024-01-01', NULL,         'ACTIVE',   'ST003'),
('FT007', 'BLD002', 'Phí gửi xe máy',             'PARKING',  250000.00, 'FIXED', '2024-01-01', NULL,         'ACTIVE',   'ST003'),
('FT008', 'BLD002', 'Phí gửi ô tô',              'PARKING',  1500000.00,'FIXED', '2024-01-01', NULL,         'ACTIVE',   'ST003'),
-- Tòa C
('FT009', 'BLD003', 'Phí quản lý chung',          'SERVICE', 11000.00, 'PER_M2', '2024-01-01', NULL,         'ACTIVE',   'ST002'),
('FT010', 'BLD003', 'Phí gửi xe đạp/xe điện',    'PARKING',   80000.00, 'FIXED', '2024-01-01', NULL,         'ACTIVE',   'ST002');

SELECT 'FeeTemplate: OK' AS test_result, COUNT(*) AS rows_inserted FROM `FeeTemplate`;

-- -----------------------------------------------------
-- 9. Invoice — Hóa đơn
-- (Tháng 3/2024 cho các căn đang OCCUPIED)
-- -----------------------------------------------------
INSERT INTO `Invoice` (`ID`, `apartment_id`, `month`, `year`, `total_amount`, `status`, `issued_at`, `due_date`, `paid_at`, `created_by`) VALUES
-- Tháng 3/2024 - Tòa A
('INV-2024-03-001', 'APT001', 3, 2024, 2305000.00, 'PAID',    '2024-03-01 08:00:00', '2024-03-15', '2024-03-10 14:30:00', 'ST004'),
('INV-2024-03-002', 'APT002', 3, 2024, 1070000.00, 'PAID',    '2024-03-01 08:00:00', '2024-03-15', '2024-03-12 09:15:00', 'ST004'),
('INV-2024-03-003', 'APT003', 3, 2024,  855000.00, 'PAID',    '2024-03-01 08:00:00', '2024-03-15', '2024-03-08 11:00:00', 'ST004'),
-- Tháng 3/2024 - Tòa B
('INV-2024-03-006', 'APT006', 3, 2024, 1410000.00, 'PAID',    '2024-03-01 08:00:00', '2024-03-15', '2024-03-14 16:00:00', 'ST004'),
('INV-2024-03-007', 'APT007', 3, 2024, 2650000.00, 'PAID',    '2024-03-01 08:00:00', '2024-03-15', '2024-03-09 10:00:00', 'ST004'),
-- Tháng 3/2024 - Tòa C
('INV-2024-03-009', 'APT009', 3, 2024, 2185000.00, 'PAID',    '2024-03-01 08:00:00', '2024-03-15', '2024-03-11 15:00:00', 'ST004'),
-- Tháng 4/2024 - Mix trạng thái
('INV-2024-04-001', 'APT001', 4, 2024, 2305000.00, 'PAID',    '2024-04-01 08:00:00', '2024-04-15', '2024-04-13 10:00:00', 'ST004'),
('INV-2024-04-002', 'APT002', 4, 2024, 1070000.00, 'UNPAID',  '2024-04-01 08:00:00', '2024-04-15', NULL,                  'ST004'),
('INV-2024-04-003', 'APT003', 4, 2024,  855000.00, 'OVERDUE', '2024-04-01 08:00:00', '2024-04-15', NULL,                  'ST004'),
('INV-2024-04-006', 'APT006', 4, 2024, 1410000.00, 'UNPAID',  '2024-04-01 08:00:00', '2024-04-15', NULL,                  'ST004');

SELECT 'Invoice: OK' AS test_result, COUNT(*) AS rows_inserted FROM `Invoice`;

-- -----------------------------------------------------
-- 10. invoice_fee_detail — Chi tiết hóa đơn
-- -----------------------------------------------------
INSERT INTO `invoice_fee_detail` (`ID`, `invoice_id`, `fee_template_id`, `fee_name`, `fee_type`, `unit_amount`, `quantity`, `amount`) VALUES
-- INV-2024-03-001 (APT001 - 65.5m², 1 xe máy, 1 ô tô)
('IFD001', 'INV-2024-03-001', 'FT001', 'Phí quản lý chung', 'SERVICE', 10000.00, 65.50,  655000.00),
('IFD002', 'INV-2024-03-001', 'FT004', 'Phí vệ sinh chung', 'SERVICE', 50000.00,  1.00,   50000.00),
('IFD003', 'INV-2024-03-001', 'FT002', 'Phí gửi xe máy',    'PARKING',200000.00,  1.00,  200000.00),
('IFD004', 'INV-2024-03-001', 'FT003', 'Phí gửi ô tô',      'PARKING',1200000.00, 1.00, 1200000.00),
('IFD005', 'INV-2024-03-001', 'FT002', 'Phí gửi xe điện',   'PARKING', 200000.00, 1.00,  200000.00),

-- INV-2024-03-002 (APT002 - 72m², 1 xe máy)
('IFD006', 'INV-2024-03-002', 'FT001', 'Phí quản lý chung', 'SERVICE', 10000.00, 72.00,  720000.00),
('IFD007', 'INV-2024-03-002', 'FT004', 'Phí vệ sinh chung', 'SERVICE', 50000.00,  1.00,   50000.00),
('IFD008', 'INV-2024-03-002', 'FT002', 'Phí gửi xe máy',    'PARKING', 200000.00, 1.00,  300000.00),

-- INV-2024-03-003 (APT003 - 65.5m², 1 xe máy)
('IFD009', 'INV-2024-03-003', 'FT001', 'Phí quản lý chung', 'SERVICE', 10000.00, 65.50,  655000.00),
('IFD010', 'INV-2024-03-003', 'FT004', 'Phí vệ sinh chung', 'SERVICE', 50000.00,  1.00,   50000.00),
('IFD011', 'INV-2024-03-003', 'FT002', 'Phí gửi xe máy',    'PARKING', 200000.00, 1.00,  150000.00),

-- INV-2024-03-006 (APT006 - 55m², 1 ô tô)
('IFD012', 'INV-2024-03-006', 'FT006', 'Phí quản lý chung', 'SERVICE', 12000.00, 55.00,  660000.00),
('IFD013', 'INV-2024-03-006', 'FT008', 'Phí gửi ô tô',      'PARKING',1500000.00, 1.00, 1500000.00),

-- INV-2024-03-007 (APT007 - 75m², 1 xe máy, 1 ô tô)
('IFD014', 'INV-2024-03-007', 'FT006', 'Phí quản lý chung', 'SERVICE', 12000.00, 75.00,  900000.00),
('IFD015', 'INV-2024-03-007', 'FT007', 'Phí gửi xe máy',    'PARKING',  250000.00, 1.00,  250000.00),
('IFD016', 'INV-2024-03-007', 'FT008', 'Phí gửi ô tô',      'PARKING',1500000.00, 1.00, 1500000.00),

-- INV-2024-03-009 (APT009 - 85m², 1 xe đạp, 1 ô tô)
('IFD017', 'INV-2024-03-009', 'FT009', 'Phí quản lý chung',   'SERVICE', 11000.00, 85.00,  935000.00),
('IFD018', 'INV-2024-03-009', 'FT010', 'Phí gửi xe đạp/điện', 'PARKING',  80000.00,  1.00,   80000.00),
('IFD019', 'INV-2024-03-009', NULL,    'Phí gửi ô tô VinFast', 'PARKING',1170000.00,  1.00, 1170000.00), -- template đã NULL (test edge case)

-- INV-2024-04-001 (APT001 - tháng 4, cùng cấu trúc tháng 3)
('IFD020', 'INV-2024-04-001', 'FT001', 'Phí quản lý chung', 'SERVICE', 10000.00, 65.50,  655000.00),
('IFD021', 'INV-2024-04-001', 'FT004', 'Phí vệ sinh chung', 'SERVICE', 50000.00,  1.00,   50000.00),
('IFD022', 'INV-2024-04-001', 'FT002', 'Phí gửi xe máy',    'PARKING', 200000.00, 1.00,  200000.00),
('IFD023', 'INV-2024-04-001', 'FT003', 'Phí gửi ô tô',      'PARKING',1200000.00, 1.00, 1200000.00),
('IFD024', 'INV-2024-04-001', 'FT002', 'Phí gửi xe điện',   'PARKING', 200000.00, 1.00,  200000.00),

-- INV-2024-04-002 (APT002 - UNPAID)
('IFD025', 'INV-2024-04-002', 'FT001', 'Phí quản lý chung', 'SERVICE', 10000.00, 72.00,  720000.00),
('IFD026', 'INV-2024-04-002', 'FT004', 'Phí vệ sinh chung', 'SERVICE', 50000.00,  1.00,   50000.00),
('IFD027', 'INV-2024-04-002', 'FT002', 'Phí gửi xe máy',    'PARKING', 200000.00, 1.00,  300000.00),

-- INV-2024-04-003 (APT003 - OVERDUE)
('IFD028', 'INV-2024-04-003', 'FT001', 'Phí quản lý chung', 'SERVICE', 10000.00, 65.50,  655000.00),
('IFD029', 'INV-2024-04-003', 'FT004', 'Phí vệ sinh chung', 'SERVICE', 50000.00,  1.00,   50000.00),
('IFD030', 'INV-2024-04-003', 'FT002', 'Phí gửi xe máy',    'PARKING', 200000.00, 1.00,  150000.00),

-- INV-2024-04-006 (APT006 - UNPAID)
('IFD031', 'INV-2024-04-006', 'FT006', 'Phí quản lý chung', 'SERVICE', 12000.00, 55.00,  660000.00),
('IFD032', 'INV-2024-04-006', 'FT008', 'Phí gửi ô tô',      'PARKING',1500000.00, 1.00, 1500000.00);

SELECT 'invoice_fee_detail: OK' AS test_result, COUNT(*) AS rows_inserted FROM `invoice_fee_detail`;

-- -----------------------------------------------------
-- 11. Payments — Lịch sử thanh toán
-- -----------------------------------------------------
INSERT INTO `Payments` (`ID`, `invoice_id`, `amount`, `paid_at`, `method`, `momo_trans_id`, `momo_order_id`, `note`, `paid_by`) VALUES
-- INV tháng 3/2024 - đã thanh toán
('PAY-UUID-001', 'INV-2024-03-001', 2305000.00, '2024-03-10 14:30:00', 'MOMO',    'MOMO-TXN-20240310-001', 'APT001-MAR-2024', 'Thanh toán qua MoMo',   'RES001'),
('PAY-UUID-002', 'INV-2024-03-002', 1070000.00, '2024-03-12 09:15:00', 'BANKING', NULL,                    NULL,              'Chuyển khoản VCB',       'RES004'),
('PAY-UUID-003', 'INV-2024-03-003',  855000.00, '2024-03-08 11:00:00', 'CASH',    NULL,                    NULL,              'Nộp trực tiếp tại BQL',  'RES006'),
('PAY-UUID-004', 'INV-2024-03-006', 1410000.00, '2024-03-14 16:00:00', 'ZALOPAY', NULL,                    NULL,              'Thanh toán qua ZaloPay', 'RES007'),
('PAY-UUID-005', 'INV-2024-03-007', 2650000.00, '2024-03-09 10:00:00', 'BANKING', NULL,                    NULL,              'Chuyển khoản Techcombank','RES009'),
('PAY-UUID-006', 'INV-2024-03-009', 2185000.00, '2024-03-11 15:00:00', 'MOMO',    'MOMO-TXN-20240311-006', 'APT009-MAR-2024', 'Thanh toán qua MoMo',   'RES011'),
-- INV tháng 4/2024
('PAY-UUID-007', 'INV-2024-04-001', 2305000.00, '2024-04-13 10:00:00', 'MOMO',    'MOMO-TXN-20240413-007', 'APT001-APR-2024', 'Thanh toán qua MoMo',   'RES001');

SELECT 'Payments: OK' AS test_result, COUNT(*) AS rows_inserted FROM `Payments`;

-- =============================================================
-- PHẦN 5: VẬN HÀNH
-- =============================================================

-- -----------------------------------------------------
-- 12. Notification — Thông báo
-- -----------------------------------------------------
INSERT INTO `Notification` (`ID`, `title`, `content`, `type`, `resident_id`, `apartment_id`, `building_id`, `is_read`, `created_by`) VALUES
-- Broadcast toàn tòa A
('NTF001', 'Lịch bảo trì thang máy tháng 4',
 'Ban quản lý thông báo: Thang máy tòa A sẽ bảo trì vào ngày 20/04/2024 từ 8h-12h. Xin quý cư dân thông cảm.',
 'MAINTENANCE', NULL, NULL, 'BLD001', 0, 'ST002'),

-- Broadcast toàn tòa B
('NTF002', 'Thông báo tổng vệ sinh tòa nhà',
 'Tòa B sẽ tổng vệ sinh vào 25/04/2024. Đề nghị cư dân giữ xe đúng chỗ.',
 'INFO', NULL, NULL, 'BLD002', 0, 'ST003'),

-- Thông báo cá nhân: nhắc đóng tiền OVERDUE
('NTF003', 'Nhắc nhở: Hóa đơn tháng 4 quá hạn',
 'Căn hộ A203 chưa thanh toán hóa đơn tháng 4/2024. Vui lòng thanh toán trước 30/04/2024 để tránh phát sinh phí phạt.',
 'PAYMENT', 'RES006', 'APT003', NULL, 0, 'ST004'),

-- Thông báo khẩn
('NTF004', 'SỰ CỐ: Mất nước tầng 1-5',
 'Hiện tại đường ống cấp nước tầng 1-5 đang gặp sự cố. Đội kỹ thuật đang xử lý. Dự kiến khắc phục xong lúc 18h hôm nay.',
 'URGENT', NULL, NULL, 'BLD001', 1, 'ST002'),

-- Thông báo cá nhân: xác nhận tài khoản
('NTF005', 'Tài khoản của bạn đã được xác minh',
 'Chào Hoàng Văn F, tài khoản cư dân của bạn đã được Ban quản lý xác minh thành công. Bạn có thể đăng nhập và sử dụng các dịch vụ.',
 'INFO', 'RES006', 'APT003', NULL, 1, 'ST002'),

-- Thông báo theo căn hộ
('NTF006', 'Xe đăng ký đã được duyệt',
 'Phương tiện biển số 29A-11111 (Toyota Fortuner) của căn hộ A101 đã được duyệt đăng ký gửi xe.',
 'INFO', 'RES001', 'APT001', NULL, 1, 'ST002'),

-- Cảnh báo
('NTF007', 'Cảnh báo: Thẻ từ sắp hết hạn',
 'Thẻ từ CARD-2024-0007 của căn hộ B201 sẽ hết hạn vào 01/12/2024. Vui lòng liên hệ BQL để gia hạn.',
 'WARNING', 'RES009', 'APT007', NULL, 0, 'ST007'),

-- Thông báo cá nhân cho cư dân PENDING
('NTF008', 'Yêu cầu xác minh danh tính',
 'Chào Trương Thị Mới, tài khoản của bạn đang chờ xác minh. Vui lòng liên hệ Ban quản lý tại quầy lễ tân với CCCD gốc.',
 'INFO', 'RES012', NULL, NULL, 0, 'ST007');

SELECT 'Notification: OK' AS test_result, COUNT(*) AS rows_inserted FROM `Notification`;

-- -----------------------------------------------------
-- 13. service_request — Yêu cầu hỗ trợ kỹ thuật
-- -----------------------------------------------------
INSERT INTO `service_request` (`ID`, `title`, `description`, `category`, `status`, `priority`, `resident_id`, `apartment_id`, `assigned_to`, `note`, `completion_image`, `resident_confirmed`, `confirmed_at`, `reject_reason`) VALUES
-- DONE + cư dân đã xác nhận
('SR001', 'Vòi nước bồn rửa bát bị rỉ',
 'Vòi nước tại bếp căn hộ A101 bị rỉ liên tục từ tối qua, gây lãng phí nước.',
 'WATER', 'DONE', 'HIGH', 'RES001', 'APT001', 'ST006',
 'Đã thay ron đệm, kiểm tra áp lực nước bình thường.',
 'https://storage.apt.vn/sr/SR001_done.jpg', 1, '2024-03-20 15:00:00', NULL),

-- DONE + chờ cư dân xác nhận
('SR002', 'Bóng đèn hành lang tầng 1 bị hỏng',
 'Hành lang tầng 1 tòa A bị tối, 3 bóng đèn liền kề đều hỏng.',
 'ELECTRIC', 'DONE', 'MEDIUM', 'RES004', 'APT002', 'ST005',
 'Đã thay 3 bóng LED 9W. Hoàn thành lúc 14:30.',
 'https://storage.apt.vn/sr/SR002_done.jpg', 0, NULL, NULL),

-- IN_PROGRESS
('SR003', 'Điều hòa không làm lạnh',
 'Điều hòa phòng ngủ căn hộ A201 bật lên nhưng không ra hơi lạnh, có tiếng kêu lạ.',
 'HVAC', 'IN_PROGRESS', 'HIGH', 'RES006', 'APT003', 'ST005',
 'Đã kiểm tra ban đầu: thiếu gas. Đặt mua gas, sẽ xử lý ngày mai.', NULL, 0, NULL, NULL),

-- PENDING (chưa phân công)
('SR004', 'Internet bị chậm vào buổi tối',
 'Từ 20h-23h mạng internet căn A101 rất chậm, ảnh hưởng làm việc từ xa.',
 'INTERNET', 'PENDING', 'MEDIUM', 'RES002', 'APT001', NULL, NULL, NULL, 0, NULL, NULL),

-- PENDING — độ ưu tiên cao
('SR005', 'Trần nhà bị thấm nước',
 'Trần phòng khách căn hộ B101 bị thấm nước từ tầng trên, xuất hiện sau mưa lớn hôm qua.',
 'STRUCTURE', 'PENDING', 'HIGH', 'RES007', 'APT006', NULL, NULL, NULL, 0, NULL, NULL),

-- REJECTED — ngoài phạm vi BQL
('SR006', 'Hỗ trợ lắp đặt máy giặt',
 'Cần kỹ thuật viên hỗ trợ lắp đặt máy giặt mới mua.',
 'OTHER', 'REJECTED', 'LOW', 'RES008', 'APT006', 'ST005',
 'Việc lắp đặt thiết bị cá nhân không nằm trong phạm vi dịch vụ BQL cung cấp.',
 NULL, 0, NULL, 'Yêu cầu lắp đặt thiết bị cá nhân không thuộc phạm vi dịch vụ của Ban quản lý. Vui lòng liên hệ đơn vị cung cấp máy giặt.'),

-- IN_PROGRESS tòa B
('SR007', 'Khoá cửa phòng tắm bị hỏng',
 'Khoá cửa phòng tắm căn B201 bị kẹt, không mở được từ trong ra.',
 'STRUCTURE', 'IN_PROGRESS', 'HIGH', 'RES009', 'APT007', 'ST005',
 'Đang tháo khóa, cần đặt mua khóa thay thế cùng loại.', NULL, 0, NULL, NULL),

-- PENDING tòa C
('SR008', 'Ống thoát sàn nhà tắm bị nghẹt',
 'Nước đọng trong nhà tắm không thoát được, mùi hôi bốc lên.',
 'WATER', 'PENDING', 'MEDIUM', 'RES011', 'APT009', NULL, NULL, NULL, 0, NULL, NULL);

SELECT 'service_request: OK' AS test_result, COUNT(*) AS rows_inserted FROM `service_request`;