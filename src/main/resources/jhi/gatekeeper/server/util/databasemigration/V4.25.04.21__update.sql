ALTER TABLE `user_has_access_to_databases`
    ADD COLUMN `primary_contact` tinyint(1) NOT NULL DEFAULT 0 AFTER `user_type_id`;