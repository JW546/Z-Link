# DEVELOPMENT.md

本文件记录当前架构模块职责，供后续开发任务使用。

## 架构原则

本项目是面向无人设备的自然语言意图驱动协同系统。前端只负责演示和实验可视化，核心链路仍然是：

自然语言意图 -> DSL 任务契约 -> IDL/DDS 发布订阅 -> 多设备 Agent 自主协同 -> 冲突消解 -> 可解释路线决策

## 模块职责

- `source/intent`：自然语言意图解析和 DSL 任务契约生成。当前通过 `IntentContractService` 复用 `NLUProcessor`、`DSLGenerator` 和 `MissionDslModel`。
- `source/dds`：DDS MessageBus、Topic、Message、QoS 抽象。当前新增 `VirtualDDSMessageBus` 作为内存发布订阅适配器，真实 DDS 仍由既有 `DDSManager` 承担。
- `source/agent`：无人车、邻车、无人机、路侧节点、通信适配节点等 Agent 逻辑。当前新增 `AgentRuntime` 和 `AgentAdvice`，并保留既有编排与演示逻辑。
- `source/worldmodel`：由 DDS 消息构建系统态势，包括拥堵、事故、道路风险、消息置信度和时间戳。
- `source/planner`：路线规划接口。当前 `RoutePlanningService` 保留 Dijkstra baseline 标签，并复用 `AdaptiveRoutePlanner` 做动态规划。
- `source/decision`：冲突检测、冲突消解、优先级判断和建议融合。当前 `ConflictResolver` 覆盖多设备路线建议冲突。
- `source/simulation`：场景库、延迟、丢包、消息冲突、重规划触发。当前通过 `SimulationScenarioService` 复用场景库和故障注入。
- `source/evaluation`：成功率、到达时间、风险暴露、重规划次数、冲突处理正确率等指标。
- `source/policy`：`AgentState`、`AgentAction`、`RewardFunction`、`AgentPolicy`、`PolicyEvaluator`、`PolicyOptimizationService`。当前是策略评估与启发式选择接口，不代表已经完成 RL/MARL 训练。
- `source/api`：前后端接口门面。`PlatformApiFacade` 用于让 HTTP Server 远离平台编排细节。
- `source/dashboard`：面向前端展示的数据适配层。

## 测试基线

- 后端与端到端基线：`powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\test_baseline.ps1`
- 前端构建：在 `frontend` 目录运行 `npm run build`
- 架构模块 smoke：已纳入 `tools/test_baseline.ps1`，入口是 `integration.ArchitectureModuleSmokeTest`

## 当前注意事项

- Dijkstra 和 Greedy 仍只能作为 baseline。
- `policy` 模块当前没有真实强化学习训练，不能声称完成 RL/MARL。
- 新模块目前是低风险外壳和适配层，后续应逐步把旧的大类实现迁移到这些深模块中。
