import { and, eq } from "drizzle-orm";
import { env } from "cloudflare:workers";
import { getDb } from "../../../../../db";
import { skills } from "../../../../../db/schema";
import { getChatGPTUser } from "../../../../chatgpt-auth";

interface StoredObject {
  body: ReadableStream;
  httpMetadata?: { contentType?: string };
}

interface SkillFileBucket {
  get(key: string): Promise<StoredObject | null>;
}

export async function GET(_request: Request, context: { params: Promise<{ id: string }> }) {
  const user = await getChatGPTUser();
  if (!user) return Response.json({ error: "请先登录。" }, { status: 401 });
  const { id } = await context.params;
  const [skill] = await getDb().select().from(skills).where(and(eq(skills.id, id), eq(skills.ownerId, user.userId))).limit(1);
  if (!skill) return Response.json({ error: "未找到该 Skill。" }, { status: 404 });

  const runtime = env as unknown as { SKILL_FILES: SkillFileBucket };
  const object = await runtime.SKILL_FILES.get(skill.r2Key);
  if (!object) return Response.json({ error: "Skill 文件不存在。" }, { status: 404 });
  const extension = skill.fileName.split(".").pop()?.toLowerCase() || "zip";

  return new Response(object.body, {
    headers: {
      "content-type": object.httpMetadata?.contentType || "application/octet-stream",
      "content-disposition": `attachment; filename="${encodeURIComponent(skill.fileName)}"`,
      "x-skill-extension": extension === "zip" ? "zip" : "md",
      "cache-control": "private, no-store",
    },
  });
}
