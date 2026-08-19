CREATE TABLE `installs` (
	`id` text PRIMARY KEY NOT NULL,
	`owner_id` text NOT NULL,
	`skill_id` text NOT NULL,
	`targets` text NOT NULL,
	`operating_system` text NOT NULL,
	`created_at` text NOT NULL
);
--> statement-breakpoint
CREATE INDEX `installs_owner_created_idx` ON `installs` (`owner_id`,`created_at`);--> statement-breakpoint
CREATE TABLE `skills` (
	`id` text PRIMARY KEY NOT NULL,
	`owner_id` text NOT NULL,
	`name` text NOT NULL,
	`description` text DEFAULT '' NOT NULL,
	`category` text DEFAULT '效率工具' NOT NULL,
	`file_name` text NOT NULL,
	`r2_key` text NOT NULL,
	`note` text DEFAULT '' NOT NULL,
	`size` integer DEFAULT 0 NOT NULL,
	`tool_compatibility` text DEFAULT 'codex,qoder,openai' NOT NULL,
	`created_at` text NOT NULL,
	`updated_at` text NOT NULL
);
--> statement-breakpoint
CREATE INDEX `skills_owner_created_idx` ON `skills` (`owner_id`,`created_at`);