import type { Metadata } from "next";
import { chatGPTSignInPath, getChatGPTUser } from "./chatgpt-auth";
import { SkillWorkspace } from "./skill-workspace";

export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "SkillPort — AI Skill 管理工作台",
  description: "集中发现、管理和分发你的 AI Skills。",
};

export default async function Home() {
  const user = await getChatGPTUser();

  return (
    <SkillWorkspace
      user={user ? { name: user.displayName, email: user.email } : null}
      signInHref={chatGPTSignInPath("/")}
    />
  );
}
