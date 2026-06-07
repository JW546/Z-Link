import { brandName, t } from "../lib/i18n";
import { useAppStore } from "../store/useAppStore";
import type { HeroSlide } from "../types";

export function HeroCarousel({
  motionEnabled
}: {
  slides: HeroSlide[];
  activeIndex: number;
  onChange: (value: number) => void;
  motionEnabled: boolean;
}) {
  const locale = useAppStore((state) => state.preferences.locale);
  const brand = brandName[locale];

  return (
    <section className={motionEnabled ? "heroOverview motionOn" : "heroOverview"}>
      <div className="heroOverviewGrid">
        <div className="heroOverviewContent">
          <span className="eyebrow">Z-Link Overview</span>
          <h2>
            {t(locale, {
              "zh-CN": "智通无阻，让协同指挥更清晰、更可控",
              "en-US": "Z-Link makes coordination clearer, smoother, and easier to use"
            })}
          </h2>
          <p className="heroOverviewLead">
            {t(locale, {
              "zh-CN": "从任务输入到协同方案、DDS 网络和设备执行，核心流程集中展示，信息简洁直观。",
              "en-US": "From mission input to planning, DDS networking, and execution, the core flow stays focused and readable."
            })}
          </p>

          <div className="overviewCards">
            <div className="overviewCard">
              <small>{t(locale, { "zh-CN": "系统名称", "en-US": "System" })}</small>
              <strong>{brand.cn}</strong>
            </div>
            <div className="overviewCard">
              <small>{t(locale, { "zh-CN": "英文名", "en-US": "English name" })}</small>
              <strong>{brand.en}</strong>
            </div>
            <div className="overviewCard">
              <small>{t(locale, { "zh-CN": "核心入口", "en-US": "Core entry" })}</small>
              <strong>{t(locale, { "zh-CN": "任务工作台", "en-US": "Command Center" })}</strong>
            </div>
          </div>
        </div>

        <div className="heroOverviewVisual" aria-hidden="true">
          <div className="overviewHalo" />
          <div className="overviewOrbit orbitOne" />
          <div className="overviewOrbit orbitTwo" />
          <div className="overviewOrbit orbitThree" />
          <div className="overviewSphere">
            <img src="/z-link-logo.png" alt="" />
          </div>
          <div className="overviewStamp">Z-Link</div>
        </div>
      </div>
    </section>
  );
}
