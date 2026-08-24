import { cookies } from "next/headers";

export const SESSION_COOKIE_NAME = "skillport_session";

export type SkillPortUser = {
  id: string;
  email: string;
  displayName: string;
};

export async function getSkillPortUser(): Promise<SkillPortUser | null> {
  const token = (await cookies()).get(SESSION_COOKIE_NAME)?.value;
  const baseUrl = process.env.SKILLPORT_BACKEND_URL?.replace(/\/$/, "");
  const gatewayKey = process.env.SKILLPORT_GATEWAY_KEY;
  if (!token || !baseUrl || !gatewayKey) return null;

  try {
    const response = await fetch(`${baseUrl}/api/v1/auth/me`, {
      cache: "no-store",
      headers: {
        Authorization: `Bearer ${token}`,
        "X-SkillPort-Gateway-Key": gatewayKey,
      },
    });
    if (!response.ok) return null;
    return (await response.json()) as SkillPortUser;
  } catch {
    return null;
  }
}

export function sessionTokenFromRequest(request: Request): string | null {
  const cookieHeader = request.headers.get("cookie");
  if (!cookieHeader) return null;
  for (const part of cookieHeader.split(";")) {
    const separator = part.indexOf("=");
    if (separator < 0) continue;
    const name = part.slice(0, separator).trim();
    if (name !== SESSION_COOKIE_NAME) continue;
    const value = part.slice(separator + 1).trim();
    return value ? decodeURIComponent(value) : null;
  }
  return null;
}
