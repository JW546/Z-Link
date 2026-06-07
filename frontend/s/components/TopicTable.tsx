import { t } from "../lib/i18n";
import { useAppStore } from "../store/useAppStore";
import type { TopicItem } from "../types";

export function TopicTable({
  title,
  topics,
  mode,
  onToggle,
  onTest
}: {
  title: string;
  topics: TopicItem[];
  mode: "sub" | "pub";
  onToggle?: (topic: TopicItem) => void;
  onTest?: (topic: TopicItem) => void;
}) {
  const locale = useAppStore((state) => state.preferences.locale);

  return (
    <section className="cardPanel">
      <div className="panelHeader">
        <div>
          <span className="eyebrow">{t(locale, { "zh-CN": "DDS", "en-US": "DDS" })}</span>
          <h3>{title}</h3>
        </div>
        <span className="softBadge">{topics.length} {t(locale, { "zh-CN": "项", "en-US": "items" })}</span>
      </div>

      <div className="tableShell">
        <table className="dataTable">
          <thead>
            <tr>
              <th>{t(locale, { "zh-CN": "主题", "en-US": "Topic" })}</th>
              <th>{t(locale, { "zh-CN": "类型", "en-US": "Type" })}</th>
              <th>QoS</th>
              <th>{mode === "sub"
                ? t(locale, { "zh-CN": "来源设备", "en-US": "Source Device" })
                : t(locale, { "zh-CN": "目标设备", "en-US": "Target Device" })}</th>
              <th>{t(locale, { "zh-CN": "最新消息", "en-US": "Latest Message" })}</th>
              <th>{t(locale, { "zh-CN": "操作", "en-US": "Actions" })}</th>
            </tr>
          </thead>
          <tbody>
            {topics.length ? (
              topics.map((topic) => (
                <tr key={`${topic.direction}-${topic.name}`}>
                  <td>
                    <strong>{topic.ddsTopic ?? topic.name}</strong>
                    <small>{topic.typeSupport ?? topic.type}</small>
                  </td>
                  <td>
                    {topic.idlModule ? `${topic.idlModule}::${topic.idlType}` : topic.type}
                    <small>{topic.keyFields?.join(", ") ?? t(locale, { "zh-CN": "无 Key", "en-US": "No key" })}</small>
                  </td>
                  <td>
                    <strong>{topic.qosProfile ?? `qos-${topic.qos}`}</strong>
                    <small>
                      {topic.reliability ?? "RELIABLE"} / {topic.durability ?? "VOLATILE"} / {topic.rateHz}Hz
                    </small>
                  </td>
                  <td>{mode === "sub" ? topic.fromDevice ?? "-" : topic.toDevice ?? "-"}</td>
                  <td>{topic.lastMessage ?? "-"}</td>
                  <td>
                    <div className="inlineActions">
                      <button className="ghostButton" onClick={() => onToggle?.(topic)}>
                        {topic.enabled === false
                          ? t(locale, { "zh-CN": "启用", "en-US": "Enable" })
                          : t(locale, { "zh-CN": "暂停", "en-US": "Pause" })}
                      </button>
                      <button onClick={() => onTest?.(topic)}>
                        {mode === "sub"
                          ? t(locale, { "zh-CN": "读取", "en-US": "Read" })
                          : t(locale, { "zh-CN": "发布", "en-US": "Publish" })}
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan={6}>
                  {t(locale, {
                    "zh-CN": "先生成方案。",
                    "en-US": "Generate a plan first."
                  })}
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </section>
  );
}
