import { useMemo, useState } from "react";
import { t } from "../lib/i18n";
import { useAppStore } from "../store/useAppStore";
import type { GuideStep } from "../types";

export function GuideOverlay({
  open,
  steps,
  onClose,
  onComplete
}: {
  open: boolean;
  steps: GuideStep[];
  onClose: () => void;
  onComplete: () => void;
}) {
  const locale = useAppStore((state) => state.preferences.locale);
  const [index, setIndex] = useState(0);
  const current = useMemo(() => steps[index] ?? steps[0], [index, steps]);

  if (!open || !current) return null;

  const isLast = index === steps.length - 1;

  return (
    <div className="guideOverlay" role="dialog" aria-modal="true">
      <div className="guidePanel">
        <div className="guideHeader">
          <span className="eyebrow">{t(locale, { "zh-CN": "Quick Start", "en-US": "Quick Start" })}</span>
          <button className="ghostButton" onClick={onClose}>
            {t(locale, { "zh-CN": "跳过", "en-US": "Skip" })}
          </button>
        </div>
        <h2>{current.title}</h2>
        <p>{current.description}</p>
        <div className="guideHint">{current.hint}</div>
        <div className="guideDots">
          {steps.map((step, stepIndex) => (
            <button
              key={step.id}
              className={stepIndex === index ? "guideDot active" : "guideDot"}
              onClick={() => setIndex(stepIndex)}
              aria-label={`Go to step ${stepIndex + 1}`}
            />
          ))}
        </div>
        <div className="guideActions">
          <button className="ghostButton" onClick={() => setIndex((value) => Math.max(0, value - 1))} disabled={index === 0}>
            {t(locale, { "zh-CN": "上一步", "en-US": "Back" })}
          </button>
          <button
            onClick={() => {
              if (isLast) {
                onComplete();
                return;
              }
              setIndex((value) => Math.min(steps.length - 1, value + 1));
            }}
          >
            {isLast
              ? t(locale, { "zh-CN": "开始使用", "en-US": "Start Using" })
              : t(locale, { "zh-CN": "下一步", "en-US": "Next" })}
          </button>
        </div>
      </div>
    </div>
  );
}
