import { and, desc, eq } from "drizzle-orm";
import { env } from "cloudflare:workers";
import { getDb } from "../../../db";
import { skills } from "../../../db/schema";
import { getChatGPTUser } from "../../chatgpt-auth";

interface SkillFileBucket {
  put(
    key: string,
    value: ArrayBuffer,
    options?: { httpMetadata?: { contentType?: string } },
  ): Promise<unknown>;
}

function unauthorized() {
  return Response.json({ error: "请先登录后再管理个人 Skill。" }, { status: 401 });
}

export async function GET() {
  const user = await getChatGPTUser();
  if (!user) return unauthorized();

  const rows = await getDb()
    .select()
    .from(skills)
    .where(eq(skills.ownerId, user.userId))
    .orderBy(desc(skills.createdAt));

  return Response.json({ skills: rows });
}

export async function POST(request: Request) {
  const user = await getChatGPTUser();
  if (!user) return unauthorized();

  const form = await request.formData();
  const file = form.get("file");
  if (!(file instanceof File)) {
    return Response.json({ error: "请选择一个 Skill 文件。" }, { status: 400 });
  }
  if (file.size > 25 * 1024 * 1024) {
    return Response.json({ error: "单个文件不能超过 25MB。" }, { status: 413 });
  }

  const id = crypto.randomUUID();
  const now = new Date().toISOString();
  const category = String(form.get("category") || "效率工具");
  const name = String(form.get("name") || file.name.replace(/\.(zip|skill|md)$/i, ""));
  const r2Key = `users/${user.userId}/skills/${id}/${file.name}`;
  const runtime = env as unknown as { SKILL_FILES: SkillFileBucket };

  await runtime.SKILL_FILES.put(r2Key, await file.arrayBuffer(), {
    httpMetadata: { contentType: file.type || "application/octet-stream" },
  });

  const [created] = await getDb()
    .insert(skills)
    .values({
      id,
      ownerId: user.userId,
      name,
      description: String(form.get("description") || "我上传的自定义 Skill"),
      category,
      fileName: file.name,
      r2Key,
      size: file.size,
      createdAt: now,
      updatedAt: now,
    })
    .returning();

  return Response.json({ skill: created }, { status: 201 });
}

export async function PATCH(request: Request) {
  const user = await getChatGPTUser();
  if (!user) return unauthorized();
  const body = (await request.json()) as { id?: string; note?: string };
  if (!body.id) return Response.json({ error: "缺少 Skill ID。" }, { status: 400 });

  const [updated] = await getDb()
    .update(skills)
    .set({ note: String(body.note || "").slice(0, 1000), updatedAt: new Date().toISOString() })
    .where(and(eq(skills.id, body.id), eq(skills.ownerId, user.userId)))
    .returning();

  if (!updated) return Response.json({ error: "未找到该 Skill。" }, { status: 404 });
  return Response.json({ skill: updated });
}
