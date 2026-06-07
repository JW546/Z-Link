import { t } from "../lib/i18n";
import { useAppStore } from "../store/useAppStore";
import type { PlanResult } from "../types";

export function TimelinePanel({ plan }: { plan: PlanResult | null }) {
  const locale = useAppStore((state) => state.preferences.locale);
  const progress = plan
    ? Math.round((plan.steps.filter((step) => step.status === "done").length / Math.max(1, plan.steps.length)) * 100)
    : 0;

  return (
    <section className="cardPanel">
      <div className="panelHeader">
        <div>
          <span className="eyebrow">{t(locale, { "zh-CN": "Flow", "en-US": "Flow" })}</span>
          <h3>{t(locale, { "zh-CN": "任务时间线", "en-US": "Mission Timeline" })}</h3>
        </div>
        <strong className="progressValue">{progress}%</strong>
      </div>

      <div className="progressTrack">
        <div className="progressFill" style={{ width: `${progress}%` }} />
      </div>

      {!plan ? (
        <div className="emptyPanel">
          {t(locale, {
            "zh-CN": "生成方案后显示步骤。",
            "en-US": "Generate a plan to view steps."
          })}
        </div>
      ) : (
        <div className="timelineList">
          {plan.steps.map((step) => (
            <div key={step.id} className={`timelineItem timeline-${step.status}`}>
              <div className="timelineDot" />
              <div>
                <strong>{step.title}</strong>
                <p>{step.description}</p>
                <span>{t(locale, { "zh-CN": "状态", "en-US": "Status" })}: {step.status}</span>
              </div>
            </div>
          ))}
        </div>
      )}
    </section>
  );
}
