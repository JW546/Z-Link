import { useAppStore } from "../store/useAppStore";
import { asList, asMap, asText, type PageMap } from "./pageData";

const fallbackPolicies: PageMap[] = [
  {
    algorithm: "Dijkstra",
    routeId: "baseline-shortest-path",
    role: "baseline",
    explanation: "只作为最短路径基线，不承担最终智能决策。"
  },
  {
    algorithm: "Greedy",
    routeId: "baseline-local-choice",
    role: "baseline",
    explanation: "只作为局部贪心基线，用于对比风险和协同质量。"
  },
  {
    algorithm: "RiskAware",
    routeId: "risk-aware-candidate",
    role: "candidate",
    explanation: "综合安全风险、拥堵、DDS 新鲜度和 Agent 置信度。"
  },
  {
    algorithm: "MultiObjective",
    routeId: "multi-objective-final",
    role: "selected",
    explanation: "当前最终策略倾向采用风险感知/多目标决策，并保留可解释评估指标。"
  }
];

export function PolicyPage() {
  const planResult = useAppStore((state) => state.planResult);
  const trace = asMap(planResult?.dashboardTrace);
  const plannerComparison = asMap(trace.plannerComparison);
  const policyComparison = asMap(trace.policyComparison);
  const optimization = asMap(trace.policyOptimization);
  const algorithms: PageMap[] = asList(plannerComparison.algorithms).length ? asList(plannerComparison.algorithms) : fallbackPolicies;
  const policyResults = asList(policyComparison.policyResults);

  return (
    <div className="stackPage routePage">
      <section className="cardPanel">
        <div className="panelHeader">
          <div>
            <span className="eyebrow">Policy Evaluation</span>
            <h3>策略评估</h3>
          </div>
          <span className="softBadge success">risk-aware / multi-objective</span>
        </div>
        <div className="plannerRows">
          {algorithms.map((item) => {
            const itemMap = asMap(item);
            const algorithm = asText(itemMap.algorithm ?? itemMap.policyName);
            const baseline = Boolean(itemMap.baseline) || asText(itemMap.role).toLowerCase() === "baseline" || ["Dijkstra", "Greedy"].includes(algorithm);
            return (
              <article key={algorithm} className={baseline ? "baseline" : "selected"}>
                <strong>{algorithm}</strong>
                <span>{asText(itemMap.routeId ?? itemMap.route)}</span>
                <div>
                  <small>time {asText(itemMap.averageTravelTime)}</small>
                  <small>risk {asText(itemMap.averageSafetyRisk ?? itemMap.riskScore)}</small>
                  <small>score {asText(itemMap.score ?? itemMap.reward)}</small>
                </div>
                <p>{asText(itemMap.explanation, baseline ? "baseline 对照策略" : "候选智能策略")}</p>
              </article>
            );
          })}
        </div>
      </section>

      <section className="comparisonPanel cardPanel">
        <div className="panelHeader">
          <div>
            <span className="eyebrow">Agent Policy</span>
            <h3>Agent 策略对比与优化状态</h3>
          </div>
        </div>
        <div className="policyRows">
          {(policyResults.length ? policyResults : [
            { policyName: "RiskAwarePolicy", metrics: { averageReward: "-", averageSafetyRisk: "low", conflictResolutionAccuracy: "-", replanningCount: "-" } },
            { policyName: "MultiObjectivePolicy", metrics: { averageReward: "-", averageSafetyRisk: "balanced", conflictResolutionAccuracy: "-", replanningCount: "-" } }
          ]).map((item) => {
            const metrics = asMap(item.metrics);
            return (
              <article key={asText(item.policyName)}>
                <strong>{asText(item.policyName)}</strong>
                <span>reward {asText(metrics.averageReward)} / risk {asText(metrics.averageSafetyRisk)}</span>
                <small>conflict {asText(metrics.conflictResolutionAccuracy)} / replan {asText(metrics.replanningCount)}</small>
              </article>
            );
          })}
        </div>
        <p className="dashboardNote">
          {asText(policyComparison.whyBestPolicy, "当前最终策略采用风险感知/多目标决策；Dijkstra 和 Greedy 仅用于 baseline 评估，不声称已完成 RL/MARL 训练。")}
        </p>
        <div className="detailGridPanel">
          <article>
            <span>训练状态</span>
            <strong>{asText(trace.trainingStatus, "no RL training")}</strong>
          </article>
          <article>
            <span>优化前</span>
            <strong>{asText(asMap(asMap(optimization.before).metrics).averageReward, "-")}</strong>
          </article>
          <article>
            <span>优化后</span>
            <strong>{asText(asMap(asMap(optimization.after).metrics).averageReward, "-")}</strong>
          </article>
        </div>
      </section>
    </div>
  );
}
