import { useAppStore } from "../store/useAppStore";
import { asList, asMap, asText } from "./pageData";

interface RouteLogItem {
  type: string;
  message: string;
  status?: string;
}

export function LogsPage() {
  const streamLogs = useAppStore((state) => state.streamLogs);
  const planResult = useAppStore((state) => state.planResult);
  const executionTimeline = planResult?.executionTimeline ?? planResult?.platformPlan?.executionTimeline ?? [];
  const communicationLogs = planResult?.communicationLogs ?? planResult?.platformPlan?.communicationLogs ?? [];
  const ddsSimulation = asList(planResult?.ddsSimulation ?? planResult?.platformPlan?.ddsSimulation);
  const trace = asMap(planResult?.dashboardTrace);
  const replanningTimeline = asList(trace.replanningTimeline);
  const composedLogs: RouteLogItem[] = [
    ...streamLogs.map((message) => ({ type: "Runtime", message })),
    ...executionTimeline.map((item) => ({ type: item.stage || "Execution", message: item.message, status: item.status })),
    ...communicationLogs.map((item) => ({ type: "DDS", message: asText(item.message ?? item.topic ?? item.phase), status: asText(item.status) })),
    ...ddsSimulation.map((item) => ({ type: "DDS Message", message: asText(item.message ?? item.topic ?? item.phase), status: asText(item.status ?? item.topic) })),
    ...replanningTimeline.map((item) => ({ type: "Replan", message: asText(item.message ?? item.reason ?? item.action), status: asText(item.status) }))
  ].slice(-160);

  const fallbackLogs: RouteLogItem[] = [
    { type: "Task", message: "等待自然语言任务生成 DSL 任务契约。" },
    { type: "DDS", message: "任务运行后展示 DDS 发布订阅消息。" },
    { type: "Tick", message: "二维虚拟交通仿真 tick 推进后展示 Agent 决策事件。" },
    { type: "Replan", message: "冲突消解触发时记录重规划请求与最终路线发布。" }
  ];
  const visibleLogs = composedLogs.length ? composedLogs : fallbackLogs;

  return (
    <div className="stackPage routePage">
      <section className="cardPanel">
        <div className="panelHeader">
          <div>
            <span className="eyebrow">Runtime Logs</span>
            <h3>运行日志</h3>
          </div>
          <span className="softBadge">{visibleLogs.length} events</span>
        </div>
        <div className="logSummaryGrid">
          <article className="metricCard">
            <span>任务生成</span>
            <strong>{visibleLogs.filter((item) => item.type.includes("Task") || item.message.includes("任务")).length}</strong>
            <small>自然语言意图与 DSL 契约</small>
          </article>
          <article className="metricCard">
            <span>DDS 消息</span>
            <strong>{visibleLogs.filter((item) => item.type.includes("DDS")).length}</strong>
            <small>发布、订阅、Topic 映射</small>
          </article>
          <article className="metricCard">
            <span>Agent 决策</span>
            <strong>{visibleLogs.filter((item) => item.message.includes("Agent") || item.message.includes("决策")).length}</strong>
            <small>路线建议与设备协同建议</small>
          </article>
          <article className="metricCard">
            <span>重规划请求</span>
            <strong>{visibleLogs.filter((item) => item.type.includes("Replan") || item.message.includes("重规划")).length}</strong>
            <small>冲突消解后的最终路线发布</small>
          </article>
        </div>
      </section>

      <section className="cardPanel">
        <div className="panelHeader">
          <div>
            <span className="eyebrow">Event Timeline</span>
            <h3>事件时间线</h3>
          </div>
        </div>
        <div className="routeLogList">
          {visibleLogs.map((item, index) => (
            <article key={`${item.type}-${item.message}-${index}`}>
              <span>{item.type}</span>
              <strong>{item.message}</strong>
              <small>{item.status ?? `#${index + 1}`}</small>
            </article>
          ))}
        </div>
      </section>
    </div>
  );
}
