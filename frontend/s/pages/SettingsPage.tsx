import { useState, useTransition } from "react";
import { localeMeta, t } from "../lib/i18n";
import { useAppStore } from "../store/useAppStore";
import type { LocaleCode, ThemeMode } from "../types";

export function SettingsPage() {
  const preferences = useAppStore((state) => state.preferences);
  const health = useAppStore((state) => state.health);
  const demoMode = useAppStore((state) => state.demoMode);
  const setDemoMode = useAppStore((state) => state.setDemoMode);
  const setLocale = useAppStore((state) => state.setLocale);
  const setThemeMode = useAppStore((state) => state.setThemeMode);
  const setAnimationEnabled = useAppStore((state) => state.setAnimationEnabled);
  const setAssistantName = useAppStore((state) => state.setAssistantName);
  const refreshHealth = useAppStore((state) => state.refreshHealth);
  const [assistantNameDraft, setAssistantNameDraft] = useState(preferences.assistantName);
  const [isPending, startTransition] = useTransition();
  const locale = preferences.locale;

  const hasAmapKey = Boolean(import.meta.env.VITE_AMAP_WEB_KEY?.trim());
  const serviceStatusLabel =
    health?.mode === "mock-fallback"
      ? t(locale, { "zh-CN": "模拟兜底", "en-US": "Mock fallback" })
      : health?.mode === "mock-forced"
        ? t(locale, { "zh-CN": "强制模拟", "en-US": "Forced mock" })
        : health?.ok
          ? t(locale, { "zh-CN": "正常", "en-US": "Healthy" })
          : t(locale, { "zh-CN": "待检查", "en-US": "Check needed" });

  return (
    <div className="stackPage">
      <section className="cardPanel">
        <div className="panelHeader">
          <div>
            <span className="eyebrow">Preferences</span>
            <h3>{t(locale, { "zh-CN": "系统设置", "en-US": "Settings" })}</h3>
          </div>
          <button className="ghostButton" onClick={() => void refreshHealth()}>
            {t(locale, { "zh-CN": "刷新状态", "en-US": "Refresh Status" })}
          </button>
        </div>
        <div className="settingsGrid">
          <section className="cardPanel innerCard">
            <h4>{t(locale, { "zh-CN": "界面与语言", "en-US": "Interface and Language" })}</h4>
            <label className="fieldLabel">
              {t(locale, { "zh-CN": "语言", "en-US": "Language" })}
              <select
                value={preferences.locale}
                onChange={(event) =>
                  startTransition(() => setLocale(event.target.value as LocaleCode))
                }
              >
                {Object.entries(localeMeta).map(([code, meta]) => (
                  <option key={code} value={code}>
                    {meta.label}
                  </option>
                ))}
              </select>
            </label>
            <label className="fieldLabel">
              {t(locale, { "zh-CN": "主题", "en-US": "Theme" })}
              <select
                value={preferences.themeMode}
                onChange={(event) =>
                  startTransition(() => setThemeMode(event.target.value as ThemeMode))
                }
              >
                <option value="dark">Dark</option>
                <option value="light">Light</option>
              </select>
            </label>
            <label className="toggleRow">
              <span>{t(locale, { "zh-CN": "轻量动态效果", "en-US": "Light motion effects" })}</span>
              <input
                type="checkbox"
                checked={preferences.animationEnabled}
                onChange={(event) => setAnimationEnabled(event.target.checked)}
              />
            </label>
            <label className="toggleRow">
              <span>{t(locale, { "zh-CN": "演示流模式", "en-US": "Demo flow mode" })}</span>
              <input type="checkbox" checked={demoMode} onChange={(event) => setDemoMode(event.target.checked)} />
            </label>
            <small>
              {isPending
                ? t(locale, { "zh-CN": "正在应用设置...", "en-US": "Applying settings..." })
                : t(locale, { "zh-CN": "设置会立即生效。", "en-US": "Changes apply instantly." })}
            </small>
          </section>

          <section className="cardPanel innerCard">
            <h4>{t(locale, { "zh-CN": "助手形象", "en-US": "Assistant Persona" })}</h4>
            <label className="fieldLabel">
              {t(locale, { "zh-CN": "助手名称", "en-US": "Assistant name" })}
              <input value={assistantNameDraft} onChange={(event) => setAssistantNameDraft(event.target.value)} />
            </label>
            <button onClick={() => setAssistantName(assistantNameDraft)}>
              {t(locale, { "zh-CN": "保存助手名称", "en-US": "Save assistant name" })}
            </button>
            <div className="settingsNote">
              <strong>
                {t(locale, { "zh-CN": "当前助手", "en-US": "Current assistant" })}: {preferences.assistantName}
              </strong>
              <span>
                {t(locale, {
                  "zh-CN": "用于工作台、引导和聊天区域中的智能助手展示。",
                  "en-US": "Used across the command center, onboarding, and assistant conversation area."
                })}
              </span>
            </div>
          </section>

          <section className="cardPanel innerCard">
            <h4>{t(locale, { "zh-CN": "地图与语音", "en-US": "Map and Voice" })}</h4>
            <div className="settingsNote">
              <strong>{hasAmapKey ? "Amap Key Configured" : "Amap Key Missing"}</strong>
              <span>
                {t(locale, {
                  "zh-CN": "这只表示前端拿到了 Key，不代表实时地图一定能成功初始化。",
                  "en-US": "This only means the frontend received a key. It does not guarantee the live map can initialize successfully."
                })}
              </span>
            </div>
            <div className="settingsNote">
              <strong>{t(locale, { "zh-CN": "语音识别", "en-US": "Voice recognition" })}</strong>
              <span>
                {t(locale, {
                  "zh-CN": "使用浏览器原生 Web Speech API，识别语言会跟随当前界面语言。",
                  "en-US": "Uses the browser Web Speech API and follows the current UI language."
                })}
              </span>
            </div>
          </section>

          <section className="cardPanel innerCard">
            <h4>{t(locale, { "zh-CN": "服务健康状态", "en-US": "Service Health" })}</h4>
            <div className="detailGrid">
              <div>
                <small>{t(locale, { "zh-CN": "API 服务", "en-US": "API service" })}</small>
                <strong>{health?.service ?? "unknown"}</strong>
              </div>
              <div>
                <small>{t(locale, { "zh-CN": "服务可用性", "en-US": "Service status" })}</small>
                <strong>{serviceStatusLabel}</strong>
              </div>
              <div>
                <small>{t(locale, { "zh-CN": "模型提供方", "en-US": "Model provider" })}</small>
                <strong>{health?.qwen?.provider ?? t(locale, { "zh-CN": "未返回", "en-US": "Unavailable" })}</strong>
              </div>
              <div>
                <small>{t(locale, { "zh-CN": "模型名称", "en-US": "Model name" })}</small>
                <strong>{health?.qwen?.model ?? t(locale, { "zh-CN": "未返回", "en-US": "Unavailable" })}</strong>
              </div>
            </div>
            <p>
              {health?.message ??
                t(locale, {
                  "zh-CN": "点击刷新状态后查看服务详情。",
                  "en-US": "Refresh the status to inspect service details."
                })}
            </p>
          </section>
        </div>
      </section>
    </div>
  );
}
