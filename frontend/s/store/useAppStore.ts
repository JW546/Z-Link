import { create } from "zustand";
import { backend } from "../api/adapter";
import { assistantGreeting, defaultMissionInput } from "../lib/i18n";
import type {
  BackendCapabilityGroup,
  CapabilityActionResult,
  ChatMessage,
  Device,
  HealthStatus,
  LocaleCode,
  LoginResult,
  Mission,
  MissionStatus,
  ParseResult,
  PlanResult,
  RegistrationPayload,
  StepStatus,
  ThemeMode,
  WorkspacePreferences,
  WorkspaceUser
} from "../types";

const STORAGE_KEYS = {
  session: "z_link_session_v4",
  users: "z_link_workspace_users_v4",
  preferences: "z_link_workspace_preferences_v4"
} as const;

const LEGACY_STORAGE_KEYS = {
  session: "aether_session_v3",
  users: "aether_workspace_users_v3",
  preferences: "aether_workspace_preferences_v3"
} as const;

const defaultPreferences: WorkspacePreferences = {
  locale: "zh-CN",
  themeMode: "dark",
  guideCompleted: false,
  animationEnabled: false,
  assistantName: "小智"
};

const normalizeThemeMode = (value: unknown): ThemeMode => {
  if (value === "light") return "light";
  if (value === "dark" || value === "aurora" || value === "midnight") return "dark";
  return "dark";
};

const defaultUsers: WorkspaceUser[] = [
  {
    id: "u-admin",
    username: "admin",
    displayName: "总指挥 admin",
    role: "mission-commander",
    permissions: ["mission:write", "dds:publish", "device:dispatch", "capability:run"],
    email: "admin@zlink.local",
    phone: "138-0000-0001",
    organization: "Z-Link Lab",
    title: "总调度指挥官",
    bio: "负责综合调度。",
    locale: "zh-CN",
    avatarAccent: "#70d6ff",
    avatarImage: "/z-link-logo.png",
    favoriteScene: "城市空地协同",
    securityLevel: "mission-critical",
    createdAt: new Date().toISOString(),
    isLocalOnly: false
  },
  {
    id: "u-operator",
    username: "operator",
    displayName: "协同操作员",
    role: "operator",
    permissions: ["mission:write", "dds:publish", "device:dispatch"],
    email: "operator@zlink.local",
    phone: "138-0000-0002",
    organization: "Z-Link Lab",
    title: "现场操作员",
    bio: "负责设备编排。",
    locale: "zh-CN",
    avatarAccent: "#8cf1c8",
    favoriteScene: "城市巡检",
    securityLevel: "elevated",
    createdAt: new Date().toISOString(),
    isLocalOnly: false
  },
  {
    id: "u-demo",
    username: "demo",
    displayName: "演示访客",
    role: "observer",
    permissions: ["mission:read", "dds:read"],
    email: "demo@zlink.local",
    phone: "138-0000-0003",
    organization: "Z-Link Demo Space",
    title: "新手体验员",
    bio: "用于演示。",
    locale: "zh-CN",
    avatarAccent: "#f2c66d",
    favoriteScene: "任务演示",
    securityLevel: "standard",
    createdAt: new Date().toISOString(),
    isLocalOnly: false
  }
];

const readStorage = <T>(key: string, fallback: T): T => {
  try {
    const raw = window.localStorage.getItem(key);
    return raw ? (JSON.parse(raw) as T) : fallback;
  } catch {
    return fallback;
  }
};

const readStorageWithLegacy = <T>(key: string, legacyKey: string, fallback: T): T => {
  const primary = readStorage<T | undefined>(key, undefined);
  if (primary !== undefined) {
    return primary;
  }
  return readStorage<T>(legacyKey, fallback);
};

const writeStorage = (key: string, value: unknown) => {
  try {
    window.localStorage.setItem(key, JSON.stringify(value));
  } catch {
    // ignore quota and serialization errors for a better operator experience
  }
};

const sanitizeUser = (user: WorkspaceUser) => ({
  ...user,
  password: undefined
});

const logLine = (line: string) => `[${new Date().toLocaleTimeString()}] ${line}`;

const pickLocaleText = (locale: LocaleCode, zhCN: string, enUS: string) =>
  locale === "en-US" ? enUS : zhCN;

const createChatMessage = (
  message: Omit<ChatMessage, "createdAt"> & { createdAt?: string }
): ChatMessage => ({
  ...message,
  createdAt: message.createdAt ?? new Date().toLocaleTimeString()
});

const localizeBuiltinUser = (user: WorkspaceUser, locale: LocaleCode): WorkspaceUser => {
  if (user.username === "admin") {
    return {
      ...user,
      displayName: pickLocaleText(locale, "总指挥 admin", "Chief Commander"),
      title: pickLocaleText(locale, "总调度指挥官", "Mission Command Lead"),
      bio: pickLocaleText(locale, "负责综合调度。", "Oversees cross-team mission coordination."),
      favoriteScene: pickLocaleText(locale, "城市空地协同", "Urban air-ground coordination")
    };
  }

  if (user.username === "operator") {
    return {
      ...user,
      displayName: pickLocaleText(locale, "协同操作员", "Field Operator"),
      title: pickLocaleText(locale, "现场操作员", "On-site Operator"),
      bio: pickLocaleText(locale, "负责设备编排。", "Coordinates devices and field execution."),
      favoriteScene: pickLocaleText(locale, "城市巡检", "Urban inspection")
    };
  }

  if (user.username === "demo") {
    return {
      ...user,
      displayName: pickLocaleText(locale, "演示访客", "Demo Guest"),
      title: pickLocaleText(locale, "新手体验员", "First-time Explorer"),
      bio: pickLocaleText(locale, "用于演示。", "Reserved for demos and onboarding."),
      favoriteScene: pickLocaleText(locale, "任务演示", "Mission walkthrough")
    };
  }

  return user;
};

const normalizeOrganization = (organization: string) => {
  if (!organization) return "Z-Link Workspace";
  return organization
    .replace(/Aether Command Lab/gi, "Z-Link Lab")
    .replace(/Aether Workspace/gi, "Z-Link Workspace")
    .replace(/Aether Demo Space/gi, "Z-Link Demo Space");
};

const normalizeUser = (user: WorkspaceUser, locale: LocaleCode): WorkspaceUser =>
  localizeBuiltinUser(
    {
      ...user,
      locale,
      email: user.email.replace(/@aether\.local/gi, "@zlink.local"),
      organization: normalizeOrganization(user.organization),
      avatarImage:
        user.avatarImage ||
        defaultUsers.find((item) => item.username === user.username)?.avatarImage,
      bio:
        user.bio === "负责综合调度、链路安全和任务审核。"
          ? "负责综合调度。"
          : user.bio === "负责设备编排、任务回放和状态确认。"
            ? "负责设备编排。"
            : user.bio === "用于演示与新手引导的体验账号。"
              ? "用于演示。"
              : user.bio,
      favoriteScene:
        user.favoriteScene === "待设置"
          ? pickLocaleText(locale, "待设置", "Not set")
          : user.favoriteScene || pickLocaleText(locale, "多设备协同", "Multi-device coordination")
    },
    locale
  );

const mergeUsers = (users: WorkspaceUser[], locale: LocaleCode) => {
  const map = new Map<string, WorkspaceUser>();
  [...defaultUsers, ...users].forEach((user) => {
    map.set(user.username.toLowerCase(), {
      ...normalizeUser(user, locale),
      password: user.isLocalOnly ? user.password : undefined
    });
  });
  return Array.from(map.values());
};

const statusByAction = (action: string): MissionStatus => {
  if (action === "pause") return "paused";
  if (action === "resume" || action === "rerun") return "running";
  if (action === "stop") return "failed";
  return "pending";
};

const buildLocalProfile = (
  user: LoginResult["user"],
  existing: WorkspaceUser | undefined,
  locale: LocaleCode,
  password?: string
): WorkspaceUser => ({
  id: user.id,
  username: user.username,
  displayName: user.displayName,
  role: user.role,
  permissions: user.permissions,
  email: existing?.email ?? `${user.username}@zlink.local`,
  phone: existing?.phone ?? "",
  organization: existing?.organization ?? "Z-Link Workspace",
  title: existing?.title ?? "协同成员",
  bio: existing?.bio ?? "已加入当前工作区。",
  locale: existing?.locale ?? locale,
  avatarAccent: existing?.avatarAccent ?? "#70d6ff",
  avatarImage: existing?.avatarImage,
  favoriteScene: existing?.favoriteScene ?? "多设备协同",
  securityLevel: existing?.securityLevel ?? "standard",
  createdAt: existing?.createdAt ?? new Date().toISOString(),
  isLocalOnly: existing?.isLocalOnly ?? false,
  password: existing?.password ?? password
});

const storedPreferences = readStorageWithLegacy<WorkspacePreferences>(
  STORAGE_KEYS.preferences,
  LEGACY_STORAGE_KEYS.preferences,
  defaultPreferences
);
const initialPreferences: WorkspacePreferences = {
  ...defaultPreferences,
  ...storedPreferences,
  themeMode: normalizeThemeMode(storedPreferences.themeMode),
  assistantName:
    !storedPreferences.assistantName ||
    storedPreferences.assistantName === "灵犀" ||
    storedPreferences.assistantName === "Lynx"
      ? "小智"
      : storedPreferences.assistantName
};
const initialUsers = mergeUsers(
  readStorageWithLegacy<WorkspaceUser[]>(STORAGE_KEYS.users, LEGACY_STORAGE_KEYS.users, []),
  initialPreferences.locale
);
const savedSession = readStorageWithLegacy<{ token: string; user: WorkspaceUser } | null>(
  STORAGE_KEYS.session,
  LEGACY_STORAGE_KEYS.session,
  null
);
const normalizedSavedSession = savedSession
  ? { ...savedSession, user: normalizeUser(savedSession.user, initialPreferences.locale) }
  : null;
const normalizedSessionUser = normalizedSavedSession
  ? mergeUsers([normalizedSavedSession.user], initialPreferences.locale).find((user) => user.username === normalizedSavedSession.user.username) ?? normalizedSavedSession.user
  : null;

interface AppState {
  inputText: string;
  parseResult: ParseResult | null;
  planResult: PlanResult | null;
  missions: Mission[];
  devices: Device[];
  capabilityGroups: BackendCapabilityGroup[];
  capabilityResult: CapabilityActionResult | null;
  streamLogs: string[];
  conversation: ChatMessage[];
  wsConnected: boolean;
  demoMode: boolean;
  debugMode: boolean;
  authToken: string;
  currentUser: WorkspaceUser | null;
  users: WorkspaceUser[];
  preferences: WorkspacePreferences;
  health: HealthStatus | null;
  loading: boolean;
  executing: boolean;
  authLoading: boolean;
  lastAction: string;
  addStreamLog: (line: string) => void;
  addConversation: (message: Omit<ChatMessage, "id" | "createdAt">) => void;
  setWsConnected: (value: boolean) => void;
  setDemoMode: (value: boolean) => void;
  setDebugMode: (value: boolean) => void;
  setInputText: (value: string) => void;
  setLocale: (locale: LocaleCode) => void;
  setThemeMode: (themeMode: ThemeMode) => void;
  setAnimationEnabled: (enabled: boolean) => void;
  setAssistantName: (value: string) => void;
  markGuideCompleted: () => void;
  login: (username: string, password: string) => Promise<boolean>;
  registerWorkspaceUser: (payload: RegistrationPayload) => Promise<{ ok: boolean; message: string }>;
  updateCurrentUser: (patch: Partial<WorkspaceUser>) => Promise<void>;
  removeWorkspaceUser: (username: string) => Promise<void>;
  logout: () => Promise<void>;
  refreshHealth: () => Promise<void>;
  identifyIntent: () => Promise<void>;
  generatePlan: () => Promise<void>;
  analyzeAndPlan: () => Promise<void>;
  executePlan: () => Promise<void>;
  missionAction: (missionId: string, action: "pause" | "resume" | "stop" | "rerun") => Promise<void>;
  deviceAction: (deviceId: string, action: "bind" | "unbind" | "reconnect" | "inspect") => Promise<void>;
  topicAction: (topicName: string, action: "toggle" | "test" | "publish", payload?: string) => Promise<void>;
  loadCapabilities: () => Promise<void>;
  runCapability: (capabilityId: string, action: string, input?: string) => Promise<void>;
  pauseMission: () => void;
  resumeMission: () => void;
  stopMission: () => void;
  bootstrap: () => Promise<void>;
}

export const useAppStore = create<AppState>((set, get) => ({
  inputText: defaultMissionInput[initialPreferences.locale],
  parseResult: null,
  planResult: null,
  missions: [],
  devices: [],
  capabilityGroups: [],
  capabilityResult: null,
  streamLogs: [],
  conversation: [
    createChatMessage({
      id: "hello",
      role: "assistant",
      title: "小智",
      content: assistantGreeting[initialPreferences.locale]
    })
  ],
  wsConnected: false,
  demoMode: false,
  debugMode: false,
  authToken: normalizedSavedSession?.token ?? "",
  currentUser: normalizedSessionUser ?? null,
  users: initialUsers,
  preferences: initialPreferences,
  health: null,
  loading: false,
  executing: false,
  authLoading: false,
  lastAction: "",

  addStreamLog: (line) =>
    set((state) => ({
      streamLogs: [...state.streamLogs, line].slice(-240)
    })),

  addConversation: (message) =>
    set((state) => ({
      conversation: [
        ...state.conversation,
        createChatMessage({
          id: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
          ...message
        })
      ].slice(-80)
    })),

  setWsConnected: (value) => set({ wsConnected: value }),
  setDemoMode: (value) => set({ demoMode: value }),
  setDebugMode: (value) => set({ debugMode: value }),
  setInputText: (value) => set({ inputText: value }),

  setLocale: (locale) =>
    set((state) => {
      const preferences = { ...state.preferences, locale };
      const shouldRefreshSeedInput = state.inputText === defaultMissionInput[state.preferences.locale];
      const inputText = shouldRefreshSeedInput ? defaultMissionInput[locale] : state.inputText;
      const users = mergeUsers(state.users, locale);
      const currentUser = state.currentUser
        ? users.find((user) => user.username === state.currentUser?.username) ?? { ...state.currentUser, locale }
        : null;
      writeStorage(STORAGE_KEYS.preferences, preferences);
      writeStorage(STORAGE_KEYS.users, users);
      if (currentUser) {
        writeStorage(STORAGE_KEYS.session, { token: state.authToken, user: sanitizeUser(currentUser) });
      }
      return {
        preferences,
        currentUser,
        users,
        inputText,
        streamLogs: [...state.streamLogs, logLine(`界面语言已切换为 ${locale}`)].slice(-240)
      };
    }),

  setThemeMode: (themeMode) =>
    set((state) => {
      const preferences = { ...state.preferences, themeMode };
      writeStorage(STORAGE_KEYS.preferences, preferences);
      return { preferences };
    }),

  setAnimationEnabled: (enabled) =>
    set((state) => {
      const preferences = { ...state.preferences, animationEnabled: enabled };
      writeStorage(STORAGE_KEYS.preferences, preferences);
      return { preferences };
    }),

  setAssistantName: (value) =>
    set((state) => {
      const assistantName = value.trim() || "小智";
      const preferences = { ...state.preferences, assistantName };
      writeStorage(STORAGE_KEYS.preferences, preferences);
      return { preferences };
    }),

  markGuideCompleted: () =>
    set((state) => {
      const preferences = { ...state.preferences, guideCompleted: true };
      writeStorage(STORAGE_KEYS.preferences, preferences);
      return { preferences };
    }),

  login: async (username, password) => {
    const locale = get().preferences.locale;
    set({ authLoading: true, lastAction: pickLocaleText(locale, "正在登录...", "Signing in...") });
    const normalized = username.trim().toLowerCase();
    const existing = get().users.find((item) => item.username.toLowerCase() === normalized);

    try {
      let profile: WorkspaceUser;
      let authToken = "";

      if (existing?.isLocalOnly && existing.password === password) {
        profile = { ...existing };
        authToken = `local-${existing.username}-${Date.now()}`;
      } else {
        const result = await backend.login(username, password);
        authToken = result.token;
        profile = buildLocalProfile(result.user, existing, get().preferences.locale, existing?.isLocalOnly ? existing.password : undefined);
      }

      const users = mergeUsers([
        ...get().users.filter((item) => item.username !== profile.username),
        profile
      ], get().preferences.locale);
      writeStorage(STORAGE_KEYS.users, users);
      writeStorage(STORAGE_KEYS.session, { token: authToken, user: sanitizeUser(profile) });

      set((state) => ({
        authToken,
        currentUser: sanitizeUser(profile) as WorkspaceUser,
        users,
        authLoading: false,
        lastAction: pickLocaleText(locale, `欢迎回来，${profile.displayName}`, `Welcome back, ${profile.displayName}`),
        streamLogs: [
          ...state.streamLogs,
          logLine(pickLocaleText(locale, `用户 ${profile.displayName} 已登录`, `User ${profile.displayName} signed in`))
        ].slice(-240),
        conversation: [
          ...state.conversation,
          createChatMessage({
            id: `welcome-${Date.now()}`,
            role: "assistant",
            title: state.preferences.assistantName,
            content: pickLocaleText(locale, `你好，${profile.displayName}。系统已就绪。`, `Hello, ${profile.displayName}. The workspace is ready.`)
          })
        ].slice(-80)
      }));
      return true;
    } catch (error) {
      const message = error instanceof Error ? error.message : "unknown error";
      set((state) => ({
        authLoading: false,
        lastAction: pickLocaleText(locale, "登录失败", "Sign-in failed"),
        streamLogs: [...state.streamLogs, logLine(`Login failed: ${message}`)].slice(-240)
      }));
      return false;
    }
  },

  registerWorkspaceUser: async (payload) => {
    const locale = get().preferences.locale;
    const users = get().users;
    const username = payload.username.trim();
    const displayName = payload.displayName.trim();
    const email = payload.email.trim();
    const organization = payload.organization.trim();

    if (!username || !payload.password.trim() || !displayName || !email) {
      return { ok: false, message: pickLocaleText(locale, "请完整填写用户名、显示名、邮箱和密码。", "Please complete the username, display name, email, and password.") };
    }
    if (users.some((item) => item.username.toLowerCase() === username.toLowerCase())) {
      return { ok: false, message: pickLocaleText(locale, "该用户名已存在，请换一个。", "That username already exists. Please choose another one.") };
    }

    const newUser: WorkspaceUser = {
      id: `local-${Date.now()}`,
      username,
      displayName,
      role: "operator",
      permissions: ["mission:write", "dds:publish", "device:dispatch"],
      email,
      phone: "",
      organization: organization || "Z-Link Workspace",
      title: "新成员",
      bio: "本地账户。",
      locale: get().preferences.locale,
      avatarAccent: "#bba4ff",
      favoriteScene: "待设置",
      securityLevel: "standard",
      createdAt: new Date().toISOString(),
      isLocalOnly: true,
      password: payload.password
    };

    const nextUsers = mergeUsers([...users, newUser], get().preferences.locale);
    writeStorage(STORAGE_KEYS.users, nextUsers);
    set((state) => ({
      users: nextUsers,
      lastAction: pickLocaleText(locale, `已注册用户 ${displayName}`, `Registered user ${displayName}`),
      streamLogs: [...state.streamLogs, logLine(`Workspace user registered: ${displayName}`)].slice(-240)
    }));
    return { ok: true, message: pickLocaleText(locale, "注册成功。", "Registration complete.") };
  },

  updateCurrentUser: async (patch) => {
    const currentUser = get().currentUser;
    if (!currentUser) return;
    const locale = get().preferences.locale;

    const nextUser = { ...currentUser, ...patch };
    const nextUsers = get().users.map((user) =>
      user.username === currentUser.username ? { ...user, ...patch } : user
    );

    writeStorage(STORAGE_KEYS.users, nextUsers);
    writeStorage(STORAGE_KEYS.session, { token: get().authToken, user: sanitizeUser(nextUser) });

    set((state) => ({
      currentUser: nextUser,
      users: nextUsers,
      lastAction: pickLocaleText(locale, "个人资料已更新", "Profile updated"),
      streamLogs: [...state.streamLogs, logLine(`Profile updated for ${nextUser.displayName}`)].slice(-240)
    }));
  },

  removeWorkspaceUser: async (username) => {
    const currentUser = get().currentUser;
    if (currentUser?.username === username) {
      return;
    }
    const locale = get().preferences.locale;
    const nextUsers = get().users.filter((user) => user.username !== username);
    writeStorage(STORAGE_KEYS.users, nextUsers);
    set((state) => ({
      users: nextUsers,
      lastAction: pickLocaleText(locale, `已移除用户 ${username}`, `Removed user ${username}`),
      streamLogs: [...state.streamLogs, logLine(`Workspace user removed: ${username}`)].slice(-240)
    }));
  },

  logout: async () => {
    const locale = get().preferences.locale;
    try {
      await backend.logout();
    } finally {
      window.localStorage.removeItem(STORAGE_KEYS.session);
      set((state) => ({
        authToken: "",
        currentUser: null,
        lastAction: pickLocaleText(locale, "已退出登录", "Signed out"),
        streamLogs: [...state.streamLogs, logLine(pickLocaleText(locale, "用户已退出登录", "User signed out"))].slice(-240)
      }));
    }
  },

  refreshHealth: async () => {
    try {
      const health = await backend.health();
      set((state) => ({
        health,
        streamLogs: [...state.streamLogs, logLine(`服务状态: ${health.service} / ${health.ok ? "ok" : "down"}`)].slice(-240)
      }));
    } catch (error) {
      const message = error instanceof Error ? error.message : "unknown error";
      set((state) => ({
        health: null,
        streamLogs: [...state.streamLogs, logLine(`Health check failed: ${message}`)].slice(-240)
      }));
    }
  },

  identifyIntent: async () => {
    const { inputText } = get();
    if (!inputText.trim()) return;
    const locale = get().preferences.locale;
    set({ loading: true, lastAction: pickLocaleText(locale, "正在识别任务意图...", "Identifying mission intent...") });
    try {
      const parseResult = await backend.parseIntent(inputText);
      set((state) => ({
        parseResult,
        loading: false,
        lastAction: pickLocaleText(locale, "意图识别完成", "Intent identified"),
        streamLogs: [...state.streamLogs, logLine(`已识别任务意图：${parseResult.intentSummary}`)].slice(-240)
      }));
    } catch (error) {
      const message = error instanceof Error ? error.message : "unknown error";
      set((state) => ({
        loading: false,
        lastAction: pickLocaleText(locale, "意图识别失败", "Intent identification failed"),
        streamLogs: [...state.streamLogs, logLine(`Parse failed: ${message}`)].slice(-240)
      }));
    }
  },

  generatePlan: async () => {
    const { inputText } = get();
    if (!inputText.trim()) return;
    const locale = get().preferences.locale;
    set({ loading: true, lastAction: pickLocaleText(locale, "正在生成协同方案...", "Generating the coordination plan...") });
    try {
      const planResult = await backend.generatePlan(inputText);
      set((state) => ({
        planResult,
        loading: false,
        lastAction: pickLocaleText(locale, "方案生成完成", "Plan generated"),
        streamLogs: [...state.streamLogs, logLine(`方案 ${planResult.missionId} 已生成`)].slice(-240)
      }));
    } catch (error) {
      const message = error instanceof Error ? error.message : "unknown error";
      set((state) => ({
        loading: false,
        lastAction: pickLocaleText(locale, "方案生成失败", "Plan generation failed"),
        streamLogs: [...state.streamLogs, logLine(`Plan failed: ${message}`)].slice(-240)
      }));
    }
  },

  analyzeAndPlan: async () => {
    const { inputText, preferences } = get();
    if (!inputText.trim()) return;
    const locale = preferences.locale;
    set({ loading: true, lastAction: pickLocaleText(locale, "正在串联完整任务链路...", "Building the full mission flow...") });
    try {
      const [parseResult, planResult] = await Promise.all([
        backend.parseIntent(inputText),
        backend.generatePlan(inputText)
      ]);
      const mission: Mission = {
        id: planResult.missionId,
        sourceText: inputText,
        status: "pending",
        intent: parseResult.intentSummary,
        createdAt: new Date().toLocaleTimeString()
      };
      set((state) => ({
        parseResult,
        planResult,
        missions: [mission, ...state.missions.filter((item) => item.id !== mission.id)],
        loading: false,
        lastAction: pickLocaleText(locale, "已生成协同方案", "Coordination plan ready"),
        streamLogs: [
          ...state.streamLogs,
          logLine(`任务链路已生成：${parseResult.intentSummary}`),
          logLine(`DDS 映射完成：SUB=${planResult.subscribeTopics.length}, PUB=${planResult.publishTopics.length}`)
        ].slice(-240),
        conversation: [
          ...state.conversation,
          createChatMessage({
            id: `user-${Date.now()}`,
            role: "user",
            title: pickLocaleText(locale, "当前任务", "Current Mission"),
            content: inputText
          }),
          createChatMessage({
            id: `assistant-${Date.now()}`,
            role: "assistant",
            title: preferences.assistantName,
            content: pickLocaleText(
              locale,
              `${parseResult.intentSummary}。${parseResult.risks.length ? `风险：${parseResult.risks.join("、")}。` : ""}`,
              `${parseResult.intentSummary}.${parseResult.risks.length ? ` Risks: ${parseResult.risks.join(", ")}.` : ""}`
            )
          })
        ].slice(-80)
      }));
    } catch (error) {
      const message = error instanceof Error ? error.message : "unknown error";
      set((state) => ({
        loading: false,
        lastAction: pickLocaleText(locale, "方案生成失败", "Plan generation failed"),
        streamLogs: [...state.streamLogs, logLine(`Analyze failed: ${message}`)].slice(-240)
      }));
    }
  },

  executePlan: async () => {
    const { planResult } = get();
    if (!planResult) return;
    const locale = get().preferences.locale;
    set({ executing: true, lastAction: pickLocaleText(locale, "正在向后端提交执行任务...", "Submitting the mission for execution...") });
    try {
      const result = await backend.executeMission(planResult.missionId);
      const timeline = result.executionTimeline ?? result.timeline ?? [];
      set((state) => ({
        executing: false,
        lastAction: result.message || pickLocaleText(locale, "任务已进入执行态", "Mission is now executing"),
        planResult: {
          ...planResult,
          executionRunId: result.runId,
          executionStatus: result.status,
          executionTimeline: timeline,
          timelineSummary: result.timelineSummary,
          platformPlan: planResult.platformPlan
            ? {
                ...planResult.platformPlan,
                executionTimeline: timeline,
                executionStatus: {
                  ...(planResult.platformPlan.executionStatus ?? {}),
                  state: result.status ?? "SIMULATED_EXECUTION_COMPLETED",
                  runId: result.runId,
                  timelineSummary: result.timelineSummary
                }
              }
            : planResult.platformPlan,
          steps: planResult.steps.map((step, index) => ({
            ...step,
            status: index < 2 ? "done" : (index === 2 ? "running" : step.status) as StepStatus
          }))
        },
        streamLogs: [
          ...state.streamLogs,
          logLine(`任务 ${planResult.missionId} 已提交执行，runId=${result.runId}`),
          ...timeline.slice(-5).map((item) => logLine(`${item.stage}: ${item.message}`))
        ].slice(-240),
        missions: state.missions.map((mission) =>
          mission.id === planResult.missionId ? { ...mission, status: "running" } : mission
        )
      }));
    } catch (error) {
      const message = error instanceof Error ? error.message : "unknown error";
      set((state) => ({
        executing: false,
        lastAction: pickLocaleText(locale, "执行任务失败", "Mission execution failed"),
        streamLogs: [...state.streamLogs, logLine(`Execute failed: ${message}`)].slice(-240)
      }));
    }
  },

  missionAction: async (missionId, action) => {
    const result = await backend.missionAction(missionId, action);
    set((state) => ({
      lastAction: result.message,
      missions: state.missions.map((mission) =>
        mission.id === missionId ? { ...mission, status: statusByAction(action) } : mission
      ),
      streamLogs: [...state.streamLogs, logLine(result.message)].slice(-240)
    }));
  },

  deviceAction: async (deviceId, action) => {
    const result = await backend.deviceAction(deviceId, action);
    const devices = await backend.listDevices();
    set((state) => ({
      lastAction: result.message,
      devices,
      streamLogs: [...state.streamLogs, logLine(result.message)].slice(-240)
    }));
  },

  topicAction: async (topicName, action, payload) => {
    const result = await backend.topicAction(topicName, action, payload);
    set((state) => {
      const updateTopic = (topic: NonNullable<PlanResult["publishTopics"][number]>) =>
        topic.name === topicName
          ? {
              ...topic,
              enabled: action === "toggle" ? !topic.enabled : topic.enabled,
              lastMessage: payload || result.message
            }
          : topic;

      return {
        lastAction: result.message,
        planResult: state.planResult
          ? {
              ...state.planResult,
              subscribeTopics: state.planResult.subscribeTopics.map(updateTopic),
              publishTopics: state.planResult.publishTopics.map(updateTopic)
            }
          : state.planResult,
        streamLogs: [...state.streamLogs, logLine(result.message)].slice(-240)
      };
    });
  },

  loadCapabilities: async () => {
    try {
      const catalog = await backend.listCapabilities();
      set((state) => ({
        capabilityGroups: catalog.groups,
        streamLogs: [
          ...state.streamLogs,
          logLine(`能力目录已同步：${catalog.groups.reduce((sum, group) => sum + group.capabilities.length, 0)} 项`)
        ].slice(-240)
      }));
    } catch (error) {
      const message = error instanceof Error ? error.message : "unknown error";
      set((state) => ({
        streamLogs: [...state.streamLogs, logLine(`Capabilities failed: ${message}`)].slice(-240)
      }));
    }
  },

  runCapability: async (capabilityId, action, input) => {
    const text = input || get().inputText;
    const locale = get().preferences.locale;
    set({ lastAction: pickLocaleText(locale, `正在执行能力：${capabilityId}.${action}`, `Running capability: ${capabilityId}.${action}`) });
    try {
      const result = await backend.runCapability(capabilityId, action, text);
      set((state) => ({
        capabilityResult: result,
        lastAction: result.message,
        streamLogs: [...state.streamLogs, logLine(result.message)].slice(-240)
      }));
    } catch (error) {
      const message = error instanceof Error ? error.message : "unknown error";
      set((state) => ({
        lastAction: pickLocaleText(locale, "能力执行失败", "Capability execution failed"),
        streamLogs: [...state.streamLogs, logLine(`Capability failed: ${message}`)].slice(-240)
      }));
    }
  },

  pauseMission: () => {
    const missionId = get().planResult?.missionId;
    if (missionId) void get().missionAction(missionId, "pause");
  },

  resumeMission: () => {
    const missionId = get().planResult?.missionId;
    if (missionId) void get().missionAction(missionId, "resume");
  },

  stopMission: () => {
    const missionId = get().planResult?.missionId;
    if (missionId) void get().missionAction(missionId, "stop");
  },

  bootstrap: async () => {
    try {
      const [health, missions, devices, catalog] = await Promise.all([
        backend.health(),
        backend.listMissions(),
        backend.listDevices(),
        backend.listCapabilities()
      ]);
      set((state) => ({
        health,
        missions,
        devices,
        capabilityGroups: catalog.groups,
        streamLogs: [...state.streamLogs, logLine("前后端初始化完成，任务、设备与能力目录已同步。")].slice(-240)
      }));
    } catch (error) {
      const message = error instanceof Error ? error.message : "unknown error";
      set((state) => ({
        streamLogs: [...state.streamLogs, logLine(`Bootstrap failed: ${message}`)].slice(-240)
      }));
    }
  }
}));
