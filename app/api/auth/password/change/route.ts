import { expiredSessionCookie, proxyAuthJson } from "../../_backend";

export async function POST(request: Request) {
  const response = await proxyAuthJson(request, "/api/v1/auth/password/change", true);
  if (!response.ok) return response;
  const headers = new Headers(response.headers);
  headers.append("set-cookie", expiredSessionCookie(request));
  return new Response(response.body, { status: response.status, headers });
}
