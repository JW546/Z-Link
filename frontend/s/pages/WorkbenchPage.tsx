import { useEffect, useRef, useState } from "react";
import { AssistantAvatar } from "../components/AssistantAvatar";
import { backend } from "../api/adapter";
import { HeroCarousel } from "../components/HeroCarousel";
import { MissionMap } from "../components/MissionMap";
import { TimelinePanel } from "../components/TimelinePanel";
import { TopicTable } from "../components/TopicTable";
import { heroSlidesByLocale, localeMeta, starterTemplates, t } from "../lib/i18n";
import { useAppStore } from "../store/useAppStore";

type SpeechCtor = new () => SpeechRecognition;
type SpeechErrorCode =
  | "aborted"
  | "audio-capture"
  | "network"
  | "no-speech"
  | "not-allowed"
  | "service-not-allowed"
  | string;

declare global {
  interface Window {
    webkitSpeechRecognition?: SpeechCtor;
    SpeechRecognition?: SpeechCtor;
  }

  interface SpeechRecognition extends EventTarget {
    continuous: boolean;
    interimResults: boolean;
    lang: string;
    onresult: ((event: SpeechRecognitionEvent) => void) | null;
    onend: (() => void) | null;
    onerror: ((event: { error: SpeechErrorCode }) => void) | null;
    abort: () => void;
    start: () => void;
    stop: () => void;
  }

  interface SpeechRecognitionEvent {
    results: {
      [index: number]: {
        [index: number]: {
          transcript: string;
        };
      };
      length: number;
    };
  }
}

export function WorkbenchPage() {
  const inputText = useAppStore((state) => state.inputText);
  const parseResult = useAppStore((state) => state.parseResult);
  const planResult = useAppStore((state) => state.planResult);
  const loading = useAppStore((state) => state.loading);
  const executing = useAppStore((state) => state.executing);
  const devices = useAppStore((state) => state.devices);
  const conversation = useAppStore((state) => state.conversation);
  const preferences = useAppStore((state) => state.preferences);
  const currentUser = useAppStore((state) => state.currentUser);
  const setInputText = useAppStore((state) => state.setInputText);
  const analyzeAndPlan = useAppStore((state) => state.analyzeAndPlan);
  const executePlan = useAppStore((state) => state.executePlan);
  const pauseMission = useAppStore((state) => state.pauseMission);
  const resumeMission = useAppStore((state) => state.resumeMission);
  const stopMission = useAppStore((state) => state.stopMission);
  const topicAction = useAppStore((state) => state.topicAction);

  const locale = preferences.locale;
  const slides = heroSlidesByLocale[locale];
  const recognitionRef = useRef<SpeechRecognition | null>(null);
  const stoppingVoiceRef = useRef(false);
  const [voiceState, setVoiceState] = useState<"idle" | "listening" | "unsupported">("idle");
  const [voiceMessage, setVoiceMessage] = useState(
    t(locale, {
      "zh-CN": "可直接口述任务。",
      "en-US": "You can speak the mission directly."
    })
  );
  const [carouselIndex, setCarouselIndex] = useState(0);
  const [selectedTemplate, setSelectedTemplate] = useState("");
  const [scenarios, setScenarios] = useState<Record<string, unknown>[]>([]);
  const [dashboard, setDashboard] = useState<Record<string, unknown>>({});
  const [faultType, setFaultType] = useState("DEVICE_OFFLINE");
  const [faultTarget, setFaultTarget] = useState("UAV-CQ-007");
  const [reportMarkdown, setReportMarkdown] = useState("");
  const speechLang = localeMeta[locale].speechLang;
  const onlineDevices = devices.filter((device) => device.online);
  const latestMessages = conversation.slice(-3);
  const collaboration = planResult?.collaboration;
  const platformPlan = planResult?.platformPlan;
  const executionTimeline = planResult?.executionTimeline ?? platformPlan?.executionTimeline ?? [];

  useEffect(() => () => recognitionRef.current?.stop(), []);

  useEffect(() => {
    backend.listScenarios().then((data) => setScenarios(data.scenarios ?? [])).catch(() => setScenarios([]));
    backend.dashboard().then((data) => setDashboard(data.dashboard ?? {})).catch(() => setDashboard({}));
  }, [planResult?.missionId]);

  useEffect(() => {
    setVoiceMessage(
      t(locale, {
        "zh-CN": "可直接口述任务。",
        "en-US": "You can speak the mission directly."
      })
    );
  }, [locale]);

  const startVoiceInput = () => {
    const Recognition = window.SpeechRecognition || window.webkitSpeechRecognition;

    if (!Recognition) {
      setVoiceState("unsupported");
      setVoiceMessage(
        t(locale, {
          "zh-CN": "当前浏览器不支持语音输入。",
          "en-US": "This browser does not support voice input."
        })
      );
      return;
    }

    recognitionRef.current?.stop();
    const recognition = new Recognition();
    recognition.lang = speechLang;
    recognition.continuous = false;
    recognition.interimResults = true;

    recognition.onresult = (event) => {
      const transcript = Array.from({ length: event.results.length })
        .map((_, index) => event.results[index]?.[0]?.transcript ?? "")
        .join("")
        .trim();

      if (transcript) {
        setInputText(transcript);
      }
    };

    recognition.onerror = () => {
      setVoiceState("idle");
      setVoiceMessage(
        t(locale, {
          "zh-CN": "语音输入失败。",
          "en-US": "Voice input failed."
        })
      );
    };

    recognition.onend = () => {
      setVoiceState("idle");
      setVoiceMessage(
        t(locale, {
          "zh-CN": "语音输入结束。",
          "en-US": "Voice capture finished."
        })
      );
    };

    recognitionRef.current = recognition;
    recognition.start();
    setVoiceState("listening");
    setVoiceMessage(
      t(locale, {
        "zh-CN": "正在聆听。",
        "en-US": "Listening."
      })
    );
  };

  const voiceErrorMessage = (error: SpeechErrorCode) => {
    switch (error) {
      case "not-allowed":
      case "service-not-allowed":
        return t(locale, {
          "zh-CN": "麦克风权限被拒绝，请在浏览器地址栏允许麦克风后重试。",
          "en-US": "Microphone permission was blocked. Allow it in the browser address bar and try again."
        });
      case "audio-capture":
        return t(locale, {
          "zh-CN": "没有检测到可用麦克风，请检查输入设备。",
          "en-US": "No microphone was detected. Check the input device."
        });
      case "no-speech":
        return t(locale, {
          "zh-CN": "没有识别到语音，请靠近麦克风再试一次。",
          "en-US": "No speech was detected. Move closer to the microphone and try again."
        });
      case "network":
        return t(locale, {
          "zh-CN": "语音识别服务暂时不可用，请检查网络或改用键盘输入。",
          "en-US": "Speech recognition is temporarily unavailable. Check the network or use keyboard input."
        });
      case "aborted":
        return t(locale, {
          "zh-CN": "语音输入已停止。",
          "en-US": "Voice input stopped."
        });
      default:
        return t(locale, {
          "zh-CN": "语音输入失败，请检查浏览器、麦克风和权限。",
          "en-US": "Voice input failed. Check the browser, microphone, and permissions."
        });
    }
  };

  const stopVoiceInput = () => {
    stoppingVoiceRef.current = true;
    recognitionRef.current?.stop();
    setVoiceState("idle");
    setVoiceMessage(
      t(locale, {
        "zh-CN": "语音输入已停止。",
        "en-US": "Voice input stopped."
      })
    );
  };

  const startStableVoiceInput = async () => {
    if (voiceState === "listening") {
      stopVoiceInput();
      return;
    }

    const Recognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!Recognition) {
      setVoiceState("unsupported");
      setVoiceMessage(
        t(locale, {
          "zh-CN": "当前浏览器不支持语音输入，请使用新版 Edge 或 Chrome。",
          "en-US": "This browser does not support voice input. Use a recent Edge or Chrome."
        })
      );
      return;
    }

    if (!window.isSecureContext) {
      setVoiceState("unsupported");
      setVoiceMessage(
        t(locale, {
          "zh-CN": "语音输入需要 localhost/127.0.0.1 或 HTTPS 环境。",
          "en-US": "Voice input requires localhost, 127.0.0.1, or HTTPS."
        })
      );
      return;
    }

    if (!navigator.mediaDevices?.getUserMedia) {
      setVoiceState("unsupported");
      setVoiceMessage(
        t(locale, {
          "zh-CN": "当前系统没有可用的浏览器麦克风接口。",
          "en-US": "The browser microphone API is unavailable on this system."
        })
      );
      return;
    }

    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      stream.getTracks().forEach((track) => track.stop());
    } catch {
      setVoiceState("idle");
      setVoiceMessage(voiceErrorMessage("not-allowed"));
      return;
    }

    try {
      recognitionRef.current?.abort();
    } catch {
      recognitionRef.current?.stop();
    }

    stoppingVoiceRef.current = false;
    const recognition = new Recognition();
    recognition.lang = speechLang;
    recognition.continuous = false;
    recognition.interimResults = true;

    recognition.onresult = (event) => {
      const transcript = Array.from({ length: event.results.length })
        .map((_, index) => event.results[index]?.[0]?.transcript ?? "")
        .join("")
        .trim();

      if (transcript) {
        setInputText(transcript);
      }
    };

    recognition.onerror = (event) => {
      setVoiceState("idle");
      setVoiceMessage(voiceErrorMessage(event.error));
    };

    recognition.onend = () => {
      setVoiceState("idle");
      recognitionRef.current = null;
      if (stoppingVoiceRef.current) {
        stoppingVoiceRef.current = false;
        return;
      }
      setVoiceMessage(
        t(locale, {
          "zh-CN": "语音输入结束。",
          "en-US": "Voice capture finished."
        })
      );
    };

    recognitionRef.current = recognition;
    try {
      recognition.start();
      setVoiceState("listening");
      setVoiceMessage(
        t(locale, {
          "zh-CN": "正在聆听。",
          "en-US": "Listening."
        })
      );
    } catch {
      recognitionRef.current = null;
      setVoiceState("idle");
      setVoiceMessage(voiceErrorMessage("aborted"));
    }
  };

  const runScenario = async (scenario: Record<string, unknown>) => {
    const text = String(scenario.defaultIntentText ?? "");
    setSelectedTemplate(String(scenario.scenarioId ?? ""));
    setInputText(text);
    await analyzeAndPlan();
  };

  const injectFault = async () => {
    await backend.injectFault({ faultType, target: faultTarget });
    await analyzeAndPlan();
  };

  const clearFaults = async () => {
    await backend.clearFaults();
    await analyzeAndPlan();
  };

  const generateReport = async () => {
    const result = await backend.report(planResult?.missionId);
    setReportMarkdown(result.markdown ?? "");
  };

  return (
    <div className="stackPage">
      <HeroCarousel
        slides={slides}
        activeIndex={carouselIndex}
        onChange={setCarouselIndex}
        motionEnabled={preferences.animationEnabled}
      />

      <section className="cardPanel">
        <div className="panelHeader">
          <div>
            <span className="eyebrow">Dashboard</span>
            <h3>{t(locale, { "zh-CN": "平台运行驾驶舱", "en-US": "Platform Dashboard" })}</h3>
          </div>
          <span className="softBadge">{String(dashboard.ddsRuntimeMode ?? "SIMULATED_DDS")}</span>
        </div>
        <div className="detailGrid">
          {[
            ["totalMissions", "任务总数"],
            ["onlineDevices", "在线设备"],
            ["averageRiskScore", "平均风险"],
            ["recentFaultCount", "故障数"],
            ["recentRetryCount", "重试数"],
            ["topicCount", "Topic"]
          ].map(([key, label]) => (
            <div key={key}>
              <small>{label}</small>
              <strong>{String(dashboard[key] ?? "-")}</strong>
            </div>
          ))}
        </div>
      </section>

      <section className="cardPanel">
        <div className="panelHeader">
          <div>
            <span className="eyebrow">Scenario Library</span>
            <h3>{t(locale, { "zh-CN": "典型场景快捷入口", "en-US": "Scenario Shortcuts" })}</h3>
          </div>
        </div>
        <div className="collaborationGrid">
          {scenarios.map((scenario) => (
            <article key={String(scenario.scenarioId)} className="adviceCard">
              <div>
                <strong>{String(scenario.name)}</strong>
                <span>{String(scenario.qosLevel)}</span>
              </div>
              <p>{String(scenario.description)}</p>
              <small>{String((scenario.recommendedDevices as string[] | undefined)?.join(" / ") ?? "")}</small>
              <small>{String((scenario.requiredTopics as string[] | undefined)?.join(" / ") ?? "")}</small>
              <button type="button" onClick={() => void runScenario(scenario)}>
                {t(locale, { "zh-CN": "填入并生成", "en-US": "Use and Plan" })}
              </button>
            </article>
          ))}
        </div>
      </section>

      <section className="workbenchLead">
        <div className="cardPanel commandPanel">
          <div className="panelHeader">
            <div>
              <span className="eyebrow">{t(locale, { "zh-CN": "Mission", "en-US": "Mission" })}</span>
              <h3>{t(locale, { "zh-CN": "任务工作台", "en-US": "Command Center" })}</h3>
            </div>
            <span className="softBadge">
              {planResult?.missionId ?? t(locale, { "zh-CN": "待生成", "en-US": "Pending" })}
            </span>
          </div>

          <div className="toolbarRow">
            <label className="fieldLabel compactField">
              {t(locale, { "zh-CN": "模板", "en-US": "Template" })}
              <select
                value={selectedTemplate}
                onChange={(event) => {
                  setSelectedTemplate(event.target.value);
                  if (event.target.value) {
                    setInputText(event.target.value);
                  }
                }}
              >
                <option value="">{t(locale, { "zh-CN": "选择模板", "en-US": "Choose template" })}</option>
                {starterTemplates[locale].map((template) => (
                  <option key={template} value={template}>
                    {template}
                  </option>
                ))}
              </select>
            </label>

          </div>

          <textarea
            className="missionInput"
            value={inputText}
            onChange={(event) => setInputText(event.target.value)}
            placeholder={t(locale, {
              "zh-CN": "输入任务内容",
              "en-US": "Describe the mission"
            })}
          />

          <div className="commandBar">
            <button className={voiceState === "listening" ? "ghostButton active" : "ghostButton"} onClick={() => void startStableVoiceInput()}>
              {voiceState === "listening"
                ? t(locale, { "zh-CN": "聆听中", "en-US": "Listening" })
                : t(locale, { "zh-CN": "语音", "en-US": "Voice" })}
            </button>
            <span className="supportText">{voiceMessage}</span>
          </div>

          <div className="actionRow">
            <button onClick={() => void analyzeAndPlan()} disabled={loading}>
              {loading
                ? t(locale, { "zh-CN": "生成中", "en-US": "Working" })
                : t(locale, { "zh-CN": "生成方案", "en-US": "Generate Plan" })}
            </button>
            <button onClick={() => void executePlan()} disabled={executing || !planResult}>
              {executing
                ? t(locale, { "zh-CN": "协同中", "en-US": "Coordinating" })
                : t(locale, { "zh-CN": "启动协同", "en-US": "Start Coordination" })}
            </button>
            <button className="ghostButton" onClick={pauseMission} disabled={!planResult}>
              {t(locale, { "zh-CN": "暂停", "en-US": "Pause" })}
            </button>
            <button className="ghostButton" onClick={resumeMission} disabled={!planResult}>
              {t(locale, { "zh-CN": "继续", "en-US": "Resume" })}
            </button>
            <button className="dangerButton" onClick={stopMission} disabled={!planResult}>
              {t(locale, { "zh-CN": "停止", "en-US": "Stop" })}
            </button>
          </div>

          <div className="statsStrip">
            <div>
              <small>{t(locale, { "zh-CN": "在线设备", "en-US": "Devices" })}</small>
              <strong>{onlineDevices.length}</strong>
            </div>
            <div>
              <small>{t(locale, { "zh-CN": "订阅", "en-US": "Subs" })}</small>
              <strong>{planResult?.subscribeTopics.length ?? 0}</strong>
            </div>
            <div>
              <small>{t(locale, { "zh-CN": "发布", "en-US": "Pubs" })}</small>
              <strong>{planResult?.publishTopics.length ?? 0}</strong>
            </div>
            <div>
              <small>{t(locale, { "zh-CN": "链路", "en-US": "Link" })}</small>
              <strong>{planResult?.linkStrategy.primary ?? "-"}</strong>
            </div>
          </div>

          {parseResult ? (
            <div className="summaryBlock">
              <strong>{parseResult.intentSummary}</strong>
              <span>{Math.round(parseResult.confidence * 100)}%</span>
            </div>
          ) : null}

        </div>

        <aside className="cardPanel assistantPanel">
          <div className="assistantHeader assistantHeaderCompact">
            <AssistantAvatar accent={currentUser?.avatarAccent ?? "#67cfff"} name={preferences.assistantName} compact />
            <div>
              <h3>{preferences.assistantName}</h3>
              <p>{currentUser?.organization ?? "Z-Link"}</p>
            </div>
          </div>

          <div className="assistantMeta">
            <span>{t(locale, { "zh-CN": "语言", "en-US": "Language" })}: {localeMeta[locale].label}</span>
            <span>{t(locale, { "zh-CN": "场景", "en-US": "Scene" })}: {currentUser?.favoriteScene ?? "-"}</span>
          </div>

          <div className="conversationList compactConversation">
            {latestMessages.map((message) => (
              <article key={message.id} className={message.role === "assistant" ? "chatBubble assistant" : "chatBubble user"}>
                <strong>{message.title || preferences.assistantName}</strong>
                <p>{message.content}</p>
              </article>
            ))}
          </div>
        </aside>
      </section>

      <section className="routeStage">
        <MissionMap overlay={planResult?.mapOverlay} />
      </section>

      {collaboration ? (
        <section className="cardPanel collaborationStage">
          <div className="panelHeader">
            <div>
              <span className="eyebrow">Autonomy</span>
              <h3>{t(locale, { "zh-CN": "乘客意图协同闭环", "en-US": "Passenger Intent Loop" })}</h3>
            </div>
            <span className="softBadge">{collaboration.decisionOwner}</span>
          </div>

          <div className="decisionBanner">
            <div>
              <small>{t(locale, { "zh-CN": "最终路线", "en-US": "Final Route" })}</small>
              <strong>{collaboration.finalDecision.route}</strong>
              <p>{collaboration.finalDecision.reason}</p>
            </div>
            <div className="decisionMetrics">
              <span>{Math.round(collaboration.finalDecision.confidence * 100)}%</span>
              <small>{t(locale, { "zh-CN": "融合置信度", "en-US": "Confidence" })}</small>
            </div>
            <div className="decisionMetrics">
              <span>{Math.round(collaboration.finalDecision.riskScore * 100)}%</span>
              <small>{t(locale, { "zh-CN": "风险评分", "en-US": "Risk" })}</small>
            </div>
          </div>

          {platformPlan?.riskAssessment ? (
            <div className="decisionBanner">
              <div>
                <small>{t(locale, { "zh-CN": "风险评估", "en-US": "Risk Assessment" })}</small>
                <strong>{String(platformPlan.riskAssessment.riskLevel ?? "-")} · {String(platformPlan.riskAssessment.riskScore ?? 0)}/100</strong>
                <p>{((platformPlan.riskAssessment.riskFactors as string[] | undefined) ?? []).join("；")}</p>
              </div>
              <div className="decisionMetrics">
                <span>{String(platformPlan.riskAssessment.whetherFallbackRequired ?? false)}</span>
                <small>fallback</small>
              </div>
            </div>
          ) : null}

          <div className="collaborationGrid">
            <div className="adviceColumn">
              <h4>{t(locale, { "zh-CN": "故障注入仿真", "en-US": "Fault Injection" })}</h4>
              <div className="toolbarRow">
                <label className="fieldLabel compactField">
                  Type
                  <select value={faultType} onChange={(event) => setFaultType(event.target.value)}>
                    {["DEVICE_OFFLINE", "LOW_BATTERY", "LINK_DEGRADED", "DDS_PUBLISH_FAILED", "HIGH_CONGESTION", "ROUTE_BLOCKED", "SENSOR_UNAVAILABLE"].map((type) => (
                      <option key={type} value={type}>{type}</option>
                    ))}
                  </select>
                </label>
                <label className="fieldLabel compactField">
                  Target
                  <input value={faultTarget} onChange={(event) => setFaultTarget(event.target.value)} />
                </label>
                <button type="button" onClick={() => void injectFault()}>{t(locale, { "zh-CN": "注入", "en-US": "Inject" })}</button>
                <button type="button" className="ghostButton" onClick={() => void clearFaults()}>{t(locale, { "zh-CN": "清除", "en-US": "Clear" })}</button>
              </div>
            </div>
            <div className="idlColumn">
              <h4>{t(locale, { "zh-CN": "任务报告", "en-US": "Mission Report" })}</h4>
              <div className="actionRow">
                <button type="button" onClick={() => void generateReport()}>{t(locale, { "zh-CN": "生成报告", "en-US": "Generate" })}</button>
                <button type="button" className="ghostButton" onClick={() => void navigator.clipboard?.writeText(reportMarkdown)}>{t(locale, { "zh-CN": "复制", "en-US": "Copy" })}</button>
              </div>
              {reportMarkdown ? <pre className="miniReport">{reportMarkdown}</pre> : null}
            </div>
          </div>

          <div className="collaborationGrid">
            <div className="adviceColumn">
              <h4>{t(locale, { "zh-CN": "设备建议", "en-US": "Device Advice" })}</h4>
              <div className="adviceList">
                {collaboration.deviceAdvice.map((item) => (
                  <article key={item.nodeId} className="adviceCard">
                    <div>
                      <strong>{item.nodeId}</strong>
                      <span>{item.role}</span>
                    </div>
                    <p>{item.advice}</p>
                    <div className="adviceMeta">
                      <span>{item.topic}</span>
                      <span>{item.route}</span>
                      <span>{Math.round(item.confidence * 100)}%</span>
                      <span>{item.latencyMs}ms</span>
                    </div>
                  </article>
                ))}
              </div>
            </div>

            <div className="idlColumn">
              <h4>{t(locale, { "zh-CN": "DSL / IDL / DDS 映射", "en-US": "DSL / IDL / DDS Mapping" })}</h4>
              <div className="mappingList">
                {collaboration.idlMappings.map((item) => (
                  <article key={`${item.topic}-${item.idlType}`} className="mappingRow">
                    <span>{item.topic}</span>
                    <strong>{item.idlModule}.{item.idlType}</strong>
                    <small>{item.qos}</small>
                  </article>
                ))}
              </div>
            </div>
          </div>

          {collaboration.routePlan || collaboration.taskSchedule ? (
            <div className="collaborationGrid">
              <div className="adviceColumn">
                <h4>{t(locale, { "zh-CN": "路径算法", "en-US": "Route Algorithm" })}</h4>
                <article className="adviceCard">
                  <div>
                    <strong>{collaboration.finalDecision.routeAlgorithm ?? "A* / Dijkstra"}</strong>
                    <span>{collaboration.finalDecision.routePath?.join(" -> ") ?? "-"}</span>
                  </div>
                  <p>
                    {t(locale, { "zh-CN": "动态边权综合距离、拥堵、风险、通信覆盖和任务偏好。", "en-US": "Dynamic edge weights combine distance, congestion, risk, coverage, and mission preference." })}
                  </p>
                  <div className="adviceMeta">
                    <span>cost={collaboration.finalDecision.routeCost ?? "-"}</span>
                    <span>{collaboration.finalDecision.consensusPolicy}</span>
                  </div>
                </article>
              </div>
              <div className="idlColumn">
                <h4>{t(locale, { "zh-CN": "任务调度", "en-US": "Task Scheduling" })}</h4>
                <div className="mappingList">
                  {((collaboration.taskSchedule?.assignments as Record<string, unknown>[] | undefined) ?? []).slice(0, 4).map((item) => (
                    <article key={`${item.taskId}-${item.deviceId}`} className="mappingRow">
                      <span>{String(item.taskName ?? item.taskId)}</span>
                      <strong>{String(item.deviceId ?? "-")}</strong>
                      <small>score={String(item.score ?? "-")}</small>
                    </article>
                  ))}
                </div>
              </div>
            </div>
          ) : null}

          <div className="loopTimeline">
            {collaboration.timeline.map((item) => (
              <article key={`${item.time}-${item.title}`}>
                <span>{item.time}</span>
                <strong>{item.title}</strong>
                <p>{item.detail}</p>
              </article>
            ))}
          </div>
        </section>
      ) : null}

      {platformPlan ? (
        <section className="cardPanel collaborationStage">
          <div className="panelHeader">
            <div>
              <span className="eyebrow">Platform</span>
              <h3>{t(locale, { "zh-CN": "完整协同计划", "en-US": "Full Coordination Plan" })}</h3>
            </div>
            <div className="actionRow">
              <span className="softBadge">{String(platformPlan.ddsRuntimeMode ?? planResult?.ddsRuntimeMode ?? "FALLBACK_SIMULATION")}</span>
              <span className="softBadge">{String(platformPlan.executionStatus.state ?? "READY")}</span>
            </div>
          </div>

          <div className="decisionBanner">
            <div>
              <small>{t(locale, { "zh-CN": "任务类型", "en-US": "Task Type" })}</small>
              <strong>{String(platformPlan.intent.taskType ?? "-")}</strong>
              <p>{platformPlan.decisionSummary}</p>
            </div>
            <div className="decisionMetrics">
              <span>{platformPlan.riskWarnings.length}</span>
              <small>{t(locale, { "zh-CN": "风险提示", "en-US": "Risks" })}</small>
            </div>
            <div className="decisionMetrics">
              <span>{platformPlan.ddsSimulation.length}</span>
              <small>DDS</small>
            </div>
            <div className="decisionMetrics">
              <span>{executionTimeline.length}</span>
              <small>{t(locale, { "zh-CN": "时间线", "en-US": "Timeline" })}</small>
            </div>
          </div>

          <div className="collaborationGrid">
            <div className="adviceColumn">
              <h4>{t(locale, { "zh-CN": "结构化 DSL", "en-US": "Structured DSL" })}</h4>
              <div className="mappingList">
                {Object.entries(platformPlan.structuredDsl).slice(0, 12).map(([key, value]) => (
                  <article key={key} className="mappingRow">
                    <span>{key}</span>
                    <strong>{Array.isArray(value) ? value.join(", ") : typeof value === "object" && value ? JSON.stringify(value) : String(value)}</strong>
                  </article>
                ))}
              </div>
            </div>

            <div className="idlColumn">
              <h4>{t(locale, { "zh-CN": "DDS Topic / QoS", "en-US": "DDS Topic / QoS" })}</h4>
              <div className="mappingList">
                {platformPlan.qosPolicies.map((qos) => (
                  <article key={String(qos.topic)} className="mappingRow">
                    <span>{String(qos.topic)}</span>
                    <strong>{String(qos.reliability)} / {String(qos.durability)}</strong>
                    <small>{String(qos.latencyBudgetMs)}ms · retry={String(qos.retryLimit ?? 0)}</small>
                    <p>{String(qos.reason ?? qos.fallbackPolicy ?? "")}</p>
                  </article>
                ))}
              </div>
            </div>
          </div>

          <div className="collaborationGrid">
            <div className="adviceColumn">
              <h4>{t(locale, { "zh-CN": "候选路线", "en-US": "Candidate Routes" })}</h4>
              <div className="mappingList">
                {(((platformPlan.routePlan?.candidateRoutes as Record<string, unknown>[] | undefined) ?? [])).slice(0, 4).map((route) => (
                  <article key={String(route.routeId)} className="mappingRow">
                    <span>{String(route.routeId)}</span>
                    <strong>cost={String(route.cost)} · risk={String(route.risk)}</strong>
                    <small>{String(route.recommended) === "true" ? t(locale, { "zh-CN": "推荐", "en-US": "Recommended" }) : String(route.reason ?? "")}</small>
                  </article>
                ))}
              </div>
            </div>
            <div className="idlColumn">
              <h4>{t(locale, { "zh-CN": "Topic 发布/订阅链路", "en-US": "Topic Pub/Sub Links" })}</h4>
              <div className="mappingList">
                {platformPlan.topics.slice(0, 6).map((topic) => (
                  <article key={String(topic.name)} className="mappingRow">
                    <span>{String(topic.name)} · {String(topic.direction)}</span>
                    <strong>{String(topic.idlType ?? "-")}</strong>
                    <small>{String(topic.ddsTopic ?? "")}</small>
                  </article>
                ))}
              </div>
            </div>
          </div>

          <div className="collaborationGrid">
            <div className="adviceColumn">
              <h4>{t(locale, { "zh-CN": "设备分工", "en-US": "Device Assignments" })}</h4>
              <div className="adviceList">
                {platformPlan.deviceAssignments.map((item) => (
                  <article key={`${item.taskId}-${item.deviceId}`} className="adviceCard">
                    <div>
                      <strong>{String(item.taskName ?? item.taskId)}</strong>
                      <span>{String(item.deviceId ?? "-")} · {String(item.score ?? "-")}</span>
                    </div>
                    <p>{String(item.reason ?? "")}</p>
                    <small>{t(locale, { "zh-CN": "备选设备", "en-US": "Backup" })}: {String(item.backupDeviceId ?? "-")} · {String(item.riskWarning ?? "")}</small>
                  </article>
                ))}
              </div>
            </div>
            <div className="idlColumn">
              <h4>{t(locale, { "zh-CN": "执行日志与降级", "en-US": "Execution Logs & Fallback" })}</h4>
              <div className="mappingList">
                {platformPlan.ddsSimulation.slice(0, 6).map((log, index) => (
                  <article key={`${log.topic}-${log.phase}-${index}`} className="mappingRow">
                    <span>{String(log.phase)} · {String(log.topic)}</span>
                    <strong>{String(log.status)}</strong>
                    <small>{String(log.reason)}</small>
                  </article>
                ))}
              </div>
              <div className="loopTimeline">
                {platformPlan.fallbackStrategies.slice(0, 4).map((item, index) => (
                  <article key={item}>
                    <span>F{index + 1}</span>
                    <strong>{t(locale, { "zh-CN": "降级策略", "en-US": "Fallback" })}</strong>
                    <p>{item}</p>
                  </article>
                ))}
              </div>
            </div>
          </div>

          {executionTimeline.length ? (
            <div className="loopTimeline executionTimeline">
              {executionTimeline.map((item) => (
                <article key={`${item.stage}-${item.timestamp}`}>
                  <span>{new Date(item.timestamp).toLocaleTimeString()}</span>
                  <strong>{item.stage} · {item.status}</strong>
                  <p>{item.actor}{item.topic ? ` / ${item.topic}` : ""}：{item.message}</p>
                  <small>{item.latencyMs}ms · retry={item.retryCount} · risk={item.riskLevel}</small>
                </article>
              ))}
            </div>
          ) : null}
        </section>
      ) : null}

      {planResult ? (
        <section className="cardPanel artifactsStage">
          <div className="panelHeader">
            <div>
              <span className="eyebrow">Artifacts</span>
              <h3>{t(locale, { "zh-CN": "生成产物", "en-US": "Generated Artifacts" })}</h3>
            </div>
            <span className="softBadge">{planResult.compileStatus}</span>
          </div>
          <div className="codePreviewGrid compactCodePreview">
            <div>
              <h4>DSL</h4>
              <pre>{planResult.dslPreview}</pre>
            </div>
            <div>
              <h4>Java</h4>
              <pre>{planResult.javaCode}</pre>
            </div>
          </div>
        </section>
      ) : null}

      <div className="dashboardGrid compactDashboard">
        <TimelinePanel plan={planResult} />

        <section className="cardPanel strategyPanel">
          <div className="panelHeader">
            <div>
              <span className="eyebrow">{t(locale, { "zh-CN": "Plan", "en-US": "Plan" })}</span>
              <h3>{t(locale, { "zh-CN": "协同策略", "en-US": "Coordination" })}</h3>
            </div>
          </div>

          {planResult ? (
            <div className="strategyGrid">
              <div>
                <small>{t(locale, { "zh-CN": "摘要", "en-US": "Summary" })}</small>
                <strong>{planResult.planNarrative}</strong>
              </div>
              <div>
                <small>{t(locale, { "zh-CN": "主链路", "en-US": "Primary" })}</small>
                <strong>{planResult.linkStrategy.primary}</strong>
              </div>
              <div>
                <small>{t(locale, { "zh-CN": "备用链路", "en-US": "Backup" })}</small>
                <strong>{planResult.linkStrategy.backup}</strong>
              </div>
              {planResult.kpis.map((metric) => (
                <div key={metric.label}>
                  <small>{metric.label}</small>
                  <strong>{metric.value}</strong>
                </div>
              ))}
            </div>
          ) : (
            <div className="emptyPanel">{t(locale, { "zh-CN": "生成方案后显示。", "en-US": "Visible after plan generation." })}</div>
          )}
        </section>
      </div>

      <div className="twoColumnSection">
        <TopicTable
          title={t(locale, { "zh-CN": "订阅主题", "en-US": "Subscriptions" })}
          topics={planResult?.subscribeTopics ?? []}
          mode="sub"
          onToggle={(topic) => void topicAction(topic.name, "toggle")}
          onTest={(topic) =>
            void topicAction(topic.name, "test", t(locale, { "zh-CN": "订阅链路测试通过", "en-US": "Subscription test passed" }))
          }
        />

        <TopicTable
          title={t(locale, { "zh-CN": "发布主题", "en-US": "Publications" })}
          topics={planResult?.publishTopics ?? []}
          mode="pub"
          onToggle={(topic) => void topicAction(topic.name, "toggle")}
          onTest={(topic) => void topicAction(topic.name, "publish", "MISSION_EVENT:READY")}
        />
      </div>
    </div>
  );
}
