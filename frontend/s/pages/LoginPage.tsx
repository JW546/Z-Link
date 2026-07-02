import { FormEvent, useState } from "react";
import { Navigate, useLocation, useNavigate } from "react-router-dom";
import { AssistantAvatar } from "../components/AssistantAvatar";
import { brandName, t } from "../lib/i18n";
import { useAppStore } from "../store/useAppStore";

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const currentUser = useAppStore((state) => state.currentUser);
  const authLoading = useAppStore((state) => state.authLoading);
  const login = useAppStore((state) => state.login);
  const registerWorkspaceUser = useAppStore((state) => state.registerWorkspaceUser);
  const locale = useAppStore((state) => state.preferences.locale);
  const brand = brandName[locale];

  const [mode, setMode] = useState<"login" | "register">("login");
  const [username, setUsername] = useState("admin");
  const [password, setPassword] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [email, setEmail] = useState("");
  const [organization, setOrganization] = useState("");
  const [message, setMessage] = useState("");

  if (currentUser) {
    return <Navigate to="/workbench" replace />;
  }

  const submitLogin = async (event: FormEvent) => {
    event.preventDefault();
    setMessage("");
    const ok = await login(username, password);
    if (ok) {
      const target = (location.state as { from?: string } | null)?.from || "/workbench";
      navigate(target, { replace: true });
    } else {
      setMessage(
        t(locale, {
          "zh-CN": "登录失败，请检查用户名或密码。",
          "en-US": "Login failed. Check the username or password."
        })
      );
    }
  };

  const submitRegister = async (event: FormEvent) => {
    event.preventDefault();
    const result = await registerWorkspaceUser({ username, password, displayName, email, organization });
    setMessage(result.message);
    if (result.ok) {
      setMode("login");
    }
  };

  return (
    <div className="loginShell">
      <section className="loginGlassPanel">
        <section className="loginShowcase">
          <div className="loginBrandHeader">
            <img className="brandHeroLogo" src="/z-link-logo.png" alt="Z-Link logo" />
            <div>
              <span className="eyebrow">{brand.en}</span>
              <h1>{brand.cn}</h1>
              <p>{brand.tagline}</p>
            </div>
          </div>

          <div className="loginShowcaseBody">
            <div className="loginSignalPill">{brand.en}</div>
            <div className="loginSignalPill">
              {t(locale, { "zh-CN": "智能指挥", "en-US": "Mission Control" })}
            </div>
            <div className="loginSignalPill">
              {t(locale, { "zh-CN": "多机协同", "en-US": "Multi-Drone Sync" })}
            </div>
          </div>
        </section>

        <section className="loginPanel">
          <div className="loginPanelTop">
            <AssistantAvatar accent="#65cfff" name="小智" compact />
            <div>
              <h2>{t(locale, { "zh-CN": "进入系统", "en-US": "Sign In" })}</h2>
              <p>{brand.en}</p>
            </div>
          </div>

          <div className="tabSwitch">
            <button className={mode === "login" ? "tabButton active" : "tabButton"} onClick={() => setMode("login")}>
              {t(locale, { "zh-CN": "登录", "en-US": "Login" })}
            </button>
            <button
              className={mode === "register" ? "tabButton active" : "tabButton"}
              onClick={() => setMode("register")}
            >
              {t(locale, { "zh-CN": "注册", "en-US": "Register" })}
            </button>
          </div>

          {mode === "login" ? (
            <form onSubmit={submitLogin} className="formGrid">
              <label>
                {t(locale, { "zh-CN": "用户名", "en-US": "Username" })}
                <input value={username} onChange={(event) => setUsername(event.target.value)} autoFocus />
              </label>
              <label>
                {t(locale, { "zh-CN": "密码", "en-US": "Password" })}
                <input type="password" value={password} onChange={(event) => setPassword(event.target.value)} />
              </label>
              <button type="submit" disabled={authLoading}>
                {authLoading
                  ? t(locale, { "zh-CN": "登录中", "en-US": "Signing in" })
                  : t(locale, { "zh-CN": "进入", "en-US": "Enter" })}
              </button>
            </form>
          ) : (
            <form onSubmit={submitRegister} className="formGrid">
              <label>
                {t(locale, { "zh-CN": "用户名", "en-US": "Username" })}
                <input value={username} onChange={(event) => setUsername(event.target.value)} autoFocus />
              </label>
              <label>
                {t(locale, { "zh-CN": "密码", "en-US": "Password" })}
                <input type="password" value={password} onChange={(event) => setPassword(event.target.value)} />
              </label>
              <label>
                {t(locale, { "zh-CN": "显示名称", "en-US": "Display name" })}
                <input value={displayName} onChange={(event) => setDisplayName(event.target.value)} />
              </label>
              <label>
                {t(locale, { "zh-CN": "邮箱", "en-US": "Email" })}
                <input value={email} onChange={(event) => setEmail(event.target.value)} />
              </label>
              <label>
                {t(locale, { "zh-CN": "组织", "en-US": "Organization" })}
                <input value={organization} onChange={(event) => setOrganization(event.target.value)} />
              </label>
              <button type="submit">{t(locale, { "zh-CN": "创建账户", "en-US": "Create account" })}</button>
            </form>
          )}

          {message ? <p className="noticeText">{message}</p> : null}

          <div className="loginHint">
            <span>{t(locale, { "zh-CN": "账号：admin / admin123", "en-US": "Account: admin / admin123" })}</span>
            <span>
              {t(locale, {
                "zh-CN": "备用：operator / operator123",
                "en-US": "Backup: operator / operator123"
              })}
            </span>
          </div>
        </section>
      </section>
    </div>
  );
}
