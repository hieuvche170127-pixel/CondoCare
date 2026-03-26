-- =============================================================
-- SWP391 - Apartment Management System
-- Schema hoàn chỉnh - Mô hình B (Điện/nước do EVN thu trực tiếp)
-- Phiên bản: 2.0
-- =============================================================

DROP DATABASE IF EXISTS SWP391;
CREATE DATABASE SWP391 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE SWP391;

SET FOREIGN_KEY_CHECKS = 0;

-- =============================================================
-- PHẦN 1: PHÂN QUYỀN & NHÂN SỰ
-- =============================================================

-- -----------------------------------------------------
-- 1. Role — Vai trò / phân quyền cho Staff
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `Role` (
                                      `ID`          char(10)     NOT NULL,
    `name`        varchar(100) NOT NULL COMMENT 'VD: ADMIN, MANAGER, ACCOUNTANT, TECHNICIAN',
    `description` varchar(255) DEFAULT NULL,
    PRIMARY KEY (`ID`)
    ) ENGINE=InnoDB COMMENT='Vai trò phân quyền cho nhân viên';

-- -----------------------------------------------------
-- 2. Staff — Tài khoản nhân viên / ban quản lý
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `Staff` (
                                       `ID`          char(10)                              NOT NULL,
    `username`    varchar(255)                          NOT NULL UNIQUE,
    `password`    varchar(255)                          NOT NULL COMMENT 'BCrypt hash',
    `full_name`   varchar(255)                          NOT NULL,
    `email`       varchar(255)                          DEFAULT NULL,
    `phone`       varchar(20)                           NOT NULL,
    `position`    varchar(255)                          NOT NULL,
    `department`  varchar(255)                          NOT NULL,
    `dob`         date                                  DEFAULT NULL,
    `gender`      ENUM('M','F')                         NOT NULL,
    `status`      ENUM('ACTIVE','RESIGNED','ON_LEAVE')  NOT NULL DEFAULT 'ACTIVE',
    `role_id`     char(10)                              NOT NULL,
    `hired_at`    timestamp                             NULL DEFAULT NULL,
    `last_login`  timestamp                             NULL DEFAULT NULL,
    `created_at`  timestamp                             NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`ID`),
    INDEX `idx_staff_role` (`role_id`),
    CONSTRAINT `FK_Staff_Role` FOREIGN KEY (`role_id`) REFERENCES `Role`(`ID`)
    ) ENGINE=InnoDB COMMENT='Tài khoản nhân viên ban quản lý';

-- =============================================================
-- PHẦN 2: TÒA NHÀ & CĂN HỘ
-- =============================================================

-- -----------------------------------------------------
-- 3. Building — Tòa nhà
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `Building` (
                                          `ID`                char(10)     NOT NULL,
    `name`              varchar(255) NOT NULL,
    `address`           varchar(255) NOT NULL,
    `total_floors`      int          NOT NULL,
    `total_apartments`  int          NOT NULL,
    `manager_id`        char(10)     NOT NULL COMMENT 'Trưởng ban quản lý tòa nhà',
    PRIMARY KEY (`ID`),
    CONSTRAINT `FK_Building_Manager` FOREIGN KEY (`manager_id`) REFERENCES `Staff`(`ID`)
    ) ENGINE=InnoDB COMMENT='Thông tin tòa nhà';

-- -----------------------------------------------------
-- 4. Apartment — Căn hộ
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `Apartment` (
                                           `ID`               char(10)                                    NOT NULL,
    `number`           varchar(10)                                 NOT NULL COMMENT 'Số hiệu căn hộ, VD: A101',
    `floor`            int                                         NOT NULL,
    `area`             decimal(8,2)                                NOT NULL COMMENT 'Diện tích m²',
    `building_id`      char(10)                                    NOT NULL,
    `status`           ENUM('EMPTY','OCCUPIED','MAINTENANCE')      NOT NULL DEFAULT 'EMPTY',
    `rental_status`    ENUM('AVAILABLE','RENTED','OWNER_OCCUPIED') NOT NULL DEFAULT 'AVAILABLE',
    `images`           text                                        DEFAULT NULL COMMENT 'JSON array đường dẫn ảnh',
    `description`      varchar(500)                                DEFAULT NULL,
    `total_residents`  int                                         NOT NULL DEFAULT 0,
    `total_vehicles`   int                                         NOT NULL DEFAULT 0,
    PRIMARY KEY (`ID`),
    UNIQUE KEY `uq_apartment_number_building` (`number`, `building_id`),
    INDEX `idx_apt_building` (`building_id`),
    CONSTRAINT `FK_Apartment_Building` FOREIGN KEY (`building_id`) REFERENCES `Building`(`ID`)
    ) ENGINE=InnoDB COMMENT='Thông tin căn hộ';

-- =============================================================
-- PHẦN 3: CƯ DÂN
-- =============================================================

-- -----------------------------------------------------
-- 5. Residents — Tài khoản cư dân
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `Residents` (
                                           `ID`              char(10)                         NOT NULL,
    `username`        varchar(255)                     NOT NULL UNIQUE,
    `password`        varchar(255)                     NOT NULL COMMENT 'BCrypt hash',
    `full_name`       varchar(255)                     NOT NULL,
    `type`            ENUM('OWNER','TENANT','GUEST')   NOT NULL DEFAULT 'TENANT',
    `dob`             date                             DEFAULT NULL,
    `gender`          ENUM('M','F')                    NOT NULL,
    `id_number`       char(12)                         DEFAULT NULL COMMENT 'Số CCCD/CMND — dùng để xác minh danh tính',
    `phone`           varchar(20)                      NOT NULL,
    `email`           varchar(255)                     DEFAULT NULL,
    `status`          ENUM('PENDING','ACTIVE','INACTIVE') NOT NULL DEFAULT 'PENDING'
    COMMENT 'PENDING=chờ manager duyệt, ACTIVE=đã xác minh',
    `apartment_id`    char(10)                         DEFAULT NULL COMMENT 'NULL khi chưa được gán căn hộ',
    `verified_by`     char(10)                         DEFAULT NULL COMMENT 'Staff đã duyệt tài khoản',
    `verified_at`     timestamp                        NULL DEFAULT NULL,
    `last_login`      timestamp                        NULL DEFAULT NULL,
    `created_at`      timestamp                        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`ID`),
    INDEX `idx_resident_apartment` (`apartment_id`),
    INDEX `idx_resident_status` (`status`),
    CONSTRAINT `FK_Residents_Apartment` FOREIGN KEY (`apartment_id`) REFERENCES `Apartment`(`ID`) ON DELETE SET NULL,
    CONSTRAINT `FK_Residents_VerifiedBy` FOREIGN KEY (`verified_by`) REFERENCES `Staff`(`ID`) ON DELETE SET NULL
    ) ENGINE=InnoDB COMMENT='Tài khoản cư dân — có luồng duyệt bởi manager';

-- -----------------------------------------------------
-- 6. access_cards — Thẻ từ ra vào
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `access_cards` (
                                              `ID`          char(10)                         NOT NULL,
    `card_number` varchar(20)                      NOT NULL UNIQUE,
    `resident_id` char(10)                         NOT NULL,
    `issued_by`   char(10)                         DEFAULT NULL,
    `issued_at`   timestamp                        NULL DEFAULT NULL,
    `expired_at`  timestamp                        NULL DEFAULT NULL,
    `status`      ENUM('ACTIVE','BLOCKED','LOST')  NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (`ID`),
    INDEX `idx_card_resident` (`resident_id`),
    CONSTRAINT `FK_Card_Resident` FOREIGN KEY (`resident_id`) REFERENCES `Residents`(`ID`) ON DELETE CASCADE,
    CONSTRAINT `FK_Card_Staff`    FOREIGN KEY (`issued_by`)   REFERENCES `Staff`(`ID`)    ON DELETE SET NULL
    ) ENGINE=InnoDB COMMENT='Thẻ từ ra vào tòa nhà';

-- =============================================================
-- 7. Vehicle — Phương tiện đăng ký gửi xe
-- =============================================================
CREATE TABLE IF NOT EXISTS `Vehicle` (
                                         `ID` char(10) 						NOT NULL COMMENT 'Mã phương tiện, VD: VH00000001',

    -- Thông tin phương tiện
    `type` ENUM('MOTORBIKE','CAR','BICYCLE','ELECTRIC_BIKE','OTHER') NOT NULL,
    `license_plate` varchar(20) 		DEFAULT NULL COMMENT 'Biển số xe (có thể NULL nếu xe đạp)',
    `brand` varchar(100) 				DEFAULT NULL COMMENT 'Hãng xe',
    `model` varchar(100) 				DEFAULT NULL COMMENT 'Dòng xe (mới thêm)',
    `color` varchar(50) 				DEFAULT NULL,

    -- Liên kết với cư dân & căn hộ
    `resident_id` char(10) 				NOT NULL,
    `apartment_id` char(10) 			NOT NULL,

    -- Thông tin đăng ký & thời hạn
    `duration_type` ENUM('MONTHLY','QUARTERLY','YEARLY') NOT NULL DEFAULT 'MONTHLY',
    `registered_at` timestamp 			NULL DEFAULT NULL COMMENT 'Ngày bắt đầu đăng ký',
    `expired_at` timestamp 				NULL DEFAULT NULL COMMENT 'Ngày hết hạn (tính tự động theo duration_type)',

    -- Phê duyệt
    `pending_status` ENUM('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING',
    `approved_by` char(10) 				DEFAULT NULL COMMENT 'Nhân viên ban quản lý phê duyệt',
    `approved_at` timestamp 			NULL DEFAULT NULL,
    `reject_reason` varchar(255) 		DEFAULT NULL COMMENT 'Lý do từ chối (nếu REJECTED)',

    -- Trạng thái & lịch sử
    `status` ENUM('ACTIVE','INACTIVE','LOST','REVOKED') NOT NULL DEFAULT 'ACTIVE',
    `revoked_at` timestamp 				NULL DEFAULT NULL COMMENT 'Ngày thu hồi đăng ký',
    `note` text DEFAULT 				NULL COMMENT 'Ghi chú nội bộ',

    -- Audit
    `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (`ID`),

    -- Unique để tránh trùng biển số trong cùng tòa nhà
    UNIQUE KEY `uq_vehicle_license_plate` (`license_plate`) COMMENT 'Tránh trùng biển số trong tòa nhà',

    INDEX `idx_vehicle_resident` (`resident_id`),
    INDEX `idx_vehicle_apartment` (`apartment_id`),
    INDEX `idx_vehicle_status` (`status`),
    INDEX `idx_vehicle_pending` (`pending_status`),
    INDEX `idx_vehicle_expired` (`expired_at`),
    INDEX `idx_vehicle_type` (`type`),

    CONSTRAINT `FK_Vehicle_Resident` FOREIGN KEY (`resident_id`) REFERENCES `Residents`(`ID`) ON DELETE CASCADE,
    CONSTRAINT `FK_Vehicle_Apartment` FOREIGN KEY (`apartment_id`) REFERENCES `Apartment`(`ID`) ON DELETE CASCADE,
    CONSTRAINT `FK_Vehicle_ApprovedBy` FOREIGN KEY (`approved_by`) REFERENCES `Staff`(`ID`) ON DELETE SET NULL
    ) ENGINE=InnoDB COMMENT='Phương tiện đăng ký gửi xe của cư dân - Hỗ trợ phê duyệt và quản lý thời hạn';

-- =============================================================
-- PHẦN 4: PHÍ DỊCH VỤ & HÓA ĐƠN
-- (Mô hình B: Điện/nước do EVN thu trực tiếp — BQL không quản lý)
-- =============================================================

-- -----------------------------------------------------
-- 8. FeeTemplate — Mẫu phí áp dụng cho tòa nhà
--    (Thay cho bảng Fees cũ — không gán theo apartment nữa)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `FeeTemplate` (
                                             `ID`             char(10)                         NOT NULL,
    `building_id`    char(10)                         NOT NULL,
    `name`           varchar(255)                     NOT NULL COMMENT 'VD: Phí quản lý, Phí gửi xe máy',
    `type`           ENUM('SERVICE','PARKING')        NOT NULL,
    `amount`         decimal(19,2)                    NOT NULL,
    `unit`           ENUM('PER_APT','PER_M2','FIXED') NOT NULL DEFAULT 'FIXED'
    COMMENT 'PER_APT=theo căn, PER_M2=theo m², FIXED=cố định',
    `effective_from` date                             NOT NULL,
    `effective_to`   date                             DEFAULT NULL COMMENT 'NULL = còn hiệu lực',
    `status`         ENUM('ACTIVE','INACTIVE')        NOT NULL DEFAULT 'ACTIVE',
    `created_by`     char(10)                         NOT NULL,
    `created_at`     timestamp                        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`ID`),
    INDEX `idx_fee_building` (`building_id`),
    INDEX `idx_fee_status` (`status`),
    CONSTRAINT `FK_FeeTemplate_Building` FOREIGN KEY (`building_id`) REFERENCES `Building`(`ID`),
    CONSTRAINT `FK_FeeTemplate_Staff`    FOREIGN KEY (`created_by`)  REFERENCES `Staff`(`ID`)
    ) ENGINE=InnoDB COMMENT='Mẫu phí dịch vụ/gửi xe cho từng tòa nhà';

-- -----------------------------------------------------
-- 9. Invoice — Hóa đơn hàng tháng
--    Chỉ gồm phí dịch vụ + phí gửi xe
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `Invoice` (
                                         `ID`           varchar(15)                        NOT NULL,
    `apartment_id` char(10)                           NOT NULL,
    `month`        tinyint                            NOT NULL COMMENT '1-12',
    `year`         smallint                           NOT NULL,
    `total_amount` decimal(19,2)                      NOT NULL DEFAULT 0,
    `status`       ENUM('UNPAID','PAID','OVERDUE')    NOT NULL DEFAULT 'UNPAID',
    `issued_at`    datetime                           DEFAULT NULL,
    `due_date`     date                               DEFAULT NULL,
    `paid_at`      datetime                           DEFAULT NULL,
    `created_by`   char(10)                           NOT NULL,
    `created_at`   timestamp                          NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`ID`),
    UNIQUE KEY `uq_invoice_apt_month` (`apartment_id`, `month`, `year`)
    COMMENT 'Mỗi căn hộ chỉ có 1 hóa đơn/tháng',
    INDEX `idx_invoice_status` (`status`),
    INDEX `idx_invoice_month`  (`year`, `month`),
    CONSTRAINT `FK_Invoice_Apartment` FOREIGN KEY (`apartment_id`) REFERENCES `Apartment`(`ID`),
    CONSTRAINT `FK_Invoice_Staff`     FOREIGN KEY (`created_by`)   REFERENCES `Staff`(`ID`)
    ) ENGINE=InnoDB COMMENT='Hóa đơn hàng tháng — chỉ phí dịch vụ và gửi xe';

-- -----------------------------------------------------
-- 10. invoice_fee_detail — Chi tiết từng dòng phí trong hóa đơn
--     Snapshot tên và số tiền tại thời điểm phát hành
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `invoice_fee_detail` (
                                                    `ID`              char(10)     NOT NULL,
    `invoice_id`      varchar(15)  NOT NULL,
    `fee_template_id` char(10)     DEFAULT NULL COMMENT 'NULL nếu template đã bị xóa',
    `fee_name`        varchar(255) NOT NULL     COMMENT 'Snapshot tên phí lúc phát hành',
    `fee_type`        ENUM('SERVICE','PARKING') NOT NULL,
    `unit_amount`     decimal(19,2) NOT NULL    COMMENT 'Đơn giá tại thời điểm phát hành',
    `quantity`        decimal(8,2)  NOT NULL DEFAULT 1 COMMENT 'Số lượng (xe, m², ...)',
    `amount`          decimal(19,2) NOT NULL    COMMENT 'Thành tiền = unit_amount × quantity',
    PRIMARY KEY (`ID`),
    INDEX `idx_detail_invoice` (`invoice_id`),
    CONSTRAINT `FK_Detail_Invoice`   FOREIGN KEY (`invoice_id`)      REFERENCES `Invoice`(`ID`)      ON DELETE CASCADE,
    CONSTRAINT `FK_Detail_FeeTempl`  FOREIGN KEY (`fee_template_id`) REFERENCES `FeeTemplate`(`ID`) ON DELETE SET NULL
    ) ENGINE=InnoDB COMMENT='Chi tiết từng khoản phí trong hóa đơn (snapshot)';

-- -----------------------------------------------------
-- 11. Payments — Lịch sử thanh toán
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `Payments` (
                                          `ID`             varchar(36)                             NOT NULL COMMENT 'UUID',
    `invoice_id`     varchar(15)                             NOT NULL,
    `amount`         decimal(19,2)                           NOT NULL,
    `paid_at`        timestamp                               NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `method`         ENUM('CASH','BANKING','MOMO','ZALOPAY') NOT NULL,
    `momo_trans_id`  varchar(50)   DEFAULT NULL              COMMENT 'MoMo transId từ IPN callback',
    `momo_order_id`  varchar(50)   DEFAULT NULL              COMMENT 'orderId gửi lên MoMo',
    `note`           varchar(255)  DEFAULT NULL,
    `paid_by`        char(10)                                NOT NULL COMMENT 'resident_id thực hiện thanh toán',
    PRIMARY KEY (`ID`),
    INDEX `idx_payment_invoice`    (`invoice_id`),
    INDEX `idx_payment_momo_trans` (`momo_trans_id`),
    CONSTRAINT `FK_Payments_Invoice`  FOREIGN KEY (`invoice_id`) REFERENCES `Invoice`(`ID`),
    CONSTRAINT `FK_Payments_Resident` FOREIGN KEY (`paid_by`)    REFERENCES `Residents`(`ID`)
    ) ENGINE=InnoDB COMMENT='Lịch sử thanh toán hóa đơn';

-- =============================================================
-- PHẦN 5: VẬN HÀNH
-- =============================================================

-- -----------------------------------------------------
-- 12. Notification — Thông báo đến cư dân
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `Notification` (
                                              `ID`          varchar(15)                                      NOT NULL,
    `title`       varchar(255)                                     NOT NULL,
    `content`     text                                             NOT NULL,
    `type`        ENUM('INFO','WARNING','URGENT','MAINTENANCE','PAYMENT') NOT NULL DEFAULT 'INFO',
    `resident_id` char(10)      DEFAULT NULL                       COMMENT 'NULL = gửi broadcast toàn tòa',
    `apartment_id` char(10)     DEFAULT NULL                       COMMENT 'NULL = gửi toàn tòa nhà',
    `building_id` char(10)      DEFAULT NULL,
    `is_read`     tinyint(1)    NOT NULL DEFAULT 0,
    `created_by`  char(10)      NOT NULL,
    `created_at`  datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`ID`),
    INDEX `idx_notif_resident` (`resident_id`),
    INDEX `idx_notif_building` (`building_id`),
    INDEX `idx_notif_created`  (`created_at`),
    CONSTRAINT `FK_Notif_Resident` FOREIGN KEY (`resident_id`) REFERENCES `Residents`(`ID`) ON DELETE CASCADE,
    CONSTRAINT `FK_Notif_Apartment` FOREIGN KEY (`apartment_id`) REFERENCES `Apartment`(`ID`) ON DELETE SET NULL,
    CONSTRAINT `FK_Notif_Building` FOREIGN KEY (`building_id`)  REFERENCES `Building`(`ID`) ON DELETE SET NULL,
    CONSTRAINT `FK_Notif_Staff`    FOREIGN KEY (`created_by`)   REFERENCES `Staff`(`ID`)
    ) ENGINE=InnoDB COMMENT='Thông báo — hỗ trợ gửi cá nhân, theo căn hoặc toàn tòa';

-- -----------------------------------------------------
-- 13. service_request — Yêu cầu hỗ trợ / sửa chữa
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `service_request` (
                                                 `ID`                 varchar(15)                                         NOT NULL,
    `title`              varchar(255)                                        NOT NULL,
    `description`        text                                                NOT NULL,
    `category`           ENUM('ELECTRIC','WATER','INTERNET','HVAC','STRUCTURE','OTHER') NOT NULL DEFAULT 'OTHER',
    `status`             ENUM('PENDING','IN_PROGRESS','DONE','REJECTED')     NOT NULL DEFAULT 'PENDING',
    `priority`           ENUM('LOW','MEDIUM','HIGH')                         NOT NULL DEFAULT 'MEDIUM',
    `resident_id`        char(10)                                            NOT NULL,
    `apartment_id`       char(10)                                            DEFAULT NULL,
    `assigned_to`        char(10)                                            DEFAULT NULL COMMENT 'Kỹ thuật viên được giao',
    `note`               text                                                DEFAULT NULL COMMENT 'Ghi chú nội bộ của staff',
    `completion_image`   text                                                DEFAULT NULL COMMENT 'URL ảnh xác nhận hoàn thành',
    `resident_confirmed` tinyint(1)  NOT NULL DEFAULT 0                      COMMENT '1 = cư dân xác nhận đã xử lý xong',
    `confirmed_at`       datetime    DEFAULT NULL,
    `reject_reason`      text        DEFAULT NULL,
    `created_at`         datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`         datetime    DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`ID`),
    INDEX `idx_sr_resident` (`resident_id`),
    INDEX `idx_sr_status`   (`status`),
    INDEX `idx_sr_assigned` (`assigned_to`),
    INDEX `idx_sr_created`  (`created_at`),
    CONSTRAINT `FK_SR_Resident`  FOREIGN KEY (`resident_id`)  REFERENCES `Residents`(`ID`),
    CONSTRAINT `FK_SR_Apartment` FOREIGN KEY (`apartment_id`) REFERENCES `Apartment`(`ID`)  ON DELETE SET NULL,
    CONSTRAINT `FK_SR_Staff`     FOREIGN KEY (`assigned_to`)  REFERENCES `Staff`(`ID`)      ON DELETE SET NULL
    ) ENGINE=InnoDB COMMENT='Yêu cầu hỗ trợ kỹ thuật từ cư dân';

-- =============================================================
-- BẬT LẠI KIỂM TRA KHÓA NGOẠI
-- =============================================================
SET FOREIGN_KEY_CHECKS = 1;