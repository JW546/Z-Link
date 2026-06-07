import { useDeferredValue, useMemo, useState } from "react";
import { backend } from "../api/adapter";
import { t } from "../lib/i18n";
import { useAppStore } from "../store/useAppStore";

export function DevicesPage() {
  const devices = useAppStore((state) => state.devices);
  const deviceAction = useAppStore((state) => state.deviceAction);
  const locale = useAppStore((state) => state.preferences.locale);
  const [query, setQuery] = useState("");
  const [faultType, setFaultType] = useState("DEVICE_OFFLINE");
  const [targetId, setTargetId] = useState("UAV-CQ-007");
  const deferredQuery = useDeferredValue(query);

  const filteredDevices = useMemo(() => {
    if (!deferredQuery.trim()) return devices;
    return devices.filter((device) =>
      `${device.name} ${device.role ?? ""} ${device.deviceType ?? ""} ${device.link ?? ""} ${(device.capabilities ?? []).join(" ")}`
        .toLowerCase()
        .includes(deferredQuery.toLowerCase())
    );
  }, [deferredQuery, devices]);

  return (
    <div className="stackPage">
      <section className="cardPanel">
        <div className="panelHeader">
          <div>
            <span className="eyebrow">Fleet View</span>
            <h3>{t(locale, { "zh-CN": "设备舰队", "en-US": "Fleet" })}</h3>
          </div>
          <label className="fieldLabel compactField searchField">
            {t(locale, { "zh-CN": "搜索设备", "en-US": "Search devices" })}
            <input
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder={t(locale, {
                "zh-CN": "搜索设备名、角色或链路",
                "en-US": "Search by device name, role, or link"
              })}
            />
          </label>
        </div>
        <div className="toolbarRow">
          <label className="fieldLabel compactField">
            {t(locale, { "zh-CN": "故障类型", "en-US": "Fault" })}
            <select value={faultType} onChange={(event) => setFaultType(event.target.value)}>
              {["DEVICE_OFFLINE", "LOW_BATTERY", "LINK_DEGRADED", "SENSOR_UNAVAILABLE"].map((type) => (
                <option key={type} value={type}>{type}</option>
              ))}
            </select>
          </label>
          <label className="fieldLabel compactField">
            Target
            <input value={targetId} onChange={(event) => setTargetId(event.target.value)} />
          </label>
          <button type="button" onClick={() => void backend.injectFault({ faultType, target: targetId }).then(() => deviceAction(targetId, "inspect"))}>
            {t(locale, { "zh-CN": "注入故障", "en-US": "Inject Fault" })}
          </button>
          <button type="button" className="ghostButton" onClick={() => void backend.clearFaults().then(() => deviceAction(targetId, "inspect"))}>
            {t(locale, { "zh-CN": "清除故障", "en-US": "Clear Faults" })}
          </button>
        </div>
        <div className="fleetGrid">
          {filteredDevices.map((device) => {
            const online = device.online || device.available;
            const battery = device.batteryLevel ?? device.battery;
            return (
            <article key={device.id} className={online ? "fleetCard online" : "fleetCard offline"}>
              <div className="fleetCardTop">
                <div>
                  <strong>{device.name}</strong>
                  <span>{device.role ?? device.deviceType} · {device.location ?? "-"}</span>
                </div>
                <span className={online ? "softBadge success" : "softBadge danger"}>
                  {String(device.status ?? (online ? "ONLINE" : "OFFLINE"))}
                </span>
              </div>
              <div className="detailGrid">
                <div>
                  <small>{t(locale, { "zh-CN": "延迟", "en-US": "Latency" })}</small>
                  <strong>{device.latencyMs} ms</strong>
                </div>
                <div>
                  <small>{t(locale, { "zh-CN": "链路", "en-US": "Link" })}</small>
                  <strong>{device.linkQuality !== undefined ? `${Math.round(device.linkQuality * 100)}%` : device.link || "-"}</strong>
                </div>
                <div>
                  <small>{t(locale, { "zh-CN": "电量", "en-US": "Battery" })}</small>
                  <strong>{battery ?? "--"}%</strong>
                </div>
                <div>
                  <small>{t(locale, { "zh-CN": "负载", "en-US": "Load" })}</small>
                  <strong>{device.currentLoad !== undefined ? `${Math.round(device.currentLoad * 100)}%` : "--"}</strong>
                </div>
              </div>
              <div className="chipCloud">
                {(device.capabilities ?? []).slice(0, 5).map((capability) => (
                  <span key={capability} className="softBadge">{capability}</span>
                ))}
              </div>
              <div className="deviceMetaLine">
                <span>{t(locale, { "zh-CN": "最近心跳", "en-US": "Heartbeat" })}: {device.lastHeartbeat ? new Date(device.lastHeartbeat).toLocaleTimeString() : "-"}</span>
                <span>{t(locale, { "zh-CN": "当前任务", "en-US": "Mission" })}: {device.assignedMissionId || "-"}</span>
              </div>
              {device.failureReason ? <p className="riskText">{device.failureReason}</p> : null}
              <div className="actionRow">
                <button className="ghostButton" onClick={() => void deviceAction(device.id, "inspect")}>
                  {t(locale, { "zh-CN": "心跳检查", "en-US": "Heartbeat" })}
                </button>
                <button className="ghostButton" onClick={() => void deviceAction(device.id, "reconnect")}>
                  {t(locale, { "zh-CN": "重连", "en-US": "Reconnect" })}
                </button>
                <button onClick={() => void deviceAction(device.id, online ? "unbind" : "bind")}>
                  {online
                    ? t(locale, { "zh-CN": "解绑", "en-US": "Unbind" })
                    : t(locale, { "zh-CN": "绑定", "en-US": "Bind" })}
                </button>
              </div>
            </article>
            );
          })}
        </div>
      </section>
    </div>
  );
}
