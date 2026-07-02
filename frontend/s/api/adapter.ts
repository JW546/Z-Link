import axios from "axios";
import type {
  ActionResult,
  CapabilityActionResult,
  CapabilityCatalog,
  Device,
  ExecutionResult,
  HealthMode,
  HealthStatus,
  LocaleCode,
  LoginResult,
  Mission,
  ParseResult,
  PlanResult
} from "../types";

const useMock = import.meta.env.VITE_USE_MOCK === "true";
const allowFallbackMock = import.meta.env.VITE_ALLOW_API_FALLBACK_MOCK !== "false";
const demoCredentials: Record<string, string> = {
  admin: "admin123",
  operator: "operator123",
  demo: "demo123"
};
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "/api",
  timeout: 20000
});

const sleep = (ms: number) => new Promise((resolve) => window.setTimeout(resolve, ms));

const createMockHealth = (mode: HealthMode, message: string): HealthStatus => ({
  ok: true,
  service: "mock-backend",
  timestamp: Date.now(),
  mode,
  degraded: mode !== "remote",
  message,
  qwen: { enabled: false, provider: "mock", model: "demo", baseUrl: "local" }
});

const getPreferredLocale = (): LocaleCode => {
  try {
    const raw =
      window.localStorage.getItem("z_link_workspace_preferences_v4") ??
      window.localStorage.getItem("aether_workspace_preferences_v3");
    if (!raw) return "zh-CN";
    const parsed = JSON.parse(raw) as { locale?: LocaleCode };
    return parsed.locale === "en-US" ? "en-US" : "zh-CN";
  } catch {
    return "zh-CN";
  }
};

const localize = (zhCN: string, enUS: string) => (getPreferredLocale() === "en-US" ? enUS : zhCN);

api.interceptors.request.use((config) => {
  const locale = getPreferredLocale();
  const headers = config.headers as { set?: (name: string, value: string) => void } | undefined;
  if (headers?.set) {
    headers.set("Accept-Language", locale);
    headers.set("X-Z-Link-Locale", locale);
  } else {
    config.headers = {
      ...(config.headers ?? {}),
      "Accept-Language": locale,
      "X-Z-Link-Locale": locale
    } as unknown as typeof config.headers;
  }
  return config;
});

const mockParse: ParseResult = {
  intentSummary: "乘客希望尽快抵达目的地，并要求无人车自主联动后车和无人机规划路线",
  confidence: 0.92,
  entities: [
    { name: "乘客意图", value: "尽快抵达 / 安全优先" },
    { name: "目标地点", value: "西南大学附属医院" },
    { name: "协同节点", value: "CAR-CQ-002 / UAV-CQ-007 / CN-BACKBONE-CQ" }
  ],
  constraints: ["车辆必须自主决策", "合规广域链路只提供态势", "DDS 主题持续订阅设备建议"],
  risks: ["前方拥堵", "无人机前视延迟", "路口状态变化"],
  naturalLanguagePlanHint: "先发布乘客意图 DSL，再订阅后车、无人机、路口与广域态势建议。",
  agentNarrative: "系统判断这是一条乘客意图驱动的无人设备自治协同任务，由车端融合设备建议并发布重规划触发。"
};

const mockPlan = (text: string): PlanResult => ({
  missionId: `MSN-${Date.now()}`,
  planNarrative: `已将“${text}”拆解为设备协同、链路调度与话题映射三个层次。`,
  agentNarrative:
    "小智建议先由高空节点建立态势视野，再让地面节点贴近确认。主链路采用 Ad Hoc Mesh，必要时切换至 5G-A 专网 / DDS Router / 国产低轨卫星互联网预留接口保持广域回传。",
  dslPreview:
    "task intentContract {\n" +
    "  intent: recon_confirm_intercept;\n" +
    "  role: air_ground_team;\n" +
    "  mode: cooperative;\n" +
    "}\n" +
    "state collaborationFlow {\n" +
    "  subscribe: telemetry, perception, gateway_status;\n" +
    "  publish: mission_event, cmd_vel, coordination_order;\n" +
    "}",
  javaClassName: "GeneratedDroneMission",
  compileStatus: "READY_FOR_DEVICE",
  javaCode:
    "public final class GeneratedDroneMission {\n" +
    "  public void run(DeviceBus bus) {\n" +
    "    bus.subscribe(\"/drone/telemetry\", \"drone-02\");\n" +
    "    bus.subscribe(\"/ugv/perception\", \"ugv-01\");\n" +
    "    bus.publish(\"/coord/mission_event\", \"MISSION_READY\");\n" +
    "    bus.publish(\"/coord/cmd_vel\", \"HOLD_POSITION\");\n" +
    "  }\n" +
    "}\n",
  steps: [
    { id: "1", title: "识别自然语言意图", description: "提取任务目标、区域、角色与安全约束。", status: "done" },
    { id: "2", title: "映射 DDS 话题", description: "建立订阅与发布关系，并给出推荐 QoS。", status: "done" },
    { id: "3", title: "挑选协同设备", description: "按照在线状态、延迟、电量与角色分配节点。", status: "running" },
    { id: "4", title: "准备执行指令", description: "生成 DSL、Java 代码和执行节拍建议。", status: "pending" }
  ],
  subscribeTopics: [
    {
      name: "DronePosition",
      ddsTopic: "chongqing::DronePosition",
      type: "IDL",
      idlModule: "chongqing",
      idlType: "DronePosition",
      typeSupport: "chongqing.DronePositionTypeSupport",
      qos: 1,
      qosProfile: "ReliableSensorData",
      reliability: "RELIABLE",
      durability: "VOLATILE",
      keyFields: ["drone_id"],
      direction: "sub",
      fromDevice: "drone-02",
      rateHz: 30,
      enabled: true,
      lastMessage: "alt=180m speed=12m/s battery=86%"
    },
    {
      name: "TrafficFlow",
      ddsTopic: "chongqing::TrafficFlow",
      type: "IDL",
      idlModule: "chongqing",
      idlType: "TrafficFlow",
      typeSupport: "chongqing.TrafficFlowTypeSupport",
      qos: 1,
      qosProfile: "ReliableTelemetry",
      reliability: "RELIABLE",
      durability: "VOLATILE",
      keyFields: ["zone_id"],
      direction: "sub",
      fromDevice: "ugv-01",
      rateHz: 15,
      enabled: true,
      lastMessage: "target confidence=0.84"
    },
    {
      name: "EmergencyStatus",
      ddsTopic: "chongqing::EmergencyStatus",
      type: "IDL",
      idlModule: "chongqing",
      idlType: "EmergencyStatus",
      typeSupport: "chongqing.EmergencyStatusTypeSupport",
      qos: 1,
      qosProfile: "ReliableState",
      reliability: "RELIABLE",
      durability: "TRANSIENT_LOCAL",
      keyFields: ["drone_id"],
      direction: "sub",
      fromDevice: "edge-gw",
      rateHz: 2,
      enabled: true,
      lastMessage: "link=AdHoc stable"
    }
  ],
  publishTopics: [
    {
      name: "MissionCommand",
      ddsTopic: "chongqing::MissionCommand",
      type: "IDL",
      idlModule: "chongqing",
      idlType: "MissionCommand",
      typeSupport: "chongqing.MissionCommandTypeSupport",
      qos: 1,
      qosProfile: "ReliableCommand",
      reliability: "RELIABLE",
      durability: "TRANSIENT_LOCAL",
      keyFields: ["command_id", "drone_id"],
      direction: "pub",
      toDevice: "swarm",
      rateHz: 5,
      enabled: true,
      lastMessage: "MISSION_READY",
      samplePayload: "{\"command_id\":\"cmd-001\",\"drone_id\":\"drone-05\",\"mission_type\":\"SURVEILLANCE_PATROL\"}"
    },
    {
      name: "SwarmMission",
      ddsTopic: "urban::SwarmMission",
      type: "IDL",
      idlModule: "urban",
      idlType: "SwarmMission",
      typeSupport: "urban.SwarmMissionTypeSupport",
      qos: 1,
      qosProfile: "ReliableMission",
      reliability: "RELIABLE",
      durability: "TRANSIENT_LOCAL",
      keyFields: ["mission_id"],
      direction: "pub",
      toDevice: "swarm",
      rateHz: 5,
      enabled: true,
      lastMessage: "MISSION_READY"
    },
    {
      name: "EmergencyAlert",
      ddsTopic: "urban::EmergencyAlert",
      type: "IDL",
      idlModule: "urban",
      idlType: "EmergencyAlert",
      typeSupport: "urban.EmergencyAlertTypeSupport",
      qos: 1,
      qosProfile: "ReliableAlert",
      reliability: "RELIABLE",
      durability: "TRANSIENT_LOCAL",
      keyFields: ["alert_id"],
      direction: "pub",
      toDevice: "ugv-01",
      rateHz: 10,
      enabled: true,
      lastMessage: "move to checkpoint"
    }
  ],
  kpis: [
    { label: "端到端延迟", value: "32ms", trend: "稳定" },
    { label: "话题匹配率", value: "96%", trend: "上升" },
    { label: "链路可用率", value: "99.2%", trend: "稳定" }
  ],
  linkStrategy: { primary: "Ad Hoc Mesh", backup: "5G-A / DDS Router / CN-LEO reserved", policy: "本地自治优先，链路抖动时自动切到合规备用链路。" },
  mapOverlay: {
    centerLabel: "重庆市观音桥步行街",
    provider: "Amap JS API",
    tileProvider: "https://webapi.amap.com/maps?v=2.0",
    areaName: "观音桥步行街 -> Zone-B",
    zoom: 13,
    origin: { id: "origin", label: "默认起点：观音桥步行街", type: "origin", x: 20, y: 58, lat: 29.5794, lng: 106.5328, status: "start" },
    destination: { id: "destination", label: "目的地：Zone-B", type: "target", x: 72, y: 42, lat: 29.592, lng: 106.568, status: "target" },
    routeCoordinates: [
      { lat: 29.5794, lng: 106.5328 },
      { lat: 29.584, lng: 106.545 },
      { lat: 29.588, lng: 106.557 },
      { lat: 29.592, lng: 106.568 }
    ],
    distanceKm: 4.1,
    durationMin: 12,
    layers: ["实时地图底图", "用户定位", "起点/终点", "协同路线"],
    points: [
      { id: "origin", label: "观音桥步行街", type: "origin", x: 20, y: 58, lat: 29.5794, lng: 106.5328, status: "start" },
      { id: "drone-02", label: "高空侦察", type: "device", x: 34, y: 36, lat: 29.588, lng: 106.545, status: "online" },
      { id: "drone-05", label: "低空确认", type: "device", x: 52, y: 58, lat: 29.585, lng: 106.557, status: "online" },
      { id: "ugv-01", label: "地面协同", type: "device", x: 67, y: 72, lat: 29.582, lng: 106.563, status: "online" },
      { id: "edge-gw", label: "边缘网关", type: "gateway", x: 38, y: 78, lat: 29.578, lng: 106.548, status: "stable" },
      { id: "destination", label: "目的地 Zone-B", type: "target", x: 72, y: 42, lat: 29.592, lng: 106.568, status: "tracking" }
    ],
    routes: [
      { id: "air-route", label: "空中确认路线", points: ["drone-02", "drone-05", "target"], status: "active" },
      { id: "ground-route", label: "地面协同路线", points: ["edge-gw", "ugv-01", "target"], status: "planned" }
    ]
  },
  collaboration: {
    autonomyMode: "vehicle_self_decision",
    passengerIntent: text,
    destination: "西南大学附属医院",
    decisionOwner: "CAR-CQ-001",
    publishTopics: ["cabin/passenger/intent.dsl", "urban/route/advice.dsl", "Vehicle/Peer/Advice", "Route/Replan/Trigger"],
    subscribeTopics: [
      "urban/vehicle/follow/sensor.dsl",
      "UAV/Perception/ForwardCorridor",
      "Traffic/Intersection/Forward",
      "CompliantBackbone/GlobalPerception",
      "urban/route/advice.dsl"
    ],
    deviceAdvice: [
      {
        nodeId: "CAR-CQ-001",
        role: "无人车",
        topic: "urban/route/advice.dsl",
        route: "route_C_rsu_bypass",
        advice: "优先时效，先订阅无人机和后车建议，再本车自主决策",
        riskScore: 0.58,
        confidence: 0.87,
        latencyMs: 18,
        idlType: "urban.SwarmMission"
      },
      {
        nodeId: "CAR-CQ-002",
        role: "后车",
        topic: "Vehicle/Peer/Advice",
        route: "route_B_hospital_ring_road",
        advice: "后车观测到前方减速，建议加大安全距离并走医院环线路径",
        riskScore: 0.29,
        confidence: 0.82,
        latencyMs: 24,
        idlType: "urban.VehiclePeerAdvice"
      },
      {
        nodeId: "UAV-CQ-007",
        role: "无人机",
        topic: "UAV/Perception/ForwardCorridor",
        route: "route_B_hospital_ring_road",
        advice: "空中前视发现主路排队，建议切换医院环线并持续发布前方走廊感知",
        riskScore: 0.34,
        confidence: 0.88,
        latencyMs: 32,
        idlType: "urban.EnvironmentPerception"
      },
      {
        nodeId: "CN-BACKBONE-CQ",
        role: "合规广域中继",
        topic: "CompliantBackbone/GlobalPerception",
        route: "route_C_rsu_bypass",
        advice: "融合区域交通流、RSU 状态和链路健康，只提供态势，不下发任务",
        riskScore: 0.52,
        confidence: 0.76,
        latencyMs: 41,
        idlType: "urban.GlobalPerception"
      }
    ],
    finalDecision: {
      route: "route_C_rsu_bypass",
      destination: "西南大学附属医院",
      reason: "乘客强调时效且检测到拥堵，车辆融合无人机前视和广域态势后选择 RSU 绕行路径",
      riskScore: 0.44,
      confidence: 0.83,
      consensusPolicy: "time_risk_balanced_consensus",
      nextAction: "publish Route/Replan/Trigger and continue DDS advice subscription"
    },
    experimentMetrics: [
      { label: "协同闭环延迟", value: "79ms", trend: "可用于比赛演示" },
      { label: "建议融合置信度", value: "83%", trend: "论文指标" },
      { label: "自治决策覆盖率", value: "100%", trend: "无控制中心派单" },
      { label: "DDS主题覆盖", value: "9/9", trend: "全覆盖" }
    ],
    idlMappings: [
      { topic: "cabin/passenger/intent.dsl", idlModule: "urban", idlType: "MissionCommand", idlFile: "PassengerIntent.idl", qos: "RELIABLE + KEEP_LAST" },
      { topic: "urban/route/advice.dsl", idlModule: "urban", idlType: "SwarmMission", idlFile: "RouteAdvice.idl", qos: "RELIABLE + TRANSIENT_LOCAL" },
      { topic: "UAV/Perception/ForwardCorridor", idlModule: "urban", idlType: "EnvironmentPerception", idlFile: "EnvironmentPerception.idl", qos: "BEST_EFFORT sensor stream" },
      { topic: "CompliantBackbone/GlobalPerception", idlModule: "urban", idlType: "GlobalPerception", idlFile: "GlobalPerception.idl", qos: "RELIABLE + TRANSIENT_LOCAL" },
      { topic: "Route/Replan/Trigger", idlModule: "urban", idlType: "RouteReplanTrigger", idlFile: "RouteReplanTrigger.idl", qos: "RELIABLE low latency" }
    ],
    timeline: [
      { time: "T0", title: "乘客意图", detail: "车内语音转 DSL 并发布 cabin/passenger/intent.dsl" },
      { time: "T1", title: "自主订阅", detail: "车辆订阅后车、无人机、路口和合规广域态势主题" },
      { time: "T2", title: "设备建议", detail: "后车与无人机发布 urban/route/advice.dsl" },
      { time: "T3", title: "融合决策", detail: "CAR-CQ-001 选择 route_C_rsu_bypass" },
      { time: "T4", title: "重规划发布", detail: "发布 Route/Replan/Trigger" }
    ]
  }
});

const mockDevices: Device[] = [
  { id: "drone-02", name: "X-WING_ALPHA", online: true, latencyMs: 12, role: "高空侦察", battery: 86, link: "Ad Hoc" },
  { id: "drone-05", name: "LOW_ALT_BRAVO", online: true, latencyMs: 18, role: "低空确认", battery: 72, link: "Ad Hoc" },
  { id: "ugv-01", name: "TERRA_BOT_V4", online: true, latencyMs: 22, role: "地面协同", battery: 68, link: "5G" },
  { id: "edge-gw", name: "EDGE_GATEWAY", online: true, latencyMs: 8, role: "主题桥接", battery: 100, link: "Ethernet" }
];

const mockMissionList: Mission[] = [
  { id: "MSN-1001", sourceText: "侦察并确认目标车辆", status: "running", intent: "侦察 + 确认", createdAt: "09:31:22" },
  { id: "MSN-0998", sourceText: "巡检边界并回传异常点", status: "success", intent: "巡检", createdAt: "09:12:10" }
];

const localizeParseResult = (result: ParseResult): ParseResult => {
  if (getPreferredLocale() !== "en-US") return result;

  return {
    ...result,
    intentSummary: "Survey the target area, confirm the situation, and prepare coordinated multi-device handling",
    entities: [
      { name: "Mission Zone", value: "Zone-B" },
      { name: "Air Nodes", value: "drone-02 / drone-05" },
      { name: "Ground Node", value: "ugv-01" }
    ],
    constraints: [
      "Prefer online devices first",
      "Keep DDS topics synchronized during the mission",
      "Low-battery devices should only handle sensing and relay"
    ],
    risks: ["Ad Hoc link jitter", "Temporary target occlusion", "Cross-device clock drift"],
    naturalLanguagePlanHint: "Subscribe to telemetry and perception first, then publish mission events and speed-control commands.",
    agentNarrative:
      "The system interprets this as an air-ground coordination task: drones keep aerial awareness, the ground vehicle confirms locally, and the edge gateway maintains topic synchronization."
  };
};

const localizePlanResult = (result: PlanResult, text: string): PlanResult => {
  if (getPreferredLocale() !== "en-US") return result;

  return {
    ...result,
    planNarrative: `The request "${text}" has been decomposed into device coordination, link scheduling, and topic mapping.`,
    agentNarrative:
      "XiaoZhi recommends building the aerial picture first, then moving the ground node in for confirmation. Keep Ad Hoc Mesh as the primary path and switch to 5G-A private edge, DDS Router, or a reserved compliant CN-LEO interface when broader backhaul is needed.",
    steps: [
      { id: "1", title: "Interpret natural-language intent", description: "Extract mission goal, area, roles, and safety constraints.", status: "done" },
      { id: "2", title: "Map DDS topics", description: "Wire up subscriptions and publications with recommended QoS.", status: "done" },
      { id: "3", title: "Select coordination devices", description: "Assign nodes by online state, latency, battery, and role fit.", status: "running" },
      { id: "4", title: "Prepare execution instructions", description: "Generate DSL, Java code, and execution cadence guidance.", status: "pending" }
    ],
    kpis: [
      { label: "End-to-end latency", value: "32ms", trend: "Stable" },
      { label: "Topic match rate", value: "96%", trend: "Up" },
      { label: "Link availability", value: "99.2%", trend: "Stable" }
    ],
    linkStrategy: {
      ...result.linkStrategy,
      policy: "Prefer local autonomy and shift to the backup link automatically when jitter rises."
    },
    mapOverlay: {
      ...result.mapOverlay,
      centerLabel: "Guanyinqiao Pedestrian Street",
      areaName: "Guanyinqiao Pedestrian Street -> Zone-B",
      origin: { ...result.mapOverlay.origin, label: "Default Start: Guanyinqiao Pedestrian Street" },
      destination: { ...result.mapOverlay.destination, label: "Destination: Zone-B" },
      layers: ["Live map base", "User position", "Start / Destination", "Coordination route"],
      points: result.mapOverlay.points.map((point) => {
        const labels: Record<string, string> = {
          origin: "Guanyinqiao Pedestrian Street",
          "drone-02": "High-altitude Survey",
          "drone-05": "Low-altitude Confirmation",
          "ugv-01": "Ground Coordination",
          "edge-gw": "Edge Gateway",
          destination: "Destination Zone-B"
        };
        return labels[point.id] ? { ...point, label: labels[point.id] } : point;
      }),
      routes: result.mapOverlay.routes.map((route) => ({
        ...route,
        label:
          route.id === "air-route"
            ? "Air Confirmation Route"
            : route.id === "ground-route"
              ? "Ground Coordination Route"
              : route.label
      }))
    }
  };
};

const localizeDevices = (devices: Device[]): Device[] => {
  if (getPreferredLocale() !== "en-US") return devices;

  return devices.map((device) => ({
    ...device,
    role:
      device.id === "drone-02"
        ? "High-altitude Survey"
        : device.id === "drone-05"
          ? "Low-altitude Confirmation"
          : device.id === "ugv-01"
            ? "Ground Coordination"
            : device.id === "edge-gw"
              ? "Topic Bridge"
              : device.role
  }));
};

const localizeMissions = (missions: Mission[]): Mission[] => {
  if (getPreferredLocale() !== "en-US") return missions;

  return missions.map((mission) => ({
    ...mission,
    sourceText:
      mission.id === "MSN-1001"
        ? "Survey and confirm the target vehicle"
        : mission.id === "MSN-0998"
          ? "Inspect the perimeter and send back anomalies"
          : mission.sourceText,
    intent:
      mission.id === "MSN-1001"
        ? "Survey + Confirm"
        : mission.id === "MSN-0998"
          ? "Inspection"
          : mission.intent
  }));
};

async function withFallback<T>(remote: () => Promise<T>, mockFactory: () => Promise<T>): Promise<T> {
  if (useMock) {
    return mockFactory();
  }
  try {
    return await remote();
  } catch (error) {
    if (!allowFallbackMock) {
      throw error;
    }
    return mockFactory();
  }
}

export const backend = {
  async health(): Promise<HealthStatus> {
    if (useMock) {
      await sleep(120);
      return createMockHealth("mock-forced", localize("当前为强制模拟模式，API 数据已被跳过。", "Forced mock mode is active. API-backed data is currently bypassed."));
    }

    try {
      const remote = (await api.get("/health")).data as HealthStatus;
      return {
        ...remote,
        mode: "remote",
        degraded: false
      };
    } catch (error) {
      if (!allowFallbackMock) {
        throw error;
      }

      await sleep(120);
      const reason = error instanceof Error ? error.message : "unknown error";
      return createMockHealth(
        "mock-fallback",
        localize(`远端 API 不可用（${reason}），已切换到模拟数据。`, `Remote API unavailable (${reason}). Showing mock data.`)
      );
    }

    return withFallback(
      async () => (await api.get("/health")).data,
      async () => {
        await sleep(120);
        return {
          ok: true,
          service: "mock-backend",
          timestamp: Date.now(),
          message: "当前使用前端演示后备数据。",
          qwen: { enabled: false, provider: "mock", model: "demo", baseUrl: "local" }
        };
      }
    );
  },

  async login(username: string, password: string): Promise<LoginResult> {
    return withFallback(
      async () => (await api.post("/auth/login", { username, password })).data,
      async () => {
        await sleep(220);
        const normalizedLogin = username.trim().toLowerCase();
        const passwordMatchesDemoAccount = demoCredentials[normalizedLogin] === password;
        if (!username.trim() || !password.trim()) {
          throw new Error(localize("用户名和密码不能为空。", "Username and password are required."));
        }
        if (!passwordMatchesDemoAccount) {
          throw new Error(localize("用户名或密码错误。", "Invalid username or password."));
        }
        return {
          ok: true,
          message: localize("已通过演示通道登录。", "Signed in through the demo channel."),
          token: `mock-session-${Date.now()}`,
          user: {
            id: `u-${normalizedLogin}`,
            username: normalizedLogin,
            displayName: localize(`指挥员 ${username}`, `Commander ${username}`),
            role: "mission-commander",
            permissions: ["mission:write", "dds:publish", "device:dispatch", "capability:run"]
          }
        };
      }
    );
  },

  async logout(): Promise<ActionResult> {
    return withFallback(
      async () => (await api.post("/auth/logout", {})).data,
      async () => {
        await sleep(80);
        return { ok: true, message: localize("已退出登录。", "Signed out.") };
      }
    );
  },

  async listCapabilities(): Promise<CapabilityCatalog> {
    return withFallback(
      async () => (await api.get("/capabilities")).data,
      async () => {
        await sleep(120);
        return {
          ok: true,
          updatedAt: Date.now(),
          summary: localize("当前能力目录来自前端演示数据。", "The capability catalog is currently sourced from frontend demo data."),
          groups: [
            {
              name: "NLU / DSL / Java",
              capabilities: [
                {
                  id: "nlu.pipeline",
                  name: localize("自然语言 -> DSL -> Java", "Natural Language -> DSL -> Java"),
                  backendClass: "nlu.NLToDSLToJavaPipeline.execute",
                  endpoint: "/api/agent/plan",
                  mode: "real",
                  status: "mock",
                  description: localize("串起意图分析、规划与代码生成。", "Connects intent analysis, planning, and code generation."),
                  actions: ["run", "preview"]
                }
              ]
            },
            {
              name: "DDS / IDL",
              capabilities: [
                {
                  id: "dds.topicCatalog",
                  name: localize("DDS / IDL 目录", "DDS / IDL Catalog"),
                  backendClass: "dds.DDSManager",
                  endpoint: "/api/capability/action",
                  mode: "real-schema",
                  status: "mock",
                  description: localize("展示主题目录、QoS 和 TypeSupport 信息。", "Shows topic catalog entries plus QoS and TypeSupport information."),
                  actions: ["list", "publish"]
                }
              ]
            },
            {
              name: "Map / Route",
              capabilities: [
                {
                  id: "map.route",
                  name: localize("真实地图与路线", "Live Map and Routing"),
                  backendClass: "utils.AmapRealAPI",
                  endpoint: "/api/capability/action",
                  mode: "real-with-fallback",
                  status: "mock",
                  description: localize("支持地理编码和路线展示。", "Supports geocoding and route visualization."),
                  actions: ["route", "geocode"]
                }
              ]
            }
          ]
        };
      }
    );
  },

  async runCapability(capabilityId: string, action: string, input: string): Promise<CapabilityActionResult> {
    return withFallback(
      async () => (await api.post("/capability/action", { capabilityId, action, input })).data,
      async () => {
        await sleep(220);
        return {
          ok: true,
          capabilityId,
          action,
          timestamp: Date.now(),
          mode: capabilityId.includes("pipeline") ? "real" : "simulated",
          simulated: !capabilityId.includes("pipeline"),
          message: localize(`能力 ${capabilityId} 已执行 ${action}。`, `Capability ${capabilityId} executed ${action}.`),
          data: { input, sample: capabilityId === "nlu.pipeline" ? localizePlanResult(mockPlan(input), input).dslPreview : "mock result" }
        };
      }
    );
  },

  async parseIntent(text: string): Promise<ParseResult> {
    return withFallback(
      async () => (await api.post("/nlu/parse", { text })).data,
      async () => {
        await sleep(240);
        return localizeParseResult({
          ...mockParse,
          intentSummary: text.includes("巡检")
            ? localize("巡检任务：多设备联动", "Inspection task: multi-device coordination")
            : mockParse.intentSummary
        });
      }
    );
  },

  async generatePlan(text: string): Promise<PlanResult> {
    return withFallback(
      async () => (await api.post("/agent/plan", { text })).data,
      async () => {
        await sleep(420);
        return localizePlanResult(mockPlan(text), text);
      }
    );
  },

  async executeMission(missionId: string): Promise<ExecutionResult> {
    return withFallback(
      async () => (await api.post("/agent/execute", { missionId })).data,
      async () => {
        await sleep(220);
        return {
          accepted: true,
          runId: `RUN-${missionId}`,
          status: "SIMULATED_EXECUTION_COMPLETED",
          timeline: [],
          timelineSummary: { eventCount: 0, retryCount: 0, warningCount: 0 },
          message: localize(`任务 ${missionId} 已进入执行态。`, `Mission ${missionId} has entered execution.`)
        };
      }
    );
  },

  async missionAction(missionId: string, action: string): Promise<ActionResult> {
    return withFallback(
      async () => (await api.post("/mission/action", { missionId, action })).data,
      async () => {
        await sleep(140);
        return { ok: true, message: localize(`任务 ${missionId} 已执行动作：${action}`, `Mission ${missionId} executed action: ${action}`) };
      }
    );
  },

  async deviceAction(deviceId: string, action: string): Promise<ActionResult> {
    return withFallback(
      async () => (await api.post("/device/action", { deviceId, action })).data,
      async () => {
        await sleep(140);
        return { ok: true, message: localize(`设备 ${deviceId} 已执行动作：${action}`, `Device ${deviceId} executed action: ${action}`) };
      }
    );
  },

  async topicAction(topicName: string, action: string, payload?: string): Promise<ActionResult> {
    return withFallback(
      async () => (await api.post("/topic/action", { topicName, action, payload })).data,
      async () => {
        await sleep(140);
        return {
          ok: true,
          message: localize(
            `话题 ${topicName} 已执行动作：${action}${payload ? `，payload=${payload}` : ""}`,
            `Topic ${topicName} executed action: ${action}${payload ? `, payload=${payload}` : ""}`
          )
        };
      }
    );
  },

  async listMissions(): Promise<Mission[]> {
    return withFallback(
      async () => (await api.get("/missions")).data,
      async () => localizeMissions(mockMissionList)
    );
  },

  async listDevices(): Promise<Device[]> {
    return withFallback(
      async () => {
        const remote = (await api.get("/agent/devices")).data;
        return (remote.devices ?? remote) as Device[];
      },
      async () => localizeDevices(mockDevices)
    );
  },

  async listAgentTopics(): Promise<{ topics: Record<string, unknown>[]; recentLogs: Record<string, unknown>[]; runtimeMode?: string }> {
    return withFallback(
      async () => (await api.get("/agent/topics")).data,
      async () => ({
        topics: [],
        recentLogs: [],
        runtimeMode: "mock-fallback"
      })
    );
  },

  async getAgentLogs(params?: { missionId?: string; runId?: string }): Promise<{ logs: Record<string, unknown>[]; executionLogs: Record<string, unknown>[] }> {
    return withFallback(
      async () => (await api.get("/agent/logs", { params })).data,
      async () => ({ logs: [], executionLogs: [] })
    );
  },

  async listScenarios(): Promise<{ scenarios: Record<string, unknown>[] }> {
    return withFallback(
      async () => (await api.get("/agent/scenarios")).data,
      async () => ({ scenarios: [] })
    );
  },

  async injectFault(payload: { faultType: string; target: string; reason?: string }): Promise<ActionResult & { faults?: Record<string, unknown>[]; devices?: Device[] }> {
    return withFallback(
      async () => (await api.post("/agent/fault/inject", payload)).data,
      async () => ({ ok: true, message: localize("已模拟注入故障。", "Fault injection simulated."), faults: [] })
    );
  },

  async clearFaults(): Promise<ActionResult & { devices?: Device[] }> {
    return withFallback(
      async () => (await api.post("/agent/fault/clear", {})).data,
      async () => ({ ok: true, message: localize("已模拟清除故障。", "Faults cleared in mock mode.") })
    );
  },

  async listFaults(): Promise<{ faults: Record<string, unknown>[] }> {
    return withFallback(
      async () => (await api.get("/agent/faults")).data,
      async () => ({ faults: [] })
    );
  },

  async topicTopology(): Promise<{ topology: Record<string, unknown> }> {
    return withFallback(
      async () => (await api.get("/agent/topic-topology")).data,
      async () => ({ topology: { nodes: [], edges: [] } })
    );
  },

  async missionHistory(): Promise<{ history: Record<string, unknown>[]; stats?: Record<string, unknown> }> {
    return withFallback(
      async () => (await api.get("/agent/history")).data,
      async () => ({ history: [], stats: {} })
    );
  },

  async clearHistory(): Promise<ActionResult> {
    return withFallback(
      async () => (await api.post("/agent/history/clear", {})).data,
      async () => ({ ok: true, message: localize("已模拟清空历史。", "History cleared in mock mode.") })
    );
  },

  async dashboard(): Promise<{ dashboard: Record<string, unknown> }> {
    return withFallback(
      async () => (await api.get("/agent/dashboard")).data,
      async () => ({ dashboard: {} })
    );
  },

  async report(missionId?: string): Promise<{ markdown: string }> {
    return withFallback(
      async () => (await api.get("/agent/report", { params: missionId ? { missionId } : undefined })).data,
      async () => ({ markdown: localize("# 任务报告\n\n当前为模拟报告。", "# Mission Report\n\nMock report.") })
    );
  }
};
