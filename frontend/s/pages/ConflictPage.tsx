import { useAppStore } from "../store/useAppStore";
import { asList, asMap, asText, percent } from "./pageData";

export function ConflictPage() {
  const planResult = useAppStore((state) => state.planResult);
  const trace = asMap(planResult?.dashboardTrace);
  const detection = asMap(trace.conflictDetection);
  const resolution = asMap(trace.conflictResolution);
  const accepted = asMap(resolution.acceptedSuggestion);
  const rejectedRoutes = asList(trace.rejectedRoutes).length ? asList(trace.rejectedRoutes) : asList(resolution.rejectedSuggestions);
  const hasConflict = Boolean(detection.hasConflict ?? resolution.hasConflict);

  return (
    <div className="stackPage routePage">
      <section className="cardPanel">
        <div className="panelHeader">
          <div>
            <span className="eyebrow">Conflict Resolver</span>
            <h3>冲突消解</h3>
          </div>
          <span className={hasConflict ? "softBadge danger" : "softBadge success"}>{hasConflict ? "CONFLICT" : "CLEAR"}</span>
        </div>
        <div className="detailGridPanel">
          <article>
            <span>冲突类型</span>
            <strong>{asText(detection.conflictType ?? resolution.conflictType, "NO_ACTIVE_CONFLICT")}</strong>
            <small>来自 Agent 建议、DDS 时延、路线风险或设备状态的冲突检测</small>
          </article>
          <article>
            <span>冲突原因</span>
            <strong>{asText(detection.reason ?? resolution.decisionReason, "等待任务运行后生成冲突证据")}</strong>
            <small>解释为什么某些路线或设备建议需要降权</small>
          </article>
          <article>
            <span>是否触发重规划</span>
            <strong>{asText(resolution.whetherReplanningTriggered ?? trace.replanningTriggered, "false")}</strong>
            <small>必要时发布 Route/Replan/Trigger 并继续订阅 Agent 建议</small>
          </article>
        </div>
      </section>

      <section className="comparisonPanel cardPanel">
        <div className="panelHeader">
          <div>
            <span className="eyebrow">Accepted / Rejected</span>
            <h3>路线采纳与拒绝</h3>
          </div>
        </div>
        <div className="conflictGrid">
          <article>
            <small>采纳路线</small>
            <strong>{asText(accepted.route, "等待最终路线")}</strong>
            <p>{asText(accepted.reason ?? resolution.decisionReason, "系统会优先采纳风险更低、置信度更高且 DDS 新鲜度更好的建议。")}</p>
            <small>confidence {percent(accepted.confidence)} / risk {percent(accepted.riskScore)}</small>
          </article>
          <article>
            <small>拒绝路线数量</small>
            <strong>{rejectedRoutes.length}</strong>
            <p>{rejectedRoutes.length ? rejectedRoutes.map((item) => `${asText(item.route)}: ${asText(item.rejectionReason ?? item.reason)}`).join("；") : "暂无被拒绝路线，等待 Agent 建议和冲突检测结果。"}</p>
          </article>
          <article>
            <small>消解策略</small>
            <strong>{asText(resolution.policy ?? resolution.strategy, "risk-aware multi-objective")}</strong>
            <p>不把 Dijkstra 或 Greedy 当成最终智能决策，只把它们作为 baseline 与风险感知/多目标策略对照。</p>
          </article>
        </div>
      </section>
    </div>
  );
}
