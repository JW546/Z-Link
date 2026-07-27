# 本地真实通信协同运行时

本模块用于第一阶段落地：没有真实无人机和无人车时，仍然让后端核心系统以真实进程通信方式运行。

它不是前端假动画，也不是把所有逻辑写进一个页面。前端如果存在，只能订阅和展示这些后端消息。

## 系统位置

核心链路仍然是：

```text
自然语言意图
-> DSL 任务契约
-> IDL/DDS 语义 Topic
-> 多设备 Agent 自主协同
-> 冲突检测与消解
-> 动态重规划
-> 可解释路线决策
-> 策略评估
```

当前新增的 `source/collaboration/runtime` 负责其中的：

- 本地 DDS 兼容 Broker；
- socket 级发布订阅；
- 无人机、自车、peer car、协同指挥 Agent；
- DJI / ROS2-DDS / Generic 私有 JSON 风格 Adapter；
- 周期状态同步；
- 冲突事件；
- 重规划请求；
- 最终决策解释；
- baseline 与协同评分策略对比结果。

## 两种启动方式

单命令运行完整后端核心：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\run_local_runtime.ps1 -IntervalMs 500 -Ticks 8
```

分布式方式运行，每个 Agent 是独立 Java 进程：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\run_local_runtime_distributed.ps1 -Port 19090 -IntervalMs 500 -Ticks 120
```

分布式脚本会启动：

- `LocalDDSBrokerMain`
- `CoordinatorAgentMain`
- `RuntimeDeviceAgent UAV`
- `RuntimeDeviceAgent EGO_CAR`
- `RuntimeDeviceAgent PEER_CAR`

## HTTP 验收接口

后端 API 也暴露了这个真实通信运行时，前端只负责调用和展示结果。

查看通信配置：

```text
GET /api/runtime/profile
```

查看 runtime Topic / IDL 绑定：

```text
GET /api/runtime/topics
```

运行一次后端 4-Agent socket 协同：

```text
POST /api/runtime/run
{
  "intervalMs": 500,
  "ticks": 6
}
```

返回内容包括：

- `history`：本地 DDS 兼容 Broker 收到的真实 socket 消息；
- `agentStatuses`：无人机、自车、peer car 周期状态；
- `nativeProtocolFrames`：DJI / ROS2-DDS / Generic 私有 JSON 风格报文；
- `conflictEvents`：路线冲突事件；
- `replanningRequests`：动态重规划请求；
- `finalDecision`：协同评分策略输出的最终决策解释；
- `policyEvaluations`：Dijkstra、Greedy、协同评分策略的对比。

工作台页面现在包含“真实后端多 Agent 通信”展示区。点击“运行一次后端协同”时，页面只调用 `/api/runtime/run` 并展示结果，不在前端实现通信、冲突消解或重规划逻辑。

## 通信频率

默认状态同步间隔为 `500ms`，也就是 `2Hz`。

可以通过 `-IntervalMs` 调整：

- `1000`：1Hz，便于观察；
- `500`：2Hz，默认准实时；
- `200`：5Hz，更接近高频状态同步实验。

## 当前协议 Adapter

统一状态先在后端核心里表达，再映射到不同设备协议风格：

- `DjiDroneProtocolAdapter`：DJI 风格无人机遥测与航点任务；
- `Ros2VehicleProtocolAdapter`：ROS2/DDS 风格车辆 Topic / IDL；
- `GenericPeerProtocolAdapter`：第三方私有 JSON 风格 peer car。

这体现的是 DSL/IDL 层消除上层任务语义异构，Adapter 负责落到厂商协议格式。

## 测试

新集成测试：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\test_baseline.ps1
```

其中 `integration.LocalRuntimeCollaborationTest` 会验证：

- 本地 DDS Broker 记录 socket 消息；
- UAV、自车、peer car 都发布状态；
- 三个设备 Adapter 都发布厂商原生帧；
- 协调 Agent 发布冲突事件；
- 冲突触发动态重规划；
- 最终策略选择不是只用 Dijkstra / Greedy baseline；
- 发布策略评估报告。

## ROS 2 / DDS 接入边界

当前本地运行时是 ROS 2/DDS Adapter 的前置替代层：它让项目在未安装 ROS 2 时也能真实运行。

后续安装 ROS 2 后，应新增 ROS 2 Adapter 替换 `LocalDDSBroker` 的通信实现，而不是改前端或重写 Agent 逻辑。

## ROS 2 Adapter

当前已经提供 `source/collaboration/ros2`：

- `Ros2DdsAdapter`：统一 runtime 消息到 ROS 2 Topic 的 Adapter；
- `Ros2TopicBinding`：语义 Topic、ROS Topic、消息类型、IDL 类型、QoS 的绑定；
- `Ros2BridgePlan`：一次后端运行产生的 ROS 2 发布计划；
- `Ros2BridgePlanMain`：命令行查看桥接计划。

没有安装 ROS 2 时，Adapter 会进入 dry-run：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\ros2_bridge_plan.ps1 -IntervalMs 500 -Ticks 4
```

安装并配置好 ROS 2 后，可以执行真实 `ros2 topic pub --once`：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\ros2_publish_plan.ps1 -IntervalMs 500 -Ticks 4 -MaxCommands 12
```

后端 API：

```text
GET /api/runtime/ros2/profile
POST /api/runtime/ros2/bridge-plan
```

当前 ROS 2 消息先使用 `std_msgs/msg/String` 承载 JSON payload，保证没有自定义 ROS 2 interface 包时也能联调 Topic。下一步如果需要更严格的类型安全，应把 `idl/urban_intent_collaboration.idl` 中的 runtime 类型生成 ROS 2 interface package，再把 Adapter 的 `messageType` 从 `std_msgs/msg/String` 替换为自定义消息类型。

## ROS 2 安装与联调建议

请优先看 ROS 2 官方安装页：

- Kilted 安装入口：https://docs.ros.org/en/kilted/Installation.html
- Jazzy 安装入口：https://docs.ros.org/en/jazzy/Installation.html
- Windows binary 安装说明入口：https://docs.ros.org/en/humble/Installation/Windows-Install-Binary.html

本项目联调顺序建议：

1. 在两个 PowerShell 终端里都先进入项目目录，并点源本项目的 ROS 2 环境脚本：

   ```powershell
   cd D:\DSLCODE\无人系统意图协同平台_免安装版
   . .\tools\setup_ros2_humble_env.ps1
   python --version
   ros2 topic list
   ```

   注意：命令开头是 `. .\tools\setup_ros2_humble_env.ps1`，第一个点表示把 ROS 2 和 Pixi Python 环境留在当前终端里。Windows 版 Humble 二进制包依赖 Python 3.10，本项目脚本会优先使用 `C:\pixi_ws\.pixi\envs\default\python.exe`，避免误用系统里的 Python 3.14。

2. 运行本项目 dry-run，确认 ROS 2 发布计划能生成：

   ```powershell
   powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\ros2_bridge_plan.ps1 -IntervalMs 500 -Ticks 4 -MaxCommands 12
   ```

3. 在第一个终端监听最终决策 Topic：

   ```powershell
   ros2 topic echo /coordination/final_decision
   ```

4. 在第二个终端运行真实发布脚本：

   ```powershell
   powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\ros2_publish_plan.ps1 -IntervalMs 500 -Ticks 4 -MaxCommands 12
   ```

5. 如果想看全部消息，也可以监听这些 Topic：

   ```powershell
   ros2 topic echo /intent/contract
   ros2 topic echo /agent/status
   ros2 topic echo /coordination/conflict_event
   ros2 topic echo /coordination/replan_request
   ros2 topic echo /coordination/final_decision
   ros2 topic echo /coordination/policy_evaluation
   ```

如果能看到 `cooperative_score_policy`、`route_D_emergency_green_wave`、冲突解释等字段，说明本项目的后端 Agent 决策已经被桥接到 ROS 2 Topic。

## ROS 2 实时协同运行

上面的 `ros2_publish_plan.ps1` 适合验证“一次后端运行结果能不能发布到 ROS 2”。如果要更接近真实无人设备协同，应运行 live runtime：

```powershell
cd D:\DSLCODE\无人系统意图协同平台_免安装版
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\run_ros2_live_runtime.ps1 -IntervalMs 500 -Ticks 8
```

这个命令会启动：

- 本地 DDS-compatible Broker；
- UAV Agent；
- Ego Car Agent；
- Peer Car Agent；
- Coordinator Agent；
- ROS 2 Live Bridge。

运行时链路是：

```text
UAV / Ego Car / Peer Car
 -> runtime/agent/status
 -> Coordinator Agent
 -> runtime/conflict/event
 -> runtime/replanning/request
 -> runtime/decision/final
 -> Ros2LiveBridge
 -> /coordination/final_decision
```

监听最终决策：

```powershell
cd D:\DSLCODE\无人系统意图协同平台_免安装版
. .\tools\setup_ros2_humble_env.ps1
ros2 topic echo /coordination/final_decision std_msgs/msg/String
```

再在另一个终端运行 live runtime：

```powershell
cd D:\DSLCODE\无人系统意图协同平台_免安装版
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\run_ros2_live_runtime.ps1 -IntervalMs 500 -Ticks 8
```

终端最后会输出类似：

```text
[ROS2-LIVE] localMessages=11
[ROS2-LIVE] forwardedToRos2=11
```

这表示本地多 Agent 实时协同消息已经全部桥接到 ROS 2。当前实现使用 `std_msgs/msg/String` 作为联调消息载体，下一阶段再升级为自定义 ROS 2 message package。

## ROS 2 反向接入外部设备

`Ros2LiveBridge` 负责“项目后端 -> ROS 2”。现在还提供 `Ros2InboundBridge`，负责“ROS 2 -> 项目后端”：

```text
真实/外部设备 ROS 2 Topic
 -> Ros2InboundBridge
 -> 本地 DDS-compatible Broker
 -> Coordinator / Agent / WorldModel
```

手动验证步骤：

1. 左边终端运行反向接入 Demo：

   ```powershell
   cd D:\DSLCODE\无人系统意图协同平台_免安装版
   powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\run_ros2_inbound_demo.ps1 -TimeoutMs 60000
   ```

2. 右边终端模拟一个外部 UAV 通过 ROS 2 发布状态：

   ```powershell
   cd D:\DSLCODE\无人系统意图协同平台_免安装版
   powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\ros2_publish_external_status.ps1 -AgentId UAV-REAL-001 -Role UAV -Route route_C_aerial_scout
   ```

3. 左边如果看到类似输出，说明外部 ROS 2 消息已经进入项目后端：

   ```text
   [ROS2-INBOUND-DEMO] received topic=runtime/agent/status source=UAV-REAL-001 payload={... route=route_C_aerial_scout ...}
   ```

当前反向接入支持的消息格式仍是 `std_msgs/msg/String`，其中 `data` 使用统一键值格式：

```text
topic=runtime/agent/status|source=UAV-REAL-001|agent=UAV-REAL-001|role=UAV|route=route_C_aerial_scout
```

这一步解决的是“外部设备怎么进入系统”。后续换真实无人机或车辆时，只要厂商 Adapter 能把原始设备协议转换成这个统一格式，系统内部 Agent 就不用关心设备来自哪个品牌。

## 自定义 ROS 2 Message 类型

项目现在提供 `urban_runtime_msgs` ROS 2 interface package，源码位置：

```text
ros2_ws/src/urban_runtime_msgs
```

包含这些结构化消息：

```text
RuntimeIntentContract.msg
RuntimeAgentStatus.msg
ProtocolNativeFrame.msg
ConflictEvent.msg
ReplanningRequest.msg
RouteDecision.msg
PolicyEvaluationReport.msg
```

由于 Windows 下 ROS 2/colcon 对中文路径支持不稳定，构建脚本会把接口包复制到纯英文路径 `C:\ros2_urban_runtime_ws` 再编译：

```powershell
cd D:\DSLCODE\无人系统意图协同平台_免安装版
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\build_ros2_interfaces.ps1 -Clean
```

成功后会看到：

```text
[ROS2-IFACE] Built and sourced urban_runtime_msgs.
```

查看接口：

```powershell
. .\tools\setup_ros2_humble_env.ps1
ros2 interface show urban_runtime_msgs/msg/RuntimeAgentStatus
ros2 interface show urban_runtime_msgs/msg/RouteDecision
```

使用结构化消息发布一次运行计划：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\ros2_publish_plan.ps1 -IntervalMs 500 -Ticks 1 -MaxCommands 2 -MessageMode custom
```

运行结构化消息实时协同：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\run_ros2_live_runtime.ps1 -IntervalMs 500 -Ticks 1 -MessageMode custom
```

成功时 ROS 2 会打印类似：

```text
urban_runtime_msgs.msg.RuntimeAgentStatus(...)
urban_runtime_msgs.msg.RouteDecision(...)
```

这说明系统已经从 `std_msgs/msg/String` 的字符串联调，升级为 ROS 2 自定义结构化协议。保留 `-MessageMode string` 是为了兼容还没有编译接口包的电脑。

## 异构厂商协议 Adapter

系统现在提供协议 Adapter 注册中心，位置：

```text
source/collaboration/runtime/ProtocolAdapterRegistry.java
```

设备配置文件：

```text
config/devices.json
```

默认配置包含三类设备：

```text
UAV-CQ-007      -> DJI-STYLE
CAR-CQ-001      -> ROS2-DDS
PEER-CAR-031    -> GENERIC-PRIVATE-JSON
```

Adapter 做两件事：

```text
统一 RuntimeAgentStatus -> 厂商原生报文
厂商原生报文 -> 统一 RuntimeAgentStatus
```

也就是说，系统内部 Agent 不需要关心设备来自哪个品牌，只处理统一字段：

```text
agentId, role, protocol, route, x, y, z, speed, battery, rateHz, missionStatus
```

运行演示：

```powershell
cd D:\DSLCODE\无人系统意图协同平台_免安装版
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\run_heterogeneous_adapter_demo.ps1
```

你会看到三种不同原生格式：

```text
DJI-STYLE
ROS2-DDS
GENERIC-PRIVATE-JSON
```

最后都转换成统一 runtime payload。

新增厂商协议时，推荐步骤：

1. 新建一个 `DeviceProtocolAdapter` 实现类；
2. 实现 `toNativeStatus` / `toNativeCommand`；
3. 实现 `fromNativeStatus` / `fromNativeCommand`；
4. 在 `ProtocolAdapterRegistry` 注册；
5. 在 `config/devices.json` 中添加设备；
6. 补一个集成测试，证明新厂商原生报文能变成统一 `RuntimeAgentStatus`。

这一步解决的是“不同品牌设备协议不一样，系统如何消除异构性”。
