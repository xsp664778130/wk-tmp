import { getDb } from "../../../db";
import { installs } from "../../../db/schema";
import { getChatGPTUser } from "../../chatgpt-auth";

export async function POST(request: Request) {
  const user = await getChatGPTUser();
  if (!user) {
    return Response.json({ error: "请先登录后再发送到本机。" }, { status: 401 });
  }

  const body = (await request.json()) as {
    skillId?: string;
    targets?: string[];
    operatingSystem?: "macos" | "windows";
  };
  if (!body.skillId || !body.targets?.length || !body.operatingSystem) {
    return Response.json({ error: "安装参数不完整。" }, { status: 400 });
  }

  const createdAt = new Date().toISOString();
  await getDb().insert(installs).values({
    id: crypto.randomUUID(),
    ownerId: user.userId,
    skillId: body.skillId,
    targets: body.targets.join(","),
    operatingSystem: body.operatingSystem,
    createdAt,
  });

  return Response.json({ ok: true, createdAt });
}
