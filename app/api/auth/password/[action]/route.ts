import { proxyAuthJson } from "../../_backend";

export async function POST(request: Request, context: { params: Promise<{ action: string }> }) {
  const { action } = await context.params;
  if (action !== "reset-code" && action !== "reset") {
    return Response.json({ error: "不支持的操作。" }, { status: 405 });
  }
  return proxyAuthJson(request, `/api/v1/auth/password/${action}`, false);
}
