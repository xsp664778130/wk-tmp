import type { Metadata } from "next";
import { getSkillPortUser } from "./session-auth";
import { SkillWorkspace } from "./skill-workspace";

export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "SkillPort — AI Skill 管理工作台",
  description: "集中发现、管理和分发你的 AI Skills。",
};

export default async function Home() {
  const user = await getSkillPortUser();

  return (
    <SkillWorkspace initialUser={user ? { id: user.id, name: user.displayName, email: user.email } : null} />
  );
}
