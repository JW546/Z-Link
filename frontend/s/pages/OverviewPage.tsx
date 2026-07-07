import { useAppStore } from "../store/useAppStore";
import { asList, asMap, asNumber, asText, percent } from "./pageData";

export function OverviewPage() {
  const planResult = useAppStore((state) => state.planResult);
  const missions = useAppStore((state) => state.missions);
  const devices = useAppStore((state) => state.devices);
  const streamLogs = useAppStore((state) => state.streamLogs);
  const health = useAppStore((state) => state.health);

  const trace = asMap(planResult?.dashboardTrace);
  const finalDecision = asMap(planResult?.collaboration?.finalDecision);
  const conflictResolution = asMap(trace.conflictResolution);
  const acceptedSuggestion = asMap(conflictResolution.acceptedSuggestion);
  const ddsTimeline = asList(trace.ddsTimeline).length ? asList(trace.ddsTimeline) : asList(planResult?.ddsSimulation);
  const currentMission = planResult?.missionId ?? missions[0]?.id ?? "等待任务生成";
  const riskScore = asNumber(acceptedSuggestion.riskScore ?? finalDecision.riskScore, 0);
  const onlineDevices = devices.filter((device) => device.online || device.available).length;
  const finalRoute = asText(trace.finalRoute ?? acceptedSuggestion.route ?? finalDecision.route, "等待 Agent 决策");

  return (
    <div className="stackPage routePage">
      <section className="cardPanel">
        <div className="panelHeader">
          <div>
            <span className="eyebrow">System Overview</span>
            <h3>系统总览</h3>
          </div>
          <span className="softBadge">{health?.mode ?? planResult?.ddsRuntimeMode ?? "SIMULATED_DDS"}</span>
        </div>
        <div className="cockpitMetricGrid">
          <article className="metricCard good">
            <span>当前任务</span>
            <strong>{currentMission}</strong>
            <small>{asText(planResult?.structuredDsl?.taskType ?? planResult?.platformPlan?.intent?.taskType, "自然语言意图待解析")}</small>
          </article>
          <article className="metricCard good">
            <span>最终路线</span>
            <strong>{finalRoute}</strong>
            <small>由 Agent 建议、冲突消解和多目标评估共同决定</small>
          </article>
          <article className={riskScore > 0.55 ? "metricCard danger" : riskScore > 0.32 ? "metricCard warn" : "metricCard good"}>
            <span>风险等级</span>
            <strong>{riskScore ? percent(riskScore) : "待评估"}</strong>
            <small>{asText(conflictResolution.decisionReason, "等待路线风险与约束证据")}</small>
          </article>
          <article className="metricCard">
            <span>协同置信度</span>
            <strong>{percent(acceptedSuggestion.confidence ?? finalDecision.confidence, "待融合")}</strong>
            <small>{asText(acceptedSuggestion.sourceId, "多设备 Agent 融合")}</small>
          </article>
          <article className="metricCard">
            <span>在线设备数</span>
            <strong>{onlineDevices}</strong>
            <small>Vehicle / UAV / RSU / Peer Agent</small>
          </article>
          <article className="metricCard">
            <span>DDS 消息数</span>
            <strong>{ddsTimeline.length || streamLogs.length}</strong>
            <small>发布订阅链路与运行事件</small>
          </article>
          <article className="metricCard">
            <span>重规划</span>
            <strong>{asText(trace.replanningTriggered ?? conflictResolution.whetherReplanningTriggered, "false")}</strong>
            <small>由冲突消解或故障注入触发</small>
          </article>
          <article className="metricCard">
            <span>系统状态</span>
            <strong>{health?.ok ? "API 在线" : "本地/模拟"}</strong>
            <small>{asText(planResult?.platformPlan?.executionStatus?.state, "simulation-ready")}</small>
          </article>
        </div>
      </section>

      <section className="comparisonPanel cardPanel">
        <div className="panelHeader">
          <div>
            <span className="eyebrow">Core Flow</span>
            <h3>核心协同链路</h3>
          </div>
        </div>
        <div className="routeFlowGrid">
          {["自然语言意图", "DSL 任务契约", "IDL/DDS 发布订阅", "多设备 Agent 自主协同", "冲突消解", "可解释路线决策"].map((item, index) => (
            <article key={item}>
              <span>{String(index + 1).padStart(2, "0")}</span>
              <strong>{item}</strong>
              <small>{index < 2 ? "任务语义进入契约层" : index < 4 ? "消息与设备建议进入协同层" : "决策证据进入解释与评估层"}</small>
            </article>
          ))}
        </div>
      </section>
    </div>
  );
}
