# PROJECT_PLAN.md

本文件用于记录“智通无阻（Z-Link）——面向无人设备的自然语言意图驱动协同系统”的长期开发流程和当前进度。

后续每轮 Codex 开发任务完成后，必须自动更新本文件中的阶段状态、当前进度和必要备注。

## 状态说明

- TODO：尚未开始；
- IN_PROGRESS：正在进行；
- DONE：已完成；
- BLOCKED：受阻，需明确阻塞原因。

## 长期阶段计划

| 阶段 | 名称 | 状态 |
| --- | --- | --- |
| 0 | baseline 备份 | DONE |
| 1 | AGENTS.md 建立 | DONE |
| 2 | PROJECT_PLAN.md 建立 | DONE |
| 3 | 项目诊断 | DONE |
| 4 | 测试基线 | DONE |
| 5 | 模块重构 | DONE |
| 6 | DDSMessageBus | DONE |
| 7 | AgentRuntime | DONE |
| 8 | WorldModel | DONE |
| 9 | RiskAwareRoutePlanner / MultiObjectivePlanner | DONE |
| 10 | ConflictResolver | DONE |
| 11 | ScenarioLibrary | DONE |
| 12 | EvaluationService | DONE |
| 13 | AgentState / AgentAction / RewardFunction | DONE |
| 14 | AgentPolicy | DONE |
| 15 | PolicyEvaluator | DONE |
| 16 | PolicyOptimizationService | DONE |
| 17 | RL / MARL 接口预留 | DONE |
| 18 | Dashboard | DONE |
| 19 | 报告 / 论文 / PPT | IN_PROGRESS |

## 当前进度

- 已完成 baseline 备份阶段记录。
- 已建立 AGENTS.md，用于约束后续 Codex 开发任务。
- 已建立 PROJECT_PLAN.md，用于记录长期开发流程和当前进度。
- 已完成只读项目诊断，明确后端入口、前端入口、NLU/DSL、DDS/IDL、Agent、路线规划、融合、冲突与测试现状。
- 已建立最小测试基线：后端 API smoke test、协同意图端到端测试、前端 TypeScript/Vite 构建验证。
- 已完成第一轮架构模块化重构，新增 intent、dds、worldmodel、planner、decision、simulation、evaluation、policy、api、dashboard 等模块外壳和关键接口；现有对外 API 表现保持不变。
- 已实现虚拟 DDSMessageBus：支持默认 Topic 注册、兼容性自动 Topic 注册、QoS、发布订阅、消息来源、置信度、时间戳、延迟、丢包、过期判断、新鲜度和过期降权。
- 已实现 AgentRuntime 与 AgentPolicy 机制：VehicleAgent、DroneAgent、RoadsideAgent、NeighborVehicleAgent、CommunicationAdapterAgent 支持 observe、updateState、evaluate、propose、arbitrate、act、explain，并通过加权策略生成建议。
- 已实现动态 WorldModel：支持乘客意图、DSL 约束、候选路线、拥堵、安全风险、多设备建议、来源可信度、消息置信度、新鲜度、冲突状态、重规划状态和时间线，并可构造 AgentState。
- 已实现风险感知与多目标路线规划：保留 DijkstraPlanner 和 GreedyPlanner 作为 baseline，新增 RiskAwareRoutePlanner、MultiObjectiveRoutePlanner 和 DynamicReplanningService，支持安全风险硬约束与 DDS 证据质量评分。
- 已实现 DDS 消息冲突检测与冲突消解：ConflictResolver 支持路线分歧、乘客意图张力、安全风险冲突、证据质量冲突，输出 conflictType、acceptedSuggestion、rejectedSuggestions、priorityOrder、decisionReason、confidence、whetherReplanningTriggered 和 timelineEvent。
- 已实现 ScenarioLibrary：包含医院急救、拥堵绕行、事故避让、无人机与路侧冲突、邻车风险冲突、DDS 延迟、DDS 丢包、低置信度消息、动态重规划、乘客意图冲突等 10 个结构化场景，并支持运行输出 DDS 时间线、最终决策和评估指标。
- 已实现 EvaluationService：支持 successRate、averageTravelTime、averageSafetyRisk、congestionAvoidanceRate、replanningCount、conflictResolutionAccuracy、explanationCompleteness、messageDelayImpact、packetLossImpact、averageReward，并支持单场景评估、ScenarioLibrary 批量评估、BaselineComparison 四算法对比和 JSON 实验日志导出。
- 已深化 AgentState / AgentAction / RewardFunction：AgentState 覆盖乘客意图、DSL 约束、路线、多源 DDS 消息、多设备建议、风险与证据质量；AgentAction 覆盖路线选择、建议接受/拒绝、重规划、补充信息、告警、等待、安全/时间优先级切换；DefaultRewardFunction 支持安全强惩罚、冲突处理、低置信度误信、高可信风险忽略、重规划次数与解释完整度评估。
- 已实现多种 AgentPolicy：RuleBasedAgentPolicy 作为 baseline，WeightedUtilityAgentPolicy 基于时间、安全、拥堵、冲突、置信度、新鲜度多目标评分，AdaptiveAgentPolicy 可根据 ScenarioLibrary 评估结果进行启发式权重更新；三类策略均支持状态抽取、动作选择、奖励计算、解释输出、策略配置和评估结果输出。
- 已实现 PolicyEvaluator：可批量运行 ScenarioLibrary，对 RuleBasedAgentPolicy、WeightedUtilityAgentPolicy、AdaptiveAgentPolicy 输出 successRate、averageTravelTime、averageSafetyRisk、conflictResolutionAccuracy、replanningCount、explanationCompleteness、averageReward，选择最优策略并生成前端展示 JSON。
- 已实现 PolicyOptimizationService：支持网格搜索和随机搜索 AgentPolicy 权重，通过 RewardFunction/ScenarioLibrary 选择最优参数，输出优化日志、最优 policy 配置、优化前后 EvaluationService 对比，并强制 safetyWeight 下限与安全风险不升高约束。
- 已完成 RL / MARL 接口预留：新增 PolicyEnvironment、Episode、StepResult、ReplayRecord、PolicyTrainer、RandomPolicy、HeuristicPolicy、RLTrainingConfig、MARLTrainingConfig，为后续 DQN、PPO、SAC、MARL 接入预留协议和数据结构；当前仅为接口与 baseline，不包含真实强化学习训练、训练曲线、episode 采样或神经网络训练。
- 已升级 Dashboard：后端 /api/agent/plan 输出 dashboardTrace，前端工作台新增“无人设备意图协同完整闭环”面板，展示乘客输入、解析意图、DSL 任务契约、IDL/DDS Topic 映射、DDS 消息时间线、多类 Agent 建议、冲突检测、冲突消解、最终路线、选择/拒绝原因、重规划状态、四类路线规划算法对比、三类 AgentPolicy 对比、AgentPolicy 优化前后对比、实验指标图表和重规划时间线。
- 已生成当前阶段技术报告材料，覆盖项目背景、总体架构、NL 意图到 DSL、DDS/IDL、多 Agent 协同、冲突消解、风险感知规划、策略优化、场景库、实验指标、baseline、前端展示、局限性与后续工作；论文和 PPT 尚未正式生成。

## 更新记录

- 2026-06-28：创建 PROJECT_PLAN.md，初始化长期阶段计划。
- 2026-06-28：完成项目诊断阶段；新增 tools/test_baseline.ps1 和 tools/e2e_intent_collaboration_test.ps1，验证后端 API 与“尽快去医院，优先安全，避开拥堵”端到端协同场景；前端构建通过。
- 2026-06-28：完成第一轮模块重构；新增 integration.ArchitectureModuleSmokeTest 并纳入测试基线；新增 DEVELOPMENT.md 说明模块职责。DDSMessageBus、AgentRuntime、WorldModel、ConflictResolver、Evaluation 与 Policy 相关阶段进入 IN_PROGRESS，表示已建立接口但仍需深化实现。
- 2026-06-28：完成虚拟 DDSMessageBus 实现；新增 integration.DDSMessageBusTest，覆盖发布订阅、Topic 隔离、延迟新鲜度、过期降权和丢包记录；后端测试基线通过。
- 2026-06-28：完成 AgentRuntime、WorldModel、RiskAwareRoutePlanner / MultiObjectivePlanner、AgentState / AgentAction / RewardFunction、AgentPolicy 阶段；新增 integration.AgentRuntimePolicyTest、integration.WorldModelTest、integration.RoutePlannerTest 并纳入测试基线；后端测试基线通过。
- 2026-06-30：完成 ConflictResolver 阶段；新增 integration.ConflictResolverTest，覆盖无人机/路侧/邻车路线冲突、乘客“尽快”与“优先安全”意图张力、DDS 延迟/过期/低置信度/低可信来源证据质量冲突，以及“尽快去医院，优先安全，避开拥堵”的端到端冲突消解场景；后端测试基线通过。
- 2026-06-30：完成 ScenarioLibrary 阶段；扩展 agent.ScenarioLibrary 为结构化实验场景库，新增 SimulationScenarioService.runScenario 与 EvaluationService.evaluateScenario；新增 integration.ScenarioLibraryTest，验证每个场景可加载、可运行、可输出 DDS 时间线、最终决策并可用于评估；后端测试基线通过。
- 2026-06-30：完成 EvaluationService 阶段；新增 evaluation.BaselineComparison，对比 DijkstraPlanner、GreedyPlanner、RiskAwareRoutePlanner、MultiObjectiveRoutePlanner；新增 integration.EvaluationServiceTest，验证单场景评估、批量评估、四算法对比和 JSON 实验日志导出；后端测试基线通过。评估仍为启发式评估，不声称完成 RL/MARL 训练。
- 2026-06-30：深化 AgentState / AgentAction / RewardFunction；新增 policy.DefaultRewardFunction 与 integration.AgentPolicyModelTest，覆盖 AgentState 构建、AgentAction 选择、RewardFunction 计算、高风险路线强惩罚和正确冲突处理奖励提升；后端测试基线通过。该奖励函数仅用于启发式策略评估，不代表已完成强化学习训练。
- 2026-06-30：深化 AgentPolicy 阶段；新增 policy.RuleBasedAgentPolicy、policy.WeightedUtilityAgentPolicy、policy.AdaptiveAgentPolicy 与 integration.AgentPolicyVariantsTest，验证三类策略的状态抽取、动作选择、奖励计算、解释输出、配置和评估输出；AdaptiveAgentPolicy 仅做启发式权重自适应，不声称完成 RL/MARL 训练；后端测试基线通过。
- 2026-06-30：完成 PolicyEvaluator 阶段；扩展 policy.PolicyEvaluator 支持三类 AgentPolicy 在 ScenarioLibrary 上批量评估、逐策略指标输出、最优策略选择、原因解释和前端 JSON 生成；新增 integration.PolicyEvaluatorTest；后端测试基线通过。
- 2026-06-30：完成 PolicyOptimizationService 阶段；扩展 policy.PolicyOptimizationService 支持 timeWeight、safetyWeight、congestionWeight、conflictWeight、confidenceWeight、freshnessWeight、replanningPenaltyWeight 的网格搜索与随机搜索；新增 integration.PolicyOptimizationServiceTest，验证优化后 averageReward 提高、averageSafetyRisk 不升高、安全权重下限、低置信度场景 confidenceWeight 生效、优化日志与前后对比输出；后端测试基线通过。该优化仍为启发式参数搜索，不代表完成 RL/MARL 训练。
- 2026-06-30：完成 RL / MARL 接口预留阶段；新增 policy.PolicyEnvironment、policy.Episode、policy.StepResult、policy.ReplayRecord、policy.PolicyTrainer、policy.RandomPolicy、policy.HeuristicPolicy、policy.RLTrainingConfig、policy.MARLTrainingConfig，并新增 integration.RLInterfaceReservationTest 纳入测试基线；README 明确当前已完成场景驱动策略评估、权重搜索优化、AgentPolicy 对比和 RL/MARL 接口预留，同时声明尚未完成真实强化学习训练、神经网络模型训练、多智能体端到端训练和真实设备数据训练。
- 2026-06-30：完成 Dashboard 阶段；新增 dashboardTrace API 数据链路和前端闭环展示面板，扩展端到端测试断言 Dashboard 所需 DDS 时间线、冲突检测/消解、路线算法对比、策略对比、优化前后、实验指标和重规划时间线；修复后端 CORS 允许 Accept-Language 与 X-Z-Link-Locale 请求头；已使用 browser/control-in-app-browser 在本地页面输入“尽快去医院，优先安全，避开拥堵”并验证完整闭环可见。
- 2026-07-02：进入报告 / 论文 / PPT 阶段；完成当前项目技术报告材料梳理。报告严格按现有代码和测试结果描述，明确“预留强化学习训练接口，当前采用场景驱动策略优化与权重搜索”，不声称已完成真实 RL/MARL 训练。
- 2026-07-02：完成 Agent 路线 ID 硬编码去除改造；新增 planner.CandidateRoute、RouteFeatureExtractor、RouteScoringContext、RouteScore、RouteSelectionResult，将 VehicleAgent、DroneAgent、RoadsideAgent、NeighborVehicleAgent、CommunicationAdapterAgent 的路线选择统一改为基于候选路线特征、WorldModel/AgentState 与加权评分的动态选择；新增 integration.RouteFeatureSelectionTest 验证最快高风险路线拒绝、非预设 ID 可胜出、路线改名后仍按特征选择、VehicleAgent 不依赖固定 routeId；完整测试基线和前端构建通过。
- 2026-07-02：完成全项目“假智能”硬编码排查与修复；保留 ScenarioLibrary、integration 测试、baseline 对比和前端演示中的固定 routeId 作为场景/测试数据，移除 AgentRuntime、CollaborationDecisionEngine、ConflictResolver、EvaluationService、PlatformOrchestrator 中根据固定路线名称或 routeId 直接决策的逻辑；新增 RouteFeatureSelectionTest 覆盖路线改名后仍基于特征、评分、风险阈值、置信度与新鲜度选择；完整测试基线和前端构建通过。
- 2026-07-02：运行完整 ScenarioLibrary 并导出 EvaluationService 实验结果；新增 integration.EvaluationSummaryExport，生成 results/evaluation_summary.json 与 results/evaluation_summary.csv，覆盖 DijkstraPlanner、GreedyPlanner、RiskAwareRoutePlanner、MultiObjectiveRoutePlanner、RuleBasedAgentPolicy、WeightedUtilityAgentPolicy、AdaptiveAgentPolicy，并为缺失指标标注 not_available 状态，避免将暂无数据误读为真实 0。
- 2026-07-02：运行 PolicyOptimizationService 场景驱动权重搜索；新增 integration.PolicyOptimizationExport，生成 results/policy_optimization_report.md、results/policy_optimization.json、results/policy_optimization.csv，输出 WeightedUtilityAgentPolicy 与 AdaptiveAgentPolicy 的优化前后权重、averageReward、averageSafetyRisk、conflictResolutionAccuracy、安全回归检查和最优参数；报告明确为 heuristic policy optimization，不包含 RL/MARL 训练声明。
- 2026-07-02：完成前端 Dashboard 产品化美化与交互重构；将 WorkbenchPage 从开发调试长页面重构为“无人设备自治协同指挥驾驶舱”，新增 TopBar 演示/调试模式切换、7 项 Sidebar 导航、总览指标首屏、任务闭环 Timeline、60/40 地图与决策主工作区、详情 Tab、默认折叠 JsonInspector 与调试工具区；核心后端接口和协同能力未改动，前端 TypeScript/Vite 构建通过，并使用 browser/control-in-app-browser 验证本地页面、生成闭环、Policy Tab、调试模式和演示模式。
- 2026-07-03：完成当前版本阶段冻结验收；使用 browser/control-in-app-browser 在 1366px、1440px、1920px 宽度验证前端页面可启动、任务工作台可输入“尽快去医院，优先安全，避开拥堵”、完整闭环可见、演示模式简洁、调试模式可查看原始 JSON 与日志、主要卡片无明显横向溢出且地图/右侧信息不重叠；重新运行后端测试、前端构建和核心端到端场景，准备以 `polish dashboard and validate autonomous decision prototype` 冻结提交当前版本。
