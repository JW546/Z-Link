# 执行仿真与 DDS 通信说明

本项目不要求用户安装真实 DDS 环境。平台通过 `DdsCommunicationService` 提供统一通信抽象，在真实 DDS 环境不可用时自动进入模拟模式，保证免安装原型仍可运行。

## DDS 运行模式

- `REAL_DDS`：真实 DDS 发布/订阅调用可用。
- `SIMULATED_DDS`：检测到 DDS 管理类，但当前免安装环境未确认原生 DDS runtime，使用兼容模拟。
- `FALLBACK_SIMULATION`：DDS 管理类或运行环境不可用，使用内存级通信仿真。

## QoS 策略

QoS 以结构化对象输出，包括：

- `reliability`
- `durability`
- `deadlineMs`
- `latencyBudgetMs`
- `historyDepth`
- `priority`
- `retryLimit`
- `fallbackPolicy`
- `reason`

应急、医疗和协同计划类 Topic 默认使用可靠传输、短 deadline、较高优先级和重试策略；感知流类 Topic 可使用 best-effort，以降低带宽和延迟压力。

## 执行时间线

`MissionExecutionService` 会为每次执行生成 `runId` 和 `executionTimeline`。核心阶段包括：

- `INTENT_PARSED`
- `DSL_GENERATED`
- `IDL_BOUND`
- `QOS_SELECTED`
- `ROUTE_PLANNED`
- `TASK_SCHEDULED`
- `DDS_PUBLISH`
- `DDS_SUBSCRIBE`
- `DEVICE_ACK`
- `RISK_CHECK`
- `FALLBACK_TRIGGERED`
- `COMPLETED`

每个事件包含 `timestamp`、`stage`、`actor`、`topic`、`action`、`status`、`message`、`latencyMs`、`retryCount` 和 `riskLevel`，用于前端回放和实验报告分析。

## 降级策略

当出现 DDS 发送失败、best-effort 丢包、任务未分配、设备离线或链路质量差时，系统会返回风险提示，并建议：

- 可靠 Topic 重试后切换广域链路摘要同步；
- 无人机不可用时由 RSU 接管局部感知；
- 路径不可达时回退主路候选并降低速度；
- 应急任务中优先请求 RSU 绿波通道，应急车辆作为备选执行主体。

## 第二轮增强

### 故障注入

系统新增 `FaultInjectionService`，支持设备离线、低电量、链路退化、DDS 发布失败、高拥堵、路径阻断和传感器不可用。故障注入会影响设备注册中心状态、DDS 通信结果、风险评估、fallback 策略和执行时间线。

### 风险评估

系统新增 `RiskAssessmentEngine`，综合任务优先级、设备电量、负载、链路质量、路径风险、DDS 失败次数和故障注入状态，输出 `riskScore`、`riskLevel`、风险因子、缓解措施和是否需要 fallback。

### Topic 拓扑

`TopicTopologyService` 将设备、Topic 和服务模块建模为节点，将 publish、subscribe、retry 和 fallback 建模为边，便于前端用表格和卡片展示 DDS 通信关系。

### 任务历史与报告

`MissionHistoryService` 记录最近任务计划和执行结果，`ReportService` 可按 missionId 生成 Markdown 任务报告，用于实验报告和答辩材料。
