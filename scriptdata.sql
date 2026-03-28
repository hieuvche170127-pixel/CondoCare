-- =============================================================
-- SWP391 - Apartment Management System
-- SCRIPT DỮ LIỆU TEST ĐẦY ĐỦ — Phiên bản 3.0
-- Phí dịch vụ theo khung diện tích, nghị định QĐ 33/2025/QĐ-UBND
-- =============================================================

USE SWP391;
SET FOREIGN_KEY_CHECKS = 0;

-- ─────────────────────────────────────────────────────────────
-- 1. Role
-- ─────────────────────────────────────────────────────────────
INSERT INTO `Role` (`ID`, `name`, `description`) VALUES
                                                     ('R001', 'ADMIN',        'Quản trị hệ thống — toàn quyền'),
                                                     ('R002', 'MANAGER',      'Trưởng ban quản lý tòa nhà'),
                                                     ('R003', 'ACCOUNTANT',   'Kế toán — chỉ xem hóa đơn, không tạo/xóa'),
                                                     ('R004', 'TECHNICIAN',   'Kỹ thuật viên — xử lý yêu cầu được phân công'),
                                                     ('R005', 'RECEPTIONIST', 'Lễ tân — tiếp nhận và phân công yêu cầu');

-- ─────────────────────────────────────────────────────────────
-- 2. Staff  (password = "Admin@123" cho tất cả)
-- ─────────────────────────────────────────────────────────────
INSERT INTO `Staff` (`ID`, `username`, `password`, `full_name`, `email`, `phone`,
                     `position`, `department`, `dob`, `gender`, `status`, `role_id`, `hired_at`) VALUES
                                                                                                     ('ST001','admin.system',  '$2a$12$G6e4EaUmdIgS.pDzEm063ex3AlTeiwqNmEwOzfdgk06LnpIogrejC',
                                                                                                      'Nguyễn Văn Khoa',  'admin@gmail.com',       '0901000001','Quản trị viên','IT',        '1985-03-15','M','ACTIVE',  'R001','2020-01-01 08:00:00'),
                                                                                                     ('ST002','manager.toaA',  '$2a$12$G6e4EaUmdIgS.pDzEm063ex3AlTeiwqNmEwOzfdgk06LnpIogrejC',
                                                                                                      'Trần Thị Lan',     'lan.tran@gmail.com',    '0901000002','Trưởng BQL Tòa A','Quản lý','1980-07-22','F','ACTIVE',  'R002','2020-02-01 08:00:00'),
                                                                                                     ('ST003','manager.toaB',  '$2a$12$G6e4EaUmdIgS.pDzEm063ex3AlTeiwqNmEwOzfdgk06LnpIogrejC',
                                                                                                      'Lê Văn Bình',      'binh.le@gmail.com',     '0901000003','Trưởng BQL Tòa B','Quản lý','1978-11-10','M','ACTIVE',  'R002','2020-02-01 08:00:00'),
                                                                                                     ('ST004','ketoan.nguyen', '$2a$12$G6e4EaUmdIgS.pDzEm063ex3AlTeiwqNmEwOzfdgk06LnpIogrejC',
                                                                                                      'Phạm Thị Hoa',     'hoa.pham@gmail.com',    '0901000004','Kế toán viên','Kế toán',   '1990-05-18','F','ACTIVE',  'R003','2021-01-15 08:00:00'),
                                                                                                     ('ST005','kythuatvien01', '$2a$12$G6e4EaUmdIgS.pDzEm063ex3AlTeiwqNmEwOzfdgk06LnpIogrejC',
                                                                                                      'Võ Văn Cường',     'cuong.vo@gmail.com',    '0901000005','KTV điện','Kỹ thuật',       '1992-09-30','M','ACTIVE',  'R004','2021-03-01 08:00:00'),
                                                                                                     ('ST006','kythuatvien02', '$2a$12$G6e4EaUmdIgS.pDzEm063ex3AlTeiwqNmEwOzfdgk06LnpIogrejC',
                                                                                                      'Đặng Thị Mỹ',      'my.dang@gmail.com',     '0901000006','KTV nước','Kỹ thuật',      '1993-02-14','F','ACTIVE',  'R004','2021-03-01 08:00:00'),
                                                                                                     ('ST007','letan.toa',     '$2a$12$G6e4EaUmdIgS.pDzEm063ex3AlTeiwqNmEwOzfdgk06LnpIogrejC',
                                                                                                      'Bùi Văn Lễ',       'le.bui@gmail.com',      '0901000007','Lễ tân','Tiếp đón',        '1995-06-20','M','ON_LEAVE','R005','2022-05-01 08:00:00'),
                                                                                                     ('ST008','staff.resigned','$2a$12$G6e4EaUmdIgS.pDzEm063ex3AlTeiwqNmEwOzfdgk06LnpIogrejC',
                                                                                                      'Hoàng Thị Xưa',    'xua.hoang@gmail.com',   '0901000008','Nhân viên cũ','Kế toán',   '1988-12-01','F','RESIGNED','R003','2019-01-01 08:00:00');

-- ─────────────────────────────────────────────────────────────
-- 3. Building
-- ─────────────────────────────────────────────────────────────
INSERT INTO `Building` (`ID`, `name`, `address`, `total_floors`, `total_apartments`, `manager_id`) VALUES
                                                                                                       ('BLD001','Tòa A - Sun City',    '123 Lê Văn Lương, Hà Nội', 25, 200, 'ST002'),
                                                                                                       ('BLD002','Tòa B - Moon Tower',  '456 Nguyễn Trãi, Hà Nội',  30, 240, 'ST003'),
                                                                                                       ('BLD003','Tòa C - Star Heights','789 Hoàng Quốc Việt, Hà Nội',20,160, 'ST002');

-- ─────────────────────────────────────────────────────────────
-- 4. Apartment  (diện tích đa dạng để test đủ 3 khung)
-- ─────────────────────────────────────────────────────────────
INSERT INTO `Apartment` (`ID`,`number`,`floor`,`area`,`building_id`,`status`,`rental_status`,`description`,`total_residents`,`total_vehicles`) VALUES
-- Tòa A
('APT001','A101',1, 45.00,'BLD001','OCCUPIED',   'RENTED',        'Studio ≤50m²',            2,1),
('APT002','A102',1, 65.50,'BLD001','OCCUPIED',   'OWNER_OCCUPIED','Căn 2PN 51–100m²',         2,1),
('APT003','A201',2, 72.00,'BLD001','OCCUPIED',   'RENTED',        'Căn 2PN 51–100m²',         1,1),
('APT004','A202',2,120.00,'BLD001','OCCUPIED',   'OWNER_OCCUPIED','Căn penthouse >100m²',      3,2),
('APT005','A301',3, 90.00,'BLD001','MAINTENANCE','AVAILABLE',     'Đang sửa chữa',             0,0),
-- Tòa B
('APT006','B101',1, 48.00,'BLD002','OCCUPIED',   'RENTED',        'Studio ≤50m²',             2,1),
('APT007','B201',2, 75.00,'BLD002','OCCUPIED',   'OWNER_OCCUPIED','Căn 3PN 51–100m²',          4,2),
('APT008','B301',3,110.00,'BLD002','EMPTY',      'AVAILABLE',     'Căn >100m² đang trống',    0,0),
-- Tòa C
('APT009','C101',1, 85.00,'BLD003','OCCUPIED',   'RENTED',        'Căn 3PN 51–100m²',          3,2),
('APT010','C201',2, 50.00,'BLD003','EMPTY',      'AVAILABLE',     'Căn đúng ngưỡng 50m²',     0,0);

-- ─────────────────────────────────────────────────────────────
-- 5. Residents
-- ─────────────────────────────────────────────────────────────
INSERT INTO `Residents` (`ID`,`username`,`password`,`full_name`,`type`,`dob`,`gender`,
                         `id_number`,`phone`,`email`,`status`,`apartment_id`,`verified_by`,`verified_at`,`created_at`) VALUES
                                                                                                                           ('RES001','nguyen.a', '$2a$12$VsH3AMnufjF0RHi3VaJKI.8dA.OWNzQwRWT/9eYw9AC3EzhMf8lbS',
                                                                                                                            'Nguyễn Văn A','OWNER', '1985-04-10','M','001085004010','0912000001','a.nguyen@gmail.com',
                                                                                                                            'ACTIVE','APT001','ST002','2023-01-10 09:00:00','2023-01-08 10:00:00'),
                                                                                                                           ('RES002','tran.b',   '$2a$12$VsH3AMnufjF0RHi3VaJKI.8dA.OWNzQwRWT/9eYw9AC3EzhMf8lbS',
                                                                                                                            'Trần Thị B',  'TENANT','1990-08-25','F','001090008025','0912000002','b.tran@gmail.com',
                                                                                                                            'ACTIVE','APT001','ST002','2023-01-10 09:30:00','2023-01-09 08:30:00'),
                                                                                                                           ('RES003','pham.d',   '$2a$12$VsH3AMnufjF0RHi3VaJKI.8dA.OWNzQwRWT/9eYw9AC3EzhMf8lbS',
                                                                                                                            'Phạm Văn D',  'OWNER', '1978-03-30','M','001078003030','0912000003','d.pham@gmail.com',
                                                                                                                            'ACTIVE','APT002','ST002','2023-02-01 09:00:00','2023-01-29 11:00:00'),
                                                                                                                           ('RES004','vo.e',     '$2a$12$VsH3AMnufjF0RHi3VaJKI.8dA.OWNzQwRWT/9eYw9AC3EzhMf8lbS',
                                                                                                                            'Võ Thị E',    'TENANT','1995-11-15','F','001095011015','0912000004','e.vo@gmail.com',
                                                                                                                            'ACTIVE','APT002','ST002','2023-02-01 09:30:00','2023-01-30 09:00:00'),
                                                                                                                           ('RES005','hoang.f',  '$2a$12$VsH3AMnufjF0RHi3VaJKI.8dA.OWNzQwRWT/9eYw9AC3EzhMf8lbS',
                                                                                                                            'Hoàng Văn F', 'TENANT','1988-07-07','M','001088007007','0912000005','f.hoang@gmail.com',
                                                                                                                            'ACTIVE','APT003','ST002','2023-03-15 09:00:00','2023-03-13 15:00:00'),
                                                                                                                           ('RES006','le.g',     '$2a$12$VsH3AMnufjF0RHi3VaJKI.8dA.OWNzQwRWT/9eYw9AC3EzhMf8lbS',
                                                                                                                            'Lê Văn G',    'OWNER', '1975-05-10','M','001075005010','0912000006','g.le@gmail.com',
                                                                                                                            'ACTIVE','APT004','ST002','2023-04-01 09:00:00','2023-03-29 10:00:00'),
                                                                                                                           ('RES007','dang.h',   '$2a$12$VsH3AMnufjF0RHi3VaJKI.8dA.OWNzQwRWT/9eYw9AC3EzhMf8lbS',
                                                                                                                            'Đặng Thị H',  'OWNER', '1982-05-20','F','001082005020','0912000007','h.dang@gmail.com',
                                                                                                                            'ACTIVE','APT006','ST003','2023-04-01 09:00:00','2023-03-29 10:00:00'),
                                                                                                                           ('RES008','bui.i',    '$2a$12$VsH3AMnufjF0RHi3VaJKI.8dA.OWNzQwRWT/9eYw9AC3EzhMf8lbS',
                                                                                                                            'Bùi Văn I',   'OWNER', '1975-09-12','M','001075009012','0912000008','i.bui@gmail.com',
                                                                                                                            'ACTIVE','APT007','ST003','2022-12-01 09:00:00','2022-11-28 09:00:00'),
                                                                                                                           ('RES009','ngo.j',    '$2a$12$VsH3AMnufjF0RHi3VaJKI.8dA.OWNzQwRWT/9eYw9AC3EzhMf8lbS',
                                                                                                                            'Ngô Thị J',   'TENANT','1998-02-28','F','001098002028','0912000009','j.ngo@gmail.com',
                                                                                                                            'ACTIVE','APT007','ST003','2022-12-01 09:30:00','2022-11-29 10:30:00'),
                                                                                                                           ('RES010','ly.k',     '$2a$12$VsH3AMnufjF0RHi3VaJKI.8dA.OWNzQwRWT/9eYw9AC3EzhMf8lbS',
                                                                                                                            'Lý Văn K',    'OWNER', '1980-06-15','M','001080006015','0912000010','k.ly@gmail.com',
                                                                                                                            'ACTIVE','APT009','ST002','2023-05-01 09:00:00','2023-04-28 14:00:00'),
-- PENDING + INACTIVE
                                                                                                                           ('RES011','pending.user','$2a$12$VsH3AMnufjF0RHi3VaJKI.8dA.OWNzQwRWT/9eYw9AC3EzhMf8lbS',
                                                                                                                            'Trương Thị Mới','TENANT','1999-03-03','F','001099003003','0912000011','new.truong@gmail.com',
                                                                                                                            'PENDING',NULL,NULL,NULL,'2024-04-10 08:00:00'),
                                                                                                                           ('RES012','old.resident','$2a$12$VsH3AMnufjF0RHi3VaJKI.8dA.OWNzQwRWT/9eYw9AC3EzhMf8lbS',
                                                                                                                            'Vương Văn Cũ','TENANT','1970-10-10','M','001070010010','0912000012','old.vuong@gmail.com',
                                                                                                                            'INACTIVE',NULL,'ST002','2021-01-01 09:00:00','2020-12-28 11:00:00');

-- ─────────────────────────────────────────────────────────────
-- 6. access_cards
-- ─────────────────────────────────────────────────────────────
INSERT INTO `access_cards` (`ID`,`card_number`,`resident_id`,`issued_by`,`issued_at`,`expired_at`,`status`) VALUES
                                                                                                                ('AC001','CARD-2025-0001','RES001','ST007','2025-01-01 10:00:00','2027-01-01 23:59:59','ACTIVE'),
                                                                                                                ('AC002','CARD-2025-0002','RES002','ST007','2025-01-01 10:15:00','2027-01-01 23:59:59','ACTIVE'),
                                                                                                                ('AC003','CARD-2025-0003','RES003','ST007','2025-02-01 10:00:00','2027-02-01 23:59:59','ACTIVE'),
                                                                                                                ('AC004','CARD-2025-0004','RES005','ST007','2025-03-15 10:00:00','2027-03-15 23:59:59','ACTIVE'),
                                                                                                                ('AC005','CARD-2025-0005','RES007','ST007','2025-04-01 10:00:00','2027-04-01 23:59:59','ACTIVE'),
                                                                                                                ('AC006','CARD-2025-0006','RES008','ST007','2025-01-01 10:00:00','2027-01-01 23:59:59','ACTIVE'),
                                                                                                                ('AC007','CARD-2025-0007','RES010','ST007','2025-05-01 10:00:00','2027-05-01 23:59:59','ACTIVE'),
                                                                                                                ('AC008','CARD-2024-OLD1','RES001','ST007','2023-01-01 10:00:00','2025-01-01 23:59:59','BLOCKED');

-- ─────────────────────────────────────────────────────────────
-- 7. Vehicle
-- ─────────────────────────────────────────────────────────────
INSERT INTO `Vehicle` (`ID`,`type`,`license_plate`,`brand`,`model`,`color`,
                       `resident_id`,`apartment_id`,`duration_type`,`registered_at`,`expired_at`,
                       `pending_status`,`approved_by`,`approved_at`,`status`) VALUES
                                                                                  ('VH001','MOTORBIKE',   '29B1-12345','Honda',  'Wave Alpha','Đen',  'RES001','APT001','MONTHLY', '2025-05-01 00:00:00','2026-04-30 23:59:59','APPROVED','ST002','2025-05-01 09:00:00','ACTIVE'),
                                                                                  ('VH002','MOTORBIKE',   '29B2-22222','Yamaha', 'Exciter',   'Đỏ',  'RES003','APT002','MONTHLY', '2025-05-01 00:00:00','2026-04-30 23:59:59','APPROVED','ST002','2025-05-01 09:00:00','ACTIVE'),
                                                                                  ('VH003','CAR',         '29A-11111', 'Toyota', 'Fortuner',  'Trắng','RES003','APT002','YEARLY',  '2025-05-01 00:00:00','2026-04-30 23:59:59','APPROVED','ST002','2025-05-01 09:15:00','ACTIVE'),
                                                                                  ('VH004','ELECTRIC_BIKE','29X2-99887','VinFast','Feliz S',  'Xanh', 'RES005','APT003','MONTHLY', '2025-05-01 00:00:00','2026-04-30 23:59:59','APPROVED','ST002','2025-05-01 10:00:00','ACTIVE'),
                                                                                  ('VH005','CAR',         '29A-44444', 'Mazda',  'CX-5',      'Đen', 'RES006','APT004','YEARLY',  '2025-05-01 00:00:00','2026-04-30 23:59:59','APPROVED','ST002','2025-05-01 09:00:00','ACTIVE'),
                                                                                  ('VH006','MOTORBIKE',   '29A-55555', 'Honda',  'SH',        'Bạc', 'RES006','APT004','MONTHLY', '2025-05-01 00:00:00','2026-04-30 23:59:59','APPROVED','ST002','2025-05-01 09:00:00','ACTIVE'),
                                                                                  ('VH007','CAR',         '51A-66666', 'Honda',  'CR-V',      'Xám', 'RES007','APT006','YEARLY',  '2025-05-01 00:00:00','2026-04-30 23:59:59','APPROVED','ST003','2025-05-01 09:00:00','ACTIVE'),
                                                                                  ('VH008','MOTORBIKE',   '29D4-77777','Suzuki', 'GSX',       'Đen', 'RES008','APT007','MONTHLY', '2025-05-01 00:00:00','2026-04-30 23:59:59','APPROVED','ST003','2025-05-01 09:00:00','ACTIVE'),
                                                                                  ('VH009','CAR',         '29D5-88888','Mazda',  'CX-3',      'Trắng','RES008','APT007','YEARLY', '2025-05-01 00:00:00','2026-04-30 23:59:59','APPROVED','ST003','2025-05-01 09:15:00','ACTIVE'),
                                                                                  ('VH010','BICYCLE',     NULL,        'Giant',  'ATX 810',   'Xanh','RES010','APT009','MONTHLY', '2025-05-01 00:00:00','2026-04-30 23:59:59','APPROVED','ST002','2025-05-01 10:00:00','ACTIVE'),
                                                                                  ('VH011','CAR',         '43A-99999', 'VinFast','VF 8',      'Trắng','RES010','APT009','YEARLY', '2025-05-01 00:00:00','2026-04-30 23:59:59','APPROVED','ST002','2025-05-01 10:15:00','ACTIVE'),
                                                                                  ('VH012','MOTORBIKE',   '29Z9-00001','Honda',  'Vision',    'Vàng','RES009','APT007','MONTHLY', NULL,NULL,'PENDING',NULL,NULL,'ACTIVE');

-- =============================================================
-- 8. FeeTemplate — Khung phí theo diện tích, QĐ 33/2025/QĐ-UBND
--    Hiệu lực: 01/05/2025
--
--    Phí quản lý vận hành (PER_M2) — 3 khung diện tích:
--      ≤ 50 m²      : 8.500 đ/m²  (trong khung 1.200–16.500)
--      51 – 100 m²  : 7.000 đ/m²  (mức phổ biến nhất)
--      > 100 m²     : 5.500 đ/m²  (khuyến khích căn lớn)
--
--    Phí vệ sinh khu vực chung (PER_APT, FIXED) — không phân khung
--
--    Phí gửi xe (PARKING, FIXED) — không phân khung, theo loại xe:
--      Xe máy        : 100.000 đ/xe/tháng
--      Ô tô          : 1.200.000 đ/xe/tháng
--      Xe điện/máy điện: 100.000 đ/xe/tháng
--      Xe đạp        : 50.000 đ/xe/tháng
--
-- Cột min_area/max_area chỉ điền với phí PER_M2.
-- Phí PARKING và PER_APT để NULL cả hai cột.
-- =============================================================

-- ── TÒA A (BLD001) ──────────────────────────────────────────
INSERT INTO `FeeTemplate`
(`ID`,`building_id`,`name`,`type`,`amount`,`unit`,`min_area`,`max_area`,`effective_from`,`effective_to`,`status`,`created_by`)
VALUES
-- Phí quản lý vận hành — 3 khung
('FT001','BLD001','Phí QLVH ≤50m²',                'SERVICE', 8500.00,'PER_M2', NULL,  50.00,'2025-05-01',NULL,'ACTIVE','ST002'),
('FT002','BLD001','Phí QLVH 51–100m²',             'SERVICE', 7000.00,'PER_M2',50.01,100.00,'2025-05-01',NULL,'ACTIVE','ST002'),
('FT003','BLD001','Phí QLVH >100m²',               'SERVICE', 5500.00,'PER_M2',100.01, NULL,'2025-05-01',NULL,'ACTIVE','ST002'),
-- Phí vệ sinh (1 căn 1 mức, không phân khung)
('FT004','BLD001','Phí vệ sinh khu vực chung',      'SERVICE',50000.00,'PER_APT',NULL,  NULL,'2025-05-01',NULL,'ACTIVE','ST002'),
-- Phí gửi xe
('FT005','BLD001','Phí gửi xe máy',                'PARKING',100000.00,'FIXED', NULL,  NULL,'2025-05-01',NULL,'ACTIVE','ST002'),
('FT006','BLD001','Phí gửi ô tô',                  'PARKING',1200000.00,'FIXED',NULL,  NULL,'2025-05-01',NULL,'ACTIVE','ST002'),
('FT007','BLD001','Phí gửi xe điện/xe máy điện',   'PARKING',100000.00,'FIXED', NULL,  NULL,'2025-05-01',NULL,'ACTIVE','ST002'),
('FT008','BLD001','Phí gửi xe đạp',                'PARKING', 50000.00,'FIXED', NULL,  NULL,'2025-05-01',NULL,'ACTIVE','ST002');

-- ── TÒA B (BLD002) ──────────────────────────────────────────
INSERT INTO `FeeTemplate`
(`ID`,`building_id`,`name`,`type`,`amount`,`unit`,`min_area`,`max_area`,`effective_from`,`effective_to`,`status`,`created_by`)
VALUES
    ('FT009','BLD002','Phí QLVH ≤50m²',                'SERVICE', 8500.00,'PER_M2', NULL,  50.00,'2025-05-01',NULL,'ACTIVE','ST003'),
    ('FT010','BLD002','Phí QLVH 51–100m²',             'SERVICE', 7000.00,'PER_M2',50.01,100.00,'2025-05-01',NULL,'ACTIVE','ST003'),
    ('FT011','BLD002','Phí QLVH >100m²',               'SERVICE', 5500.00,'PER_M2',100.01, NULL,'2025-05-01',NULL,'ACTIVE','ST003'),
    ('FT012','BLD002','Phí vệ sinh khu vực chung',      'SERVICE',50000.00,'PER_APT',NULL,  NULL,'2025-05-01',NULL,'ACTIVE','ST003'),
    ('FT013','BLD002','Phí gửi xe máy',                'PARKING',100000.00,'FIXED', NULL,  NULL,'2025-05-01',NULL,'ACTIVE','ST003'),
    ('FT014','BLD002','Phí gửi ô tô',                  'PARKING',1200000.00,'FIXED',NULL,  NULL,'2025-05-01',NULL,'ACTIVE','ST003'),
    ('FT015','BLD002','Phí gửi xe điện/xe máy điện',   'PARKING',100000.00,'FIXED', NULL,  NULL,'2025-05-01',NULL,'ACTIVE','ST003'),
    ('FT016','BLD002','Phí gửi xe đạp',                'PARKING', 50000.00,'FIXED', NULL,  NULL,'2025-05-01',NULL,'ACTIVE','ST003');

-- ── TÒA C (BLD003) ──────────────────────────────────────────
INSERT INTO `FeeTemplate`
(`ID`,`building_id`,`name`,`type`,`amount`,`unit`,`min_area`,`max_area`,`effective_from`,`effective_to`,`status`,`created_by`)
VALUES
    ('FT017','BLD003','Phí QLVH ≤50m²',                'SERVICE', 8500.00,'PER_M2', NULL,  50.00,'2025-05-01',NULL,'ACTIVE','ST002'),
    ('FT018','BLD003','Phí QLVH 51–100m²',             'SERVICE', 7000.00,'PER_M2',50.01,100.00,'2025-05-01',NULL,'ACTIVE','ST002'),
    ('FT019','BLD003','Phí QLVH >100m²',               'SERVICE', 5500.00,'PER_M2',100.01, NULL,'2025-05-01',NULL,'ACTIVE','ST002'),
    ('FT020','BLD003','Phí vệ sinh khu vực chung',      'SERVICE',50000.00,'PER_APT',NULL,  NULL,'2025-05-01',NULL,'ACTIVE','ST002'),
    ('FT021','BLD003','Phí gửi xe máy',                'PARKING',100000.00,'FIXED', NULL,  NULL,'2025-05-01',NULL,'ACTIVE','ST002'),
    ('FT022','BLD003','Phí gửi ô tô',                  'PARKING',1200000.00,'FIXED',NULL,  NULL,'2025-05-01',NULL,'ACTIVE','ST002'),
    ('FT023','BLD003','Phí gửi xe điện/xe máy điện',   'PARKING',100000.00,'FIXED', NULL,  NULL,'2025-05-01',NULL,'ACTIVE','ST002'),
    ('FT024','BLD003','Phí gửi xe đạp',                'PARKING', 50000.00,'FIXED', NULL,  NULL,'2025-05-01',NULL,'ACTIVE','ST002');

-- =============================================================
-- 9. Invoice — Tháng 5–6/2025 (sau khi nghị định có hiệu lực)
--
-- Kiểm tra đúng logic phân khung:
--   APT001 (45m²  ≤50)  → 8.500 × 45  = 382.500  + vs + 50.000  = 432.500  + 100.000xe máy = 532.500
--   APT002 (65.5m² 51–100)→7.000 × 65.5= 458.500  + 50.000      = 508.500  + 100.000 + 1.200.000 = 1.808.500
--   APT003 (72m²  51–100)→7.000 × 72  = 504.000  + 50.000      = 554.000  + 100.000xe điện = 654.000
--   APT004 (120m² >100) → 5.500 × 120 = 660.000  + 50.000      = 710.000  + 1.200.000 + 100.000 = 2.010.000
--   APT006 (48m²  ≤50)  → 8.500 × 48  = 408.000  + 50.000      = 458.000  + 1.200.000 = 1.658.000
--   APT007 (75m²  51–100)→7.000 × 75  = 525.000  + 50.000      = 575.000  + 100.000 + 1.200.000 = 1.875.000
--   APT009 (85m²  51–100)→7.000 × 85  = 595.000  + 50.000      = 645.000  + 50.000 + 1.200.000 = 1.895.000
-- =============================================================
INSERT INTO `Invoice` (`ID`,`apartment_id`,`month`,`year`,`total_amount`,`status`,`issued_at`,`due_date`,`paid_at`,`created_by`) VALUES
-- Tháng 5/2025
('INV202505001','APT001',5,2025,  532500.00,'PAID',   '2025-05-01 08:00:00','2025-05-15','2025-05-10 14:00:00','ST004'),
('INV202505002','APT002',5,2025,1808500.00,'PAID',   '2025-05-01 08:00:00','2025-05-15','2025-05-12 09:00:00','ST004'),
('INV202505003','APT003',5,2025,  654000.00,'PAID',   '2025-05-01 08:00:00','2025-05-15','2025-05-08 11:00:00','ST004'),
('INV202505004','APT004',5,2025,2010000.00,'PAID',   '2025-05-01 08:00:00','2025-05-15','2025-05-14 10:00:00','ST004'),
('INV202505006','APT006',5,2025,1658000.00,'PAID',   '2025-05-01 08:00:00','2025-05-15','2025-05-13 16:00:00','ST004'),
('INV202505007','APT007',5,2025,1875000.00,'PAID',   '2025-05-01 08:00:00','2025-05-15','2025-05-09 10:00:00','ST004'),
('INV202505009','APT009',5,2025,1895000.00,'PAID',   '2025-05-01 08:00:00','2025-05-15','2025-05-11 15:00:00','ST004'),
-- Tháng 6/2025 — mix trạng thái
('INV202506001','APT001',6,2025,  532500.00,'PAID',   '2025-06-01 08:00:00','2025-06-15','2025-06-13 10:00:00','ST004'),
('INV202506002','APT002',6,2025,1808500.00,'UNPAID',  '2025-06-01 08:00:00','2025-06-15', NULL,                'ST004'),
('INV202506003','APT003',6,2025,  654000.00,'OVERDUE','2025-06-01 08:00:00','2025-06-15', NULL,                'ST004'),
('INV202506006','APT006',6,2025,1658000.00,'UNPAID',  '2025-06-01 08:00:00','2025-06-15', NULL,                'ST004');

-- ─────────────────────────────────────────────────────────────
-- 10. invoice_fee_detail (snapshot tại thời điểm phát hành)
-- ─────────────────────────────────────────────────────────────
INSERT INTO `invoice_fee_detail`
(`ID`,`invoice_id`,`fee_template_id`,`fee_name`,`fee_type`,`unit_amount`,`quantity`,`amount`)
VALUES
-- INV202505001: APT001 45m² ≤50 → FT001 + FT004 + FT005(xe máy)
('IFD001','INV202505001','FT001','Phí QLVH ≤50m²',             'SERVICE', 8500.00, 45.00,  382500.00),
('IFD002','INV202505001','FT004','Phí vệ sinh khu vực chung',   'SERVICE',50000.00,  1.00,   50000.00),
('IFD003','INV202505001','FT005','Phí gửi xe máy',             'PARKING',100000.00, 1.00,  100000.00),

-- INV202505002: APT002 65.5m² → FT002 + FT004 + FT005 + FT006(ô tô)
('IFD004','INV202505002','FT002','Phí QLVH 51–100m²',          'SERVICE', 7000.00, 65.50,  458500.00),
('IFD005','INV202505002','FT004','Phí vệ sinh khu vực chung',   'SERVICE',50000.00,  1.00,   50000.00),
('IFD006','INV202505002','FT005','Phí gửi xe máy',             'PARKING',100000.00, 1.00,  100000.00),
('IFD007','INV202505002','FT006','Phí gửi ô tô',               'PARKING',1200000.00,1.00, 1200000.00),

-- INV202505003: APT003 72m² → FT002 + FT004 + FT007(xe điện)
('IFD008','INV202505003','FT002','Phí QLVH 51–100m²',          'SERVICE', 7000.00, 72.00,  504000.00),
('IFD009','INV202505003','FT004','Phí vệ sinh khu vực chung',   'SERVICE',50000.00,  1.00,   50000.00),
('IFD010','INV202505003','FT007','Phí gửi xe điện/xe máy điện','PARKING',100000.00, 1.00,  100000.00),

-- INV202505004: APT004 120m² → FT003 + FT004 + FT006(ô tô) + FT005(xe máy)
('IFD011','INV202505004','FT003','Phí QLVH >100m²',            'SERVICE', 5500.00,120.00,  660000.00),
('IFD012','INV202505004','FT004','Phí vệ sinh khu vực chung',   'SERVICE',50000.00,  1.00,   50000.00),
('IFD013','INV202505004','FT006','Phí gửi ô tô',               'PARKING',1200000.00,1.00, 1200000.00),
('IFD014','INV202505004','FT005','Phí gửi xe máy',             'PARKING',100000.00, 1.00,  100000.00),

-- INV202505006: APT006 48m² → FT009 + FT012 + FT014(ô tô)
('IFD015','INV202505006','FT009','Phí QLVH ≤50m²',             'SERVICE', 8500.00, 48.00,  408000.00),
('IFD016','INV202505006','FT012','Phí vệ sinh khu vực chung',   'SERVICE',50000.00,  1.00,   50000.00),
('IFD017','INV202505006','FT014','Phí gửi ô tô',               'PARKING',1200000.00,1.00, 1200000.00),

-- INV202505007: APT007 75m² → FT010 + FT012 + FT013(xe máy) + FT014(ô tô)
('IFD018','INV202505007','FT010','Phí QLVH 51–100m²',          'SERVICE', 7000.00, 75.00,  525000.00),
('IFD019','INV202505007','FT012','Phí vệ sinh khu vực chung',   'SERVICE',50000.00,  1.00,   50000.00),
('IFD020','INV202505007','FT013','Phí gửi xe máy',             'PARKING',100000.00, 1.00,  100000.00),
('IFD021','INV202505007','FT014','Phí gửi ô tô',               'PARKING',1200000.00,1.00, 1200000.00),

-- INV202505009: APT009 85m² → FT018 + FT020 + FT024(xe đạp) + FT022(ô tô)
('IFD022','INV202505009','FT018','Phí QLVH 51–100m²',          'SERVICE', 7000.00, 85.00,  595000.00),
('IFD023','INV202505009','FT020','Phí vệ sinh khu vực chung',   'SERVICE',50000.00,  1.00,   50000.00),
('IFD024','INV202505009','FT024','Phí gửi xe đạp',             'PARKING', 50000.00, 1.00,   50000.00),
('IFD025','INV202505009','FT022','Phí gửi ô tô',               'PARKING',1200000.00,1.00, 1200000.00),

-- INV202506001: APT001 tháng 6 (cùng cấu trúc tháng 5)
('IFD026','INV202506001','FT001','Phí QLVH ≤50m²',             'SERVICE', 8500.00, 45.00,  382500.00),
('IFD027','INV202506001','FT004','Phí vệ sinh khu vực chung',   'SERVICE',50000.00,  1.00,   50000.00),
('IFD028','INV202506001','FT005','Phí gửi xe máy',             'PARKING',100000.00, 1.00,  100000.00),

-- INV202506002: APT002 tháng 6 UNPAID
('IFD029','INV202506002','FT002','Phí QLVH 51–100m²',          'SERVICE', 7000.00, 65.50,  458500.00),
('IFD030','INV202506002','FT004','Phí vệ sinh khu vực chung',   'SERVICE',50000.00,  1.00,   50000.00),
('IFD031','INV202506002','FT005','Phí gửi xe máy',             'PARKING',100000.00, 1.00,  100000.00),
('IFD032','INV202506002','FT006','Phí gửi ô tô',               'PARKING',1200000.00,1.00, 1200000.00),

-- INV202506003: APT003 tháng 6 OVERDUE
('IFD033','INV202506003','FT002','Phí QLVH 51–100m²',          'SERVICE', 7000.00, 72.00,  504000.00),
('IFD034','INV202506003','FT004','Phí vệ sinh khu vực chung',   'SERVICE',50000.00,  1.00,   50000.00),
('IFD035','INV202506003','FT007','Phí gửi xe điện/xe máy điện','PARKING',100000.00, 1.00,  100000.00),

-- INV202506006: APT006 tháng 6 UNPAID
('IFD036','INV202506006','FT009','Phí QLVH ≤50m²',             'SERVICE', 8500.00, 48.00,  408000.00),
('IFD037','INV202506006','FT012','Phí vệ sinh khu vực chung',   'SERVICE',50000.00,  1.00,   50000.00),
('IFD038','INV202506006','FT014','Phí gửi ô tô',               'PARKING',1200000.00,1.00, 1200000.00);

-- ─────────────────────────────────────────────────────────────
-- 11. Payments
-- ─────────────────────────────────────────────────────────────
INSERT INTO `Payments` (`ID`,`invoice_id`,`amount`,`paid_at`,`method`,`momo_trans_id`,`momo_order_id`,`note`,`paid_by`) VALUES
                                                                                                                            ('PAY-001','INV202505001', 532500.00,'2025-05-10 14:00:00','MOMO',   'MOMO-TXN-20250510-001','APT001-MAY25','Thanh toán qua MoMo',   'RES001'),
                                                                                                                            ('PAY-002','INV202505002',1808500.00,'2025-05-12 09:00:00','BANKING',NULL,NULL,'Chuyển khoản VCB','RES003'),
                                                                                                                            ('PAY-003','INV202505003', 654000.00,'2025-05-08 11:00:00','CASH',   NULL,NULL,'Nộp tại BQL','RES005'),
                                                                                                                            ('PAY-004','INV202505004',2010000.00,'2025-05-14 10:00:00','MOMO',   'MOMO-TXN-20250514-004','APT004-MAY25','Thanh toán qua MoMo','RES006'),
                                                                                                                            ('PAY-005','INV202505006',1658000.00,'2025-05-13 16:00:00','ZALOPAY',NULL,NULL,'ZaloPay','RES007'),
                                                                                                                            ('PAY-006','INV202505007',1875000.00,'2025-05-09 10:00:00','BANKING',NULL,NULL,'Techcombank','RES008'),
                                                                                                                            ('PAY-007','INV202505009',1895000.00,'2025-05-11 15:00:00','MOMO',   'MOMO-TXN-20250511-007','APT009-MAY25','Thanh toán qua MoMo','RES010'),
                                                                                                                            ('PAY-008','INV202506001', 532500.00,'2025-06-13 10:00:00','MOMO',   'MOMO-TXN-20250613-008','APT001-JUN25','Thanh toán qua MoMo','RES001');

-- ─────────────────────────────────────────────────────────────
-- 12. Notification
-- ─────────────────────────────────────────────────────────────
INSERT INTO `Notification` (`ID`,`title`,`content`,`type`,`resident_id`,`apartment_id`,`building_id`,`is_read`,`created_by`) VALUES
                                                                                                                                 ('NTF001','Áp dụng khung phí mới từ 01/05/2025',
                                                                                                                                  'Theo QĐ 33/2025/QĐ-UBND của UBND TP. Hà Nội, phí quản lý vận hành được điều chỉnh theo diện tích căn hộ từ 01/05/2025. Vui lòng xem chi tiết trong mục Thông tin căn hộ.',
                                                                                                                                  'INFO',NULL,NULL,'BLD001',0,'ST002'),
                                                                                                                                 ('NTF002','Bảo trì thang máy tháng 6',
                                                                                                                                  'Thang máy tòa B sẽ bảo trì ngày 20/06/2025 từ 8h-12h. Cư dân vui lòng sắp xếp lịch trình.',
                                                                                                                                  'MAINTENANCE',NULL,NULL,'BLD002',0,'ST003'),
                                                                                                                                 ('NTF003','Nhắc nhở: Hóa đơn T6/2025 quá hạn',
                                                                                                                                  'Căn hộ A201 chưa thanh toán hóa đơn tháng 6/2025. Vui lòng thanh toán để tránh phí phạt.',
                                                                                                                                  'PAYMENT','RES005','APT003',NULL,0,'ST004'),
                                                                                                                                 ('NTF004','SỰ CỐ: Mất nước tầng 1–3',
                                                                                                                                  'Đường ống tầng 1-3 đang sự cố. Kỹ thuật đang xử lý, dự kiến xong lúc 18h.',
                                                                                                                                  'URGENT',NULL,NULL,'BLD001',0,'ST002'),
                                                                                                                                 ('NTF005','Xe đăng ký đã được duyệt',
                                                                                                                                  'Phương tiện biển số 29A-11111 (Toyota Fortuner) đã được duyệt.',
                                                                                                                                  'INFO','RES003','APT002',NULL,1,'ST002'),
                                                                                                                                 ('NTF006','Tài khoản xác minh thành công',
                                                                                                                                  'Tài khoản cư dân của bạn đã được Ban quản lý kích hoạt.',
                                                                                                                                  'INFO','RES005','APT003',NULL,1,'ST002'),
                                                                                                                                 ('NTF007','Yêu cầu xác minh danh tính',
                                                                                                                                  'Tài khoản đang chờ xác minh, vui lòng mang CCCD gốc đến quầy lễ tân.',
                                                                                                                                  'INFO','RES011',NULL,NULL,0,'ST007');

-- ─────────────────────────────────────────────────────────────
-- 13. service_request
-- ─────────────────────────────────────────────────────────────
INSERT INTO `service_request`
(`ID`,`title`,`description`,`category`,`status`,`priority`,`resident_id`,`apartment_id`,`assigned_to`,
 `note`,`completion_image`,`resident_confirmed`,`confirmed_at`,`reject_reason`)
VALUES
    ('SR001','Vòi nước bếp bị rỉ','Vòi bếp rỉ liên tục từ tối qua.',
     'WATER','DONE','HIGH','RES001','APT001','ST006',
     'Đã thay ron đệm, áp lực nước bình thường.',
     'https://storage.apt.vn/sr/SR001_done.jpg',1,'2025-05-20 15:00:00',NULL),

    ('SR002','Đèn hành lang tầng 1 hỏng','3 bóng liền kề đều hỏng.',
     'ELECTRIC','DONE','MEDIUM','RES003','APT002','ST005',
     'Đã thay 3 bóng LED 9W.',
     'https://storage.apt.vn/sr/SR002_done.jpg',0,NULL,NULL),

    ('SR003','Điều hòa không lạnh','Có tiếng kêu lạ, không ra hơi lạnh.',
     'HVAC','IN_PROGRESS','HIGH','RES005','APT003','ST005',
     'Thiếu gas, đang đặt mua.',NULL,0,NULL,NULL),

    ('SR004','Internet chậm buổi tối','Từ 20h-23h rất chậm.',
     'INTERNET','PENDING','MEDIUM','RES002','APT001',NULL,NULL,NULL,0,NULL,NULL),

    ('SR005','Trần thấm nước','Trần phòng khách thấm sau mưa lớn.',
     'STRUCTURE','PENDING','HIGH','RES007','APT006',NULL,NULL,NULL,0,NULL,NULL),

    ('SR006','Lắp đặt máy giặt','Cần hỗ trợ lắp máy giặt mới.',
     'OTHER','REJECTED','LOW','RES009','APT007','ST005',
     'Ngoài phạm vi BQL.',NULL,0,NULL,
     'Lắp đặt thiết bị cá nhân không thuộc phạm vi dịch vụ BQL.'),

    ('SR007','Khóa phòng tắm kẹt','Không mở được từ trong ra.',
     'STRUCTURE','IN_PROGRESS','HIGH','RES008','APT007','ST005',
     'Đang đặt mua khóa thay thế.',NULL,0,NULL,NULL),

    ('SR008','Thoát sàn nhà tắm nghẹt','Nước đọng, mùi hôi.',
     'WATER','PENDING','MEDIUM','RES010','APT009',NULL,NULL,NULL,0,NULL,NULL);

SET FOREIGN_KEY_CHECKS = 1;

-- ─────────────────────────────────────────────────────────────
-- VERIFY
-- ─────────────────────────────────────────────────────────────
SELECT 'Role' AS tbl, COUNT(*) AS n FROM Role
UNION ALL SELECT 'Staff',       COUNT(*) FROM Staff
UNION ALL SELECT 'Building',    COUNT(*) FROM Building
UNION ALL SELECT 'Apartment',   COUNT(*) FROM Apartment
UNION ALL SELECT 'Residents',   COUNT(*) FROM Residents
UNION ALL SELECT 'FeeTemplate', COUNT(*) FROM FeeTemplate
UNION ALL SELECT 'Invoice',     COUNT(*) FROM Invoice
UNION ALL SELECT 'FeeDetail',   COUNT(*) FROM invoice_fee_detail
UNION ALL SELECT 'Payments',    COUNT(*) FROM Payments;