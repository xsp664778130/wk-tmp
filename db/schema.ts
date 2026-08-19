import { index, integer, sqliteTable, text } from "drizzle-orm/sqlite-core";

export const skills = sqliteTable(
  "skills",
  {
    id: text("id").primaryKey(),
    ownerId: text("owner_id").notNull(),
    name: text("name").notNull(),
    description: text("description").notNull().default(""),
    category: text("category").notNull().default("效率工具"),
    fileName: text("file_name").notNull(),
    r2Key: text("r2_key").notNull(),
    note: text("note").notNull().default(""),
    size: integer("size").notNull().default(0),
    toolCompatibility: text("tool_compatibility").notNull().default("codex,qoder,openai"),
    createdAt: text("created_at").notNull(),
    updatedAt: text("updated_at").notNull(),
  },
  (table) => [index("skills_owner_created_idx").on(table.ownerId, table.createdAt)],
);

export const installs = sqliteTable(
  "installs",
  {
    id: text("id").primaryKey(),
    ownerId: text("owner_id").notNull(),
    skillId: text("skill_id").notNull(),
    targets: text("targets").notNull(),
    operatingSystem: text("operating_system").notNull(),
    createdAt: text("created_at").notNull(),
  },
  (table) => [index("installs_owner_created_idx").on(table.ownerId, table.createdAt)],
);
