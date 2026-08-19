import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import { headers } from "next/headers";
import "./globals.css";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export async function generateMetadata(): Promise<Metadata> {
  const requestHeaders = await headers();
  const host = requestHeaders.get("x-forwarded-host") ?? requestHeaders.get("host") ?? "localhost:3000";
  const protocol = requestHeaders.get("x-forwarded-proto") ?? (host.startsWith("localhost") ? "http" : "https");
  const origin = `${protocol}://${host}`;

  return {
    metadataBase: new URL(origin),
    title: "SkillPort — AI Skill 管理工作台",
    description: "按分类发现、上传、备注并分发 Skills 到本机 AI 工具。",
    icons: { icon: "/og.png", shortcut: "/og.png" },
    openGraph: {
      title: "SkillPort — AI Skill 管理工作台",
      description: "管理、备注、加载你的 AI Skills。",
      images: [{ url: `${origin}/og.png`, width: 1200, height: 630, alt: "SkillPort AI Skill 管理工作台" }],
    },
    twitter: { card: "summary_large_image", title: "SkillPort", description: "管理、备注、加载你的 AI Skills。", images: [`${origin}/og.png`] },
  };
}

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="zh-CN">
      <body className={`${geistSans.variable} ${geistMono.variable}`}>
        {children}
      </body>
    </html>
  );
}
