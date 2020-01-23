/*
 * Copyright $today.year Information and Computational Sciences,
 * The James Hutton Institute.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for access_requests
-- ----------------------------
CREATE TABLE IF NOT EXISTS `access_requests` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Primary id for this table. This uniquely identifies the row.',
  `user_id` int(11) NOT NULL COMMENT 'Foreign key to users (users.id).',
  `database_system_id` int(11) NOT NULL COMMENT 'Foreign key to database_systems (database_systems.id).',
  `has_been_rejected` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'Indicates if the access request has been rejected.',
  `activation_key` varchar(36) DEFAULT NULL COMMENT 'The activation key used during the automated registration process.',
  `created_on` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'When the record was created.',
  PRIMARY KEY (`id`),
  KEY `access_requests_ibfk1` (`user_id`) USING BTREE,
  KEY `access_requests_ibfk2` (`database_system_id`) USING BTREE,
  CONSTRAINT `access_requests_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `access_requests_ibfk_2` FOREIGN KEY (`database_system_id`) REFERENCES `database_systems` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COMMENT='This table contains user access requests, i.e. existing users requesting access to another instance of Germinate.';

-- ----------------------------
-- Records of access_requests
-- ----------------------------

-- ----------------------------
-- Table structure for database_systems
-- ----------------------------
CREATE TABLE IF NOT EXISTS `database_systems` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Primary id for this table. This uniquely identifies the row.',
  `system_name` varchar(64) NOT NULL COMMENT 'The name of the database.',
  `server_name` varchar(64) NOT NULL COMMENT 'The name of the database server.',
  `description` varchar(255) DEFAULT NULL COMMENT 'Describes the database system in more detail.',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=latin1 COMMENT='This table contains all databases for which Gatekeeper manages the users. Each entry represents an individual database.';

-- ----------------------------
-- Records of database_systems
-- ----------------------------
INSERT INTO `database_systems` VALUES ('-1', 'gatekeeper', '--', 'Gatekeeper Database');

-- ----------------------------
-- Table structure for institutions
-- ----------------------------
CREATE TABLE IF NOT EXISTS `institutions` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Primary id for this table. This uniquely identifies the row.',
  `name` varchar(128) NOT NULL COMMENT 'The name of the institution.',
  `acronym` varchar(45) NOT NULL COMMENT 'The acronym of the institution if available, an abbreviation otherwise.',
  `address` varchar(255) DEFAULT NULL COMMENT 'The address of the institution.',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COMMENT='This table holds all the institutions associated with registered users.';

-- ----------------------------
-- Records of institutions
-- ----------------------------

-- ----------------------------
-- Table structure for password_reset_log
-- ----------------------------
CREATE TABLE IF NOT EXISTS `password_reset_log` (
  `user_id` int(11) NOT NULL COMMENT 'Primary id for this table. This uniquely identifies the row.',
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'The timestamp at which the new password has been requested.',
  `ip_address` varchar(40) NOT NULL COMMENT 'The IP address of the user requesting a new password.',
  PRIMARY KEY (`user_id`,`timestamp`),
  KEY `user_id_users_user_id_idx` (`user_id`) USING BTREE,
  CONSTRAINT `password_reset_log_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COMMENT='In this table we keep track of user password resets.';

-- ----------------------------
-- Records of password_reset_log
-- ----------------------------

-- ----------------------------
-- Table structure for unapproved_users
-- ----------------------------
CREATE TABLE IF NOT EXISTS `unapproved_users` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Primary id for this table. This uniquely identifies the row.',
  `user_username` varchar(45) NOT NULL COMMENT 'The username of this user. This is used to log in to Gatekeeper and Germinate.',
  `user_password` varchar(128) NOT NULL COMMENT 'The password of this user. This is used to log in to Gatekeeper and Germinate.',
  `user_full_name` varchar(128) NOT NULL COMMENT 'The full name of this user, i.e. the first name, any middle names, and surname of the user.',
  `user_email_address` varchar(128) NOT NULL COMMENT 'The email address of the user. This is used to contact the user, e.g. to send them a new password.',
  `institution_id` int(11) DEFAULT NULL COMMENT 'Foreign key to institutions (institutions.id).',
  `institution_name` varchar(128) DEFAULT NULL COMMENT 'The name of the institution the user is associated with. If this institution already exists, this will be empty and the ''institution_id'' will point to the associated institution instead.',
  `institution_acronym` varchar(45) DEFAULT NULL COMMENT 'The acronym of the institution the user is associated with. If this institution already exists, this will be empty and the ''institution_id'' will point to the associated institution instead.',
  `institution_address` varchar(255) DEFAULT NULL COMMENT 'The address of the institution the user is associated with. If this institution already exists, this will be empty and the ''institution_id'' will point to the associated institution instead.',
  `database_system_id` int(11) NOT NULL COMMENT 'Foreign key to database_systems (database_systems.id). This describes which database the user is requesting access to.',
  `created_on` timestamp NULL DEFAULT NULL COMMENT 'When the record was created.',
  `has_been_rejected` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'Indicates if the user request has been rejected by an administrator.',
  `activation_key` varchar(36) DEFAULT NULL COMMENT 'The activation key used during the automated registration process.',
  PRIMARY KEY (`id`),
  KEY `unapproved_users_ibfk_1` (`institution_id`) USING BTREE,
  KEY `unapproved_users_ibfk_2` (`database_system_id`) USING BTREE,
  CONSTRAINT `unapproved_users_ibfk_1` FOREIGN KEY (`institution_id`) REFERENCES `institutions` (`id`) ON DELETE NO ACTION ON UPDATE CASCADE,
  CONSTRAINT `unapproved_users_ibfk_2` FOREIGN KEY (`database_system_id`) REFERENCES `database_systems` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COMMENT='This table contains user access requests, i.e. new user registrations. All the information about the user as well as the associated institution is held here as well as the database system the user is requesting access to.';

-- ----------------------------
-- Records of unapproved_users
-- ----------------------------

-- ----------------------------
-- Table structure for user_has_access_to_databases
-- ----------------------------
CREATE TABLE IF NOT EXISTS `user_has_access_to_databases` (
  `user_id` int(11) NOT NULL COMMENT 'Foreign key to users (users.id).',
  `database_id` int(11) NOT NULL COMMENT 'Foreign key to database_systems (database_systems.id).',
  `user_type_id` int(11) NOT NULL COMMENT 'Foreign key to user_types (user_types.id).',
  PRIMARY KEY (`user_id`,`database_id`),
  KEY `fk_users_has_databases_databases1_idx` (`database_id`) USING BTREE,
  KEY `fk_users_has_databases_users1_idx` (`user_id`) USING BTREE,
  KEY `fk_users_has_access_to_databases_user_types1_idx` (`user_type_id`) USING BTREE,
  CONSTRAINT `user_has_access_to_databases_ibfk_1` FOREIGN KEY (`user_type_id`) REFERENCES `user_types` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `user_has_access_to_databases_ibfk_2` FOREIGN KEY (`database_id`) REFERENCES `database_systems` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
  CONSTRAINT `user_has_access_to_databases_ibfk_3` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COMMENT='This table determines which user can access which instance of Germinate. A user can have access to multiple instances of Germinate without the need to have separate user accounts. The user type determines the level of access this user has to the given instance of Germinate.';

-- ----------------------------
-- Records of user_has_access_to_databases
-- ----------------------------

-- ----------------------------
-- Table structure for user_types
-- ----------------------------
CREATE TABLE IF NOT EXISTS `user_types` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Primary id for this table. This uniquely identifies the row.',
  `description` varchar(64) NOT NULL COMMENT 'Describes the user type.',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=latin1 COMMENT='User types are the roles a user can have. Gatekeeper currently supports ''Administrator'', ''Regular User'' and ''Suspended User''.';

-- ----------------------------
-- Records of user_types
-- ----------------------------
INSERT INTO `user_types` VALUES ('1', 'Administrator');
INSERT INTO `user_types` VALUES ('2', 'Regular User');
INSERT INTO `user_types` VALUES ('3', 'Suspended User');

-- ----------------------------
-- Table structure for users
-- ----------------------------
CREATE TABLE IF NOT EXISTS `users` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Primary id for this table. This uniquely identifies the row.',
  `username` varchar(45) NOT NULL COMMENT 'The username of this user. This is used to log in to Gatekeeper and Germinate.',
  `password` varchar(128) NOT NULL COMMENT 'The password of this user. This is used to log in to Gatekeeper and Germinate.',
  `full_name` varchar(128) NOT NULL COMMENT 'The full name of this user, i.e. the first name, any middle names, and surname of the user.',
  `email_address` varchar(128) NOT NULL COMMENT 'The email address of the user. This is used to contact the user, e.g. to send them a new password.',
  `created_on` timestamp NULL DEFAULT NULL COMMENT 'When the record was created.',
  `institution_id` int(11) DEFAULT NULL COMMENT 'Foreign key to institutions (institutions.id).',
  `has_access_to_gatekeeper` tinyint(1) DEFAULT '1' COMMENT 'Indicates if this user has access to Gatekeeper. If set to 1, the user can log in to Gatekeeper and change email address and password. If set to 0 the user cannot log in to Gatekeeper, but the user account can still be used to log in to Germinate.',
  PRIMARY KEY (`id`),
  KEY `fk_users_institutions1_idx` (`institution_id`) USING BTREE,
  CONSTRAINT `users_ibfk_1` FOREIGN KEY (`institution_id`) REFERENCES `institutions` (`id`) ON DELETE SET NULL ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COMMENT='The ''users'' table contains all the users that have registered with Gatekeeper through Germinate.';

-- ----------------------------
-- Records of users
-- ----------------------------
SET FOREIGN_KEY_CHECKS=1;
