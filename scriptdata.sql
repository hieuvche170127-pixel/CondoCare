-- 1. ROLE
INSERT INTO Role VALUES
('R_ADMIN', 'Admin', 'System Administrator'),
('R_MGR',   'Manager', 'Building Manager'),
('R_STAFF', 'Staff', 'General Staff/Receptionist'),
('R_SEC',   'Security', 'Security Guard'),
('R_ACC',   'Accountant', 'Finance & Accounting'),
('R_RES',   'Resident', 'Apartment Resident');

-- 2. USER
INSERT INTO User (ID, username, password, full_name, email, phone, status, role_id) VALUES
('U001', 'admin',    '123456', 'System Admin',      'admin@swp.com', '0900000001', 'ACTIVE', 'R_ADMIN'),
('U002', 'manager', '123456', 'Building Manager',  'mgr@swp.com',   '0900000002', 'ACTIVE', 'R_MGR'),
('U003', 'staff01',  '123456', 'Le Thi Le Tan',  'staff01@swp.com', '0900000003', 'ACTIVE', 'R_STAFF'),
('U004', 'baove01',  '123456', 'Tran Van Bao Ve','sec01@swp.com',   '0900000004', 'ACTIVE', 'R_SEC'),
('U005', 'ketoan01', '123456', 'Nguyen Thi Tien','acc01@swp.com',   '0900000005', 'ACTIVE', 'R_ACC'),
('U101', 'res01', '123456', 'Nguyen Van A', 'a@gmail.com', '0901000001', 'ACTIVE', 'R_RES'),
('U102', 'res02', '123456', 'Tran Thi B',  'b@gmail.com', '0901000002', 'ACTIVE', 'R_RES'),
('U103', 'res03', '123456', 'Le Van C',     'c@gmail.com', '0901000003', 'ACTIVE', 'R_RES'),
('U104', 'res04', '123456', 'Pham Van D',   'd@gmail.com', '0901000004', 'ACTIVE', 'R_RES'),
('U105', 'res05', '123456', 'Hoang Thi E',  'e@gmail.com', '0901000005', 'ACTIVE', 'R_RES'),
('U106', 'res06', '123456', 'Do Van F',     'f@gmail.com', '0901000006', 'ACTIVE', 'R_RES'),
('U107', 'res07', '123456', 'Ngo Thi G',    'g@gmail.com', '0901000007', 'INACTIVE','R_RES'),
('U108', 'res08', '123456', 'Bui Van H',    'h@gmail.com', '0901000008', 'ACTIVE', 'R_RES');

-- 3. STAFF
INSERT INTO Staff VALUES
('S001', 'Manager', 'Operation', '1985-05-10', 'M', '0900000002', 'mgr@swp.com', 'ACTIVE', NOW(), NULL, 'U002'),
('S002', 'Receptionist', 'Front Desk', '1990-08-15', 'F', '0900000003', 'staff01@swp.com', 'ACTIVE', NOW(), NULL, 'U003'),
('S003', 'Security Lead','Security',   '1980-02-20', 'M', '0900000004', 'sec01@swp.com',   'ACTIVE', NOW(), NULL, 'U004'),
('S004', 'Accountant',   'Finance',    '1992-11-11', 'F', '0900000005', 'acc01@swp.com',   'ACTIVE', NOW(), NULL, 'U005');
 
-- 4. BUILDING
INSERT INTO Building VALUES
('B001', 'Sunrise Apartment', '123 Nguyen Van Linh, HCM', 5, 15, 'U002'),
('B002', 'Moonlight Tower', '456 Le Van Luong, Ha Noi', 10, 50, 'U002');

-- 5. APARTMENT
INSERT INTO Apartment VALUES
('A101','101',1,70,'B001','OCCUPIED','RENTED',NULL,'Floor 1 - Room 1',1,1),
('A102','102',1,72,'B001','OCCUPIED','RENTED',NULL,'Floor 1 - Room 2',1,1),
('A103','103',1,75,'B001','EMPTY','AVAILABLE',NULL,'Floor 1 - Room 3',0,0),
('A201','201',2,70,'B001','OCCUPIED','RENTED',NULL,'Floor 2 - Room 1',1,1),
('A202','202',2,72,'B001','EMPTY','AVAILABLE',NULL,'Floor 2 - Room 2',0,0),
('A203','203',2,75,'B001','EMPTY','AVAILABLE',NULL,'Floor 2 - Room 3',0,0),
('C101','101',1, 80, 'B002', 'OCCUPIED', 'RENTED', NULL, 'Moonlight F1-01', 2, 1),
('C102','102',1, 85, 'B002', 'OCCUPIED', 'RENTED', NULL, 'Moonlight F1-02', 1, 1),
('C103','103',1, 90, 'B002', 'EMPTY',    'AVAILABLE', NULL, 'Moonlight F1-03', 0, 0),
('C201','201',2, 80, 'B002', 'OCCUPIED', 'OWNER',  NULL, 'Moonlight F2-01', 3, 2);

-- 6. RESIDENTS
INSERT INTO Residents VALUES
('R001','OWNER','1995-01-01','M','123456789012','0901000001', 'a@gmail.com','ACTIVE','A101','U101',NULL,NULL),
('R002','TENANT','1996-02-02','F','123456789013','0901000002', 'b@gmail.com','ACTIVE','A102','U102',NULL,NULL),
('R003','TENANT','1997-03-03','M','123456789014','0901000003', 'c@gmail.com','ACTIVE','A201','U103',NULL,NULL),
('R004', 'TENANT', '1998-04-04', 'M', '123456789015', '0901000004', 'd@gmail.com', 'ACTIVE', 'A202', 'U104', NULL, NULL),
('R005', 'OWNER',  '1988-05-05', 'F', '123456789016', '0901000005', 'e@gmail.com', 'ACTIVE', 'A203', 'U105', NULL, NULL),
('R006', 'TENANT', '1999-06-06', 'M', '123456789017', '0901000006', 'f@gmail.com', 'ACTIVE', 'C101', 'U106', NULL, NULL),
('R007', 'TENANT', '1995-07-07', 'F', '123456789018', '0901000007', 'g@gmail.com', 'INACTIVE','C102','U107', 'Ha Noi', 'Cong tac'),
('R008', 'OWNER',  '1985-08-08', 'M', '123456789019', '0901000008', 'h@gmail.com', 'ACTIVE', 'C201', 'U108', NULL, NULL);

-- 7. ACCESS CARDS
INSERT INTO access_cards VALUES
('AC001','CARD0001','U002',NOW(),NULL,'ACTIVE','R001'),
('AC002','CARD0002','U002',NOW(),NULL,'ACTIVE','R002'),
('AC003','CARD0003','U002',NOW(),NULL,'ACTIVE','R003'),
('AC004', 'CARD0004', 'U002', NOW(), NULL, 'ACTIVE', 'R004'),
('AC005', 'CARD0005', 'U002', NOW(), NULL, 'ACTIVE', 'R005'),
('AC006', 'CARD0006', 'U003', NOW(), NULL, 'ACTIVE', 'R006'),
('AC007', 'CARD0007', 'U003', NOW(), NULL, 'BLOCKED','R007'),
('AC008', 'CARD0008', 'U002', NOW(), NULL, 'ACTIVE', 'R008');

-- 8. VEHICLE
INSERT INTO Vehicle VALUES
('V001','Motorbike','59A1-12345',NOW(),NULL,'ACTIVE','R001'),
('V002','Car','51G-67890',NOW(),NULL,'ACTIVE','R002'),
('V003', 'Motorbike', '29A1-11111', NOW(), NULL, 'ACTIVE', 'R004'),
('V004', 'Car',       '30E-22222',  NOW(), NULL, 'ACTIVE', 'R005'),
('V005', 'Bicycle',   'NONE',       NOW(), NULL, 'ACTIVE', 'R006'),
('V006', 'Motorbike', '29B1-33333', NOW(), NOW(),'INACTIVE','R007'),
('V007', 'Car',       '30F-44444',  NOW(), NULL, 'ACTIVE', 'R008');

-- 9. UNIT_PRICE
INSERT INTO Unit_Price VALUES
('UP001','Electricity','ELECTRIC',3500,'U001',NOW()),
('UP002','Water','WATER',15000,'U001',NOW()),
('UP003','Service Fee','SERVICE',500000,'U001',NOW()),
('UP004','Parking Fee','PARKING',100000,'U001',NOW()),
('UP005', 'Gym Fee',      'SERVICE', 200000, 'U001', NOW()),
('UP006', 'BBQ Area',     'SERVICE', 100000, 'U001', NOW()),
('UP007', 'Car Parking',  'PARKING', 1200000,'U001', NOW()),
('UP008', 'Lost Card Fee','PENALTY', 50000,  'U001', NOW()),
('UP009', 'Cleaning',     'SERVICE', 50000,  'U001', NOW());

-- 10. FEES
INSERT INTO Fees VALUES
('F001','A101','Service Fee','Monthly service','SERVICE',500000,'2024-01-01','2024-12-31','U002'),
('F002','A101','Parking Fee','Motorbike parking','PARKING',100000,'2024-01-01','2024-12-31','U002'),
('F003', 'A202', 'Management Fee', 'Monthly', 'SERVICE', 500000, '2024-01-01', '2024-12-31', 'U002'),
('F004', 'A202', 'Motorbike Parking','1 bike','PARKING', 100000, '2024-01-01', '2024-12-31', 'U002'),
('F005', 'A203', 'Car Parking', '1 car',      'PARKING', 1200000,'2024-01-01', '2024-12-31', 'U002'),
('F006', 'C101', 'Management Fee', 'Monthly', 'SERVICE', 600000, '2024-01-01', '2024-12-31', 'U002'),
('F007', 'C201', 'Gym Membership', '2 people','SERVICE', 400000, '2024-01-01', '2024-12-31', 'U002');

-- 11. INVOICE
INSERT INTO Invoice VALUES
('INV001','A101','F001','2024-09-01','2024-09-30',500000, NOW(),'2024-10-05','UNPAID',NULL,'U002'),
('INV002', 'A202', 'F003', '2024-09-01', '2024-09-30', 500000,  NOW(), '2024-10-05', 'PAID',   NOW(), 'U002'),
('INV003', 'A202', 'F004', '2024-09-01', '2024-09-30', 100000,  NOW(), '2024-10-05', 'PAID',   NOW(), 'U002'),
('INV004', 'A203', 'F005', '2024-09-01', '2024-09-30', 1200000, NOW(), '2024-10-05', 'UNPAID', NULL,  'U002'),
('INV005', 'C101', 'F006', '2024-10-01', '2024-10-31', 600000,  NOW(), '2024-11-05', 'UNPAID', NULL,  'U002'),
('INV006', 'C201', 'F007', '2024-10-01', '2024-10-31', 400000,  NOW(), '2024-11-05', 'PAID',   NOW(), 'U002');

-- 12. PAYMENTS
INSERT INTO Payments VALUES
('P001','INV001',500000,NOW(),'CASH','Paid full','U101'),
('P002', 'INV002', 500000, NOW(), 'BANKING', 'VCB Transfer', 'U104'),
('P003', 'INV003', 100000, NOW(), 'MOMO',    'Momo payment', 'U104'),
('P004', 'INV006', 400000, NOW(), 'CASH',    'Paid at desk', 'U108');

-- 13. METER READING
INSERT INTO meter_reading VALUES
('M001','A101','ELECTRIC',9,2024,1200,1250,175000,'U002',NOW()),
('M002','A101','WATER',9,2024,300,320,300000,'U002',NOW()),
('M003', 'A102', 'ELECTRIC', 9, 2024, 2000, 2150, 525000, 'U002', NOW()),
('M004', 'A102', 'WATER',    9, 2024, 400,  415,  225000, 'U002', NOW()),
('M005', 'A202', 'ELECTRIC', 9, 2024, 0,    100,  350000, 'U003', NOW()),
('M006', 'A202', 'WATER',    9, 2024, 0,    10,   150000, 'U003', NOW()),
('M007', 'C101', 'ELECTRIC', 10,2024, 0,    50,   175000, 'U003', NOW());