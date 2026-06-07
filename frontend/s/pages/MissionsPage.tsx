import { useEffect, useMemo, useState } from "react";
import { backend } from "../api/adapter";
import { t } from "../lib/i18n";
import { useAppStore } from "../store/useAppStore";

export function MissionsPage() {
  const missions = useAppStore((state) => state.missions);
  const missionAction = useAppStore((state) => state.missionAction);
  const locale = useAppStore((state) => state.preferences.locale);
  const [selectedMissionId, setSelectedMissionId] = useState<string | null>(missions[0]?.id ?? null);
  const [history, setHistory] = useState<Record<string, unknown>[]>([]);

  const selectedMission = useMemo(
    () => missions.find((mission) => mission.id === selectedMissionId) ?? missions[0] ?? null,
    [missions, selectedMissionId]
  );

  useEffect(() => {
    backend.missionHistory().then((data) => setHistory(data.history ?? [])).catch(() => setHistory([]));
  }, [missions.length]);

  return (
    <div className="stackPage">
      <section className="cardPanel">
        <div className="panelHeader">
          <div>
            <span className="eyebrow">Mission Archive</span>
            <h3>{t(locale, { "zh-CN": "任务档案", "en-US": "Mission Archive" })}</h3>
          </div>
          <span className="softBadge">
            {missions.length} {t(locale, { "zh-CN": "条记录", "en-US": "records" })}
          </span>
        </div>
        <div className="missionArchiveLayout">
          <div className="missionList">
            {missions.map((mission) => (
              <button
                key={mission.id}
                className={mission.id === selectedMission?.id ? "missionCard active" : "missionCard"}
                onClick={() => setSelectedMissionId(mission.id)}
              >
                <div className="missionCardTop">
                  <strong>{mission.id}</strong>
                  <span className={`softBadge status-${mission.status}`}>{mission.status}</span>
                </div>
                <p>{mission.sourceText}</p>
                <small>
                  {mission.intent || t(locale, { "zh-CN": "待补充", "en-US": "Pending" })} · {mission.createdAt || "--"}
                </small>
              </button>
            ))}
          </div>

          <div className="cardPanel missionDetailPanel">
            {selectedMission ? (
              <>
                <span className="eyebrow">Mission Detail</span>
                <h3>{selectedMission.id}</h3>
                <p>{selectedMission.sourceText}</p>
                <div className="detailGrid">
                  <div>
                    <small>{t(locale, { "zh-CN": "意图", "en-US": "Intent" })}</small>
                    <strong>{selectedMission.intent || t(locale, { "zh-CN": "未标注", "en-US": "Unlabeled" })}</strong>
                  </div>
                  <div>
                    <small>{t(locale, { "zh-CN": "状态", "en-US": "Status" })}</small>
                    <strong>{selectedMission.status}</strong>
                  </div>
                  <div>
                    <small>{t(locale, { "zh-CN": "创建时间", "en-US": "Created at" })}</small>
                    <strong>{selectedMission.createdAt || "--"}</strong>
                  </div>
                </div>
                <div className="actionRow">
                  <button className="ghostButton" onClick={() => void missionAction(selectedMission.id, "pause")}>
                    {t(locale, { "zh-CN": "暂停", "en-US": "Pause" })}
                  </button>
                  <button className="ghostButton" onClick={() => void missionAction(selectedMission.id, "resume")}>
                    {t(locale, { "zh-CN": "恢复", "en-US": "Resume" })}
                  </button>
                  <button onClick={() => void missionAction(selectedMission.id, "rerun")}>
                    {t(locale, { "zh-CN": "重新执行", "en-US": "Run Again" })}
                  </button>
                </div>
              </>
            ) : (
              <div className="emptyPanel">
                {t(locale, { "zh-CN": "当前还没有任务记录。", "en-US": "There are no mission records yet." })}
              </div>
            )}
          </div>
        </div>
        <div className="panelHeader">
          <div>
            <span className="eyebrow">Replay</span>
            <h3>{t(locale, { "zh-CN": "任务历史与回放", "en-US": "Mission History & Replay" })}</h3>
          </div>
          <button className="ghostButton" onClick={() => void backend.clearHistory().then(() => setHistory([]))}>
            {t(locale, { "zh-CN": "清空历史", "en-US": "Clear History" })}
          </button>
        </div>
        <div className="mappingList">
          {history.slice(0, 10).map((item, index) => (
            <article key={`${item.missionId}-${index}`} className="mappingRow">
              <span>{String(item.missionId)} · {String(item.status)}</span>
              <strong>{String(item.taskType)} / {String(item.riskLevel)}</strong>
              <small>{String(item.originalText)}</small>
              <p>{JSON.stringify(item.timelineSummary ?? {})}</p>
            </article>
          ))}
        </div>
      </section>
    </div>
  );
}
