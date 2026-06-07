import { useEffect } from "react";
import { useAppStore } from "../store/useAppStore";

export function useWsBridge() {
  const addStreamLog = useAppStore((state) => state.addStreamLog);
  const setWsConnected = useAppStore((state) => state.setWsConnected);
  const locale = useAppStore((state) => state.preferences.locale);

  useEffect(() => {
    const wsUrl = import.meta.env.VITE_WS_URL;
    let ws: WebSocket | null = null;
    let timer: number | null = null;
    let reconnectTimer: number | null = null;
    let retry = 0;
    const copy = (zhCN: string, enUS: string) => (locale === "en-US" ? enUS : zhCN);

    const startLocalPulse = () => {
      setWsConnected(false);
      if (timer) window.clearInterval(timer);
      timer = window.setInterval(() => {
        addStreamLog(
          `[LOCAL] ${new Date().toLocaleTimeString()} ${copy("协同心跳正常", "Coordination heartbeat is healthy")}`
        );
      }, 3200);
    };

    const connect = () => {
      if (!wsUrl) {
        addStreamLog(
          `[WS] ${copy("未配置地址，已切换为本地心跳模式。", "No WS URL configured. Switched to the local heartbeat mode.")}`
        );
        startLocalPulse();
        return;
      }

      ws = new WebSocket(wsUrl);
      ws.onopen = () => {
        retry = 0;
        if (timer) window.clearInterval(timer);
        setWsConnected(true);
        addStreamLog(`[WS] ${copy("已连接", "Connected to")} ${wsUrl}`);
      };
      ws.onmessage = (event) => {
        addStreamLog(`[WS] ${event.data}`);
      };
      ws.onclose = () => {
        setWsConnected(false);
        retry += 1;
        const delay = Math.min(2000 * retry, 10000);
        addStreamLog(
          `[WS] ${copy("连接已断开，将在", "Connection closed. Reconnecting in")} ${delay}ms`
        );
        reconnectTimer = window.setTimeout(connect, delay);
      };
      ws.onerror = () => {
        addStreamLog(
          `[WS] ${copy("连接异常，暂时维持 API 与本地心跳。", "Connection error. Keeping the API surface and local heartbeat active.")}`
        );
        startLocalPulse();
      };
    };

    connect();
    return () => {
      if (timer) window.clearInterval(timer);
      if (reconnectTimer) window.clearTimeout(reconnectTimer);
      if (ws) ws.close();
    };
  }, [addStreamLog, locale, setWsConnected]);
}
