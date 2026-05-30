-- phpMyAdmin SQL Dump
-- version 5.2.1
-- Host: 127.0.0.1
-- Server version: 10.4.32-MariaDB

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

-- Database: `auction_house`

-- --------------------------------------------------------

CREATE TABLE `items` (
                         `id`                varchar(20)  NOT NULL,
                         `owner_id`          varchar(20)  DEFAULT NULL,
                         `name`              varchar(100) NOT NULL,
                         `current_price`     double       DEFAULT 0,
                         `state`             varchar(20)  DEFAULT 'PENDING',
                         `type`              varchar(30)  DEFAULT NULL,
                         `description`       text         DEFAULT NULL,
                         `starting_price`    double       DEFAULT 0,
                         `start_time`        datetime     DEFAULT NULL,
                         `end_time`          datetime     DEFAULT NULL,
                         `highest_bidder_id` varchar(20)  DEFAULT NULL,
                         `image_urls`        text         DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Dữ liệu mẫu khớp với tất cả cột mới
INSERT INTO `items`
(`id`, `owner_id`, `name`, `current_price`, `state`, `type`,
 `description`, `starting_price`, `start_time`, `end_time`,
 `highest_bidder_id`, `image_urls`)
VALUES
    ('IT01', '25020335', 'Laptop Dell', 1500, 'OPEN', 'ELECTRONICS',
     'Laptop Dell XPS 15 inch', 1500,
     '2026-05-01 08:00:00', '2026-12-31 23:59:59',
     NULL, NULL);

-- --------------------------------------------------------
-- Bảng users — thêm cột email_address mà checkLogin() đọc
-- --------------------------------------------------------

CREATE TABLE `users` (
                         `id`            varchar(20)  NOT NULL,
                         `username`      varchar(50)  NOT NULL,
                         `password`      varchar(50)  NOT NULL,
                         `full_name`     varchar(100) DEFAULT NULL,
                         `role`          varchar(20)  DEFAULT 'MEMBER',
                         `email_address` varchar(100) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Dữ liệu mẫu — giữ nguyên user cũ, thêm email
INSERT INTO `users` (`id`, `username`, `password`, `full_name`, `role`, `email_address`)
VALUES
    ('25020335', 'quan_uet',  '123456', 'Nguyen Van Quan', 'MEMBER', 'quan@uet.vnu.edu.vn'),
    ('25020667', 'thanhdo',   '67',     'Phung Thanh Do',  'MEMBER', 'thanh@uet.vnu.edu.vn');

-- --------------------------------------------------------
-- Indexes
-- --------------------------------------------------------

ALTER TABLE `items`
    ADD PRIMARY KEY (`id`);

ALTER TABLE `users`
    ADD PRIMARY KEY (`id`),
    ADD UNIQUE KEY `username` (`username`);

COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;