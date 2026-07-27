import { useEffect, useMemo, useRef, useState } from "react";
import type { ReactNode } from "react";
import { backend } from "../api/adapter";
import { MissionMap } from "../components/MissionMap";
import { localeMeta, starterTemplates, t } from "../lib/i18n";
import { useAppStore } from "../store/useAppStore";
import type { PlanResult } from "../types";

type SpeechCtor = new () => SpeechRecognition;
type SpeechErrorCode =
  | "aborted"
  | "audio-capture"
  | "network"
  | "no-speech"
  | "not-allowed"
  | "service-not-allowed"
  | string;
type DashboardMap = Record<string, unknown>;

declare global {
  interface Window {
    webkitSpeechRecognition?: SpeechCtor;
    SpeechRecognition?: SpeechCtor;
  }

  interface SpeechRecognition extends EventTarget {
    continuous: boolean;
    interimResults: boolean;
    lang: string;
    onresult: ((event: SpeechRecognitionEvent) => void) | null;
    onend: (() => void) | null;
    onerror: ((event: { error: SpeechErrorCode }) => void) | null;
    abort: () => void;
    start: () => void;
    stop: () => void;
  }

  interface SpeechRecognitionEvent {
    results: {
      [index: number]: {
        [index: number]: {
          transcript: string;
        };
      };
      length: number;
    };
  }
}

const asMap = (value: unknown): DashboardMap =>
  value && typeof value === "object" && !Array.isArray(value) ? (value as DashboardMap) : {};

const asList = (value: unknown): DashboardMap[] =>
  Array.isArray(value) ? value.filter((item) => item && typeof item === "object").map((item) => item as DashboardMap) : [];

const asText = (value: unknown, fallback = "-"): string => {
  if (value === null || value === undefined || value === "") return fallback;
  if (Array.isArray(value)) return value.map((item) => asText(item, "")).filter(Boolean).join(" / ");
  if (typeof value === "object") return JSON.stringify(value);
  return String(value);
};

const asNumber = (value: unknown, fallback = 0) =>
  typeof value === "number" && Number.isFinite(value) ? value : fallback;

const percent = (value: unknown) => `${Math.round(asNumber(value, 0) * 100)}%`;

const short = (value: unknown, max = 120) => {
  const text = asText(value);
  return text.length > max ? `${text.slice(0, max)}...` : text;
};

function StatusBadge({ tone = "neutral", children }: { tone?: "neutral" | "good" | "warn" | "danger"; children: ReactNode }) {
  return <span className={`cockpitBadge ${tone}`}>{children}</span>;
}

function MetricCard({ label, value, note, tone = "neutral" }: { label: string; value: string | number; note?: string; tone?: "neutral" | "good" | "warn" | "danger" }) {
  return (
    <article className={`metricCard ${tone}`}>
      <span>{label}</span>
      <strong>{value}</strong>
      {note ? <small>{note}</small> : null}
    </article>
  );
}

function JsonInspector({ value, title = "原始 JSON" }: { value: unknown; title?: string }) {
  return (
    <details className="jsonInspector">
      <summary>{title}</summary>
      <pre>{JSON.stringify(value ?? {}, null, 2)}</pre>
    </details>
  );
}

function WorkflowStepper({ replanning }: { replanning: boolean }) {
  const steps = ["乘客意图", "DSL 任务契约", "DDS 发布订阅", "Agent 规划与协同建议", "冲突消解", "最终决策", replanning ? "重规划/评估" : "策略评估"];
  return (
    <section className="workflowStepper" aria-label="任务闭环 Timeline">
      {steps.map((step, index) => (
        <article key={step}>
          <span>{String(index + 1).padStart(2, "0")}</span>
          <strong>{step}</strong>
        </article>
      ))}
    </section>
  );
}

function MissionInputPanel({
  inputText,
  setInputText,
  selectedTemplate,
  setSelectedTemplate,
  onVoice,
  voiceState,
  voiceMessage,
  onAnalyze,
  onExecute,
  loading,
  executing,
  hasPlan
}: {
  inputText: string;
  setInputText: (value: string) => void;
  selectedTemplate: string;
  setSelectedTemplate: (value: string) => void;
  onVoice: () => void;
  voiceState: string;
  voiceMessage: string;
  onAnalyze: () => void;
  onExecute: () => void;
  loading: boolean;
  executing: boolean;
  hasPlan: boolean;
}) {
  const preferences = useAppStore((state) => state.preferences);
  const locale = preferences.locale;
  return (
    <section id="mission" className="missionInputPanel cardPanel">
      <div className="panelHeader">
        <div>
          <span className="eyebrow">Mission Input</span>
          <h3>自然语言任务输入</h3>
        </div>
        <StatusBadge tone={hasPlan ? "good" : "warn"}>{hasPlan ? "READY" : "PENDING"}</StatusBadge>
      </div>
      <select
        value={selectedTemplate}
        onChange={(event) => {
          setSelectedTemplate(event.target.value);
          if (event.target.value) setInputText(event.target.value);
        }}
      >
        <option value="">选择演示模板</option>
        {starterTemplates[locale].map((template) => (
          <option key={template} value={template}>{template}</option>
        ))}
      </select>
      <textarea className="missionInput cockpitInput" value={inputText} onChange={(event) => setInputText(event.target.value)} />
      <div className="commandBar">
        <button onClick={onAnalyze} disabled={loading}>{loading ? "生成中" : "生成协同闭环"}</button>
        <button className="ghostButton" onClick={onExecute} disabled={executing || !hasPlan}>{executing ? "协同中" : "启动协同"}</button>
        <button className={voiceState === "listening" ? "ghostButton active" : "ghostButton"} onClick={onVoice}>语音</button>
        <span className="supportText">{voiceMessage}</span>
      </div>
    </section>
  );
}

function ScenarioCard({ scenario, onRun }: { scenario: DashboardMap; onRun: (scenario: DashboardMap) => void }) {
  return (
    <article className="scenarioCard">
      <strong>{asText(scenario.name)}</strong>
      <p>{short(scenario.description, 86)}</p>
      <small>{asText(scenario.expectedDecisionPrinciple)}</small>
      <button className="ghostButton" onClick={() => onRun(scenario)}>载入</button>
    </article>
  );
}

function RouteSummaryCard({ trace, finalDecision, conflictResolution }: { trace: DashboardMap; finalDecision: DashboardMap; conflictResolution: DashboardMap }) {
  const accepted = asMap(conflictResolution.acceptedSuggestion);
  return (
    <article className="routeSummaryCard">
      <span className="eyebrow">Final Decision</span>
      <h2>{asText(trace.finalRoute ?? finalDecision.route, "等待路线")}</h2>
      <p>{asText(trace.routeSelectionReason ?? finalDecision.reason, "生成方案后显示决策解释。")}</p>
      <div className="routeEvidenceStrip">
        <span>risk {asText(accepted.riskScore ?? finalDecision.riskScore)}</span>
        <span>confidence {asText(accepted.confidence ?? finalDecision.confidence)}</span>
        <span>source {asText(accepted.sourceId)}</span>
      </div>
    </article>
  );
}

function AgentSuggestionCard({ title, items }: { title: string; items: DashboardMap[] }) {
  return (
    <section className="agentSuggestionGroup">
      <h4>{title}</h4>
      {items.length ? items.slice(0, 3).map((item, index) => (
        <article key={`${title}-${asText(item.nodeId)}-${index}`} className="agentSuggestionCard">
          <div>
            <strong>{asText(item.nodeId)}</strong>
            <StatusBadge tone={asNumber(item.riskScore, 0.4) > 0.55 ? "danger" : "good"}>risk {asText(item.riskScore)}</StatusBadge>
          </div>
          <span>{asText(item.route)}</span>
          <p>{short(item.advice, 110)}</p>
          <small>{asText(item.topic)} · confidence {asText(item.confidence)} · {asText(item.latencyMs)}ms</small>
        </article>
      )) : <div className="emptyPanel">暂无该类 Agent 建议。</div>}
    </section>
  );
}

function DDSMessageTimeline({ items, compact = false }: { items: DashboardMap[]; compact?: boolean }) {
  return (
    <div className={compact ? "ddsMiniTimeline" : "ddsMessageTimeline"}>
      {items.slice(0, compact ? 4 : 10).map((item, index) => {
        const payload = asMap(item.payload);
        return (
          <article key={`${asText(item.topic)}-${index}`}>
            <span>{asText(item.sourceId ?? item.actor ?? `DDS-${index + 1}`)}</span>
            <strong>{asText(item.topic ?? item.phase)}</strong>
            <p>{short(payload.explanation ?? item.message ?? item.reason ?? payload.route, compact ? 70 : 140)}</p>
            <small>confidence {asText(item.confidence)} · freshness {asText(item.freshness)} · delay {asText(payload.ddsDelayMs ?? item.latencyMs)}ms</small>
          </article>
        );
      })}
    </div>
  );
}

function ConflictResolutionPanel({ detection, resolution, rejected }: { detection: DashboardMap; resolution: DashboardMap; rejected: DashboardMap[] }) {
  const accepted = asMap(resolution.acceptedSuggestion);
  return (
    <section id="conflict" className="conflictPanel">
      <div className="panelHeader">
        <div>
          <span className="eyebrow">Conflict Resolver</span>
          <h3>冲突检测与消解</h3>
        </div>
        <StatusBadge tone={detection.hasConflict ? "warn" : "good"}>{detection.hasConflict ? "CONFLICT" : "CLEAR"}</StatusBadge>
      </div>
      <div className="conflictGrid">
        <article>
          <small>冲突类型</small>
          <strong>{asText(detection.conflictType ?? resolution.conflictType)}</strong>
          <p>{asText(resolution.decisionReason)}</p>
        </article>
        <article>
          <small>采纳建议</small>
          <strong>{asText(accepted.route)}</strong>
          <p>{asText(accepted.reason)}</p>
        </article>
        <article>
          <small>拒绝/降权</small>
          <strong>{rejected.length}</strong>
          <p>{rejected.map((item) => `${asText(item.route)}: ${asText(item.rejectionReason ?? item.reason)}`).join("；") || "无"}</p>
        </article>
      </div>
    </section>
  );
}

function PlannerComparisonPanel({ algorithms }: { algorithms: DashboardMap[] }) {
  return (
    <section className="comparisonPanel">
      <h3>路线规划对比</h3>
      <div className="plannerRows">
        {algorithms.map((item) => (
          <article key={asText(item.algorithm)} className={item.baseline ? "baseline" : "selected"}>
            <strong>{asText(item.algorithm)}</strong>
            <span>{asText(item.routeId)}</span>
            <div>
              <small>time {asText(item.averageTravelTime)}</small>
              <small>risk {asText(item.averageSafetyRisk)}</small>
              <small>score {asText(item.score)}</small>
            </div>
            <p>{short(item.explanation, 130)}</p>
          </article>
        ))}
      </div>
    </section>
  );
}

function PolicyComparisonPanel({ policies, whyBest }: { policies: DashboardMap[]; whyBest?: unknown }) {
  return (
    <section id="policy" className="comparisonPanel">
      <h3>AgentPolicy 评估</h3>
      <div className="policyRows">
        {policies.map((item) => {
          const metrics = asMap(item.metrics);
          return (
            <article key={asText(item.policyName)}>
              <strong>{asText(item.policyName)}</strong>
              <span>reward {asText(metrics.averageReward)} · risk {asText(metrics.averageSafetyRisk)}</span>
              <small>conflict {asText(metrics.conflictResolutionAccuracy)} · replan {asText(metrics.replanningCount)}</small>
            </article>
          );
        })}
      </div>
      <p className="dashboardNote">{asText(whyBest)}</p>
    </section>
  );
}

function EvaluationMetricsPanel({ items }: { items: DashboardMap[] }) {
  return (
    <section className="evaluationPanel">
      <h3>实验指标</h3>
      <div className="metricBars">
        {items.slice(0, 8).map((item, index) => {
          const value = Math.max(0, Math.min(100, asNumber(item.value, 0)));
          return (
            <article key={`${asText(item.label)}-${index}`} className="metricBarRow">
              <div>
                <strong>{asText(item.label)}</strong>
                <small>{asText(item.note)}</small>
              </div>
              <div className="metricTrack"><span style={{ width: `${value}%` }} /></div>
              <b>{value.toFixed(value % 1 === 0 ? 0 : 1)}</b>
            </article>
          );
        })}
      </div>
    </section>
  );
}

function DetailsTabs({
  active,
  setActive,
  debugMode,
  sections
}: {
  active: string;
  setActive: (value: string) => void;
  debugMode: boolean;
  sections: Record<string, { label: string; content: JSX.Element }>;
}) {
  const visibleKeys = Object.keys(sections).filter((key) => debugMode || !["json", "logs"].includes(key));
  return (
    <section className="detailsTabs cardPanel">
      <div className="tabBar">
        {visibleKeys.map((key) => (
          <button key={key} className={active === key ? "ghostButton active" : "ghostButton"} onClick={() => setActive(key)}>
            {sections[key].label}
          </button>
        ))}
      </div>
      <div className="tabSurface">{sections[visibleKeys.includes(active) ? active : visibleKeys[0]]?.content}</div>
    </section>
  );
}

function DashboardOverview({
  planResult,
  dashboard,
  trace,
  ddsTimeline,
  finalDecision,
  conflictResolution,
  onlineDevices
}: {
  planResult: PlanResult | null;
  dashboard: DashboardMap;
  trace: DashboardMap;
  ddsTimeline: DashboardMap[];
  finalDecision: DashboardMap;
  conflictResolution: DashboardMap;
  onlineDevices: number;
}) {
  const accepted = asMap(conflictResolution.acceptedSuggestion);
  const risk = asNumber(accepted.riskScore ?? finalDecision.riskScore, 0);
  const riskTone = risk > 0.55 ? "danger" : risk > 0.32 ? "warn" : "good";
  return (
    <section id="overview" className="cockpitHero cardPanel">
      <div className="cockpitHeroMain">
        <span className="eyebrow">Autonomous Collaboration Command Deck</span>
        <h1>无人设备自治协同指挥驾驶舱</h1>
        <p>{asText(trace.routeSelectionReason ?? finalDecision.reason, "输入任务后，系统将展示从自然语言意图到可解释路线决策的完整闭环。")}</p>
      </div>
      <div className="cockpitMetricGrid">
        <MetricCard label="当前任务" value={planResult?.missionId ?? "待生成"} note={asText(planResult?.structuredDsl?.taskType ?? planResult?.platformPlan?.intent?.taskType, "自然语言意图驱动")} />
        <MetricCard label="最终路线" value={asText(trace.finalRoute ?? finalDecision.route, "待决策")} note="基于风险/置信度/新鲜度" tone="good" />
        <MetricCard label="风险等级" value={risk ? percent(risk) : "-"} note={risk ? "安全硬约束参与决策" : "等待评估"} tone={riskTone} />
        <MetricCard label="协同置信度" value={percent(accepted.confidence ?? finalDecision.confidence)} note={asText(accepted.sourceId, "多 Agent 融合")} />
        <MetricCard label="在线设备数" value={onlineDevices || asText(dashboard.onlineDevices, "-")} note="Vehicle / UAV / RSU / Peer" />
        <MetricCard label="DDS 消息数" value={ddsTimeline.length || asText(dashboard.recentLogCount, "-")} note="发布订阅时间线" />
        <MetricCard label="重规划" value={asText(trace.replanningTriggered ?? conflictResolution.whetherReplanningTriggered, "false")} tone={(trace.replanningTriggered ?? conflictResolution.whetherReplanningTriggered) ? "warn" : "good"} />
        <MetricCard label="当前状态" value={planResult ? "闭环就绪" : "等待输入"} note={asText(planResult?.platformPlan?.executionStatus?.state, "simulation-ready")} />
      </div>
    </section>
  );
}

function VirtualTrafficSimulationPanel({
  scenarios,
  state,
  tickResult,
  ddsEvents,
  selectedScenario,
  setSelectedScenario,
  onStart,
  onTick,
  onStop
}: {
  scenarios: DashboardMap[];
  state: DashboardMap;
  tickResult: DashboardMap;
  ddsEvents: DashboardMap[];
  selectedScenario: string;
  setSelectedScenario: (value: string) => void;
  onStart: () => void;
  onTick: () => void;
  onStop: () => void;
}) {
  const world = asMap(state.world);
  const effectiveTickResult = Object.keys(tickResult).length ? tickResult : asMap(state.lastTickResult);
  const roadNetwork = asMap(world.roadNetwork);
  const nodes = asList(roadNetwork.nodes);
  const edges = asList(roadNetwork.edges);
  const routes = asList(roadNetwork.routes);
  const devices = asList(world.devices);
  const resultAdvice = asList(effectiveTickResult.agentAdvice);
  const coordinationAdvice = asList(effectiveTickResult.deviceCoordinationAdvice ?? effectiveTickResult.coordinationAdvice ?? state.deviceCoordinationAdvice ?? state.coordinationAdvice);
  const plannerResult = asMap(effectiveTickResult.plannerResult);
  const conflict = asMap(effectiveTickResult.conflictResolution);
  const finalDecision = asMap(effectiveTickResult.finalDecision);
  const currentTick = asText(state.currentTick ?? effectiveTickResult.tick, "0");
  const running = Boolean(state.running);
  const nodeById = new Map(nodes.map((node) => [asText(node.nodeId), node]));
  const finalRouteId = asText(finalDecision.route ?? world.finalRouteId, "");
  const finalRouteKey = finalRouteId.toLowerCase();

  const clampPercent = (value: unknown, min = 3, max = 97) => Math.max(min, Math.min(max, asNumber(value, 0)));

  const edgeEndpointText = (edge: DashboardMap) => `${asText(edge.fromNodeId)} ${asText(edge.toNodeId)} ${asText(edge.edgeId)} ${asText(edge.label)}`.toLowerCase();

  const hasBoth = (text: string, a: string[], b: string[]) =>
    a.some((token) => text.includes(token)) && b.some((token) => text.includes(token));

  const edgeMatchesFinalRoute = (edge: DashboardMap) => {
    if (!finalRouteKey) return false;

    const explicitRouteText = [
      edge.routeId,
      edge.routeIds,
      edge.routes,
      edge.route,
      edge.routeName,
      edge.pathId,
      edge.edgeId
    ]
      .map((item) => asText(item, ""))
      .join(" ")
      .toLowerCase();

    if (explicitRouteText.includes(finalRouteKey)) return true;

    const text = edgeEndpointText(edge);
    if (finalRouteKey.includes("emergency") || finalRouteKey.includes("green_wave") || finalRouteKey.includes("green")) {
      return (
        hasBoth(text, ["origin", "passenger", "start"], ["main", "junction"]) ||
        hasBoth(text, ["main", "junction"], ["emergency", "green"]) ||
        hasBoth(text, ["emergency", "green"], ["hospital", "destination", "target"])
      );
    }

    if (finalRouteKey.includes("elevated") || finalRouteKey.includes("detour")) {
      return (
        hasBoth(text, ["origin", "passenger", "start"], ["main", "junction"]) ||
        hasBoth(text, ["main", "junction"], ["elevated", "detour"]) ||
        hasBoth(text, ["elevated", "detour"], ["hospital", "destination", "target"])
      );
    }

    if (finalRouteKey.includes("main")) {
      return (
        hasBoth(text, ["origin", "passenger", "start"], ["main", "junction"]) ||
        hasBoth(text, ["main", "junction"], ["hospital", "destination", "target"]) ||
        hasBoth(text, ["main", "junction"], ["school"]) ||
        hasBoth(text, ["school"], ["hospital", "destination", "target"])
      );
    }

    return false;
  };

  const deviceKind = (device: DashboardMap) => {
    const type = asText(device.deviceType).toUpperCase();
    const id = asText(device.deviceId).toUpperCase();
    if (type.includes("UAV") || id.includes("UAV")) return "uav";
    if (type.includes("RSU") || id.includes("RSU")) return "rsu";
    if (type.includes("NEIGHBOR") || id.includes("PEER")) return "peer";
    return "vehicle";
  };

  const deviceIcon = (kind: string) => {
    if (kind === "uav") return "UAV";
    if (kind === "rsu") return "RSU";
    if (kind === "peer") return "PEER";
    return "CAR";
  };

  const deviceOffset = (device: DashboardMap, index: number) => {
    const kind = deviceKind(device);
    const x = asNumber(device.x);
    const y = asNumber(device.y);
    const clusterShift = [
      { dx: -1.2, dy: -1.2 },
      { dx: 1.2, dy: -1.2 },
      { dx: 1.2, dy: 1.2 },
      { dx: -1.2, dy: 1.2 }
    ][index % 4];

    const nearRight = x > 82;
    const nearLeft = x < 12;

    const base =
      kind === "uav"
        ? { dx: nearRight ? -8 : 0, dy: -9 }
        : kind === "rsu"
          ? { dx: -8, dy: 0 }
          : kind === "peer"
            ? { dx: nearRight ? -12 : 8, dy: -3 }
            : { dx: nearRight ? -9 : 0, dy: 8 };

    const destinationCluster = x > 70 && y < 45;
    const destinationExtra =
      destinationCluster && kind === "uav"
        ? { dx: -7, dy: -11 }
        : destinationCluster && kind === "peer"
          ? { dx: -9, dy: -2 }
          : destinationCluster && kind === "vehicle"
            ? { dx: -4, dy: 11 }
            : { dx: 0, dy: 0 };

    return {
      x: clampPercent(x + base.dx + destinationExtra.dx + clusterShift.dx, nearLeft ? 5 : 3, nearRight ? 90 : 97),
      y: clampPercent(y + base.dy + destinationExtra.dy + clusterShift.dy, 5, 95)
    };
  };

  const nodeLabelStyle = (node: DashboardMap) => {
    const id = asText(node.nodeId).toLowerCase();
    const label = asText(node.label).toLowerCase();
    if (id.includes("hospital") || id.includes("destination") || label.includes("hospital")) {
      return { transform: "translate(-50%, 18px)" };
    }
    if (id.includes("elevated") || label.includes("elevated")) {
      return { transform: "translate(-50%, -30px)" };
    }
    if (id.includes("school") || label.includes("school")) {
      return { transform: "translate(-50%, 20px)" };
    }
    return { transform: "translate(-50%, 18px)" };
  };

  const nodeDisplayLabel = (node: DashboardMap) => {
    const id = `${asText(node.nodeId)} ${asText(node.label)}`.toLowerCase();
    if (id.includes("origin") || id.includes("passenger") || id.includes("start")) return "乘客起点";
    if (id.includes("main") || id.includes("junction")) return "主路路口";
    if (id.includes("elevated") || id.includes("detour")) return "高架绕行";
    if (id.includes("school")) return "学校区域";
    if (id.includes("emergency") || id.includes("green")) return "应急绿波";
    if (id.includes("hospital") || id.includes("destination") || id.includes("target")) return "医院终点";
    return asText(node.label);
  };

  const deviceDisplayId = (device: DashboardMap) => {
    const raw = asText(device.deviceId).replace("-CQ-", "-").trim();
    const firstPart = raw.split(/\s+/)[0];
    return firstPart || raw;
  };

  return (
    <section id="simulation" className="simulationDeck cardPanel">
      <div className="panelHeader">
        <div>
          <span className="eyebrow">Virtual Traffic Simulation</span>
          <h3>二维虚拟交通协同仿真环境</h3>
        </div>
        <StatusBadge tone={running ? "good" : "warn"}>{running ? `TICK ${currentTick}` : "STOPPED"}</StatusBadge>
      </div>

      <div className="simulationControls">
        <select value={selectedScenario} onChange={(event) => setSelectedScenario(event.target.value)}>
          {scenarios.map((scenario) => (
            <option key={asText(scenario.scenarioId)} value={asText(scenario.scenarioId)}>
              {asText(scenario.name, asText(scenario.scenarioId))}
            </option>
          ))}
        </select>
        <button onClick={onStart}>启动仿真</button>
        <button className="ghostButton" onClick={onTick} disabled={!running}>推进 tick</button>
        <button className="dangerButton" onClick={onStop}>停止</button>
      </div>

      <div className="simulationGrid">
        <div className="topologyCanvas">
          <svg className="topologySvg" viewBox="0 0 100 100" preserveAspectRatio="none" aria-label="virtual traffic topology">
            {edges.map((edge) => {
              const from = nodeById.get(asText(edge.fromNodeId));
              const to = nodeById.get(asText(edge.toNodeId));
              if (!from || !to) return null;
              const congestion = asNumber(edge.congestionLevel);
              const risk = asNumber(edge.riskScore);
              const tone = risk > 0.55 ? "danger" : congestion > 0.6 ? "warn" : "normal";
              return (
                <line
                  key={asText(edge.edgeId)}
                  className={`roadEdgeLine ${tone} ${finalRouteId && !edgeMatchesFinalRoute(edge) ? "nonFinal" : ""}`}
                  x1={clampPercent(from.x, 0, 100)}
                  y1={clampPercent(from.y, 0, 100)}
                  x2={clampPercent(to.x, 0, 100)}
                  y2={clampPercent(to.y, 0, 100)}
                  vectorEffect="non-scaling-stroke"
                >
                  <title>{`${asText(edge.edgeId)} congestion=${percent(congestion)} risk=${percent(risk)}`}</title>
                </line>
              );
            })}

            {finalRouteId ? edges.filter(edgeMatchesFinalRoute).map((edge) => {
              const from = nodeById.get(asText(edge.fromNodeId));
              const to = nodeById.get(asText(edge.toNodeId));
              if (!from || !to) return null;
              return (
                <line
                  key={`final-${asText(edge.edgeId)}`}
                  className="finalRouteLine"
                  x1={clampPercent(from.x, 0, 100)}
                  y1={clampPercent(from.y, 0, 100)}
                  x2={clampPercent(to.x, 0, 100)}
                  y2={clampPercent(to.y, 0, 100)}
                  vectorEffect="non-scaling-stroke"
                >
                  <title>{`最终路线 ${finalRouteId}: ${asText(edge.edgeId)}`}</title>
                </line>
              );
            }) : null}
          </svg>

          {nodes.map((node) => (
            <div
              key={asText(node.nodeId)}
              className="roadNode"
              style={{ left: `${clampPercent(node.x)}%`, top: `${clampPercent(node.y)}%` }}
              title={`${asText(node.nodeId)} · ${asText(node.label)}`}
            >
              <span style={nodeLabelStyle(node)}>{nodeDisplayLabel(node)}</span>
            </div>
          ))}

          {devices.map((device, index) => {
            const kind = deviceKind(device);
            const position = deviceOffset(device, index);
            const deviceLabel = deviceDisplayId(device);
            return (
              <div
                key={asText(device.deviceId)}
                className={`simDevice ${kind} ${position.x > 84 ? "nearRight" : ""} ${position.x < 12 ? "nearLeft" : ""} ${position.y < 12 ? "nearTop" : ""} ${position.y > 88 ? "nearBottom" : ""}`}
                style={{ left: `${position.x}%`, top: `${position.y}%` }}
                title={`${asText(device.deviceId)} · ${asText(device.deviceType)} · ${asText(device.status)} · tick ${currentTick}`}
              >
                <span className="deviceIcon">{deviceIcon(kind)}</span>
                <span className="deviceLabel">{deviceLabel}</span>
              </div>
            );
          })}
        </div>

        <div className="simulationSide">
          <article>
            <h4>当前自然语言任务</h4>
            <p>{asText(world.naturalLanguageTask, "启动场景后显示任务意图")}</p>
          </article>
          <article>
            <h4>候选路线</h4>
            {routes.map((route) => (
              <div key={asText(route.routeId)} className={`routeStat ${asText(route.routeId).toLowerCase() === finalRouteKey ? "active" : ""}`}>
                <strong>{asText(route.routeId)}</strong>
                <span>拥堵 {percent(route.congestionLevel)} / 风险 {percent(route.riskScore)} / {asText(route.estimatedMinutes)} min</span>
              </div>
            ))}
          </article>
          <article>
            <h4>最终路线</h4>
            <strong>{asText(finalDecision.route ?? world.finalRouteId, "等待 tick")}</strong>
            <p>{short(finalDecision.explanation ?? world.finalExplanation, 180)}</p>
          </article>
        </div>
      </div>

      <div className="simulationEvidenceGrid">
        <article>
          <h4>DDS 消息时间线</h4>
          <div className="compactTimeline">
            {ddsEvents.slice(-8).map((event, index) => (
              <div key={`${asText(event.topic)}-${index}`}>
                <span>{asText(event.topic)}</span>
                <strong>{asText(event.sourceId)}</strong>
                <small>{short(asMap(event.payload).explanation ?? asMap(event.payload).missionStatus ?? asMap(event.payload).road_event, 90)}</small>
              </div>
            ))}
          </div>
        </article>
        <article>
          <h4>决策 Agent：路线规划与设备协同建议</h4>
          <p className="simulationHint">
            Agent 读取 DDS/DSL 数据，动态评估风险、拥堵、队列和置信度，然后规划路线并向目标无人设备发布协同建议。
          </p>
          <div className="compactTimeline coordinationAdviceList">
            {(coordinationAdvice.length ? coordinationAdvice : resultAdvice).map((advice, index) => {
              const evidence = asMap(advice.evidence);
              const sourceDevices = asText(advice.sourceDevices, asText(advice.role, "DDS 数据"));
              const target = asText(advice.targetDevice, asText(advice.agentId ?? advice.nodeId, "协同设备"));
              const recommendation = asText(advice.recommendation ?? advice.explanation ?? advice.advice, "基于 DDS/DSL 证据生成协同建议");
              return (
                <div key={`${target}-${asText(advice.recommendationType)}-${index}`}>
                  <span>{sourceDevices} → {target}</span>
                  <strong>{asText(advice.recommendationType, asText(advice.route, "设备协同建议"))}</strong>
                  <small>{short(recommendation, 120)}</small>
                  <small>
                    route {asText(advice.route, "-")} · score {asText(advice.computedScore, "-")} · risk {percent(evidence.selectedRouteRisk)} · congestion {percent(evidence.selectedRouteCongestion)}
                  </small>
                </div>
              );
            })}
          </div>
        </article>
        <article>
          <h4>Planner 路线对比</h4>
          <div className="compactTimeline">
            {["dijkstraBaseline", "greedyBaseline", "riskAware", "multiObjective"].map((key) => {
              const plan = asMap(plannerResult[key]);
              return (
                <div key={key}>
                  <span>{key}</span>
                  <strong>{asText(plan.routeId, "-")}</strong>
                  <small>{short(plan.explanation, 90)}</small>
                </div>
              );
            })}
          </div>
        </article>
        <article>
          <h4>ConflictResolver</h4>
          <div className="detailGridPanel">
            <article><span>conflict</span><strong>{asText(conflict.conflictType, "NONE")}</strong></article>
            <article><span>replan</span><strong>{asText(conflict.whetherReplanningTriggered, "false")}</strong></article>
            <article><span>confidence</span><strong>{percent(conflict.confidence)}</strong></article>
          </div>
          <p>{short(conflict.decisionReason, 180)}</p>
        </article>
      </div>
    </section>
  );
}

function RuntimeCollaborationPanel({
  profile,
  topics,
  result,
  running,
  onRun
}: {
  profile: DashboardMap;
  topics: DashboardMap[];
  result: DashboardMap;
  running: boolean;
  onRun: () => void;
}) {
  const finalDecision = asMap(result.finalDecision);
  const finalPayload = asMap(finalDecision.payload);
  const nativeFrames = asList(result.nativeProtocolFrames);
  const conflicts = asList(result.conflictEvents);
  const replans = asList(result.replanningRequests);
  return (
    <section className="cardPanel">
      <div className="panelHeader">
        <div>
          <span className="eyebrow">Backend Runtime</span>
          <h3>真实后端多 Agent 通信</h3>
        </div>
        <StatusBadge tone={result.ok ? "good" : "warn"}>{asText(profile.runtimeMode, "local_tcp_dds_compatible")}</StatusBadge>
      </div>
      <div className="detailGridPanel">
        <article><span>状态频率</span><strong>{asText(profile.defaultStatusIntervalMs, "500")}ms</strong></article>
        <article><span>Agent 数</span><strong>{asList(profile.agents).length || 4}</strong></article>
        <article><span>Topic/IDL</span><strong>{topics.length}</strong></article>
        <article><span>消息数</span><strong>{asText(result.messageCount, "-")}</strong></article>
        <article><span>冲突事件</span><strong>{conflicts.length}</strong></article>
        <article><span>重规划</span><strong>{replans.length}</strong></article>
      </div>
      <div className="commandBar">
        <button onClick={onRun} disabled={running}>{running ? "运行中" : "运行一次后端协同"}</button>
        <span className="supportText">前端只触发和观察，通信、冲突消解、重规划都在后端完成。</span>
      </div>
      <div className="compactTimeline">
        {nativeFrames.slice(0, 3).map((frame, index) => {
          const payload = asMap(frame.payload);
          const native = asMap(payload.native);
          return (
            <div key={`${asText(frame.sourceId)}-${index}`}>
              <span>{asText(frame.sourceId)}</span>
              <strong>{asText(native.protocol)}</strong>
              <small>{short(native, 120)}</small>
            </div>
          );
        })}
        {result.ok ? (
          <div>
            <span>{asText(finalDecision.sourceId, "COORDINATOR-CQ-001")}</span>
            <strong>{asText(finalPayload.selectedStrategy)}</strong>
            <small>{short(finalPayload.explanation, 140)}</small>
          </div>
        ) : null}
      </div>
    </section>
  );
}

export function WorkbenchPage() {
  const inputText = useAppStore((state) => state.inputText);
  const parseResult = useAppStore((state) => state.parseResult);
  const planResult = useAppStore((state) => state.planResult);
  const loading = useAppStore((state) => state.loading);
  const executing = useAppStore((state) => state.executing);
  const devices = useAppStore((state) => state.devices);
  const preferences = useAppStore((state) => state.preferences);
  const debugMode = useAppStore((state) => state.debugMode);
  const demoMode = useAppStore((state) => state.demoMode);
  const streamLogs = useAppStore((state) => state.streamLogs);
  const setInputText = useAppStore((state) => state.setInputText);
  const analyzeAndPlan = useAppStore((state) => state.analyzeAndPlan);
  const executePlan = useAppStore((state) => state.executePlan);
  const pauseMission = useAppStore((state) => state.pauseMission);
  const resumeMission = useAppStore((state) => state.resumeMission);
  const stopMission = useAppStore((state) => state.stopMission);
  const topicAction = useAppStore((state) => state.topicAction);

  const recognitionRef = useRef<SpeechRecognition | null>(null);
  const [voiceState, setVoiceState] = useState<"idle" | "listening" | "unsupported">("idle");
  const [voiceMessage, setVoiceMessage] = useState("可直接口述任务。");
  const [selectedTemplate, setSelectedTemplate] = useState("");
  const [scenarios, setScenarios] = useState<DashboardMap[]>([]);
  const [simulationScenarios, setSimulationScenarios] = useState<DashboardMap[]>([]);
  const [selectedSimulationScenario, setSelectedSimulationScenario] = useState("hospital-emergency");
  const [simulationState, setSimulationState] = useState<DashboardMap>({});
  const [simulationTick, setSimulationTick] = useState<DashboardMap>({});
  const [simulationDdsEvents, setSimulationDdsEvents] = useState<DashboardMap[]>([]);
  const [runtimeProfile, setRuntimeProfile] = useState<DashboardMap>({});
  const [runtimeTopics, setRuntimeTopics] = useState<DashboardMap[]>([]);
  const [runtimeResult, setRuntimeResult] = useState<DashboardMap>({});
  const [runtimeRunning, setRuntimeRunning] = useState(false);
  const [dashboard, setDashboard] = useState<DashboardMap>({});
  const [activeTab, setActiveTab] = useState("dsl");
  const [faultType, setFaultType] = useState("DEVICE_OFFLINE");
  const [faultTarget, setFaultTarget] = useState("UAV-CQ-007");
  const [reportMarkdown, setReportMarkdown] = useState("");

  const locale = preferences.locale;
  const speechLang = localeMeta[locale].speechLang;

  useEffect(() => () => recognitionRef.current?.stop(), []);

  useEffect(() => {
    backend.listScenarios().then((data) => setScenarios(data.scenarios ?? [])).catch(() => setScenarios([]));
    backend.listSimulationScenarios().then((data) => {
      const items = data.scenarios ?? [];
      setSimulationScenarios(items);
      if (items.length && !selectedSimulationScenario) {
        setSelectedSimulationScenario(asText(items[0].scenarioId, "hospital-emergency"));
      }
    }).catch(() => setSimulationScenarios([]));
    backend.simulationState().then((data) => {
      setSimulationState(data);
      setSimulationDdsEvents(asList(data.recentDdsMessages));
    }).catch(() => setSimulationState({}));
    backend.runtimeProfile().then((data) => setRuntimeProfile(data)).catch(() => setRuntimeProfile({}));
    backend.runtimeTopics().then((data) => setRuntimeTopics(data.topics ?? [])).catch(() => setRuntimeTopics([]));
    backend.dashboard().then((data) => setDashboard(data.dashboard ?? {})).catch(() => setDashboard({}));
  }, [planResult?.missionId, selectedSimulationScenario]);

  const trace = asMap(planResult?.dashboardTrace);
  const collaboration = planResult?.collaboration;
  const platformPlan = planResult?.platformPlan;
  const finalDecision = asMap(collaboration?.finalDecision);
  const conflictDetection = asMap(trace.conflictDetection);
  const conflictResolution = asMap(trace.conflictResolution);
  const plannerComparison = asMap(trace.plannerComparison);
  const policyComparison = asMap(trace.policyComparison);
  const optimization = asMap(trace.policyOptimization);
  const optimizationBefore = asMap(asMap(optimization.before).metrics);
  const optimizationAfter = asMap(asMap(optimization.after).metrics);
  const dslContract = trace.dslTaskContract ?? planResult?.structuredDsl ?? platformPlan?.structuredDsl ?? {};
  const idlMappings = asList(trace.idlDdsTopicMappings).length ? asList(trace.idlDdsTopicMappings) : asList(collaboration?.idlMappings);
  const ddsTimeline = asList(trace.ddsTimeline).length ? asList(trace.ddsTimeline) : asList(planResult?.ddsSimulation);
  const allAdvice = asList(trace.allAgentSuggestions).length ? asList(trace.allAgentSuggestions) : asList(collaboration?.deviceAdvice);
  const agentSuggestions = asMap(trace.agentSuggestions);
  const vehicleAdvice = asList(agentSuggestions.vehicle).length ? asList(agentSuggestions.vehicle) : allAdvice.filter((item) => asText(item.nodeId).includes("CAR"));
  const droneAdvice = asList(agentSuggestions.drone).length ? asList(agentSuggestions.drone) : allAdvice.filter((item) => asText(item.nodeId).includes("UAV"));
  const roadsideAdvice = asList(agentSuggestions.roadside).length ? asList(agentSuggestions.roadside) : allAdvice.filter((item) => asText(item.nodeId).includes("RSU"));
  const neighborAdvice = asList(agentSuggestions.neighborVehicle).length ? asList(agentSuggestions.neighborVehicle) : allAdvice.filter((item) => asText(item.role).includes("邻"));
  const rejectedRoutes = asList(trace.rejectedRoutes).length ? asList(trace.rejectedRoutes) : asList(conflictResolution.rejectedSuggestions);
  const charts = asList(trace.experimentMetricCharts);
  const replanningTimeline = asList(trace.replanningTimeline);
  const onlineDevices = devices.filter((device) => device.online).length;

  const parsedIntent = useMemo(() => parseResult ?? asMap(trace.parsedIntent), [parseResult, trace.parsedIntent]);

  const startVoiceInput = async () => {
    const Recognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!Recognition || !window.isSecureContext) {
      setVoiceState("unsupported");
      setVoiceMessage("当前浏览器环境不支持语音输入。");
      return;
    }
    try {
      recognitionRef.current?.abort();
    } catch {
      recognitionRef.current?.stop();
    }
    const recognition = new Recognition();
    recognition.lang = speechLang;
    recognition.continuous = false;
    recognition.interimResults = true;
    recognition.onresult = (event) => {
      const transcript = Array.from({ length: event.results.length })
        .map((_, index) => event.results[index]?.[0]?.transcript ?? "")
        .join("")
        .trim();
      if (transcript) setInputText(transcript);
    };
    recognition.onerror = () => {
      setVoiceState("idle");
      setVoiceMessage("语音输入失败，请检查权限。");
    };
    recognition.onend = () => {
      setVoiceState("idle");
      recognitionRef.current = null;
      setVoiceMessage("语音输入结束。");
    };
    recognitionRef.current = recognition;
    recognition.start();
    setVoiceState("listening");
    setVoiceMessage("正在聆听。");
  };

  const runScenario = async (scenario: DashboardMap) => {
    const text = asText(scenario.defaultIntentText, "");
    setSelectedTemplate(asText(scenario.scenarioId, ""));
    setInputText(text);
    await analyzeAndPlan();
  };

  const injectFault = async () => {
    await backend.injectFault({ faultType, target: faultTarget });
    await analyzeAndPlan();
  };

  const generateReport = async () => {
    const result = await backend.report(planResult?.missionId);
    setReportMarkdown(result.markdown ?? "");
  };

  const refreshSimulation = async () => {
    const [state, events] = await Promise.all([
      backend.simulationState(),
      backend.simulationDdsEvents()
    ]);
    setSimulationState(state);
    setSimulationDdsEvents(events.events ?? asList(state.recentDdsMessages));
  };

  const startSimulation = async () => {
    const state = await backend.startSimulation(selectedSimulationScenario || "hospital-emergency");
    setSimulationState(state);
    setSimulationTick(asMap(state.lastTickResult));
    setSimulationDdsEvents(asList(state.recentDdsMessages));
  };

  const tickSimulation = async () => {
    const result = await backend.tickSimulation();
    setSimulationTick(result);
    await refreshSimulation();
  };

  const stopSimulation = async () => {
    await backend.stopSimulation();
    await refreshSimulation();
  };

  const runBackendRuntime = async () => {
    setRuntimeRunning(true);
    try {
      const result = await backend.runRuntime(500, 6);
      setRuntimeResult(result);
    } finally {
      setRuntimeRunning(false);
    }
  };

  const detailsSections = {
    dsl: {
      label: "DSL 详情",
      content: (
        <div className="detailGridPanel">
          {Object.entries(asMap(dslContract)).slice(0, 14).map(([key, value]) => (
            <article key={key}><span>{key}</span><strong>{asText(value)}</strong></article>
          ))}
        </div>
      )
    },
    dds: {
      label: "DDS Topic",
      content: (
        <div className="mappingList">
          {idlMappings.map((item, index) => (
            <article key={`${asText(item.topic)}-${index}`} className="mappingRow">
              <span>{asText(item.topic)}</span>
              <strong>{asText(item.idlModule)}.{asText(item.idlType)}</strong>
              <small>{asText(item.qos)}</small>
            </article>
          ))}
          {debugMode ? <DDSMessageTimeline items={ddsTimeline} /> : null}
        </div>
      )
    },
    agents: {
      label: "Agent 消息",
      content: <div className="agentGrid"><AgentSuggestionCard title="无人车" items={vehicleAdvice} /><AgentSuggestionCard title="无人机" items={droneAdvice} /><AgentSuggestionCard title="路侧节点" items={roadsideAdvice} /><AgentSuggestionCard title="邻车" items={neighborAdvice} /></div>
    },
    conflict: {
      label: "冲突消解",
      content: <ConflictResolutionPanel detection={conflictDetection} resolution={conflictResolution} rejected={rejectedRoutes} />
    },
    planner: {
      label: "Planner 对比",
      content: <PlannerComparisonPanel algorithms={asList(plannerComparison.algorithms)} />
    },
    policy: {
      label: "Policy 对比",
      content: (
        <div className="policyDetailStack">
          <PolicyComparisonPanel policies={asList(policyComparison.policyResults)} whyBest={policyComparison.whyBestPolicy} />
          <div className="optimizationGrid">
            <article><small>优化前</small><strong>reward {asText(optimizationBefore.averageReward)}</strong><span>risk {asText(optimizationBefore.averageSafetyRisk)}</span></article>
            <article><small>优化后</small><strong>reward {asText(optimizationAfter.averageReward)}</strong><span>risk {asText(optimizationAfter.averageSafetyRisk)}</span></article>
            <article><small>训练状态</small><strong>heuristic</strong><span>{asText(trace.trainingStatus, "no RL training")}</span></article>
          </div>
        </div>
      )
    },
    metrics: {
      label: "指标",
      content: <EvaluationMetricsPanel items={charts} />
    },
    json: {
      label: "原始 JSON",
      content: <JsonInspector value={{ parseResult: parsedIntent, planResult }} title="完整接口响应" />
    },
    logs: {
      label: "日志",
      content: (
        <div id="logs" className="debugLogPanel">
          {streamLogs.length ? streamLogs.slice(-120).map((line, index) => <div key={`${line}-${index}`}>{line}</div>) : "暂无日志"}
        </div>
      )
    }
  };

  return (
    <div className={debugMode ? "cockpitPage debugOn" : "cockpitPage"}>
      <DashboardOverview
        planResult={planResult}
        dashboard={dashboard}
        trace={trace}
        ddsTimeline={ddsTimeline}
        finalDecision={finalDecision}
        conflictResolution={conflictResolution}
        onlineDevices={onlineDevices}
      />

      <WorkflowStepper replanning={Boolean(trace.replanningTriggered ?? conflictResolution.whetherReplanningTriggered)} />

      <section className="missionDeck">
        <MissionInputPanel
          inputText={inputText}
          setInputText={setInputText}
          selectedTemplate={selectedTemplate}
          setSelectedTemplate={setSelectedTemplate}
          onVoice={startVoiceInput}
          voiceState={voiceState}
          voiceMessage={voiceMessage}
          onAnalyze={() => void analyzeAndPlan()}
          onExecute={() => void executePlan()}
          loading={loading}
          executing={executing}
          hasPlan={Boolean(planResult)}
        />
        {!demoMode ? (
          <section className="scenarioRail cardPanel">
            <div className="panelHeader"><h3>Scenario Library</h3><StatusBadge>{String(scenarios.length)}</StatusBadge></div>
            <div className="scenarioGrid">{scenarios.slice(0, 6).map((scenario) => <ScenarioCard key={asText(scenario.scenarioId)} scenario={scenario} onRun={runScenario} />)}</div>
          </section>
        ) : null}
      </section>

      <section className="mainWorkspace">
        <div className="mapPanel cardPanel">
          <div className="panelHeader">
            <div><span className="eyebrow">Map / Route</span><h3>路线态势与候选路线</h3></div>
            <StatusBadge tone="good">{asText(planResult?.ddsRuntimeMode ?? platformPlan?.ddsRuntimeMode, "SIMULATED_DDS")}</StatusBadge>
          </div>
          <MissionMap overlay={planResult?.mapOverlay} />
          <PlannerComparisonPanel algorithms={asList(plannerComparison.algorithms)} />
        </div>

        <aside className="decisionPanel cardPanel">
          <RouteSummaryCard trace={trace} finalDecision={finalDecision} conflictResolution={conflictResolution} />
          <div className="riskCallout">
            <strong>风险提示</strong>
            <p>{rejectedRoutes.length ? rejectedRoutes.map((item) => `${asText(item.route)} 被降权：${asText(item.rejectionReason ?? item.reason)}`).join("；") : asText(platformPlan?.riskWarnings, "暂无高风险路线。")}</p>
          </div>
          <AgentSuggestionCard title="Agent 建议摘要" items={[...vehicleAdvice, ...droneAdvice, ...roadsideAdvice, ...neighborAdvice]} />
          <DDSMessageTimeline items={ddsTimeline} compact />
        </aside>
      </section>

      <VirtualTrafficSimulationPanel
        scenarios={simulationScenarios}
        state={simulationState}
        tickResult={simulationTick}
        ddsEvents={simulationDdsEvents}
        selectedScenario={selectedSimulationScenario}
        setSelectedScenario={setSelectedSimulationScenario}
        onStart={() => void startSimulation()}
        onTick={() => void tickSimulation()}
        onStop={() => void stopSimulation()}
      />

      <RuntimeCollaborationPanel
        profile={runtimeProfile}
        topics={runtimeTopics}
        result={runtimeResult}
        running={runtimeRunning}
        onRun={() => void runBackendRuntime()}
      />

      <DetailsTabs active={activeTab} setActive={setActiveTab} debugMode={debugMode} sections={detailsSections} />

      {debugMode ? (
        <section className="debugToolbox cardPanel">
          <div className="panelHeader">
            <div><span className="eyebrow">Debug Mode</span><h3>调试工具与底层产物</h3></div>
            <StatusBadge tone="warn">DEBUG</StatusBadge>
          </div>
          <div className="debugToolGrid">
            <article>
              <h4>故障注入</h4>
              <select value={faultType} onChange={(event) => setFaultType(event.target.value)}>
                {["DEVICE_OFFLINE", "LOW_BATTERY", "LINK_DEGRADED", "DDS_PUBLISH_FAILED", "HIGH_CONGESTION", "ROUTE_BLOCKED", "SENSOR_UNAVAILABLE"].map((type) => <option key={type} value={type}>{type}</option>)}
              </select>
              <input value={faultTarget} onChange={(event) => setFaultTarget(event.target.value)} />
              <button onClick={() => void injectFault()}>注入并重算</button>
            </article>
            <article>
              <h4>任务控制</h4>
              <button className="ghostButton" onClick={pauseMission} disabled={!planResult}>暂停</button>
              <button className="ghostButton" onClick={resumeMission} disabled={!planResult}>继续</button>
              <button className="dangerButton" onClick={stopMission} disabled={!planResult}>停止</button>
            </article>
            <article>
              <h4>报告 / 产物</h4>
              <button onClick={() => void generateReport()} disabled={!planResult}>生成报告</button>
              <button className="ghostButton" onClick={() => void navigator.clipboard?.writeText(reportMarkdown)}>复制报告</button>
              {reportMarkdown ? <pre>{reportMarkdown}</pre> : null}
            </article>
            <article>
              <h4>Topic 快测</h4>
              {(planResult?.publishTopics ?? []).slice(0, 4).map((topic) => (
                <button key={topic.name} className="ghostButton" onClick={() => void topicAction(topic.name, "publish", "MISSION_EVENT:READY")}>{topic.name}</button>
              ))}
            </article>
          </div>
          <JsonInspector value={{ dslPreview: planResult?.dslPreview, javaCode: planResult?.javaCode, platformPlan }} title="生成产物与平台计划" />
        </section>
      ) : null}
    </div>
  );
}
