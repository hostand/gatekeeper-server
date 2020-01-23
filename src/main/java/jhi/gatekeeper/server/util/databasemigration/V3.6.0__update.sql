ALTER TABLE `access_requests`
ADD COLUMN `needs_approval` tinyint(1) NOT NULL DEFAULT 1 AFTER `has_been_rejected`;

ALTER TABLE `unapproved_users`
ADD COLUMN `needs_approval` tinyint(1) NOT NULL DEFAULT 1 AFTER `has_been_rejected`;