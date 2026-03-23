-- =============================================================
-- SWP391 - Database Initialization Script (Clean & Optimized)
-- =============================================================

DROP DATABASE IF EXISTS SWP391;
CREATE DATABASE SWP391 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE SWP391;

-- Tắt kiểm tra khóa ngoại để tạo bảng không bị vướng víu
SET FOREIGN_KEY_CHECKS = 1; 

-- -----------------------------------------------------
-- 1. Table: Role (Phân quyền cho Staff)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `Role` (
                                      `ID` char(10) NOT NULL,
    `name` varchar(100) NOT NULL,
    `description` varchar(255) DEFAULT NULL,
    PRIMARY KEY (`ID`)
    ) ENGINE=InnoDB;

-- -----------------------------------------------------
-- 2. Table: Staff (Tài khoản nhân viên)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `Staff` (
                                       `ID`         char(10)      NOT NULL,
    `username`   varchar(255)  NOT NULL UNIQUE,
    `password`   varchar(255)  NOT NULL,
    `full_name`  varchar(255)  NOT NULL,
    `email`      varchar(255)  DEFAULT NULL,
    `phone`      varchar(20)   NOT NULL,
    `position`   varchar(255)  NOT NULL,
    `department` varchar(255)  NOT NULL,
    `dob`        date          DEFAULT NULL,
    `gender`     ENUM('M','F') NOT NULL,
    `status`     ENUM('ACTIVE','RESIGNED','ON_LEAVE') NOT NULL DEFAULT 'ACTIVE',
    `role_id`    char(10)      NOT NULL,
    `hired_at`   timestamp     NULL DEFAULT NULL,
    `last_login` timestamp     NULL DEFAULT NULL,
    `create_at`  timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`ID`),
    CONSTRAINT `FK_Staff_Role` FOREIGN KEY (`role_id`) REFERENCES `Role`(`ID`)
    ) ENGINE=InnoDB;

-- -----------------------------------------------------
-- 3. Table: Building (Tòa nhà)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `Building` (
                                          `ID` char(10) NOT NULL,
    `name` varchar(255) NOT NULL,
    `address` varchar(255) NOT NULL,
    `total_floors` int(10) NOT NULL,
    `total_apartments` int(10) NOT NULL,
    `manager_id` char(10) NOT NULL,
    PRIMARY KEY (`ID`),
    CONSTRAINT `FK_Building_Manager` FOREIGN KEY (`manager_id`) REFERENCES `Staff` (`ID`)
    ) ENGINE=InnoDB;

-- -----------------------------------------------------
-- 4. Table: Apartment (Căn hộ)
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
-- 5. Table: Residents (Cư dân)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `Residents` (
                                           `ID`             char(10)      NOT NULL,
    `username`       varchar(255)  NOT NULL UNIQUE,
    `password`       varchar(255)  NOT NULL,
    `full_name`      varchar(255)  NOT NULL,
    `type`           ENUM('OWNER','TENANT','GUEST') NOT NULL,
    `dob`            date          DEFAULT NULL,
    `gender`         ENUM('M','F') NOT NULL,
    `id_number`      char(12)      NULL,
    `phone`          varchar(20)   NOT NULL,
    `email`          varchar(255)  DEFAULT NULL,
    `status`         ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    `apartment_id`   char(10)      NULL,
    `temp_residence` varchar(255)  DEFAULT NULL,
    `temp_absence`   varchar(255)  DEFAULT NULL,
    `last_login`     timestamp     NULL DEFAULT NULL,
    `create_at`      timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`ID`),
    CONSTRAINT `FK_Residents_Apartment`
    FOREIGN KEY (`apartment_id`) REFERENCES `Apartment`(`ID`) ON DELETE SET NULL
    ) ENGINE=InnoDB;

-- -----------------------------------------------------
-- 6. Table: access_cards (Thẻ ra vào)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `access_cards` (
                                              `ID` char(10) NOT NULL,
    `card_number` char(8) NOT NULL,
    `issued_by` char(10) DEFAULT NULL,
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
-- 7. Table: Vehicle (Phương tiện)
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
-- 9. Table: Fees (Phí dịch vụ - Đã chuẩn hóa tiền tệ)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `Fees` (
                                      `ID`             char(10)      NOT NULL,
    `apartment_id`   char(10)      NOT NULL,
    `name`           varchar(255)  NOT NULL,
    `description`    varchar(255)  DEFAULT NULL,
    `type`           ENUM('SERVICE','PARKING','MANAGEMENT','OTHER') NOT NULL,
    `amount`         decimal(19,2) NOT NULL,
    `effective_from` date          NOT NULL,
    `effective_to`   date          NULL,
    `create_by`      char(10)      NOT NULL,
    PRIMARY KEY (`ID`),
    CONSTRAINT `FK_Fees_Apartment` FOREIGN KEY (`apartment_id`) REFERENCES `Apartment`(`ID`),
    CONSTRAINT `FK_Fees_Staff`     FOREIGN KEY (`create_by`)    REFERENCES `Staff`(`ID`)
    ) ENGINE=InnoDB;
-- -----------------------------------------------------
-- 10. Table: meter_reading (Chỉ số điện nước)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `meter_reading` (
                                               `ID`             char(15)      NOT NULL,
    `apartment_id`   char(10)      NOT NULL,
    `meter_type`     ENUM('ELECTRIC','WATER') NOT NULL,
    `month`          int           NOT NULL,
    `year`           int           NOT NULL,
    `previous_index` decimal(19,0) NOT NULL,
    `current_index`  decimal(19,0) NOT NULL,
    `consumption`    decimal(19,0) NOT NULL DEFAULT 0,
    `unit_price`     decimal(19,2) NOT NULL DEFAULT 0,
    `total_amount`   decimal(19,2) NOT NULL,
    `recorded_by`    char(10)      NOT NULL,
    `recorded_at`    timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`ID`),
    CONSTRAINT `FK_Meter_Apartment` FOREIGN KEY (`apartment_id`) REFERENCES `Apartment`(`ID`),
    CONSTRAINT `FK_Meter_Staff`     FOREIGN KEY (`recorded_by`)  REFERENCES `Staff`(`ID`)
    ) ENGINE=InnoDB;

-- -----------------------------------------------------
-- 11. Table: Invoice (Hóa đơn)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `Invoice` (
                                         `ID`                   varchar(15)   NOT NULL,
    `apartment_id`         char(10)      NOT NULL,
    `month`                int           NOT NULL,
    `year`                 int           NOT NULL,
    `electric_reading_id`  char(15)      NULL,
    `water_reading_id`     char(15)      NULL,
    `service_fee_id`       char(10)      NULL,
    `parking_fee_id`       char(10)      NULL,
    `electric_amount`      decimal(19,2) NULL DEFAULT 0,
    `water_amount`         decimal(19,2) NULL DEFAULT 0,
    `service_amount`       decimal(19,2) NULL DEFAULT 0,
    `parking_amount`       decimal(19,2) NULL DEFAULT 0,
    `total_amount`         decimal(19,2) NULL DEFAULT 0,
    `status`               ENUM('UNPAID','PAID','OVERDUE') NOT NULL DEFAULT 'UNPAID',
    `issued_at`            datetime      NULL,
    `due_date`             date          NULL,
    `paid_at`              datetime      NULL,
    `create_by`            char(10)      NOT NULL,
    PRIMARY KEY (`ID`),
    CONSTRAINT `FK_Invoice_Apartment`       FOREIGN KEY (`apartment_id`)       REFERENCES `Apartment`(`ID`),
    CONSTRAINT `FK_Invoice_ElectricReading` FOREIGN KEY (`electric_reading_id`) REFERENCES `meter_reading`(`ID`) ON DELETE SET NULL,
    CONSTRAINT `FK_Invoice_WaterReading`    FOREIGN KEY (`water_reading_id`)    REFERENCES `meter_reading`(`ID`) ON DELETE SET NULL,
    CONSTRAINT `FK_Invoice_ServiceFee`      FOREIGN KEY (`service_fee_id`)      REFERENCES `Fees`(`ID`) ON DELETE SET NULL,
    CONSTRAINT `FK_Invoice_ParkingFee`      FOREIGN KEY (`parking_fee_id`)      REFERENCES `Fees`(`ID`) ON DELETE SET NULL,
    CONSTRAINT `FK_Invoice_Staff`           FOREIGN KEY (`create_by`)           REFERENCES `Staff`(`ID`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- 12. Table: Payments (Lịch sử thanh toán)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `Payments` (
                                          `ID`             varchar(36)   NOT NULL,
    `invoice_id`     varchar(15)   NOT NULL,
    `amount`         decimal(19,2) NOT NULL,
    `paid_at`        timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `method`         ENUM('CASH','BANKING','MOMO','ZALOPAY') NOT NULL,
    `momo_trans_id`  varchar(30)   NULL     COMMENT 'MoMo transId từ IPN callback',
    `momo_order_id`  varchar(50)   NULL     COMMENT 'orderId gửi lên MoMo (invoiceId + timestamp)',
    `note`           varchar(100)  DEFAULT NULL,
    `paid_by`        char(10)      NOT NULL,
    PRIMARY KEY (`ID`),
    INDEX `idx_invoice` (`invoice_id`),
    INDEX `idx_momo_trans` (`momo_trans_id`),
    CONSTRAINT `FK_Payments_Invoice`  FOREIGN KEY (`invoice_id`) REFERENCES `Invoice`(`ID`),
    CONSTRAINT `FK_Payments_Resident` FOREIGN KEY (`paid_by`)    REFERENCES `Residents`(`ID`)
    ) ENGINE=InnoDB;
-- -----------------------------------------------------
-- 13. Table: Notification (Thông báo)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `Notification` (
                                              `ID` VARCHAR(15) NOT NULL,
    `title` VARCHAR(255) NOT NULL,
    `content` TEXT NOT NULL,
    `type` ENUM('INFO','WARNING','URGENT','MAINTENANCE','PAYMENT') NOT NULL DEFAULT 'INFO',
    `resident_id` CHAR(10) NULL,
    `is_read` TINYINT(1) NOT NULL DEFAULT 0,
    `created_by` CHAR(10) NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`ID`),
    INDEX `idx_resident` (`resident_id`),
    INDEX `idx_created` (`created_at`),
    CONSTRAINT `fk_notif_resident` FOREIGN KEY (`resident_id`) REFERENCES `Residents`(`ID`) ON DELETE CASCADE,
    CONSTRAINT `fk_notif_staff` FOREIGN KEY (`created_by`) REFERENCES `Staff`(`ID`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- 14. Table: service_request (Yêu cầu hỗ trợ)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `service_request` (
                                                 `ID`                 varchar(15)  NOT NULL,
    `title`              varchar(255) NOT NULL,
    `description`        text         NOT NULL,
    `category`           ENUM('ELECTRIC','WATER','INTERNET','HVAC','STRUCTURE','OTHER') NOT NULL DEFAULT 'OTHER',
    `status`             ENUM('PENDING','IN_PROGRESS','DONE','REJECTED') NOT NULL DEFAULT 'PENDING',
    `priority`           ENUM('LOW','MEDIUM','HIGH') NOT NULL DEFAULT 'MEDIUM',
    `resident_id`        char(10)     NOT NULL,
    `apartment_id`       char(10)     NULL,
    `assigned_to`        char(10)     NULL,
    `note`               text         NULL,
    `completion_image`   mediumtext   NULL     COMMENT 'Base64 ảnh xác nhận hoàn thành',
    `resident_confirmed` tinyint(1)   NOT NULL DEFAULT 0 COMMENT 'Resident xác nhận đã xử lý xong',
    `confirmed_at`       datetime     NULL     COMMENT 'Thời điểm resident xác nhận',
    `reject_reason`      text         NULL     COMMENT 'Lý do từ chối',
    `created_at`         datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`         datetime     NULL     ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`ID`),
    INDEX `idx_sr_resident` (`resident_id`),
    INDEX `idx_sr_status`   (`status`),
    INDEX `idx_sr_created`  (`created_at`),
    CONSTRAINT `fk_sr_resident`  FOREIGN KEY (`resident_id`)  REFERENCES `Residents`(`ID`),
    CONSTRAINT `fk_sr_apartment` FOREIGN KEY (`apartment_id`) REFERENCES `Apartment`(`ID`) ON DELETE SET NULL,
    CONSTRAINT `fk_sr_staff`     FOREIGN KEY (`assigned_to`)  REFERENCES `Staff`(`ID`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Bật lại kiểm tra khóa ngoại sau khi đã tạo xong
SET FOREIGN_KEY_CHECKS = 1; 