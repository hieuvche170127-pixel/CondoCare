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
    `ID` char(10) NOT NULL,
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
ALTER TABLE service_request
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