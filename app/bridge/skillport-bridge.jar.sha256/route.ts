import { publicBackendRequest } from "../_public-backend";

export async function GET() {
  return publicBackendRequest("/bridge/skillport-bridge.jar.sha256", {
    headers: { accept: "text/plain" },
  });
}
