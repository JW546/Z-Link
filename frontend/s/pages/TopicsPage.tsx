import { useDeferredValue, useEffect, useMemo, useState } from "react";
import { backend } from "../api/adapter";
import { TopicTable } from "../components/TopicTable";
import { t } from "../lib/i18n";
import { useAppStore } from "../store/useAppStore";

export function TopicsPage() {
  const planResult = useAppStore((state) => state.planResult);
  const topicAction = useAppStore((state) => state.topicAction);
  const locale = useAppStore((state) => state.preferences.locale);
  const [query, setQuery] = useState("");
  const [catalog, setCatalog] = useState<Record<string, unknown>[]>([]);
  const [recentLogs, setRecentLogs] = useState<Record<string, unknown>[]>([]);
  const [topology, setTopology] = useState<Record<string, unknown>>({});
  const [runtimeMode, setRuntimeMode] = useState("");
  const deferredQuery = useDeferredValue(query);

  useEffect(() => {
    let mounted = true;
    backend.listAgentTopics().then((result) => {
      if (!mounted) return;
      setCatalog(result.topics ?? []);
      setRecentLogs(result.recentLogs ?? []);
      setRuntimeMode(result.runtimeMode ?? "");
    }).catch(() => {
      if (!mounted) return;
      setCatalog([]);
      setRecentLogs([]);
    });
    backend.topicTopology().then((result) => setTopology(result.topology ?? {})).catch(() => setTopology({}));
    return () => {
      mounted = false;
    };
  }, []);

  const subscribeTopics = useMemo(() => {
    const source = planResult?.subscribeTopics ?? [];
    if (!deferredQuery.trim()) return source;
    return source.filter((topic) =>
      `${topic.name} ${topic.ddsTopic} ${topic.fromDevice}`.toLowerCase().includes(deferredQuery.toLowerCase())
    );
  }, [deferredQuery, planResult?.subscribeTopics]);

  const publishTopics = useMemo(() => {
    const source = planResult?.publishTopics ?? [];
    if (!deferredQuery.trim()) return source;
    return source.filter((topic) =>
      `${topic.name} ${topic.ddsTopic} ${topic.toDevice}`.toLowerCase().includes(deferredQuery.toLowerCase())
    );
  }, [deferredQuery, planResult?.publishTopics]);

  return (
    <div className="stackPage">
      <section className="cardPanel">
        <div className="panelHeader">
          <div>
            <span className="eyebrow">DDS Topics</span>
            <h3>{t(locale, { "zh-CN": "DDS 网络总览", "en-US": "DDS Network" })}</h3>
          </div>
          <label className="fieldLabel compactField searchField">
            {t(locale, { "zh-CN": "筛选主题", "en-US": "Filter topics" })}
            <input
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder={t(locale, {
                "zh-CN": "搜索主题名、设备或 TypeSupport",
                "en-US": "Search by topic, device, or TypeSupport"
              })}
            />
          </label>
        </div>
        <div className="twoColumnSection">
          <TopicTable
            title={t(locale, { "zh-CN": "订阅链路", "en-US": "Subscription Routes" })}
            topics={subscribeTopics}
            mode="sub"
            onToggle={(topic) => void topicAction(topic.name, "toggle")}
            onTest={(topic) =>
              void topicAction(topic.name, "test", t(locale, { "zh-CN": "订阅读取成功", "en-US": "Subscription read successful" }))
            }
          />
          <TopicTable
            title={t(locale, { "zh-CN": "发布链路", "en-US": "Publish Routes" })}
            topics={publishTopics}
            mode="pub"
            onToggle={(topic) => void topicAction(topic.name, "toggle")}
            onTest={(topic) =>
              void topicAction(topic.name, "publish", t(locale, { "zh-CN": "发布写入成功", "en-US": "Publish write successful" }))
            }
          />
        </div>
        <div className="collaborationGrid">
          <div className="adviceColumn">
            <h4>{t(locale, { "zh-CN": "Topic 目录", "en-US": "Topic Catalog" })}</h4>
            <div className="mappingList">
              {catalog
                .filter((topic) => !deferredQuery.trim() || JSON.stringify(topic).toLowerCase().includes(deferredQuery.toLowerCase()))
                .map((topic) => (
                  <article key={String(topic.name)} className="mappingRow">
                    <span>{String(topic.name)} · {String(topic.direction)}</span>
                    <strong>{String(topic.idlType ?? "-")}</strong>
                    <small>{String(topic.ddsTopic ?? "")}</small>
                    <p>{String(topic.description ?? "")}</p>
                  </article>
                ))}
            </div>
          </div>
          <div className="idlColumn">
            <h4>{t(locale, { "zh-CN": "运行模式与最近日志", "en-US": "Runtime & Recent Logs" })}</h4>
            <div className="mappingList">
              <article className="mappingRow">
                <span>DDS Runtime</span>
                <strong>{runtimeMode || "FALLBACK_SIMULATION"}</strong>
                <small>{t(locale, { "zh-CN": "真实 DDS 不可用时自动回退模拟", "en-US": "Falls back automatically when real DDS is unavailable" })}</small>
              </article>
              {recentLogs.slice(0, 8).map((log, index) => (
                <article key={`${log.timestamp}-${index}`} className="mappingRow">
                  <span>{String(log.phase)} · {String(log.topic)}</span>
                  <strong>{String(log.status)}</strong>
                  <small>{String(log.reason ?? "")}</small>
                </article>
              ))}
            </div>
          </div>
        </div>
        <div className="collaborationGrid">
          <div className="adviceColumn">
            <h4>{t(locale, { "zh-CN": "Topic 拓扑节点", "en-US": "Topology Nodes" })}</h4>
            <div className="mappingList">
              {((topology.nodes as Record<string, unknown>[] | undefined) ?? []).slice(0, 12).map((node) => (
                <article key={String(node.id)} className="mappingRow">
                  <span>{String(node.id)}</span>
                  <strong>{String(node.type)}</strong>
                  <small>{String(node.label ?? "")}</small>
                </article>
              ))}
            </div>
          </div>
          <div className="idlColumn">
            <h4>{t(locale, { "zh-CN": "通信边", "en-US": "Communication Edges" })}</h4>
            <div className="mappingList">
              {((topology.edges as Record<string, unknown>[] | undefined) ?? []).slice(0, 12).map((edge, index) => (
                <article key={`${edge.from}-${edge.to}-${index}`} className="mappingRow">
                  <span>{String(edge.from)} {"->"} {String(edge.to)}</span>
                  <strong>{String(edge.relation)}</strong>
                  <small>{String(topology.runtimeMode ?? runtimeMode)}</small>
                </article>
              ))}
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}
