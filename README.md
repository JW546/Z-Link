# Z-Link 无人系统意图协同平台（实验型原型）

本项目是一个面向无人车、无人机、RSU、应急车辆和广域通信链路的异构无人系统意图协同平台原型。平台通过自然语言任务输入、结构化 DSL、IDL/DDS Topic 映射、QoS 策略选择、多设备协同调度、路径规划、通信仿真和执行时间线，模拟无人系统之间的协同决策闭环。

> 说明：本项目用于课程实验、原型验证和报告展示，不是生产级真实无人系统控制平台。

## 技术栈

- 后端：Java，原生 `com.sun.net.httpserver.HttpServer`
- 前端：React + Vite + TypeScript
- 数据交换：JSON API
- 模型与算法：结构化 Mission DSL、IDL/DDS Topic 映射、A*/Dijkstra 动态路径规划、基于动态权重评分的贪心任务调度
- 通信模式：真实 DDS 管理类探测 + 自动模拟 fallback

## 目录结构

- `source/server/FrontendApiServer.java`：后端 HTTP API 入口
- `source/agent/PlatformOrchestrator.java`：平台任务编排主流程
- `source/agent/MissionDslModel.java`：统一任务模型与结构化 DSL Schema
- `source/agent/DeviceRegistry.java`：内存级设备注册、心跳、故障与恢复
- `source/agent/DdsCommunicationService.java`：DDS 通信统一抽象与模拟 fallback
- `source/agent/MissionExecutionService.java`：可回放执行时间线
- `source/agent/AdaptiveRoutePlanner.java`：A*/Dijkstra 动态边权路径规划
- `source/agent/DynamicTaskScheduler.java`：动态权重贪心任务调度
- `frontend/s`：Z-Link React/Vite 前端工作台
- `idl`：IDL 数据结构定义
- `docs`：实验报告材料与设计说明
- `tools/api_smoke_test.ps1`：API smoke test 脚本

## 启动方式

后端编译：

```powershell
.\runtime\jdk\bin\javac.exe -encoding UTF-8 -cp "build-classes;generated-src;source;lib/*" -sourcepath source -d build-classes source\server\FrontendApiServer.java
```

后端运行：

```powershell
.\runtime\jdk\bin\java.exe -cp "build-classes;generated-src;source;lib/*" server.FrontendApiServer 8080
```

前端构建：

```powershell
cd frontend\s
npm run build
```

前端开发运行：

```powershell
cd frontend\s
npm run dev
```

## 核心功能

- 自然语言任务输入与规则意图识别
- 统一 `MissionDslModel` 结构化 DSL 输出，并保留旧 `dslPreview` 字符串兼容字段
- IDL 类型绑定与 DDS Topic 发布/订阅链路展示
- QoS 结构化策略：`reliability`、`durability`、`deadlineMs`、`latencyBudgetMs`、`historyDepth`、`priority`、`retryLimit`、`fallbackPolicy`
- DDS 通信统一入口：`REAL_DDS`、`SIMULATED_DDS`、`FALLBACK_SIMULATION`
- 设备注册中心：注册、心跳、状态更新、故障标记、恢复在线
- A*/Dijkstra 动态路径规划，输出候选路线、代价、风险和推荐理由
- 基于动态权重评分的贪心任务调度，输出设备评分、风险提示和备选设备
- `/api/agent/execute` 与 `/api/agent/simulate` 生成可回放执行时间线
- 前端展示 DSL、设备状态、Topic/QoS、路径候选、任务分配、DDS 日志、风险与降级策略
- 典型场景库：内置医疗应急转运、事故巡检、联合巡逻、物资配送、极端天气风险监测和跨区域态势同步
- 故障注入：支持设备离线、低电量、链路退化、DDS 发布失败、高拥堵、路径阻断和传感器不可用
- 风险评估：输出 riskScore、riskLevel、riskFactors、mitigationActions 和 fallbackRequired
- Topic 拓扑：输出设备、Topic、服务模块节点和 publish/subscribe/retry/fallback 边
- 任务历史与回放：记录最近任务计划、执行结果、设备分工、风险和时间线摘要
- 平台 Dashboard：展示任务、设备、风险、DDS 模式、故障、重试和 Topic 数
- Markdown 报告导出：生成可放入实验报告的任务报告文本

## API 摘要

- `GET /api/health`：基础服务健康状态
- `POST /api/agent/plan`：输入自然语言任务，返回完整协同计划
- `POST /api/agent/execute`：按 missionId 创建执行记录并返回 runId、状态和时间线摘要
- `POST /api/agent/simulate`：生成完整模拟执行过程和 executionTimeline
- `GET /api/agent/devices`：返回设备注册中心实时状态
- `GET /api/agent/topics`：返回 DDS Topic、IDL、QoS、发布者/订阅者和最近通信日志
- `GET /api/agent/logs?missionId=&runId=`：返回 DDS 通信日志和执行时间线日志
- `GET /api/agent/health`：返回平台模块健康状态
- `POST /api/device/action`：执行 `bind`、`unbind`、`reconnect`、`inspect/check` 并更新设备注册中心
- `GET /api/agent/scenarios`：返回典型场景库
- `GET /api/agent/scenario?id=xxx`：返回单个场景详情
- `POST /api/agent/scenario/plan`：按场景快速生成计划
- `GET /api/agent/faults`：返回当前故障注入状态
- `POST /api/agent/fault/inject`：注入实验故障
- `POST /api/agent/fault/clear`：清除故障并恢复默认设备状态
- `GET /api/agent/topic-topology`：返回 DDS Topic 拓扑
- `GET /api/agent/history`：返回任务历史和回放摘要
- `POST /api/agent/history/clear`：清空任务历史
- `GET /api/agent/dashboard`：返回平台运行驾驶舱指标
- `GET /api/agent/report?missionId=xxx`：返回 Markdown 任务报告

## 测试方式

启动后端后运行：

```powershell
.\tools\api_smoke_test.ps1 -BaseUrl http://localhost:8080/api
```

脚本会验证 `/api/health`、`/api/agent/plan`、`/api/agent/devices`、`/api/agent/topics`、`/api/agent/simulate`、`/api/agent/execute` 和 `/api/agent/logs`。
第二轮脚本还会验证场景库、故障注入、Topic 拓扑、任务历史、Dashboard 和报告导出接口。

## 截图建议

- 工作台顶部 Dashboard 指标卡片
- 典型场景快捷入口和一键生成计划
- 风险评估卡片与故障注入面板
- Topic 拓扑节点与通信边
- 任务历史与回放记录
- Markdown 任务报告
- smoke test 15 项通过结果

## 后续优化方向

- 接入真实 DDS Runtime 后，将 `DdsCommunicationService` 的反射探测替换为明确适配器
- 将内存级设备注册中心替换为可持久化状态服务
- 接入真实地图路网与实时交通数据，替换当前实验路网图
- 增加自动化端到端测试和接口契约测试
- 扩展 DSL/IDL 的双向校验能力，减少手工映射错误
