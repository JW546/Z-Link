export type MissionStatus = "pending" | "running" | "success" | "failed" | "paused";
export type StepStatus = "pending" | "running" | "done" | "failed";
export type LocaleCode = "zh-CN" | "en-US";
export type ThemeMode = "dark" | "light";
export type HealthMode = "remote" | "mock-fallback" | "mock-forced";

export interface ParseResult {
  intentSummary: string;
  confidence: number;
  entities: { name: string; value: string }[];
  constraints: string[];
  risks: string[];
  naturalLanguagePlanHint: string;
  agentNarrative?: string;
}

export interface PlanStep {
  id: string;
  title: string;
  description: string;
  status: StepStatus;
}

export interface KpiMetric {
  label: string;
  value: string;
  trend: string;
}

export interface LinkStrategy {
  primary: string;
  backup: string;
  policy: string;
}

export interface CollaborationAdvice {
  nodeId: string;
  role: string;
  topic: string;
  route: string;
  advice: string;
  riskScore: number;
  confidence: number;
  latencyMs: number;
  idlType: string;
}

export interface CollaborationDecision {
  route: string;
  destination: string;
  reason: string;
  riskScore: number;
  confidence: number;
  consensusPolicy: string;
  nextAction: string;
  routeAlgorithm?: string;
  routePath?: string[];
  routeCost?: number;
}

export interface CollaborationIdlMapping {
  topic: string;
  idlModule: string;
  idlType: string;
  idlFile: string;
  qos: string;
}

export interface CollaborationTimelineItem {
  time: string;
  title: string;
  detail: string;
}

export interface CollaborationTrace {
  autonomyMode: string;
  passengerIntent: string;
  destination: string;
  decisionOwner: string;
  publishTopics: string[];
  subscribeTopics: string[];
  deviceAdvice: CollaborationAdvice[];
  finalDecision: CollaborationDecision;
  experimentMetrics: KpiMetric[];
  idlMappings: CollaborationIdlMapping[];
  timeline: CollaborationTimelineItem[];
  routePlan?: Record<string, unknown>;
  taskSchedule?: Record<string, unknown>;
  algorithmSummary?: KpiMetric[];
}

export interface PlatformPlan {
  missionId: string;
  input: string;
  intent: Record<string, unknown>;
  structuredDsl: Record<string, unknown>;
  idlBindings: Record<string, unknown>[];
  topics: Record<string, unknown>[];
  qosPolicies: Record<string, unknown>[];
  routePlan: Record<string, unknown>;
  routeAnalysis: Record<string, unknown>;
  deviceCatalog: Record<string, unknown>[];
  taskSchedule: Record<string, unknown>;
  deviceAssignments: Record<string, unknown>[];
  ddsRuntimeMode?: "REAL_DDS" | "SIMULATED_DDS" | "FALLBACK_SIMULATION" | string;
  ddsSimulation: Record<string, unknown>[];
  communicationLogs: Record<string, unknown>[];
  riskWarnings: string[];
  riskAssessment?: Record<string, unknown>;
  topicTopology?: Record<string, unknown>;
  activeFaults?: Record<string, unknown>[];
  fallbackStrategies: string[];
  executionStatus: Record<string, unknown>;
  executionTimeline?: ExecutionTimelineEvent[];
  decisionSummary: string;
}

export interface QosPolicy {
  topic: string;
  reliability: "RELIABLE" | "BEST_EFFORT" | string;
  durability: "VOLATILE" | "TRANSIENT_LOCAL" | string;
  deadlineMs: number;
  latencyBudgetMs: number;
  historyDepth: number;
  priority: number;
  retryLimit?: number;
  fallbackPolicy?: string;
  reason?: string;
}

export interface ExecutionTimelineEvent {
  timestamp: number;
  stage: string;
  actor: string;
  topic?: string;
  action: string;
  status: string;
  message: string;
  latencyMs: number;
  retryCount: number;
  riskLevel: string;
}

export interface ExecutionResult {
  accepted: boolean;
  missionId?: string;
  runId: string;
  status?: string;
  message?: string;
  timeline?: ExecutionTimelineEvent[];
  executionTimeline?: ExecutionTimelineEvent[];
  timelineSummary?: Record<string, unknown>;
}

export interface MapPoint {
  id: string;
  label: string;
  type: "device" | "target" | "gateway" | "waypoint" | "origin" | "user";
  x: number;
  y: number;
  lat?: number;
  lng?: number;
  status?: string;
}

export interface MapRoute {
  id: string;
  label: string;
  points: string[];
  status: "planned" | "active" | "done";
}

export interface MapOverlay {
  centerLabel: string;
  provider: string;
  areaName: string;
  tileProvider?: string;
  zoom?: number;
  origin: MapPoint;
  destination: MapPoint;
  routeCoordinates: { lat: number; lng: number }[];
  distanceKm?: number;
  durationMin?: number;
  points: MapPoint[];
  routes: MapRoute[];
  layers: string[];
}

export interface PlanResult {
  missionId: string;
  planNarrative: string;
  agentNarrative: string;
  dslPreview: string;
  javaCode: string;
  javaClassName: string;
  compileStatus: string;
  dslFilePath?: string;
  javaFilePath?: string;
  steps: PlanStep[];
  subscribeTopics: TopicItem[];
  publishTopics: TopicItem[];
  kpis: KpiMetric[];
  linkStrategy: LinkStrategy;
  mapOverlay: MapOverlay;
  collaboration?: CollaborationTrace;
  platformPlan?: PlatformPlan;
  structuredDsl?: Record<string, unknown>;
  ddsSimulation?: Record<string, unknown>[];
  communicationLogs?: Record<string, unknown>[];
  riskWarnings?: string[];
  fallbackStrategies?: string[];
  ddsRuntimeMode?: string;
  riskAssessment?: Record<string, unknown>;
  topicTopology?: Record<string, unknown>;
  activeFaults?: Record<string, unknown>[];
  executionRunId?: string;
  executionStatus?: string;
  executionTimeline?: ExecutionTimelineEvent[];
  timelineSummary?: Record<string, unknown>;
}

export interface TopicItem {
  name: string;
  type: string;
  ddsTopic?: string;
  idlModule?: string;
  idlType?: string;
  typeSupport?: string;
  qosProfile?: string;
  reliability?: "RELIABLE" | "BEST_EFFORT";
  durability?: "VOLATILE" | "TRANSIENT_LOCAL";
  partition?: string;
  keyFields?: string[];
  samplePayload?: string;
  direction?: "sub" | "pub";
  fromDevice?: string;
  toDevice?: string;
  qos: number;
  rateHz: number;
  enabled?: boolean;
  lastMessage?: string;
}

export interface Mission {
  id: string;
  sourceText: string;
  status: MissionStatus;
  intent?: string;
  createdAt?: string;
}

export interface Device {
  id: string;
  name: string;
  online: boolean;
  latencyMs: number;
  role?: string;
  battery?: number;
  link?: string;
  deviceId?: string;
  deviceType?: string;
  capabilities?: string[];
  supportedTopics?: string[];
  batteryLevel?: number;
  currentLoad?: number;
  linkQuality?: number;
  location?: string;
  status?: string;
  lastHeartbeat?: number;
  failureReason?: string;
  assignedMissionId?: string;
  available?: boolean;
}

export interface ActionResult {
  ok: boolean;
  message: string;
}

export interface UserSession {
  id: string;
  username: string;
  displayName: string;
  role: string;
  permissions: string[];
}

export interface LoginResult extends ActionResult {
  token: string;
  user: UserSession;
}

export interface BackendCapability {
  id: string;
  name: string;
  backendClass: string;
  endpoint: string;
  mode: "real" | "real+simulated" | "real-schema" | "real-with-fallback" | "simulated" | string;
  status: string;
  description: string;
  actions: string[];
}

export interface BackendCapabilityGroup {
  name: string;
  capabilities: BackendCapability[];
}

export interface CapabilityCatalog {
  ok: boolean;
  updatedAt: number;
  summary: string;
  groups: BackendCapabilityGroup[];
}

export interface CapabilityActionResult extends ActionResult {
  capabilityId: string;
  action: string;
  timestamp: number;
  mode?: string;
  simulated?: boolean;
  data?: unknown;
}

export interface WorkspaceUser extends UserSession {
  email: string;
  phone: string;
  organization: string;
  title: string;
  bio: string;
  locale: LocaleCode;
  avatarAccent: string;
  avatarImage?: string;
  favoriteScene: string;
  securityLevel: "standard" | "elevated" | "mission-critical";
  createdAt: string;
  isLocalOnly: boolean;
  password?: string;
}

export interface RegistrationPayload {
  username: string;
  password: string;
  displayName: string;
  email: string;
  organization: string;
}

export interface ChatMessage {
  id: string;
  role: "assistant" | "user" | "system";
  title?: string;
  content: string;
  createdAt: string;
}

export interface HeroSlide {
  id: string;
  imageUrl: string;
  imageAlt: string;
}

export interface GuideStep {
  id: string;
  title: string;
  description: string;
  hint: string;
}

export interface WorkspacePreferences {
  locale: LocaleCode;
  themeMode: ThemeMode;
  guideCompleted: boolean;
  animationEnabled: boolean;
  assistantName: string;
}

export interface HealthStatus {
  ok: boolean;
  service: string;
  timestamp: number;
  message?: string;
  mode?: HealthMode;
  degraded?: boolean;
  qwen?: {
    enabled?: boolean;
    provider?: string;
    model?: string;
    baseUrl?: string;
  };
}
