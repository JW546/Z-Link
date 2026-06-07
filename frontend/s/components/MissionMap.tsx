import { useEffect, useMemo, useRef, useState } from "react";
import { t } from "../lib/i18n";
import { useAppStore } from "../store/useAppStore";
import type { MapOverlay, MapPoint } from "../types";

const DEFAULT_COORDINATE = { lng: 106.568, lat: 29.592 };
const PREVIEW_WIDTH = 100;
const PREVIEW_HEIGHT = 100;
const PREVIEW_PADDING = 12;

const escapeHtml = (value: string) =>
  value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");

const loadAmapScript = (key: string, securityJsCode?: string) => {
  const existing = document.querySelector<HTMLScriptElement>('script[data-amap-js="true"]');
  const globalWindow = window as typeof window & {
    AMap?: any;
    _AMapSecurityConfig?: { securityJsCode: string };
  };

  if (securityJsCode && !globalWindow._AMapSecurityConfig) {
    globalWindow._AMapSecurityConfig = { securityJsCode };
  }

  if (existing && globalWindow.AMap) {
    return new Promise<void>((resolve) => {
      if (typeof globalWindow.AMap.plugin === "function") {
        globalWindow.AMap.plugin(["AMap.Scale", "AMap.ToolBar", "AMap.Geolocation"], () => resolve());
      } else {
        resolve();
      }
    });
  }

  return new Promise<void>((resolve, reject) => {
    const script = existing ?? document.createElement("script");
    script.dataset.amapJs = "true";
    script.async = true;
    script.src = `https://webapi.amap.com/maps?v=2.0&key=${encodeURIComponent(key)}&plugin=AMap.Scale,AMap.ToolBar,AMap.Geolocation`;
    script.onload = () => resolve();
    script.onerror = () => reject(new Error("AMap JS API load failed"));
    if (!existing) {
      document.head.appendChild(script);
    }
  });
};

const getPointLng = (point: MapPoint) => (typeof point.lng === "number" ? point.lng : DEFAULT_COORDINATE.lng);
const getPointLat = (point: MapPoint) => (typeof point.lat === "number" ? point.lat : DEFAULT_COORDINATE.lat);
const isSameCoordinate = (coordinate: { lat: number; lng: number }, point: MapPoint) =>
  typeof point.lat === "number" &&
  typeof point.lng === "number" &&
  Math.abs(coordinate.lat - point.lat) < 0.000001 &&
  Math.abs(coordinate.lng - point.lng) < 0.000001;

const makeLocalMapBaseSvg = () => `
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1200 520" preserveAspectRatio="none">
    <defs>
      <linearGradient id="base" x1="0" y1="0" x2="1" y2="1">
        <stop offset="0" stop-color="#e9eef2"/>
        <stop offset="1" stop-color="#d9e6ef"/>
      </linearGradient>
      <linearGradient id="water" x1="0" y1="0" x2="1" y2="1">
        <stop offset="0" stop-color="#a9d7ed"/>
        <stop offset="1" stop-color="#78b9dd"/>
      </linearGradient>
      <filter id="softShadow" x="-20%" y="-20%" width="140%" height="140%">
        <feDropShadow dx="0" dy="2" stdDeviation="2" flood-color="#9baab6" flood-opacity="0.18"/>
      </filter>
    </defs>
    <rect width="1200" height="520" fill="url(#base)"/>
    <g opacity="0.42" stroke="#ffffff" stroke-width="1">
      ${Array.from({ length: 25 }, (_, index) => `<path d="M${index * 52} 0 L${index * 52 - 160} 520"/>`).join("")}
      ${Array.from({ length: 13 }, (_, index) => `<path d="M0 ${index * 44} L1200 ${index * 44 - 95}"/>`).join("")}
    </g>
    <path d="M-40 410 C120 352 210 435 350 362 C486 292 548 210 705 228 C866 246 946 153 1240 136 L1240 560 L-40 560 Z" fill="url(#water)" opacity="0.8"/>
    <g fill="#cfdbe4" stroke="#b5c5d1" stroke-width="2" opacity="0.82" filter="url(#softShadow)">
      <path d="M78 52 L186 34 L226 112 L98 130 Z"/>
      <path d="M256 92 L384 64 L438 154 L288 190 Z"/>
      <path d="M512 36 L668 54 L646 150 L494 126 Z"/>
      <path d="M792 52 L948 38 L1000 132 L828 168 Z"/>
      <path d="M122 228 L280 210 L330 318 L148 338 Z"/>
      <path d="M430 258 L584 214 L646 320 L462 370 Z"/>
      <path d="M724 268 L884 230 L962 348 L768 392 Z"/>
      <path d="M984 246 L1136 224 L1172 346 L1014 372 Z"/>
    </g>
    <g fill="none" stroke-linecap="round" stroke-linejoin="round">
      <path d="M-40 166 C166 138 278 106 420 58 C612 -8 760 22 1240 8" stroke="#f4d17d" stroke-width="18" opacity="0.9"/>
      <path d="M-35 166 C166 138 278 106 420 58 C612 -8 760 22 1240 8" stroke="#ffffff" stroke-width="7" opacity="0.75"/>
      <path d="M-20 470 C182 412 330 358 486 288 C684 198 840 196 1240 182" stroke="#f4d17d" stroke-width="16" opacity="0.88"/>
      <path d="M-20 470 C182 412 330 358 486 288 C684 198 840 196 1240 182" stroke="#ffffff" stroke-width="6" opacity="0.74"/>
      <path d="M240 -20 C278 88 320 176 354 270 C396 386 422 450 456 550" stroke="#ffffff" stroke-width="8" opacity="0.9"/>
      <path d="M694 -20 C668 108 686 204 730 306 C770 398 856 466 920 548" stroke="#ffffff" stroke-width="8" opacity="0.9"/>
      <path d="M52 310 C242 286 372 258 520 218 C690 170 878 148 1180 128" stroke="#ffffff" stroke-width="7" opacity="0.82"/>
      <path d="M72 94 C196 180 308 252 432 342 C548 426 652 468 816 540" stroke="#ffffff" stroke-width="6" opacity="0.75"/>
      <path d="M1040 0 C1018 86 988 164 950 248 C908 342 872 430 846 540" stroke="#ffffff" stroke-width="6" opacity="0.72"/>
    </g>
    <g fill="#6d7f8e" font-family="Microsoft YaHei, Segoe UI, sans-serif" font-size="24" opacity="0.72">
      <text x="76" y="386">观音桥商圈</text>
      <text x="706" y="444">解放碑方向</text>
      <text x="936" y="112">Zone-B</text>
    </g>
  </svg>
`;

const drawLocalMapBase = (canvas: HTMLCanvasElement) => {
  const ctx = canvas.getContext("2d");
  if (!ctx) return;

  const width = canvas.width;
  const height = canvas.height;
  ctx.clearRect(0, 0, width, height);

  const bg = ctx.createLinearGradient(0, 0, width, height);
  bg.addColorStop(0, "#e9eef2");
  bg.addColorStop(1, "#d9e6ef");
  ctx.fillStyle = bg;
  ctx.fillRect(0, 0, width, height);

  ctx.save();
  ctx.globalAlpha = 0.42;
  ctx.strokeStyle = "#ffffff";
  ctx.lineWidth = 1;
  for (let x = 0; x < width + 80; x += Math.max(42, width / 24)) {
    ctx.beginPath();
    ctx.moveTo(x, 0);
    ctx.lineTo(x - width * 0.16, height);
    ctx.stroke();
  }
  for (let y = 0; y < height + 80; y += Math.max(34, height / 12)) {
    ctx.beginPath();
    ctx.moveTo(0, y);
    ctx.lineTo(width, y - height * 0.18);
    ctx.stroke();
  }
  ctx.restore();

  const sx = width / 1200;
  const sy = height / 520;
  const path = (points: Array<[number, number]>, color: string, lineWidth: number, alpha = 1) => {
    ctx.save();
    ctx.globalAlpha = alpha;
    ctx.strokeStyle = color;
    ctx.lineWidth = lineWidth;
    ctx.lineCap = "round";
    ctx.lineJoin = "round";
    ctx.beginPath();
    points.forEach(([x, y], index) => {
      if (index === 0) ctx.moveTo(x * sx, y * sy);
      else ctx.lineTo(x * sx, y * sy);
    });
    ctx.stroke();
    ctx.restore();
  };

  ctx.save();
  const water = ctx.createLinearGradient(0, height * 0.55, width, height);
  water.addColorStop(0, "#a9d7ed");
  water.addColorStop(1, "#78b9dd");
  ctx.globalAlpha = 0.82;
  ctx.fillStyle = water;
  ctx.beginPath();
  ctx.moveTo(-40 * sx, 410 * sy);
  ctx.bezierCurveTo(120 * sx, 352 * sy, 210 * sx, 435 * sy, 350 * sx, 362 * sy);
  ctx.bezierCurveTo(486 * sx, 292 * sy, 548 * sx, 210 * sy, 705 * sx, 228 * sy);
  ctx.bezierCurveTo(866 * sx, 246 * sy, 946 * sx, 153 * sy, 1240 * sx, 136 * sy);
  ctx.lineTo(1240 * sx, 560 * sy);
  ctx.lineTo(-40 * sx, 560 * sy);
  ctx.closePath();
  ctx.fill();
  ctx.restore();

  [
    [[-40, 166], [166, 138], [278, 106], [420, 58], [612, -8], [760, 22], [1240, 8]],
    [[-20, 470], [182, 412], [330, 358], [486, 288], [684, 198], [840, 196], [1240, 182]]
  ].forEach((road) => {
    path(road as Array<[number, number]>, "#f4d17d", 18, 0.9);
    path(road as Array<[number, number]>, "#ffffff", 7, 0.75);
  });
  path([[240, -20], [278, 88], [320, 176], [354, 270], [396, 386], [422, 450], [456, 550]], "#ffffff", 8, 0.9);
  path([[694, -20], [668, 108], [686, 204], [730, 306], [770, 398], [856, 466], [920, 548]], "#ffffff", 8, 0.9);
  path([[52, 310], [242, 286], [372, 258], [520, 218], [690, 170], [878, 148], [1180, 128]], "#ffffff", 7, 0.82);

  ctx.save();
  ctx.globalAlpha = 0.82;
  ctx.fillStyle = "#cfdbe4";
  ctx.strokeStyle = "#b5c5d1";
  ctx.lineWidth = 2;
  [
    [[78, 52], [186, 34], [226, 112], [98, 130]],
    [[256, 92], [384, 64], [438, 154], [288, 190]],
    [[512, 36], [668, 54], [646, 150], [494, 126]],
    [[792, 52], [948, 38], [1000, 132], [828, 168]],
    [[122, 228], [280, 210], [330, 318], [148, 338]],
    [[430, 258], [584, 214], [646, 320], [462, 370]],
    [[724, 268], [884, 230], [962, 348], [768, 392]]
  ].forEach((block) => {
    ctx.beginPath();
    block.forEach(([x, y], index) => {
      if (index === 0) ctx.moveTo(x * sx, y * sy);
      else ctx.lineTo(x * sx, y * sy);
    });
    ctx.closePath();
    ctx.fill();
    ctx.stroke();
  });
  ctx.restore();
};

const createLocalBaseLayer = (AMap: any, container: HTMLDivElement) => {
  if (typeof AMap.CustomLayer !== "function") {
    return null;
  }

  const canvas = document.createElement("canvas");
  const resizeCanvas = () => {
    const rect = container.getBoundingClientRect();
    const dpr = Math.max(1, window.devicePixelRatio || 1);
    canvas.width = Math.max(1, Math.floor(rect.width * dpr));
    canvas.height = Math.max(1, Math.floor(rect.height * dpr));
    canvas.style.width = `${rect.width}px`;
    canvas.style.height = `${rect.height}px`;
    drawLocalMapBase(canvas);
  };
  resizeCanvas();

  const layer = new AMap.CustomLayer(canvas, {
    zooms: [3, 20],
    zIndex: 8
  });
  layer.render = resizeCanvas;
  return layer;
};

export function MissionMap({ overlay }: { overlay: MapOverlay | null | undefined }) {
  const locale = useAppStore((state) => state.preferences.locale);
  const fallbackOverlay = useMemo<MapOverlay>(() => ({
    centerLabel: t(locale, { "zh-CN": "观音桥步行街", "en-US": "Guanyinqiao" }),
    provider: t(locale, { "zh-CN": "本地预览", "en-US": "Local Preview" }),
    areaName: t(locale, { "zh-CN": "任务预览路线", "en-US": "Preview Route" }),
    zoom: 13,
    origin: {
      id: "origin",
      label: t(locale, { "zh-CN": "起点", "en-US": "Start" }),
      type: "origin",
      x: 18,
      y: 74,
      lat: 29.5794,
      lng: 106.5328,
      status: t(locale, { "zh-CN": "待命", "en-US": "Standby" })
    },
    destination: {
      id: "destination",
      label: t(locale, { "zh-CN": "目标区", "en-US": "Target" }),
      type: "target",
      x: 82,
      y: 28,
      lat: 29.592,
      lng: 106.568,
      status: t(locale, { "zh-CN": "待确认", "en-US": "Pending" })
    },
    routeCoordinates: [
      { lat: 29.5794, lng: 106.5328 },
      { lat: 29.584, lng: 106.545 },
      { lat: 29.588, lng: 106.557 },
      { lat: 29.592, lng: 106.568 }
    ],
    distanceKm: 4.1,
    durationMin: 12,
    layers: [
      t(locale, { "zh-CN": "路线预览", "en-US": "Route Preview" }),
      t(locale, { "zh-CN": "节点标签", "en-US": "Node Labels" }),
      t(locale, { "zh-CN": "状态提示", "en-US": "Status" })
    ],
    points: [
      {
        id: "origin",
        label: t(locale, { "zh-CN": "起点", "en-US": "Start" }),
        type: "origin",
        x: 18,
        y: 74,
        lat: 29.5794,
        lng: 106.5328,
        status: t(locale, { "zh-CN": "待命", "en-US": "Standby" })
      },
      {
        id: "drone-02",
        label: t(locale, { "zh-CN": "空中节点", "en-US": "Air Node" }),
        type: "device",
        x: 38,
        y: 48,
        lat: 29.5865,
        lng: 106.5472,
        status: t(locale, { "zh-CN": "在线", "en-US": "Online" })
      },
      {
        id: "gateway",
        label: t(locale, { "zh-CN": "边缘网关", "en-US": "Gateway" }),
        type: "gateway",
        x: 58,
        y: 58,
        lat: 29.584,
        lng: 106.5565,
        status: t(locale, { "zh-CN": "稳定", "en-US": "Stable" })
      },
      {
        id: "destination",
        label: t(locale, { "zh-CN": "目标区", "en-US": "Target" }),
        type: "target",
        x: 82,
        y: 28,
        lat: 29.592,
        lng: 106.568,
        status: t(locale, { "zh-CN": "待确认", "en-US": "Pending" })
      }
    ],
    routes: [
      {
        id: "preview-route",
        label: t(locale, { "zh-CN": "协同路线", "en-US": "Coordination Route" }),
        points: ["origin", "drone-02", "gateway", "destination"],
        status: "active"
      }
    ]
  }), [locale]);

  const activeOverlay = overlay ?? fallbackOverlay;
  const [userPoint, setUserPoint] = useState<MapPoint | null>(null);
  const [geoMsg, setGeoMsg] = useState(
    t(locale, {
      "zh-CN": "等待定位授权",
      "en-US": "Waiting for location access"
    })
  );
  const [mapStatus, setMapStatus] = useState<"loading" | "ready" | "error">("loading");
  const [activePointId, setActivePointId] = useState<string | null>(activeOverlay.destination.id);
  const mapContainerRef = useRef<HTMLDivElement | null>(null);
  const mapRef = useRef<any>(null);
  const amapKey = String(import.meta.env.VITE_AMAP_WEB_KEY ?? import.meta.env.VITE_AMAP_API_KEY ?? "").trim();
  const amapSecurityJsCode = String(import.meta.env.VITE_AMAP_SECURITY_JS_CODE ?? "").trim();

  const locateUserWithBrowserOnly = () => {
    if (!navigator.geolocation) {
      setGeoMsg(
        t(locale, {
          "zh-CN": "当前浏览器不支持定位。",
          "en-US": "Geolocation is not supported."
        })
      );
      return;
    }

    navigator.geolocation.getCurrentPosition(
      (position) => {
        setUserPoint({
          id: "user-location",
          label: t(locale, { "zh-CN": "当前位置", "en-US": "My Location" }),
          type: "user",
          x: 12,
          y: 82,
          lat: position.coords.latitude,
          lng: position.coords.longitude,
          status: `${Math.round(position.coords.accuracy)}m`
        });
        setGeoMsg(
          t(locale, {
            "zh-CN": "定位已更新",
            "en-US": "Location updated"
          })
        );
      },
      () =>
        setGeoMsg(
          t(locale, {
            "zh-CN": "未获得定位授权",
            "en-US": "Location access denied"
          })
        ),
      { enableHighAccuracy: true, timeout: 8000, maximumAge: 15000 }
    );
  };

  const createUserPoint = (lat: number, lng: number, accuracy?: number): MapPoint => ({
    id: "user-location",
    label: t(locale, { "zh-CN": "\u5f53\u524d\u4f4d\u7f6e", "en-US": "My Location" }),
    type: "user",
    x: 12,
    y: 82,
    lat,
    lng,
    status: typeof accuracy === "number" ? `${Math.round(accuracy)}m` : t(locale, { "zh-CN": "\u5df2\u5b9a\u4f4d", "en-US": "Located" })
  });

  const locateByBrowser = () =>
    new Promise<MapPoint>((resolve, reject) => {
      if (!navigator.geolocation) {
        reject(new Error("Browser geolocation is not supported"));
        return;
      }

      navigator.geolocation.getCurrentPosition(
        (position) => {
          resolve(createUserPoint(position.coords.latitude, position.coords.longitude, position.coords.accuracy));
        },
        (error) => reject(error),
        { enableHighAccuracy: true, timeout: 8000, maximumAge: 15000 }
      );
    });

  const locateByAmap = async () => {
    if (!amapKey) {
      throw new Error("AMap key is missing");
    }

    await loadAmapScript(amapKey, amapSecurityJsCode || undefined);

    const globalWindow = window as typeof window & { AMap?: any };
    const AMap = globalWindow.AMap;
    if (!AMap || typeof AMap.Geolocation !== "function") {
      throw new Error("AMap geolocation is not available");
    }

    return new Promise<MapPoint>((resolve, reject) => {
      const geolocation = new AMap.Geolocation({
        enableHighAccuracy: true,
        timeout: 10000,
        needAddress: false,
        convert: true,
        showButton: false,
        showMarker: false,
        showCircle: false
      });

      geolocation.getCurrentPosition((status: string, result: any) => {
        if (status === "complete" && result?.position) {
          resolve(createUserPoint(result.position.lat, result.position.lng, result.accuracy));
          return;
        }

        reject(new Error(result?.message || result?.info || "AMap geolocation failed"));
      });
    });
  };

  const locateUser = async () => {
    setGeoMsg(t(locale, { "zh-CN": "\u6b63\u5728\u5b9a\u4f4d...", "en-US": "Locating..." }));

    try {
      const point = await locateByBrowser();
      setUserPoint(point);
      setGeoMsg(t(locale, { "zh-CN": "\u5b9a\u4f4d\u5df2\u66f4\u65b0", "en-US": "Location updated" }));
      return;
    } catch {
      // AMap can still succeed when the browser location API is unavailable or blocked.
    }

    try {
      const point = await locateByAmap();
      setUserPoint(point);
      setGeoMsg(t(locale, { "zh-CN": "\u5b9a\u4f4d\u5df2\u66f4\u65b0", "en-US": "Location updated" }));
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      setGeoMsg(
        t(locale, {
          "zh-CN": `\u5b9a\u4f4d\u5931\u8d25\uff1a${message}`,
          "en-US": `Location failed: ${message}`
        })
      );
    }
  };

  useEffect(() => {
    locateUser();
  }, []);

  useEffect(() => {
    setActivePointId(userPoint?.id ?? activeOverlay.destination.id);
  }, [activeOverlay.destination.id, activeOverlay.areaName, userPoint]);

  const routeUrl = useMemo(() => {
    if (!userPoint) return null;

    const start = userPoint;
    const from = `${getPointLng(start)},${getPointLat(start)},${encodeURIComponent(start.label)}`;
    const to = `${getPointLng(activeOverlay.destination)},${getPointLat(activeOverlay.destination)},${encodeURIComponent(activeOverlay.destination.label)}`;
    return `https://uri.amap.com/navigation?from=${from}&to=${to}&mode=car&policy=0&callnative=0&src=z-link-console`;
  }, [activeOverlay, userPoint]);

  const mapData = useMemo(() => {
    const start = userPoint;
    const routeWithoutDefaultOrigin = activeOverlay.routeCoordinates.filter(
      (coordinate) => !isSameCoordinate(coordinate, activeOverlay.origin)
    );
    const routeTail = routeWithoutDefaultOrigin.length
      ? routeWithoutDefaultOrigin
      : [{ lat: getPointLat(activeOverlay.destination), lng: getPointLng(activeOverlay.destination) }];
    const routePath = start ? [{ lat: getPointLat(start), lng: getPointLng(start) }, ...routeTail] : routeTail;

    const points = activeOverlay.points.filter((point) => point.type !== "origin" && point.id !== activeOverlay.origin.id);
    if (userPoint) {
      points.unshift(userPoint);
    }

    return {
      points,
      routePath,
      centerLat: routePath[Math.floor(routePath.length / 2)]?.lat ?? getPointLat(activeOverlay.destination),
      centerLng: routePath[Math.floor(routePath.length / 2)]?.lng ?? getPointLng(activeOverlay.destination)
    };
  }, [activeOverlay, userPoint]);

  const previewGeometry = useMemo(() => {
    const latLngPoints = [
      ...mapData.routePath,
      ...mapData.points
        .filter((point) => typeof point.lat === "number" && typeof point.lng === "number")
        .map((point) => ({ lat: getPointLat(point), lng: getPointLng(point) }))
    ];

    const minLat = Math.min(...latLngPoints.map((point) => point.lat));
    const maxLat = Math.max(...latLngPoints.map((point) => point.lat));
    const minLng = Math.min(...latLngPoints.map((point) => point.lng));
    const maxLng = Math.max(...latLngPoints.map((point) => point.lng));
    const latSpan = Math.max(maxLat - minLat, 0.0001);
    const lngSpan = Math.max(maxLng - minLng, 0.0001);

    const project = (lat: number, lng: number) => ({
      x: PREVIEW_PADDING + ((lng - minLng) / lngSpan) * (PREVIEW_WIDTH - PREVIEW_PADDING * 2),
      y: PREVIEW_HEIGHT - PREVIEW_PADDING - ((lat - minLat) / latSpan) * (PREVIEW_HEIGHT - PREVIEW_PADDING * 2)
    });

    const routePoints = mapData.routePath.map((point) => project(point.lat, point.lng));
    const pointNodes = mapData.points.map((point) => {
      if (typeof point.lat === "number" && typeof point.lng === "number") {
        return { ...point, ...project(point.lat, point.lng) };
      }
      return {
        ...point,
        x: PREVIEW_PADDING + (point.x / 100) * (PREVIEW_WIDTH - PREVIEW_PADDING * 2),
        y: PREVIEW_PADDING + (point.y / 100) * (PREVIEW_HEIGHT - PREVIEW_PADDING * 2)
      };
    });

    return { routePoints, pointNodes };
  }, [mapData]);

  useEffect(() => {
    if (!amapKey) {
      setMapStatus("error");
      return;
    }

    let cancelled = false;

    const initMap = async () => {
      try {
        setMapStatus("loading");
        await loadAmapScript(amapKey, amapSecurityJsCode || undefined);
        if (cancelled) return;

        const globalWindow = window as typeof window & { AMap?: any };
        const AMap = globalWindow.AMap;
        if (!AMap || !mapContainerRef.current) {
          throw new Error("AMap not available");
        }

        if (mapRef.current && typeof mapRef.current.destroy === "function") {
          mapRef.current.destroy();
        }

        const map = new AMap.Map(mapContainerRef.current, {
          zoom: activeOverlay.zoom ?? 13,
          center: [mapData.centerLng, mapData.centerLat],
          viewMode: "2D",
          resizeEnable: true,
          mapStyle: "amap://styles/normal",
          features: ["bg", "road", "building", "point"]
        });

        if (typeof AMap.TileLayer === "function") {
          map.setLayers([new AMap.TileLayer()]);
        }

        map.addControl(new AMap.Scale());
        map.addControl(new AMap.ToolBar({ position: "RB" }));

        const markers = mapData.points.map((point) => {
          const marker = new AMap.Marker({
            position: [getPointLng(point), getPointLat(point)],
            title: point.label,
            zIndex: 120,
            anchor: "bottom-center",
            label: {
              content: `<div class="amapMarkerLabel">${escapeHtml(point.label)}</div>`,
              direction: "top"
            }
          });
          marker.on("click", () => setActivePointId((prev) => (prev === point.id ? null : point.id)));
          return marker;
        });

        const polyline = new AMap.Polyline({
          path: mapData.routePath.map((point) => [point.lng, point.lat]),
          strokeColor: "#66b7ff",
          strokeWeight: 6,
          strokeOpacity: 0.96,
          zIndex: 90,
          lineJoin: "round"
        });

        map.add([...markers, polyline]);
        map.setFitView([...markers, polyline], false, [56, 56, 56, 56]);

        mapRef.current = map;
        setMapStatus("ready");
      } catch {
        if (!cancelled) {
          setMapStatus("error");
        }
      }
    };

    void initMap();

    return () => {
      cancelled = true;
      if (mapRef.current && typeof mapRef.current.destroy === "function") {
        mapRef.current.destroy();
      }
      mapRef.current = null;
    };
  }, [activeOverlay.zoom, amapKey, amapSecurityJsCode, mapData]);

  const activePoint = previewGeometry.pointNodes.find((point) => point.id === activePointId) ?? previewGeometry.pointNodes[0];
  const isLiveMap = mapStatus === "ready";
  const modeLabel = isLiveMap
    ? t(locale, { "zh-CN": "实时地图", "en-US": "Live Map" })
    : t(locale, { "zh-CN": "路线预览", "en-US": "Route Preview" });

  const startLabel = userPoint?.label ?? t(locale, { "zh-CN": "\u7b49\u5f85\u5b9a\u4f4d", "en-US": "Waiting for location" });
  const displayAreaName = `${startLabel} -> ${activeOverlay.destination.label}`;

  return (
    <section className="cardPanel mapStage">
      <div className="panelHeader mapStageHeader">
        <div>
          <span className="eyebrow">Route</span>
          <h3>{t(locale, { "zh-CN": "实时地图", "en-US": "Live Map" })}</h3>
        </div>

        <div className="mapToolbar">
          <span className={isLiveMap ? "softBadge success" : "softBadge"}>{modeLabel}</span>
          <button className="ghostButton" onClick={locateUser}>
            {t(locale, { "zh-CN": "定位", "en-US": "Locate" })}
          </button>
          <a
            className="ghostInline"
            href={routeUrl ?? undefined}
            target="_blank"
            rel="noreferrer"
            aria-disabled={!routeUrl}
            onClick={(event) => {
              if (!routeUrl) {
                event.preventDefault();
                locateUser();
              }
            }}
          >
            {t(locale, { "zh-CN": "打开大图", "en-US": "Open" })}
          </a>
        </div>
      </div>

      <div className="mapSummaryBar">
        <span>{displayAreaName}</span>
        <span>{activeOverlay.distanceKm?.toFixed(1) ?? "-"} km</span>
        <span>{activeOverlay.durationMin?.toFixed(0) ?? "-"} min</span>
        <span>{activeOverlay.provider}</span>
      </div>

      <div className="realMapCanvas realMapCanvasLarge">
        <div
          className="localMapBackplate"
          aria-hidden="true"
          dangerouslySetInnerHTML={{ __html: makeLocalMapBaseSvg() }}
        />
        <div className={isLiveMap ? "amapCanvasLayer visible" : "amapCanvasLayer"} ref={mapContainerRef} />

        {!isLiveMap ? (
          <div className="mapPreviewCanvas">
            <svg viewBox={`0 0 ${PREVIEW_WIDTH} ${PREVIEW_HEIGHT}`} className="mapPreviewSvg" role="img" aria-label={modeLabel}>
              <defs>
                <linearGradient id="routeLine" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" stopColor="#5bc2ff" />
                  <stop offset="100%" stopColor="#9b7dff" />
                </linearGradient>
              </defs>

              {[20, 40, 60, 80].map((row) => (
                <line key={`row-${row}`} x1="0" y1={row} x2="100" y2={row} className="mapGridLine" />
              ))}
              {[20, 40, 60, 80].map((col) => (
                <line key={`col-${col}`} x1={col} y1="0" x2={col} y2="100" className="mapGridLine" />
              ))}

              <polyline
                points={previewGeometry.routePoints.map((point) => `${point.x},${point.y}`).join(" ")}
                className="mapPreviewPath"
              />

              {previewGeometry.pointNodes.map((point) => (
                <g key={point.id} className="mapPreviewNode" onClick={() => setActivePointId(point.id)}>
                  <circle
                    cx={point.x}
                    cy={point.y}
                    r={point.id === activePoint?.id ? 2.8 : 2.2}
                    className={point.id === activePoint?.id ? "mapPreviewDot active" : "mapPreviewDot"}
                  />
                  <text x={point.x + 2.6} y={point.y - 2.4} className="mapPreviewLabel">
                    {point.label}
                  </text>
                </g>
              ))}
            </svg>

            <div className="mapPreviewHint">
              {amapKey
                ? t(locale, { "zh-CN": "真实地图暂不可用，已切换为路线预览。", "en-US": "Live map unavailable. Showing route preview." })
                : t(locale, { "zh-CN": "未检测到地图 Key，当前显示本地路线预览。", "en-US": "Map key not found. Showing local route preview." })}
            </div>
          </div>
        ) : null}
      </div>

      <div className="mapInfoGrid">
        <div className="mapInfoCard">
          <small>{t(locale, { "zh-CN": "状态", "en-US": "Status" })}</small>
          <strong>{geoMsg}</strong>
        </div>
        <div className="mapInfoCard">
          <small>{t(locale, { "zh-CN": "图层", "en-US": "Layers" })}</small>
          <strong>{activeOverlay.layers.join(" / ")}</strong>
        </div>
        <div className="mapInfoCard">
          <small>{t(locale, { "zh-CN": "起点", "en-US": "Start" })}</small>
          <strong>{startLabel}</strong>
        </div>
        <div className="mapInfoCard">
          <small>{t(locale, { "zh-CN": "终点", "en-US": "Destination" })}</small>
          <strong>{activeOverlay.destination.label}</strong>
        </div>
      </div>

      {activePoint ? (
        <div className="mapPointPanel">
          <strong>{activePoint.label}</strong>
          <span>{t(locale, { "zh-CN": "类型", "en-US": "Type" })}: {activePoint.type}</span>
          <span>{t(locale, { "zh-CN": "状态", "en-US": "Status" })}: {activePoint.status || "-"}</span>
          <span>
            {t(locale, { "zh-CN": "坐标", "en-US": "Coordinates" })}: {getPointLat(activePoint).toFixed(5)}, {getPointLng(activePoint).toFixed(5)}
          </span>
        </div>
      ) : null}
    </section>
  );
}
