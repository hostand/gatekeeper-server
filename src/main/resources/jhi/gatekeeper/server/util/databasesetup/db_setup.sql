SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for access_requests
-- ----------------------------
DROP TABLE IF EXISTS `access_requests`;
CREATE TABLE `access_requests`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `database_system_id` int(11) NOT NULL,
  `has_been_rejected` tinyint(1) NOT NULL DEFAULT 0,
  `needs_approval` tinyint(1) NOT NULL DEFAULT 1,
  `activation_key` varchar(36) CHARACTER SET latin1 COLLATE latin1_swedish_ci NULL DEFAULT NULL,
  `created_on` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `access_requests_ibfk1`(`user_id`) USING BTREE,
  INDEX `access_requests_ibfk2`(`database_system_id`) USING BTREE,
  CONSTRAINT `access_requests_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `access_requests_ibfk_2` FOREIGN KEY (`database_system_id`) REFERENCES `database_systems` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of access_requests
-- ----------------------------

-- ----------------------------
-- Table structure for database_systems
-- ----------------------------
DROP TABLE IF EXISTS `database_systems`;
CREATE TABLE `database_systems`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `system_name` varchar(64) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL,
  `server_name` varchar(64) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL,
  `description` varchar(255) CHARACTER SET latin1 COLLATE latin1_swedish_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of database_systems
-- ----------------------------
INSERT INTO `database_systems` VALUES (-1, 'gatekeeper', '--', 'Gatekeeper Database');

-- ----------------------------
-- Table structure for institutions
-- ----------------------------
DROP TABLE IF EXISTS `institutions`;
CREATE TABLE `institutions`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(128) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL,
  `acronym` varchar(45) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL,
  `address` varchar(255) CHARACTER SET latin1 COLLATE latin1_swedish_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of institutions
-- ----------------------------

-- ----------------------------
-- Table structure for password_reset_log
-- ----------------------------
DROP TABLE IF EXISTS `password_reset_log`;
CREATE TABLE `password_reset_log`  (
  `user_id` int(11) NOT NULL,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `ip_address` varchar(40) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL,
  PRIMARY KEY (`user_id`, `timestamp`) USING BTREE,
  INDEX `user_id_users_user_id_idx`(`user_id`) USING BTREE,
  CONSTRAINT `password_reset_log_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'Log table of password reset requests.' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of password_reset_log
-- ----------------------------

-- ----------------------------
-- Table structure for schema_version
-- ----------------------------
DROP TABLE IF EXISTS `schema_version`;
CREATE TABLE `schema_version`  (
  `installed_rank` int(11) NOT NULL,
  `version` varchar(50) CHARACTER SET latin1 COLLATE latin1_swedish_ci NULL DEFAULT NULL,
  `description` varchar(200) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL,
  `type` varchar(20) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL,
  `script` varchar(1000) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL,
  `checksum` int(11) NULL DEFAULT NULL,
  `installed_by` varchar(100) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `execution_time` int(11) NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`) USING BTREE,
  INDEX `schema_version_s_idx`(`success`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = latin1 COLLATE = latin1_swedish_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of schema_version
-- ----------------------------
INSERT INTO `schema_version` VALUES (1, '1', '<< Flyway Baseline >>', 'BASELINE', '<< Flyway Baseline >>', NULL, 'germinate3', '2018-12-11 10:49:35', 0, 1);
INSERT INTO `schema_version` VALUES (2, '3.6.0', 'update', 'SQL', 'V3.6.0__update.sql', 1602255684, 'germinate3', '2018-12-11 10:49:35', 20, 1);
INSERT INTO `schema_version` VALUES (3, '4.0.0', 'update', 'SQL', 'V4.0.0__update.sql', -1057957509, 'root', '2020-02-27 17:54:31', 22, 1);
INSERT INTO `schema_version` VALUES (4, '4.21.08.31', 'update', 'SQL', 'V4.21.08.31__update.sql', -2056903133, 'root', '2021-08-31 11:37:41', 22, 1);

-- ----------------------------
-- Table structure for unapproved_users
-- ----------------------------
DROP TABLE IF EXISTS `unapproved_users`;
CREATE TABLE `unapproved_users`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_username` varchar(45) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL,
  `user_password` varchar(128) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL,
  `user_full_name` varchar(128) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL,
  `user_email_address` varchar(128) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL,
  `institution_id` int(11) NULL DEFAULT NULL,
  `institution_name` varchar(128) CHARACTER SET latin1 COLLATE latin1_swedish_ci NULL DEFAULT NULL,
  `institution_acronym` varchar(45) CHARACTER SET latin1 COLLATE latin1_swedish_ci NULL DEFAULT NULL,
  `institution_address` varchar(255) CHARACTER SET latin1 COLLATE latin1_swedish_ci NULL DEFAULT NULL,
  `database_system_id` int(11) NOT NULL,
  `created_on` timestamp NULL DEFAULT NULL,
  `has_been_rejected` tinyint(1) NOT NULL DEFAULT 0,
  `needs_approval` tinyint(1) NOT NULL DEFAULT 1,
  `activation_key` varchar(36) CHARACTER SET latin1 COLLATE latin1_swedish_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `unapproved_users_ibfk_1`(`institution_id`) USING BTREE,
  INDEX `unapproved_users_ibfk_2`(`database_system_id`) USING BTREE,
  CONSTRAINT `unapproved_users_ibfk_1` FOREIGN KEY (`institution_id`) REFERENCES `institutions` (`id`) ON DELETE NO ACTION ON UPDATE CASCADE,
  CONSTRAINT `unapproved_users_ibfk_2` FOREIGN KEY (`database_system_id`) REFERENCES `database_systems` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of unapproved_users
-- ----------------------------

-- ----------------------------
-- Table structure for user_has_access_to_databases
-- ----------------------------
DROP TABLE IF EXISTS `user_has_access_to_databases`;
CREATE TABLE `user_has_access_to_databases`  (
  `user_id` int(11) NOT NULL,
  `database_id` int(11) NOT NULL,
  `user_type_id` int(11) NOT NULL,
  PRIMARY KEY (`user_id`, `database_id`) USING BTREE,
  INDEX `fk_users_has_databases_databases1_idx`(`database_id`) USING BTREE,
  INDEX `fk_users_has_databases_users1_idx`(`user_id`) USING BTREE,
  INDEX `fk_users_has_access_to_databases_user_types1_idx`(`user_type_id`) USING BTREE,
  CONSTRAINT `user_has_access_to_databases_ibfk_1` FOREIGN KEY (`user_type_id`) REFERENCES `user_types` (`id`) ON DELETE NO ACTION ON UPDATE CASCADE,
  CONSTRAINT `user_has_access_to_databases_ibfk_2` FOREIGN KEY (`database_id`) REFERENCES `database_systems` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `user_has_access_to_databases_ibfk_3` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_has_access_to_databases
-- ----------------------------

-- ----------------------------
-- Table structure for user_types
-- ----------------------------
DROP TABLE IF EXISTS `user_types`;
CREATE TABLE `user_types`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `description` varchar(64) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_types
-- ----------------------------
INSERT INTO `user_types` VALUES (1, 'Administrator');
INSERT INTO `user_types` VALUES (2, 'Regular User');
INSERT INTO `user_types` VALUES (3, 'Suspended User');
INSERT INTO `user_types` VALUES (4, 'Data Curator');

-- ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `username` varchar(45) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL,
  `password` varchar(128) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL,
  `full_name` varchar(128) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL,
  `email_address` varchar(128) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL,
  `created_on` timestamp NULL DEFAULT NULL,
  `last_login` timestamp NULL DEFAULT NULL,
  `institution_id` int(11) NULL DEFAULT NULL,
  `has_access_to_gatekeeper` tinyint(1) NULL DEFAULT 1,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `fk_users_institutions1_idx`(`institution_id`) USING BTREE,
  CONSTRAINT `users_ibfk_1` FOREIGN KEY (`institution_id`) REFERENCES `institutions` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of users
-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;
