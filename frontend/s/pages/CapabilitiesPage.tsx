import { useState } from "react";
import { t } from "../lib/i18n";
import { useAppStore } from "../store/useAppStore";

export function CapabilitiesPage() {
  const capabilityGroups = useAppStore((state) => state.capabilityGroups);
  const capabilityResult = useAppStore((state) => state.capabilityResult);
  const locale = useAppStore((state) => state.preferences.locale);
  const runCapability = useAppStore((state) => state.runCapability);
  const [input, setInput] = useState(
    t(locale, {
      "zh-CN": "请给出一条适合城市空地协同巡检的 DSL 合约。",
      "en-US": "Generate a DSL contract suitable for urban air-ground coordinated inspection."
    })
  );

  return (
    <div className="stackPage">
      <section className="cardPanel">
        <div className="panelHeader">
          <div>
            <span className="eyebrow">Backend Catalog</span>
            <h3>{t(locale, { "zh-CN": "能力面板", "en-US": "Capabilities" })}</h3>
          </div>
          <span className="softBadge">
            {capabilityGroups.reduce((sum, group) => sum + group.capabilities.length, 0)}{" "}
            {t(locale, { "zh-CN": "项能力", "en-US": "capabilities" })}
          </span>
        </div>
        <div className="capabilityLayout">
          <div className="capabilityGroups">
            {capabilityGroups.map((group) => (
              <section key={group.name} className="cardPanel innerCard">
                <h4>{group.name}</h4>
                <div className="capabilityCardGrid">
                  {group.capabilities.map((capability) => (
                    <article key={capability.id} className="capabilityCard">
                      <div className="capabilityCardHead">
                        <strong>{capability.name}</strong>
                        <span className="softBadge">{capability.mode}</span>
                      </div>
                      <p>{capability.description}</p>
                      <small>{capability.backendClass}</small>
                      <div className="actionRow">
                        {capability.actions.map((action) => (
                          <button
                            key={action}
                            className={action === "run" ? "" : "ghostButton"}
                            onClick={() => void runCapability(capability.id, action, input)}
                          >
                            {action}
                          </button>
                        ))}
                      </div>
                    </article>
                  ))}
                </div>
              </section>
            ))}
          </div>
          <section className="cardPanel innerCard">
            <h4>{t(locale, { "zh-CN": "能力输入", "en-US": "Capability Input" })}</h4>
            <textarea value={input} onChange={(event) => setInput(event.target.value)} className="missionInput" />
            <h4>{t(locale, { "zh-CN": "执行结果", "en-US": "Execution Result" })}</h4>
            {capabilityResult ? (
              <pre>{JSON.stringify(capabilityResult, null, 2)}</pre>
            ) : (
              <div className="emptyPanel">
                {t(locale, {
                  "zh-CN": "点击任一能力动作后，这里会显示结果。",
                  "en-US": "Run any capability action and the result will appear here."
                })}
              </div>
            )}
          </section>
        </div>
      </section>
    </div>
  );
}
