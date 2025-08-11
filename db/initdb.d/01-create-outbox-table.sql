-- Create outboxevent table for outbox pattern
CREATE TABLE IF NOT EXISTS `msa_pay`.`outboxevent` (
  `id` char(36) NOT NULL,
  `timestamp` datetime(6) NOT NULL,
  `aggregateid` varchar(255) NOT NULL,
  `aggregatetype` varchar(255) NOT NULL,
  `type` varchar(255) NOT NULL,
  `payload` text NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create index for better performance
CREATE INDEX IF NOT EXISTS `idx_outbox_timestamp` ON `msa_pay`.`outboxevent` (`timestamp`);
CREATE INDEX IF NOT EXISTS `idx_outbox_aggregate` ON `msa_pay`.`outboxevent` (`aggregateid`, `aggregatetype`);
