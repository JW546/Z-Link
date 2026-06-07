# API 接口说明

本文档描述 Z-Link 无人系统意图协同平台实验型原型的主要 HTTP API。后端使用 Java 原生 `HttpServer`，默认地址为 `http://localhost:8080/api`。

## GET /api/health

返回基础服务健康状态、时间戳和 LLM 配置信息。

## POST /api/agent/plan

输入：

```json
{ "text": "紧急调度无人车将患者送往医院，并请求无人机和 RSU 协同" }
```

返回主要字段：

- `missionId`：任务编号
- `dslPreview`：兼容旧链路的字符串 DSL
- `structuredDsl`：统一 `MissionDslModel` 结构化任务模型
- `platformPlan`：完整平台计划
- `collaboration`：协同决策结果
- `subscribeTopics` / `publishTopics`：兼容前端 Topic 表格
- `ddsRuntimeMode`：DDS 运行模式
- `riskWarnings` / `fallbackStrategies`：风险与降级策略

## POST /api/agent/execute

输入：

```json
{ "missionId": "MSN-..." }
```

返回：

- `accepted`
- `missionId`
- `runId`
- `status`
- `timeline`
- `timelineSummary`

该接口会创建一次可回放执行记录，而不是只返回 accepted。

## POST /api/agent/simulate

输入自然语言任务，返回一次完整执行仿真，包括 DDS 发布/订阅、重试、降级和 `executionTimeline`。

## GET /api/agent/devices

返回设备注册中心中的设备状态，字段包括：

- `deviceId`
- `deviceType`
- `name`
- `capabilities`
- `supportedTopics`
- `batteryLevel`
- `currentLoad`
- `linkQuality`
- `location`
- `status`
- `lastHeartbeat`
- `failureReason`
- `assignedMissionId`

同时保留旧前端字段 `id`、`online`、`battery`、`link`、`role`。

## GET /api/agent/topics

返回 DDS Topic 目录、IDL 类型、方向、默认 QoS、发布者、订阅者、最近通信日志和 `runtimeMode`。

## GET /api/agent/logs

支持简单 query 参数：

- `missionId`
- `runId`

返回 DDS 通信日志 `logs` 和执行时间线日志 `executionLogs`。

## POST /api/device/action

输入：

```json
{ "deviceId": "UAV-CQ-007", "action": "reconnect" }
```

支持动作：

- `bind`：恢复在线
- `unbind`：标记离线
- `reconnect`：恢复在线并改善链路状态
- `inspect` / `check`：刷新心跳并降低少量负载

动作会真实更新 `DeviceRegistry`，后续调度会读取更新后的设备状态。

## 第二轮新增接口

### GET /api/agent/scenarios

返回典型任务场景库。每个场景包含 `scenarioId`、`name`、`description`、`defaultIntentText`、`taskType`、`recommendedDevices`、`requiredTopics`、`qosLevel`、`riskFactors` 和 `expectedOutput`。

### GET /api/agent/scenario?id=medical-transfer

返回指定场景详情。原生 HttpServer 使用 query 参数实现简单路径兼容。

### POST /api/agent/scenario/plan

输入：

```json
{ "scenarioId": "medical-transfer" }
```

返回该场景对应的 `platformPlan`、`structuredDsl`、`riskAssessment`、`ddsSimulation` 和 `fallbackStrategies`。

### POST /api/agent/fault/inject

输入：

```json
{ "faultType": "LINK_DEGRADED", "target": "UAV-CQ-007" }
```

支持 `DEVICE_OFFLINE`、`LOW_BATTERY`、`LINK_DEGRADED`、`DDS_PUBLISH_FAILED`、`HIGH_CONGESTION`、`ROUTE_BLOCKED`、`SENSOR_UNAVAILABLE`。

### POST /api/agent/fault/clear

清除所有故障注入，并恢复设备注册中心默认状态。

### GET /api/agent/topic-topology

返回 Topic 拓扑，包括 `nodes`、`edges`、`qosProfile`、`runtimeMode` 和 `recentStatus`。

### GET /api/agent/history

返回最近任务历史，包含 `missionId`、`runId`、`originalText`、`taskType`、`priority`、`status`、`routePath`、`assignedDevices`、`riskLevel`、`timelineSummary` 等字段。

### GET /api/agent/dashboard

返回平台运行驾驶舱指标：任务数、设备数、平均风险、DDS 模式、故障数、重试数、Topic 数和更新时间。

### GET /api/agent/report?missionId=xxx

返回 Markdown 格式任务报告，内容覆盖任务概述、结构化 DSL、设备、DDS Topic/QoS、路径、调度、风险、fallback、执行时间线和通信日志。
