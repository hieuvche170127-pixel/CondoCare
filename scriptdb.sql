DROP DATABASE IF EXISTS SWP391;
CREATE DATABASE SWP391 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE SWP391;

SET FOREIGN_KEY_CHECKS = 1;

-- -----------------------------------------------------
-- Table: Role (Dùng để phân quyền cho Staff: Admin, Manager, Staff...)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `Role` (
                                      `ID` char(10) NOT NULL,
    `name` varchar(100) NOT NULL,
    `description` varchar(255) DEFAULT NULL,
    PRIMARY KEY (`ID`)
    ) ENGINE=InnoDB;

-- -----------------------------------------------------
-- Table: Staff (Gộp thông tin Account và Staff)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `Staff` (
                                       `ID` char(10) NOT NULL,
    `username` varchar(255) NOT NULL UNIQUE,
    `password` varchar(255) NOT NULL,
    `full_name` varchar(255) NOT NULL,
    `email` varchar(255) DEFAULT NULL,
    `phone` varchar(20) NOT NULL,
    `position` varchar(255) NOT NULL,
    `department` varchar(255) NOT NULL,
    `dob` date DEFAULT NULL,
    `gender` ENUM('M', 'F') NOT NULL,
    `status` ENUM('ACTIVE', 'RESIGNED', 'ON_LEAVE') NOT NULL DEFAULT 'ACTIVE',
    `role_id` char(10) NOT NULL,
    `hired_at` timestamp NULL DEFAULT NULL,
    `last_login` timestamp NULL DEFAULT NULL,
    `create_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`ID`),
    CONSTRAINT `FK_Staff_Role` FOREIGN KEY (`role_id`) REFERENCES `Role` (`ID`)
    ) ENGINE=InnoDB;

-- -----------------------------------------------------
-- Table: Building (Quản lý bởi Staff/Manager)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `Building` (
                                          `ID` char(10) NOT NULL,
    `name` varchar(255) NOT NULL,
    `address` varchar(255) NOT NULL,
    `total_floors` int(10) NOT NULL,
    `total_apartments` int(10) NOT NULL,
    `manager_id` char(10) NOT NULL, -- Trỏ về Staff (Người có Role Manager)
    PRIMARY KEY (`ID`),
    CONSTRAINT `FK_Building_Manager` FOREIGN KEY (`manager_id`) REFERENCES `Staff` (`ID`)
    ) ENGINE=InnoDB;

-- -----------------------------------------------------
-- Table: Apartment
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `Apartment` (
                                           `ID` char(4) NOT NULL,
    `number` char(4) NOT NULL,
    `floor` int(10) NOT NULL,
    `area` float NOT NULL,
    `building_id` char(10) NOT NULL,
    `status` ENUM('EMPTY', 'OCCUPIED', 'MAINTENANCE') DEFAULT 'EMPTY',
    `rental_status` ENUM('AVAILABLE', 'RENTED', 'OWNER') DEFAULT 'AVAILABLE',
    `images` varchar(255) DEFAULT NULL,
    `description` varchar(255) DEFAULT NULL,
    `total_resident` int(10) DEFAULT 0,
    `total_vehicle` int(10) DEFAULT 0,
    PRIMARY KEY (`ID`),
    CONSTRAINT `FK_Apartment_Building` FOREIGN KEY (`building_id`) REFERENCES `Building` (`ID`)
    ) ENGINE=InnoDB;

-- -----------------------------------------------------
-- Table: Residents (Gộp thông tin Account và Resident)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `Residents` (
                                           `ID` char(10) NOT NULL,
    `username` varchar(255) NOT NULL UNIQUE,
    `password` varchar(255) NOT NULL,
    `full_name` varchar(255) NOT NULL,
    `type` ENUM('OWNER', 'TENANT', 'GUEST') NOT NULL,
    `dob` date DEFAULT NULL,
    `gender` ENUM('M', 'F') NOT NULL,
    `id_number` char(12) NOT NULL,
    `phone` varchar(20) NOT NULL,
    `email` varchar(255) DEFAULT NULL,
    `status` ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    `apartment_id` char(4) NOT NULL,
    `temp_residence` varchar(255) DEFAULT NULL,
    `temp_absence` varchar(255) DEFAULT NULL,
    `last_login` timestamp NULL DEFAULT NULL,
    `create_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`ID`),
    CONSTRAINT `FK_Residents_Apartment` FOREIGN KEY (`apartment_id`) REFERENCES `Apartment` (`ID`)
    ) ENGINE=InnoDB;

-- -----------------------------------------------------
-- Table: access_cards (Cấp bởi Staff)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `access_cards` (
                                              `ID` char(10) NOT NULL,
    `card_number` char(8) NOT NULL,
    `issued_by` char(10) DEFAULT NULL, -- Trỏ về Staff
    `issued_at` timestamp NULL DEFAULT NULL,
    `expired_at` timestamp NULL DEFAULT NULL,
    `status` ENUM('ACTIVE', 'BLOCKED', 'LOST') NOT NULL DEFAULT 'ACTIVE',
    `resident_id` char(10) NOT NULL,
    PRIMARY KEY (`ID`),
    UNIQUE KEY `unique_card_number` (`card_number`),
    CONSTRAINT `FK_AccessCards_Resident` FOREIGN KEY (`resident_id`) REFERENCES `Residents` (`ID`),
    CONSTRAINT `FK_AccessCards_Staff` FOREIGN KEY (`issued_by`) REFERENCES `Staff` (`ID`)
    ) ENGINE=InnoDB;

-- -----------------------------------------------------
-- Table: Vehicle
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `Vehicle` (
                                         `ID` char(10) NOT NULL,
    `type` varchar(100) NOT NULL,
    `license_plate` varchar(100) DEFAULT NULL,
    `registered_at` timestamp NULL DEFAULT NULL,
    `revoked_at` timestamp NULL DEFAULT NULL,
    `status` ENUM('ACTIVE', 'INACTIVE') DEFAULT 'ACTIVE',
    `resident_id` char(10) NOT NULL,
    PRIMARY KEY (`ID`),
    CONSTRAINT `FK_Vehicle_Resident` FOREIGN KEY (`resident_id`) REFERENCES `Residents` (`ID`)
    ) ENGINE=InnoDB;

-- -----------------------------------------------------
-- Table: Unit_Price (Cập nhật bởi Staff)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `Unit_Price` (
                                            `ID` char(10) NOT NULL,
    `name` varchar(255) NOT NULL,
    `type` ENUM('ELECTRIC', 'WATER', 'SERVICE', 'PARKING', 'PENALTY') NOT NULL,
    `amount` float NOT NULL,
    `update_by` char(10) NOT NULL, -- Trỏ về Staff
    `update_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`ID`),
    CONSTRAINT `FK_UnitPrice_Staff` FOREIGN KEY (`update_by`) REFERENCES `Staff` (`ID`)
    ) ENGINE=InnoDB;

-- -----------------------------------------------------
-- Table: Fees (Tạo bởi Staff)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `Fees` (
                                      `ID` char(10) NOT NULL,
    `apartment_id` char(4) NOT NULL,
    `name` varchar(255) NOT NULL,
    `description` varchar(255) DEFAULT NULL,
    `type` ENUM('SERVICE', 'PARKING', 'MANAGEMENT', 'OTHER') NOT NULL,
    `amount` float NOT NULL,
    `effective_from` date NOT NULL,
    `effective_to` date NOT NULL,
    `create_by` char(10) NOT NULL, -- Trỏ về Staff
    PRIMARY KEY (`ID`),
    CONSTRAINT `FK_Fees_Apartment` FOREIGN KEY (`apartment_id`) REFERENCES `Apartment` (`ID`),
    CONSTRAINT `FK_Fees_Staff` FOREIGN KEY (`create_by`) REFERENCES `Staff` (`ID`)
    ) ENGINE=InnoDB;

ALTER TABLE Fees
    MODIFY COLUMN effective_to DATE NULL;

-- -----------------------------------------------------
-- Table: Invoice (Tạo bởi Staff)
-- -----------------------------------------------------
CREATE TABLE `Invoice` (
                           `ID`                  varchar(15)    NOT NULL,
                           `apartment_id`        char(4)        NOT NULL,
                           `month`               int            NOT NULL,
                           `year`                int            NOT NULL,
                           `electric_reading_id` char(10)       NULL,
                           `water_reading_id`    char(10)       NULL,
                           `service_fee_id`      char(10)       NULL,
                           `parking_fee_id`      char(10)       NULL,
                           `electric_amount`     decimal(19,2)  NULL DEFAULT 0,
                           `water_amount`        decimal(19,2)  NULL DEFAULT 0,
                           `service_amount`      decimal(19,2)  NULL DEFAULT 0,
                           `parking_amount`      decimal(19,2)  NULL DEFAULT 0,
                           `total_amount`        decimal(19,2)  NULL DEFAULT 0,
                           `status`              ENUM('UNPAID','PAID','OVERDUE') NOT NULL DEFAULT 'UNPAID',
                           `issued_at`           datetime       NULL,
                           `due_date`            date           NULL,
                           `paid_at`             datetime       NULL,
                           `create_by`           char(10)       NOT NULL,
                           PRIMARY KEY (`ID`),
                           CONSTRAINT `FK_Invoice_Apartment`      FOREIGN KEY (`apartment_id`)        REFERENCES `Apartment`(`ID`),
                           CONSTRAINT `FK_Invoice_ElectricReading` FOREIGN KEY (`electric_reading_id`) REFERENCES `meter_reading`(`ID`) ON DELETE SET NULL,
                           CONSTRAINT `FK_Invoice_WaterReading`    FOREIGN KEY (`water_reading_id`)    REFERENCES `meter_reading`(`ID`) ON DELETE SET NULL,
                           CONSTRAINT `FK_Invoice_ServiceFee`      FOREIGN KEY (`service_fee_id`)      REFERENCES `Fees`(`ID`) ON DELETE SET NULL,
                           CONSTRAINT `FK_Invoice_ParkingFee`      FOREIGN KEY (`parking_fee_id`)      REFERENCES `Fees`(`ID`) ON DELETE SET NULL,
                           CONSTRAINT `FK_Invoice_Staff`           FOREIGN KEY (`create_by`)           REFERENCES `Staff`(`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
ALTER TABLE Invoice
    MODIFY COLUMN electric_reading_id char(15) NULL,
    MODIFY COLUMN water_reading_id    char(15) NULL;


-- -----------------------------------------------------
-- Table: Payments (Cư dân thanh toán)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `Payments` (
                                          `ID` char(10) NOT NULL,
    `invoice_id` char(10) NOT NULL,
    `amount` float NOT NULL,
    `paid_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `method` ENUM('CASH', 'BANKING', 'MOMO', 'ZALOPAY') NOT NULL,
    `note` varchar(100) DEFAULT NULL,
    `paid_by` char(10) NOT NULL, -- Trỏ về Residents
    PRIMARY KEY (`ID`),
    UNIQUE KEY `UQ_Payments_Invoice` (`invoice_id`),
    CONSTRAINT `FK_Payments_Invoice` FOREIGN KEY (`invoice_id`) REFERENCES `Invoice` (`ID`),
    CONSTRAINT `FK_Payments_Resident` FOREIGN KEY (`paid_by`) REFERENCES `Residents` (`ID`)
    ) ENGINE=InnoDB;

-- -----------------------------------------------------
-- Table: meter_reading (Ghi nhận bởi Staff)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `meter_reading` (
                                               `ID` char(10) NOT NULL,
    `apartment_id` char(4) NOT NULL,
    `meter_type` ENUM('ELECTRIC', 'WATER') NOT NULL,
    `month` int(10) NOT NULL,
    `year` int(10) NOT NULL,
    `previous_index` decimal(19,0) NOT NULL,
    `current_index` decimal(19,0) NOT NULL,
    `total_amount` float NOT NULL,
    `recorded_by` char(10) NOT NULL, -- Trỏ về Staff
    `recorded_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`ID`),
    CONSTRAINT `FK_Meter_Apartment` FOREIGN KEY (`apartment_id`) REFERENCES `Apartment` (`ID`),
    CONSTRAINT `FK_Meter_Staff` FOREIGN KEY (`recorded_by`) REFERENCES `Staff` (`ID`)
    ) ENGINE=InnoDB;
DESCRIBE meter_reading;
ALTER TABLE meter_reading
    ADD COLUMN consumption decimal(19,0) NOT NULL DEFAULT 0 AFTER current_index,
    ADD COLUMN unit_price float NOT NULL DEFAULT 0 AFTER consumption;
ALTER TABLE meter_reading
    MODIFY COLUMN ID char(15) NOT NULL;

SET FOREIGN_KEY_CHECKS = 1;

-- Sample Data cho Apartment Management System

USE SWP391;

-- ===================================
-- 1. INSERT ROLES
-- ===================================
INSERT INTO Role (ID, name, description) VALUES
                                             ('R001', 'ADMIN', 'Administrator - Full system access'),
                                             ('R002', 'MANAGER', 'Building Manager - Manage buildings'),
                                             ('R003', 'STAFF', 'Staff Member - Basic operations');

-- ===================================
-- 2. INSERT STAFF
-- Password cho tất cả: admin123
-- Đã mã hóa bằng BCrypt
-- ===================================
INSERT INTO Staff (ID, username, password, full_name, email, phone, position, department, dob, gender, status, role_id, hired_at, create_at) VALUES
                                                                                                                                                 ('S001', 'admin', '$2a$10$rqI5kxYqCQPQvLCvGJGz5.5PQvYvYvLXKqVXGJGz5u5PQvYvYvLXK', 'Nguyễn Hoàng Anh', 'admin@apartment.com', '0901234567', 'Administrator', 'Management', '1990-01-01', 'M', 'ACTIVE', 'R001', '2024-01-01 08:00:00', NOW()),
                                                                                                                                                 ('S002', 'manager1', '$2a$10$rqI5kxYqCQPQvLCvGJGz5.5PQvYvYvLXKqVXGJGz5u5PQvYvYvLXK', 'Trần Thị Minh Châu', 'manager@apartment.com', '0902345678', 'Building Manager', 'Building A', '1985-05-15', 'F', 'ACTIVE', 'R002', '2024-01-01 08:00:00', NOW()),
                                                                                                                                                 ('S003', 'staff1', '$2a$10$rqI5kxYqCQPQvLCvGJGz5.5PQvYvYvLXKqVXGJGz5u5PQvYvYvLXK', 'Phạm Minh Đức', 'staff1@apartment.com', '0903456789', 'Staff', 'Customer Service', '1995-08-20', 'M', 'ACTIVE', 'R003', '2024-02-01 08:00:00', NOW());
INSERT INTO Staff (ID, username, password, full_name, email, phone, position, department, dob, gender, status, role_id, hired_at, create_at) VALUES
    ('RES003', 'resident3', 'resident123', 'Phạm Minh C', 'staff1@apartment.com', '0903456789', '', '', '1995-08-20', 'M', 'ACTIVE', 'R002', '2024-02-01 08:00:00', NOW());
-- ===================================
-- 3. INSERT BUILDINGS
-- ===================================
INSERT INTO Building (ID, name, address, total_floors, total_apartments, manager_id) VALUES
                                                                                         ('B001', 'Tòa A', '123 Đường Nguyễn Huệ, Quận 1, TP.HCM', 20, 100, 'S002'),
                                                                                         ('B002', 'Tòa B', '456 Đường Lê Lợi, Quận 1, TP.HCM', 15, 75, 'S002'),
                                                                                         ('B003', 'Tòa C', '789 Đường Trần Hưng Đạo, Quận 5, TP.HCM', 25, 125, 'S002');

-- ===================================
-- 4. INSERT APARTMENTS
-- ===================================
-- Tòa A - Tầng 1
INSERT INTO Apartment (ID, number, floor, area, building_id, status, rental_status, total_resident, total_vehicle) VALUES
                                                                                                                       ('A101', 'A101', 1, 80, 'B001', 'EMPTY', 'AVAILABLE', 0, 0),
                                                                                                                       ('A102', 'A102', 1, 80, 'B001', 'OCCUPIED', 'RENTED', 3, 1),
                                                                                                                       ('A103', 'A103', 1, 85, 'B001', 'OCCUPIED', 'OWNER', 4, 2),
                                                                                                                       ('A104', 'A104', 1, 85, 'B001', 'MAINTENANCE', 'AVAILABLE', 0, 0);

-- Tòa A - Tầng 2
INSERT INTO Apartment (ID, number, floor, area, building_id, status, rental_status, total_resident, total_vehicle) VALUES
                                                                                                                       ('A201', 'A201', 2, 90, 'B001', 'OCCUPIED', 'OWNER', 2, 1),
                                                                                                                       ('A202', 'A202', 2, 90, 'B001', 'EMPTY', 'AVAILABLE', 0, 0),
                                                                                                                       ('A203', 'A203', 2, 95, 'B001', 'OCCUPIED', 'RENTED', 3, 1),
                                                                                                                       ('A204', 'A204', 2, 95, 'B001', 'OCCUPIED', 'OWNER', 5, 2);

-- Tòa B
INSERT INTO Apartment (ID, number, floor, area, building_id, status, rental_status, total_resident, total_vehicle) VALUES
                                                                                                                       ('B101', 'B101', 1, 75, 'B002', 'EMPTY', 'AVAILABLE', 0, 0),
                                                                                                                       ('B102', 'B102', 1, 75, 'B002', 'OCCUPIED', 'RENTED', 2, 1),
                                                                                                                       ('B201', 'B201', 2, 80, 'B002', 'OCCUPIED', 'OWNER', 3, 1),
                                                                                                                       ('B202', 'B202', 2, 80, 'B002', 'EMPTY', 'AVAILABLE', 0, 0);

-- ===================================
-- 5. INSERT RESIDENTS (Sample)
-- Password: resident123
-- ===================================
INSERT INTO Residents (ID, username, password, full_name, type, dob, gender, id_number, phone, email, status, apartment_id, create_at) VALUES
                                                                                                                                           ('RES001', 'resident1', '$2a$10$rqI5kxYqCQPQvLCvGJGz5.5PQvYvYvLXKqVXGJGz5u5PQvYvYvLXK', 'Phạm Văn A', 'OWNER', '1980-03-15', 'M', '001234567890', '0912345678', 'resident1@gmail.com', 'ACTIVE', 'A103', NOW()),
                                                                                                                                           ('RES002', 'resident2', '$2a$10$rqI5kxYqCQPQvLCvGJGz5.5PQvYvYvLXKqVXGJGz5u5PQvYvYvLXK', 'Hoàng Thị B', 'TENANT', '1992-07-20', 'F', '001234567891', '0912345679', 'resident2@gmail.com', 'ACTIVE', 'A102', NOW()),
                                                                                                                                           ('RES003', 'resident3', '$2a$10$rqI5kxYqCQPQvLCvGJGz5.5PQvYvYvLXKqVXGJGz5u5PQvYvYvLXK', 'Nguyễn Văn C', 'OWNER', '1985-11-10', 'M', '001234567892', '0912345680', 'resident3@gmail.com', 'ACTIVE', 'A201', NOW());

-- ===================================
-- 6. INSERT UNIT PRICES
-- ===================================
INSERT INTO Unit_Price (ID, name, type, amount, update_by, update_at) VALUES
                                                                          ('UP001', 'Điện sinh hoạt', 'ELECTRIC', 2500, 'S001', NOW()),
                                                                          ('UP002', 'Nước sinh hoạt', 'WATER', 15000, 'S001', NOW()),
                                                                          ('UP003', 'Phí quản lý', 'SERVICE', 50000, 'S001', NOW()),
                                                                          ('UP004', 'Phí gửi xe máy', 'PARKING', 100000, 'S001', NOW()),
                                                                          ('UP005', 'Phí gửi xe ô tô', 'PARKING', 1500000, 'S001', NOW());

-- ===================================
-- HOÀN TẤT
-- ===================================
SELECT 'Sample data inserted successfully!' AS Status;

-- Kiểm tra dữ liệu
SELECT 'Total Roles:', COUNT(*) FROM Role;
SELECT 'Total Staff:', COUNT(*) FROM Staff;
SELECT 'Total Buildings:', COUNT(*) FROM Building;
SELECT 'Total Apartments:', COUNT(*) FROM Apartment;
SELECT 'Total Residents:', COUNT(*) FROM Residents;
select * from residents;
UPDATE Staff
SET password = 'admin123'
WHERE username = 'admin';

UPDATE Staff
SET password = 'admin123'
WHERE username = 'manager1';

UPDATE Residents
SET password = 'resident123'
WHERE username = 'resident3';

-- 1a. Cho phép id_number và apartment_id NULL trong Residents
--     (Vì giờ đăng ký không bắt buộc 2 trường này nữa)
ALTER TABLE Residents
    MODIFY COLUMN id_number CHAR(12) NULL,
    MODIFY COLUMN apartment_id CHAR(4) NULL;

-- 1b. Bỏ foreign key constraint cũ của apartment_id
ALTER TABLE Residents DROP FOREIGN KEY FK_Residents_Apartment;

-- 1c. Thêm lại foreign key MỚI với ON DELETE SET NULL
--     (Khi xóa apartment, resident vẫn giữ nhưng apartment_id = NULL)
ALTER TABLE Residents
    ADD CONSTRAINT FK_Residents_Apartment
        FOREIGN KEY (apartment_id) REFERENCES Apartment(ID)
            ON DELETE SET NULL;


-- ============================================================
-- BƯỚC 2: TẠO CÁC BẢNG MỚI
-- ============================================================

-- 2a. Bảng Notification
CREATE TABLE IF NOT EXISTS Notification (
                                            ID          VARCHAR(15)  NOT NULL PRIMARY KEY,
    title       VARCHAR(255) NOT NULL,
    content     TEXT         NOT NULL,
    type        ENUM('INFO','WARNING','URGENT','MAINTENANCE','PAYMENT') NOT NULL DEFAULT 'INFO',
    resident_id CHAR(10)     NULL,       -- NULL = gửi cho tất cả
    is_read     TINYINT(1)   NOT NULL DEFAULT 0,
    created_by  CHAR(10)     NOT NULL,   -- Staff ID
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_resident (resident_id),
    INDEX idx_created (created_at),
    CONSTRAINT fk_notif_resident FOREIGN KEY (resident_id) REFERENCES Residents(ID) ON DELETE CASCADE,
    CONSTRAINT fk_notif_staff    FOREIGN KEY (created_by)  REFERENCES Staff(ID)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- 2b. Bảng ServiceRequest
CREATE TABLE IF NOT EXISTS service_request (
                                               ID           VARCHAR(15)  NOT NULL PRIMARY KEY,
    title        VARCHAR(255) NOT NULL,
    description  TEXT         NOT NULL,
    category     ENUM('ELECTRIC','WATER','INTERNET','HVAC','STRUCTURE','OTHER') NOT NULL DEFAULT 'OTHER',
    status       ENUM('PENDING','IN_PROGRESS','DONE','REJECTED') NOT NULL DEFAULT 'PENDING',
    priority     ENUM('LOW','MEDIUM','HIGH') NOT NULL DEFAULT 'MEDIUM',
    resident_id  CHAR(10)     NOT NULL,
    apartment_id CHAR(4)      NULL,
    assigned_to  CHAR(10)     NULL,
    note         TEXT         NULL,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NULL ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_resident (resident_id),
    INDEX idx_status   (status),
    INDEX idx_created  (created_at),
    CONSTRAINT fk_sr_resident  FOREIGN KEY (resident_id)  REFERENCES Residents(ID),
    CONSTRAINT fk_sr_apartment FOREIGN KEY (apartment_id) REFERENCES Apartment(ID) ON DELETE SET NULL,
    CONSTRAINT fk_sr_staff     FOREIGN KEY (assigned_to)  REFERENCES Staff(ID)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
ALTER TABLE ServiceRequest
    MODIFY COLUMN resident_id CHAR(10) NOT NULL,
    MODIFY COLUMN assigned_to CHAR(10) NULL;
ALTER TABLE service_request
    ADD COLUMN completion_image MEDIUMTEXT NULL
        COMMENT 'Base64 ảnh xác nhận hoàn thành (staff upload)' AFTER note;
ALTER TABLE service_request
    ADD COLUMN resident_confirmed TINYINT(1) NOT NULL DEFAULT 0
        COMMENT 'Resident xác nhận đã được xử lý xong' AFTER completion_image;
ALTER TABLE service_request
    ADD COLUMN confirmed_at DATETIME NULL
        COMMENT 'Thời điểm resident xác nhận' AFTER resident_confirmed;
ALTER TABLE service_request
    ADD COLUMN reject_reason TEXT NULL
        COMMENT 'Lý do từ chối yêu cầu' AFTER confirmed_at;

-- 2c. Bảng Invoice_Monthly (Tên mới để không conflict với Invoice cũ)
--     Bảng này cho Resident Dashboard - khác với Invoice trong DB gốc
-- ============================================================
-- BƯỚC 3: DỮ LIỆU MẪU
-- ============================================================

-- 3a. Notifications mẫu
INSERT INTO Notification (ID, title, content, type, resident_id, is_read, created_by, created_at) VALUES
                                                                                                      ('N001', 'Thông báo bảo trì thang máy',
                                                                                                       'Ban quản lý thông báo: thang máy tòa A sẽ được bảo trì vào ngày 20/02/2026 từ 8h-12h. Cư dân vui lòng sử dụng cầu thang bộ trong thời gian này.',
                                                                                                       'MAINTENANCE', NULL, 0, 'S001', NOW()),

                                                                                                      ('N002', 'Nhắc nhở đóng phí tháng 2/2026',
                                                                                                       'Phí quản lý và các khoản phí tháng 2/2026 đã đến hạn thanh toán (28/02/2026). Quý cư dân vui lòng thanh toán đúng hạn để tránh bị phạt trễ hạn.',
                                                                                                       'PAYMENT', NULL, 0, 'S001', NOW()),

                                                                                                      ('N003', 'Quy định mới về gửi xe',
                                                                                                       'Kể từ ngày 01/03/2026, ban quản lý áp dụng thẻ từ bãi đỗ xe thông minh. Cư dân vui lòng đăng ký tại văn phòng ban quản lý trước ngày 28/02/2026.',
                                                                                                       'INFO', NULL, 0, 'S002', NOW()),

                                                                                                      ('N004', 'Cúp điện định kỳ',
                                                                                                       'Điện lực thông báo sẽ cúp điện toàn khu vực vào ngày 25/02/2026 từ 7h-17h để nâng cấp hệ thống. Ban quản lý đã chuẩn bị máy phát điện dự phòng.',
                                                                                                       'WARNING', NULL, 0, 'S001', NOW()),

                                                                                                      ('N005', 'Chào mừng cư dân mới!',
                                                                                                       'Ban quản lý chân thành chào mừng bạn đến với cộng đồng chung cư. Nếu cần hỗ trợ, vui lòng liên hệ văn phòng ban quản lý tại tầng 1 tòa A, từ 8h-17h các ngày trong tuần.',
                                                                                                       'INFO', 'RES001', 0, 'S001', DATE_SUB(NOW(), INTERVAL 5 DAY));

-- 3b. ServiceRequests mẫu
INSERT INTO ServiceRequest (ID, title, description, category, status, priority, resident_id, apartment_id, assigned_to, created_at) VALUES
                                                                                                                                        ('SR001', 'Bóng đèn hành lang bị hỏng',
                                                                                                                                         'Bóng đèn phía trước cửa phòng bị hỏng từ 3 ngày nay, nhờ ban quản lý cho người kiểm tra và thay mới.',
                                                                                                                                         'ELECTRIC', 'DONE', 'LOW', 'RES001', 'A103', 'S003', DATE_SUB(NOW(), INTERVAL 10 DAY)),

                                                                                                                                        ('SR002', 'Vòi nước bồn rửa bị rỉ',
                                                                                                                                         'Vòi nước tại bồn rửa nhà bếp bị rỉ liên tục, gây lãng phí nước. Mong ban quản lý cử người đến sửa chữa sớm.',
                                                                                                                                         'WATER', 'IN_PROGRESS', 'MEDIUM', 'RES001', 'A103', 'S003', DATE_SUB(NOW(), INTERVAL 2 DAY)),

                                                                                                                                        ('SR003', 'Điều hòa không mát',
                                                                                                                                         'Điều hòa phòng khách hoạt động nhưng không làm mát được. Có thể cần nạp gas hoặc vệ sinh.',
                                                                                                                                         'HVAC', 'PENDING', 'MEDIUM', 'RES002', 'A102', NULL, DATE_SUB(NOW(), INTERVAL 1 DAY));

INSERT INTO Fees (ID, apartment_id, name, description, type, amount, effective_from, effective_to, create_by) VALUES

-- ── Phí quản lý (SERVICE) ── tính theo diện tích ──────────────
-- A102 = 80m²  → 80  × 7.000 = 560.000đ
-- A103 = 85m²  → 85  × 7.000 = 595.000đ
-- A201 = 90m²  → 90  × 7.000 = 630.000đ
-- A203 = 95m²  → 95  × 7.000 = 665.000đ
-- A204 = 95m²  → 95  × 7.000 = 665.000đ
('F001', 'A102', 'Phí quản lý',     'Phí quản lý chung cư theo diện tích 80m²',  'SERVICE', 560000, '2026-01-01', NULL, 'S001'),
('F002', 'A103', 'Phí quản lý',     'Phí quản lý chung cư theo diện tích 85m²',  'SERVICE', 595000, '2026-01-01', NULL, 'S001'),
('F003', 'A201', 'Phí quản lý',     'Phí quản lý chung cư theo diện tích 90m²',  'SERVICE', 630000, '2026-01-01', NULL, 'S001'),
('F004', 'A203', 'Phí quản lý',     'Phí quản lý chung cư theo diện tích 95m²',  'SERVICE', 665000, '2026-01-01', NULL, 'S001'),
('F005', 'A204', 'Phí quản lý',     'Phí quản lý chung cư theo diện tích 95m²',  'SERVICE', 665000, '2026-01-01', NULL, 'S001'),

-- ── Phí xe máy (PARKING) ───────────────────────────────────────
('F010', 'A102', 'Phí xe máy',      'Giữ xe máy tầng hầm - 1 xe',    'PARKING', 100000,  '2026-01-01', NULL, 'S001'),
('F011', 'A103', 'Phí xe máy (1)',  'Giữ xe máy tầng hầm - xe thứ 1', 'PARKING', 100000,  '2026-01-01', NULL, 'S001'),
('F012', 'A103', 'Phí xe máy (2)',  'Giữ xe máy tầng hầm - xe thứ 2', 'PARKING', 100000,  '2026-01-01', NULL, 'S001'),
('F013', 'A201', 'Phí xe máy',      'Giữ xe máy tầng hầm - 1 xe',    'PARKING', 100000,  '2026-01-01', NULL, 'S001'),
('F014', 'A203', 'Phí xe máy',      'Giữ xe máy tầng hầm - 1 xe',    'PARKING', 100000,  '2026-01-01', NULL, 'S001'),

-- ── Phí ô tô (PARKING) ─────────────────────────────────────────
('F020', 'A103', 'Phí ô tô',        'Giữ ô tô tầng hầm - 1 xe',      'PARKING', 1300000, '2026-01-01', NULL, 'S001'),
('F021', 'A204', 'Phí ô tô (1)',    'Giữ ô tô tầng hầm - xe thứ 1',   'PARKING', 1300000, '2026-01-01', NULL, 'S001'),
('F022', 'A204', 'Phí ô tô (2)',    'Giữ ô tô tầng hầm - xe thứ 2',   'PARKING', 1300000, '2026-01-01', NULL, 'S001'),

-- ── Phí xe đạp điện (PARKING) ──────────────────────────────────
('F030', 'A102', 'Phí xe đạp điện', 'Giữ xe đạp điện tầng hầm - 1 xe', 'PARKING', 75000, '2026-01-01', NULL, 'S001'),
('F031', 'A203', 'Phí xe đạp điện', 'Giữ xe đạp điện tầng hầm - 1 xe', 'PARKING', 75000, '2026-01-01', NULL, 'S001');


-- ============================================================
-- BƯỚC 3: METER READING (Điện + Nước)
-- Giá điện EVN 2025: trung bình ~2.800đ/kWh (bậc 3-4 phổ biến HCM)
-- Giá nước TP.HCM 2025: 15.929đ/m³ (bậc 2, sinh hoạt chung cư)
-- consumption = current_index - previous_index
-- total_amount = consumption × unit_price
-- ============================================================

-- ── Tháng 1/2026 ───────────────────────────────────────────────
INSERT INTO meter_reading (ID, apartment_id, meter_type, month, year, previous_index, current_index, consumption, unit_price, total_amount, recorded_by, recorded_at) VALUES

-- A102: điện 150kWh | nước 8m³
('MR0100001', 'A102', 'ELECTRIC', 1, 2026, 1250, 1400, 150, 2800, 420000,  'S003', '2026-01-31 09:00:00'),
('MR0100002', 'A102', 'WATER',    1, 2026,   88,   96,   8, 15929, 127432, 'S003', '2026-01-31 09:10:00'),

-- A103: điện 200kWh | nước 12m³
('MR0100003', 'A103', 'ELECTRIC', 1, 2026, 3100, 3300, 200, 2800, 560000,  'S003', '2026-01-31 09:20:00'),
('MR0100004', 'A103', 'WATER',    1, 2026,  210,  222,  12, 15929, 191148, 'S003', '2026-01-31 09:30:00'),

-- A201: điện 180kWh | nước 10m³
('MR0100005', 'A201', 'ELECTRIC', 1, 2026, 2400, 2580, 180, 2800, 504000,  'S003', '2026-01-31 09:40:00'),
('MR0100006', 'A201', 'WATER',    1, 2026,  155,  165,  10, 15929, 159290, 'S003', '2026-01-31 09:50:00'),

-- A203: điện 220kWh | nước 14m³
('MR0100007', 'A203', 'ELECTRIC', 1, 2026, 1800, 2020, 220, 2800, 616000,  'S003', '2026-01-31 10:00:00'),
('MR0100008', 'A203', 'WATER',    1, 2026,  320,  334,  14, 15929, 223006, 'S003', '2026-01-31 10:10:00'),

-- A204: điện 260kWh | nước 18m³ (5 người)
('MR0100009', 'A204', 'ELECTRIC', 1, 2026, 4500, 4760, 260, 2800, 728000,  'S003', '2026-01-31 10:20:00'),
('MR0100010', 'A204', 'WATER',    1, 2026,  400,  418,  18, 15929, 286722, 'S003', '2026-01-31 10:30:00');

-- ── Tháng 2/2026 ───────────────────────────────────────────────
INSERT INTO meter_reading (ID, apartment_id, meter_type, month, year, previous_index, current_index, consumption, unit_price, total_amount, recorded_by, recorded_at) VALUES

-- A102: điện 145kWh | nước 7m³
('MR0200001', 'A102', 'ELECTRIC', 2, 2026, 1400, 1545, 145, 2800, 406000,  'S003', '2026-02-28 09:00:00'),
('MR0200002', 'A102', 'WATER',    2, 2026,   96,  103,   7, 15929, 111503, 'S003', '2026-02-28 09:10:00'),

-- A103: điện 190kWh | nước 11m³
('MR0200003', 'A103', 'ELECTRIC', 2, 2026, 3300, 3490, 190, 2800, 532000,  'S003', '2026-02-28 09:20:00'),
('MR0200004', 'A103', 'WATER',    2, 2026,  222,  233,  11, 15929, 175219, 'S003', '2026-02-28 09:30:00'),

-- A201: điện 175kWh | nước 9m³
('MR0200005', 'A201', 'ELECTRIC', 2, 2026, 2580, 2755, 175, 2800, 490000,  'S003', '2026-02-28 09:40:00'),
('MR0200006', 'A201', 'WATER',    2, 2026,  165,  174,   9, 15929, 143361, 'S003', '2026-02-28 09:50:00'),

-- A203: điện 210kWh | nước 13m³
('MR0200007', 'A203', 'ELECTRIC', 2, 2026, 2020, 2230, 210, 2800, 588000,  'S003', '2026-02-28 10:00:00'),
('MR0200008', 'A203', 'WATER',    2, 2026,  334,  347,  13, 15929, 207077, 'S003', '2026-02-28 10:10:00'),

-- A204: điện 250kWh | nước 17m³
('MR0200009', 'A204', 'ELECTRIC', 2, 2026, 4760, 5010, 250, 2800, 700000,  'S003', '2026-02-28 10:20:00'),
('MR0200010', 'A204', 'WATER',    2, 2026,  418,  435,  17, 15929, 286793, 'S003', '2026-02-28 10:30:00');


-- ============================================================
-- BƯỚC 4: INVOICE
-- Mỗi Invoice = 1 khoản phí (theo schema: fee_id → Fees)
-- Điện/nước KHÔNG có fee_id trực tiếp → tạo Fee ảo loại OTHER
-- hoặc tạo Invoice riêng với fee_id = phí điện/nước từ Unit_Price
--
-- ⚠ Cách xử lý: tạo thêm Fee loại SERVICE cho điện/nước mỗi tháng
--   (vì schema Invoice BẮT BUỘC có fee_id → Fees)
--   amount = total_amount từ meter_reading tương ứng
-- ============================================================

-- Fee điện/nước tháng 1 (dùng để gắn vào Invoice)
INSERT INTO Fees (ID, apartment_id, name, description, type, amount, effective_from, effective_to, create_by) VALUES
-- Tháng 1 điện
('FE10001', 'A102', 'Tiền điện T1/2026', 'Điện tháng 1/2026 - 150kWh',  'OTHER', 420000,  '2026-01-01', '2026-01-31', 'S003'),
('FE10002', 'A103', 'Tiền điện T1/2026', 'Điện tháng 1/2026 - 200kWh',  'OTHER', 560000,  '2026-01-01', '2026-01-31', 'S003'),
('FE10003', 'A201', 'Tiền điện T1/2026', 'Điện tháng 1/2026 - 180kWh',  'OTHER', 504000,  '2026-01-01', '2026-01-31', 'S003'),
('FE10004', 'A203', 'Tiền điện T1/2026', 'Điện tháng 1/2026 - 220kWh',  'OTHER', 616000,  '2026-01-01', '2026-01-31', 'S003'),
('FE10005', 'A204', 'Tiền điện T1/2026', 'Điện tháng 1/2026 - 260kWh',  'OTHER', 728000,  '2026-01-01', '2026-01-31', 'S003'),
-- Tháng 1 nước
('FW10001', 'A102', 'Tiền nước T1/2026', 'Nước tháng 1/2026 - 8m³',   'OTHER', 127432,  '2026-01-01', '2026-01-31', 'S003'),
('FW10002', 'A103', 'Tiền nước T1/2026', 'Nước tháng 1/2026 - 12m³',  'OTHER', 191148,  '2026-01-01', '2026-01-31', 'S003'),
('FW10003', 'A201', 'Tiền nước T1/2026', 'Nước tháng 1/2026 - 10m³',  'OTHER', 159290,  '2026-01-01', '2026-01-31', 'S003'),
('FW10004', 'A203', 'Tiền nước T1/2026', 'Nước tháng 1/2026 - 14m³',  'OTHER', 223006,  '2026-01-01', '2026-01-31', 'S003'),
('FW10005', 'A204', 'Tiền nước T1/2026', 'Nước tháng 1/2026 - 18m³',  'OTHER', 286722,  '2026-01-01', '2026-01-31', 'S003'),
-- Tháng 2 điện
('FE20001', 'A102', 'Tiền điện T2/2026', 'Điện tháng 2/2026 - 145kWh', 'OTHER', 406000,  '2026-02-01', '2026-02-28', 'S003'),
('FE20002', 'A103', 'Tiền điện T2/2026', 'Điện tháng 2/2026 - 190kWh', 'OTHER', 532000,  '2026-02-01', '2026-02-28', 'S003'),
('FE20003', 'A201', 'Tiền điện T2/2026', 'Điện tháng 2/2026 - 175kWh', 'OTHER', 490000,  '2026-02-01', '2026-02-28', 'S003'),
('FE20004', 'A203', 'Tiền điện T2/2026', 'Điện tháng 2/2026 - 210kWh', 'OTHER', 588000,  '2026-02-01', '2026-02-28', 'S003'),
('FE20005', 'A204', 'Tiền điện T2/2026', 'Điện tháng 2/2026 - 250kWh', 'OTHER', 700000,  '2026-02-01', '2026-02-28', 'S003'),
-- Tháng 2 nước
('FW20001', 'A102', 'Tiền nước T2/2026', 'Nước tháng 2/2026 - 7m³',   'OTHER', 111503,  '2026-02-01', '2026-02-28', 'S003'),
('FW20002', 'A103', 'Tiền nước T2/2026', 'Nước tháng 2/2026 - 11m³',  'OTHER', 175219,  '2026-02-01', '2026-02-28', 'S003'),
('FW20003', 'A201', 'Tiền nước T2/2026', 'Nước tháng 2/2026 - 9m³',   'OTHER', 143361,  '2026-02-01', '2026-02-28', 'S003'),
('FW20004', 'A203', 'Tiền nước T2/2026', 'Nước tháng 2/2026 - 13m³',  'OTHER', 207077,  '2026-02-01', '2026-02-28', 'S003'),
('FW20005', 'A204', 'Tiền nước T2/2026', 'Nước tháng 2/2026 - 17m³',  'OTHER', 286793,  '2026-02-01', '2026-02-28', 'S003');

-- ── INVOICES tháng 1/2026 (đã thanh toán) ─────────────────────
INSERT INTO Invoice (ID, apartment_id, month, year,
                     electric_reading_id, water_reading_id, service_fee_id, parking_fee_id,
                     electric_amount, water_amount, service_amount, parking_amount, total_amount,
                     status, issued_at, due_date, paid_at, create_by)
VALUES
-- A102: điện 420k | nước 127k | quản lý 560k | xe máy 100k + xe đạp điện 75k = 175k
('INV2601A102', 'A102', 1, 2026,
 'MR0100001', 'MR0100002', 'F001', 'F010',
 420000, 127432, 560000, 175000, 1282432,
 'PAID', '2026-01-31 08:00:00', '2026-02-15', '2026-02-10 10:00:00', 'S001'),

-- A103: điện 560k | nước 191k | quản lý 595k | xe máy 200k + ô tô 1300k = 1500k
('INV2601A103', 'A103', 1, 2026,
 'MR0100003', 'MR0100004', 'F002', 'F011',
 560000, 191148, 595000, 1500000, 2846148,
 'PAID', '2026-01-31 08:00:00', '2026-02-15', '2026-02-08 09:00:00', 'S001'),

-- A201: điện 504k | nước 159k | quản lý 630k | xe máy 100k
('INV2601A201', 'A201', 1, 2026,
 'MR0100005', 'MR0100006', 'F003', 'F013',
 504000, 159290, 630000, 100000, 1393290,
 'PAID', '2026-01-31 08:00:00', '2026-02-15', '2026-02-12 14:00:00', 'S001'),

-- A203: điện 616k | nước 223k | quản lý 665k | xe máy 100k + xe đạp điện 75k = 175k
('INV2601A203', 'A203', 1, 2026,
 'MR0100007', 'MR0100008', 'F004', 'F031',
 616000, 223006, 665000, 175000, 1679006,
 'PAID', '2026-01-31 08:00:00', '2026-02-15', '2026-02-09 11:00:00', 'S001'),

-- A204: điện 728k | nước 287k | quản lý 665k | 2 ô tô = 2600k
('INV2601A204', 'A204', 1, 2026,
 'MR0100009', 'MR0100010', 'F005', 'F021',
 728000, 286722, 665000, 2600000, 4279722,
 'PAID', '2026-01-31 08:00:00', '2026-02-15', '2026-02-11 16:00:00', 'S001'),

-- ── Tháng 2/2026 — mix PAID / UNPAID / OVERDUE ───────────────
-- A102: OVERDUE (quá hạn 15/3 chưa trả)
('INV2602A102', 'A102', 2, 2026,
 'MR0200001', 'MR0200002', 'F001', 'F010',
 406000, 111503, 560000, 175000, 1252503,
 'OVERDUE', '2026-02-28 08:00:00', '2026-03-15', NULL, 'S001'),

-- A103: PAID
('INV2602A103', 'A103', 2, 2026,
 'MR0200003', 'MR0200004', 'F002', 'F011',
 532000, 175219, 595000, 1500000, 2802219,
 'PAID', '2026-02-28 08:00:00', '2026-03-15', '2026-03-05 10:00:00', 'S001'),

-- A201: UNPAID
('INV2602A201', 'A201', 2, 2026,
 'MR0200005', 'MR0200006', 'F003', 'F013',
 490000, 143361, 630000, 100000, 1363361,
 'UNPAID', '2026-02-28 08:00:00', '2026-03-15', NULL, 'S001'),

-- A203: UNPAID
('INV2602A203', 'A203', 2, 2026,
 'MR0200007', 'MR0200008', 'F004', 'F031',
 588000, 207077, 665000, 175000, 1635077,
 'UNPAID', '2026-02-28 08:00:00', '2026-03-15', NULL, 'S001'),

-- A204: OVERDUE
('INV2602A204', 'A204', 2, 2026,
 'MR0200009', 'MR0200010', 'F005', 'F021',
 700000, 286793, 665000, 2600000, 4251793,
 'OVERDUE', '2026-02-28 08:00:00', '2026-03-15', NULL, 'S001');

select * from residents;