import { publicBackendRequest } from "../_public-backend";

export async function GET() {
  return publicBackendRequest("/bridge/skillport-bridge.jar", {
    headers: { accept: "application/java-archive" },
  });
}
