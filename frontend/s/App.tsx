import { useEffect, useState } from "react";
import type { ReactNode } from "react";
import { Link, NavLink, Navigate, Route, Routes, useLocation } from "react-router-dom";
import { GuideOverlay } from "./components/GuideOverlay";
import { StatusBadge } from "./components/StatusBadge";
import { useWsBridge } from "./hooks/useWsBridge";
import { brandName, guideSteps, localeMeta, t } from "./lib/i18n";
import { CapabilitiesPage } from "./pages/CapabilitiesPage";
import { DevicesPage } from "./pages/DevicesPage";
import { LoginPage } from "./pages/LoginPage";
import { MissionsPage } from "./pages/MissionsPage";
import { SettingsPage } from "./pages/SettingsPage";
import { TopicsPage } from "./pages/TopicsPage";
import { UsersPage } from "./pages/UsersPage";
import { WorkbenchPage } from "./pages/WorkbenchPage";
import { useAppStore } from "./store/useAppStore";
import type { LocaleCode } from "./types";

function navItems(locale: LocaleCode) {
  return [
    { to: "/workbench", label: t(locale, { "zh-CN": "总览", "en-US": "Overview" }) },
    { to: "/workbench#mission", label: t(locale, { "zh-CN": "任务工作台", "en-US": "Mission Desk" }) },
    { to: "/topics", label: t(locale, { "zh-CN": "DDS 网络", "en-US": "DDS Network" }) },
    { to: "/devices", label: t(locale, { "zh-CN": "设备 Agent", "en-US": "Device Agents" }) },
    { to: "/workbench#conflict", label: t(locale, { "zh-CN": "冲突消解", "en-US": "Conflict" }) },
    { to: "/workbench#policy", label: t(locale, { "zh-CN": "策略评估", "en-US": "Policy Eval" }) },
    { to: "/workbench#logs", label: t(locale, { "zh-CN": "日志", "en-US": "Logs" }) }
  ];
}

function Layout({ children }: { children: ReactNode }) {
  const wsConnected = useAppStore((state) => state.wsConnected);
  const streamLogs = useAppStore((state) => state.streamLogs);
  const demoMode = useAppStore((state) => state.demoMode);
  const debugMode = useAppStore((state) => state.debugMode);
  const setDemoMode = useAppStore((state) => state.setDemoMode);
  const setDebugMode = useAppStore((state) => state.setDebugMode);
  const lastAction = useAppStore((state) => state.lastAction);
  const planResult = useAppStore((state) => state.planResult);
  const currentUser = useAppStore((state) => state.currentUser);
  const logout = useAppStore((state) => state.logout);
  const health = useAppStore((state) => state.health);
  const preferences = useAppStore((state) => state.preferences);
  const markGuideCompleted = useAppStore((state) => state.markGuideCompleted);
  const [logsOpen, setLogsOpen] = useState(false);
  const [guideOpen, setGuideOpen] = useState(!preferences.guideCompleted);
  const locale = preferences.locale;
  const nav = navItems(locale);
  const brand = brandName[locale];
  const apiHealthy = Boolean(health?.ok && health.mode !== "mock-fallback" && health.mode !== "mock-forced");
  const apiBadgeText =
    health?.mode === "mock-fallback" || health?.mode === "mock-forced"
      ? t(locale, { "zh-CN": "模拟", "en-US": "Mock" })
      : t(locale, { "zh-CN": "检查", "en-US": "Check" });

  const closeGuide = () => {
    setGuideOpen(false);
    markGuideCompleted();
  };

  return (
    <div className="appShell">
      <header className="topbar">
        <Link className="brandBlock" to="/workbench">
          <img className="brandLogo" src="/z-link-logo.png" alt="Z-Link logo" />
          <div>
            <strong>{brand.cn}</strong>
            <span>{brand.en} · {brand.tagline}</span>
          </div>
        </Link>

        <div className="topbarActions">
          <StatusBadge
            ok={wsConnected}
            okText="WS"
            badText={t(locale, { "zh-CN": "本地", "en-US": "Local" })}
          />
          <StatusBadge
            ok={apiHealthy}
            okText="API"
            badText={apiBadgeText}
          />
          <span className="softBadge">{planResult?.platformPlan?.ddsRuntimeMode ?? planResult?.ddsRuntimeMode ?? "SIMULATED_DDS"}</span>
          <span className="softBadge">{lastAction || t(locale, { "zh-CN": "待命", "en-US": "Standby" })}</span>
          <button className={debugMode ? "ghostButton active" : "ghostButton"} onClick={() => setDebugMode(!debugMode)}>
            {t(locale, { "zh-CN": "调试", "en-US": "Debug" })}
          </button>
          <button
            className={demoMode ? "ghostButton active" : "ghostButton"}
            onClick={() => {
              const nextDemoMode = !demoMode;
              setDemoMode(nextDemoMode);
              if (nextDemoMode) setDebugMode(false);
            }}
          >
            {t(locale, { "zh-CN": "演示", "en-US": "Demo" })}
          </button>
          <button className="ghostButton" onClick={() => setGuideOpen(true)}>
            {t(locale, { "zh-CN": "引导", "en-US": "Guide" })}
          </button>
          <button className="ghostButton" onClick={() => setLogsOpen((value) => !value)}>
            {t(locale, { "zh-CN": "日志", "en-US": "Logs" })} {streamLogs.length}
          </button>
          <button className="userBadgeLarge" onClick={() => void logout()}>
            <span>{currentUser?.displayName}</span>
            <small>{t(locale, { "zh-CN": "退出", "en-US": "Sign out" })}</small>
          </button>
        </div>
      </header>

      <div className="workspaceShell">
        <aside className="sidebar cardPanel">
          <div className="sidebarSection">
            <span className="eyebrow">{brand.en}</span>
            <h3>{t(locale, { "zh-CN": "导航", "en-US": "Navigation" })}</h3>
          </div>

          <div className="sidebarLinks">
            {nav.map((item) => (
              <NavLink key={item.to} to={item.to} className="sidebarLink">
                <span>{item.label}</span>
              </NavLink>
            ))}
          </div>

          <div className="sidebarMeta">
            <small>{t(locale, { "zh-CN": "状态", "en-US": "Status" })}</small>
            <strong>{lastAction || t(locale, { "zh-CN": "空闲", "en-US": "Idle" })}</strong>
          </div>

          <div className="sidebarMeta">
            <small>{t(locale, { "zh-CN": "语言", "en-US": "Language" })}</small>
            <strong>{localeMeta[locale].label}</strong>
          </div>
        </aside>

        <main className="pageShell">{children}</main>
      </div>

      <aside className={logsOpen ? "logDrawer cardPanel open" : "logDrawer cardPanel"}>
        <div className="panelHeader">
          <h3>{t(locale, { "zh-CN": "运行日志", "en-US": "Runtime Logs" })}</h3>
          <button className="ghostButton" onClick={() => setLogsOpen(false)}>
            {t(locale, { "zh-CN": "关闭", "en-US": "Close" })}
          </button>
        </div>
        <div className="logBox">
          {streamLogs.length
            ? streamLogs.map((line, index) => <div key={`${line}-${index}`}>{line}</div>)
            : t(locale, { "zh-CN": "暂无日志。", "en-US": "No logs." })}
        </div>
      </aside>

      <GuideOverlay open={guideOpen} steps={guideSteps[locale]} onClose={closeGuide} onComplete={closeGuide} />
    </div>
  );
}

function RequireAuth({ children }: { children: ReactNode }) {
  const currentUser = useAppStore((state) => state.currentUser);
  const location = useLocation();

  if (!currentUser) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  return <Layout>{children}</Layout>;
}

export default function App() {
  const bootstrap = useAppStore((state) => state.bootstrap);
  const demoMode = useAppStore((state) => state.demoMode);
  const currentUser = useAppStore((state) => state.currentUser);
  const preferences = useAppStore((state) => state.preferences);
  const locale = preferences.locale;

  useEffect(() => {
    document.documentElement.dataset.theme = preferences.themeMode;
    document.documentElement.style.colorScheme = preferences.themeMode;
    document.body.dataset.theme = preferences.themeMode;
  }, [preferences.themeMode]);

  useEffect(() => {
    if (!currentUser) return;
    void bootstrap();
  }, [bootstrap, currentUser]);

  useWsBridge();

  useEffect(() => {
    if (!demoMode || !currentUser || !preferences.animationEnabled) return;

    const timer = window.setInterval(() => {
      useAppStore.setState((state) => ({
        streamLogs: [
          ...state.streamLogs,
          `[${new Date().toLocaleTimeString()}] ${t(locale, {
            "zh-CN": "状态已刷新",
            "en-US": "State refreshed"
          })}`
        ].slice(-240)
      }));
    }, 6800);

    return () => window.clearInterval(timer);
  }, [demoMode, currentUser, locale, preferences.animationEnabled]);

  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/" element={<Navigate to="/workbench" replace />} />
      <Route path="/workbench" element={<RequireAuth><WorkbenchPage /></RequireAuth>} />
      <Route path="/missions" element={<RequireAuth><MissionsPage /></RequireAuth>} />
      <Route path="/topics" element={<RequireAuth><TopicsPage /></RequireAuth>} />
      <Route path="/devices" element={<RequireAuth><DevicesPage /></RequireAuth>} />
      <Route path="/capabilities" element={<RequireAuth><CapabilitiesPage /></RequireAuth>} />
      <Route path="/users" element={<RequireAuth><UsersPage /></RequireAuth>} />
      <Route path="/settings" element={<RequireAuth><SettingsPage /></RequireAuth>} />
    </Routes>
  );
}
