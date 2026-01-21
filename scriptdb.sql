DROP DATABASE IF EXISTS SWP391;
CREATE DATABASE SWP391 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE SWP391;

SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------------------------------
-- Table: Role
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `Role` (
  `ID` char(10) NOT NULL,
  `name` varchar(100) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB;

-- -----------------------------------------------------
-- Table: User
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `User` (
  `ID` char(10) NOT NULL,
  `username` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `full_name` varchar(255) NOT NULL,
  `email` varchar(255) DEFAULT NULL,
  `phone` varchar(20) DEFAULT NULL,
  -- [FIX CỨNG] Chỉ cho phép 3 trạng thái
  `status` ENUM('ACTIVE', 'INACTIVE', 'BLOCKED') NOT NULL DEFAULT 'ACTIVE',
  `role_id` char(10) NOT NULL,
  `create_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `last_login` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `FK_User_Role` (`role_id`),
  CONSTRAINT `FK_User_Role` FOREIGN KEY (`role_id`) REFERENCES `Role` (`ID`)
) ENGINE=InnoDB;

-- -----------------------------------------------------
-- Table: Staff
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `Staff` (
  `ID` char(10) NOT NULL,
  `position` varchar(255) NOT NULL,
  `department` varchar(255) NOT NULL,
  `dob` date DEFAULT NULL,
  -- [FIX CỨNG] Giới tính
  `gender` ENUM('M', 'F') NOT NULL,
  `phone` varchar(20) NOT NULL,
  `email` varchar(100) DEFAULT NULL,
  -- [FIX CỨNG] Trạng thái nhân viên
  `status` ENUM('ACTIVE', 'RESIGNED', 'ON_LEAVE') NOT NULL DEFAULT 'ACTIVE',
  `hired_at` timestamp NULL DEFAULT NULL,
  `terminated_at` timestamp NULL DEFAULT NULL,
  `user_id` char(10) NOT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `UQ_Staff_User` (`user_id`), 
  CONSTRAINT `FK_Staff_User` FOREIGN KEY (`user_id`) REFERENCES `User` (`ID`)
) ENGINE=InnoDB;

-- -----------------------------------------------------
-- Table: Building
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `Building` (
  `ID` char(10) NOT NULL,
  `name` varchar(255) NOT NULL,
  `address` varchar(255) NOT NULL,
  `total_floors` int(10) NOT NULL,
  `total_apartments` int(10) NOT NULL,
  `manager_id` char(10) NOT NULL,
  PRIMARY KEY (`ID`),
  KEY `FK_Building_Manager` (`manager_id`),
  CONSTRAINT `FK_Building_Manager` FOREIGN KEY (`manager_id`) REFERENCES `User` (`ID`)
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
  -- [FIX CỨNG] Trạng thái phòng
  `status` ENUM('EMPTY', 'OCCUPIED', 'MAINTENANCE') DEFAULT 'EMPTY',
  -- [FIX CỨNG] Tình trạng cho thuê
  `rental_status` ENUM('AVAILABLE', 'RENTED', 'OWNER') DEFAULT 'AVAILABLE',
  `images` varchar(255) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `total_resident` int(10) DEFAULT 0,
  `total_vehicle` int(10) DEFAULT 0,
  PRIMARY KEY (`ID`),
  KEY `FK_Apartment_Building` (`building_id`),
  CONSTRAINT `FK_Apartment_Building` FOREIGN KEY (`building_id`) REFERENCES `Building` (`ID`)
) ENGINE=InnoDB;

-- -----------------------------------------------------
-- Table: Residents
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `Residents` (
  `ID` char(10) NOT NULL,
  -- [FIX CỨNG] Loại cư dân
  `type` ENUM('OWNER', 'TENANT', 'GUEST') NOT NULL,
  `dob` date DEFAULT NULL,
  `gender` ENUM('M', 'F') NOT NULL,
  `id_number` char(12) NOT NULL,
  `phone` varchar(20) NOT NULL,
  `email` varchar(255) DEFAULT NULL,
  `status` ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',
  `apartment_id` char(4) NOT NULL,
  `user_id` char(10) NOT NULL,
  `temp_residence` varchar(255) DEFAULT NULL,
  `temp_absence` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `FK_Residents_Apartment` (`apartment_id`),
  UNIQUE KEY `UQ_Residents_User` (`user_id`),
  CONSTRAINT `FK_Residents_Apartment` FOREIGN KEY (`apartment_id`) REFERENCES `Apartment` (`ID`),
  CONSTRAINT `FK_Residents_User` FOREIGN KEY (`user_id`) REFERENCES `User` (`ID`)
) ENGINE=InnoDB;

-- -----------------------------------------------------
-- Table: access_cards
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
  KEY `FK_AccessCards_Resident` (`resident_id`),
  KEY `FK_AccessCards_Issuer` (`issued_by`),
  CONSTRAINT `FK_AccessCards_Resident` FOREIGN KEY (`resident_id`) REFERENCES `Residents` (`ID`),
  CONSTRAINT `FK_AccessCards_Issuer` FOREIGN KEY (`issued_by`) REFERENCES `User` (`ID`)
) ENGINE=InnoDB;

-- -----------------------------------------------------
-- Table: Vehicle
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `Vehicle` (
  `ID` char(10) NOT NULL,
  `type` varchar(100) NOT NULL, -- Loại xe (Xe máy, Oto) có thể để varchar hoặc Enum tùy ý
  `license_plate` varchar(100) DEFAULT NULL,
  `registered_at` timestamp NULL DEFAULT NULL,
  `revoked_at` timestamp NULL DEFAULT NULL,
  `status` ENUM('ACTIVE', 'INACTIVE') DEFAULT 'ACTIVE',
  `resident_id` char(10) NOT NULL,
  PRIMARY KEY (`ID`),
  KEY `FK_Vehicle_Resident` (`resident_id`),
  CONSTRAINT `FK_Vehicle_Resident` FOREIGN KEY (`resident_id`) REFERENCES `Residents` (`ID`)
) ENGINE=InnoDB;

-- -----------------------------------------------------
-- Table: Unit_Price
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `Unit_Price` (
  `ID` char(10) NOT NULL,
  `name` varchar(255) NOT NULL,
  `type` ENUM('ELECTRIC', 'WATER', 'SERVICE', 'PARKING', 'PENALTY') NOT NULL,
  `amount` float NOT NULL,
  `update_by` char(10) NOT NULL,
  `update_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`ID`),
  KEY `FK_UnitPrice_Updater` (`update_by`),
  CONSTRAINT `FK_UnitPrice_Updater` FOREIGN KEY (`update_by`) REFERENCES `User` (`ID`)
) ENGINE=InnoDB;

-- -----------------------------------------------------
-- Table: Fees
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
  `create_by` char(10) NOT NULL,
  PRIMARY KEY (`ID`),
  KEY `FK_Fees_Apartment` (`apartment_id`),
  KEY `FK_Fees_Creator` (`create_by`),
  CONSTRAINT `FK_Fees_Apartment` FOREIGN KEY (`apartment_id`) REFERENCES `Apartment` (`ID`),
  CONSTRAINT `FK_Fees_Creator` FOREIGN KEY (`create_by`) REFERENCES `User` (`ID`)
) ENGINE=InnoDB;

-- -----------------------------------------------------
-- Table: Invoice
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `Invoice` (
  `ID` char(10) NOT NULL,
  `apartment_id` char(4) NOT NULL,
  `fee_id` char(10) NOT NULL,
  `period_from` date NOT NULL,
  `period_to` date NOT NULL,
  `amount` float NOT NULL,
  `issued_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `due_date` date NOT NULL,
  `status` ENUM('UNPAID', 'PAID', 'OVERDUE') NOT NULL DEFAULT 'UNPAID',
  `paid_at` timestamp NULL DEFAULT NULL,
  `create_by` char(10) NOT NULL,
  PRIMARY KEY (`ID`),
  KEY `FK_Invoice_Apartment` (`apartment_id`),
  KEY `FK_Invoice_Fee` (`fee_id`),
  KEY `FK_Invoice_Creator` (`create_by`),
  CONSTRAINT `FK_Invoice_Apartment` FOREIGN KEY (`apartment_id`) REFERENCES `Apartment` (`ID`),
  CONSTRAINT `FK_Invoice_Fee` FOREIGN KEY (`fee_id`) REFERENCES `Fees` (`ID`),
  CONSTRAINT `FK_Invoice_Creator` FOREIGN KEY (`create_by`) REFERENCES `User` (`ID`)
) ENGINE=InnoDB;

-- -----------------------------------------------------
-- Table: Payments
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `Payments` (
  `ID` char(10) NOT NULL,
  `invoice_id` char(10) NOT NULL,
  `amount` float NOT NULL,
  `paid_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `method` ENUM('CASH', 'BANKING', 'MOMO', 'ZALOPAY') NOT NULL,
  `note` varchar(100) DEFAULT NULL,
  `paid_by` char(10) NOT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `UQ_Payments_Invoice` (`invoice_id`),
  KEY `FK_Payments_Payer` (`paid_by`),
  CONSTRAINT `FK_Payments_Invoice` FOREIGN KEY (`invoice_id`) REFERENCES `Invoice` (`ID`),
  CONSTRAINT `FK_Payments_Payer` FOREIGN KEY (`paid_by`) REFERENCES `User` (`ID`)
) ENGINE=InnoDB;

-- -----------------------------------------------------
-- Table: meter_reading
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
  `recorded_by` char(10) NOT NULL,
  `recorded_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`ID`),
  KEY `FK_Meter_Apartment` (`apartment_id`),
  KEY `FK_Meter_Recorder` (`recorded_by`),
  CONSTRAINT `FK_Meter_Apartment` FOREIGN KEY (`apartment_id`) REFERENCES `Apartment` (`ID`),
  CONSTRAINT `FK_Meter_Recorder` FOREIGN KEY (`recorded_by`) REFERENCES `User` (`ID`)
) ENGINE=InnoDB;

SET FOREIGN_KEY_CHECKS = 1;