# 面向无人设备的自然语言意图驱动协同系统

本项目不是普通 Web App，也不是中心化远程调度平台。前端只用于演示和实验可视化，核心在后端协同链路：

```text
自然语言意图
-> DSL 任务契约
-> IDL/DDS 或 ROS2 发布订阅
-> 多设备 Agent 自主协同
-> 冲突检测与消解
-> 动态重规划
-> 可解释路线决策
-> 策略评估
```

## 已完成能力

- 自然语言意图解析与 DSL 任务契约生成；
- 本地 DDS-compatible 发布订阅运行时；
- ROS2 outbound、inbound 和双向闭环；
- `urban_runtime_msgs` ROS2 自定义消息包；
- UAV、自车、peer car、Coordinator 四类运行时 Agent；
- DJI 风格、ROS2-DDS 风格、第三方私有 JSON 风格协议 Adapter；
- 外部设备原生报文到统一 `RuntimeAgentStatus` 的归一化；
- 路线占用冲突检测、动态重规划、最终路线决策；
- Dijkstra / Greedy baseline 与协同策略对比；
- 场景库、策略评估、启发式策略优化；
- 小规模表格 Q-learning 原型和 RL/MARL 接口预留；
- 后端 API、最小前端演示、完整测试基线。

## 重要边界

当前系统已经适合在电脑上验证“无人机、车辆、peer car 的协同通信和决策闭环”。但它还不是可以直接上真实车辆或无人机的安全控制系统。

真实部署前仍需要：

- 真实无人机 SDK；
- 真实车辆 CAN/V2X/车控接口；
- 真实 DDS 域或 ROS2 网络环境压力测试；
- 认证、授权、签名、审计；
- 安全冗余、急停、人工接管；
- 真实传感器和硬件在环验证。

## 常用命令

运行完整本地闭环：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\run_full_closed_loop_demo.ps1 -TimeoutMs 5000
```

运行 ROS2 双向 dry-run：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\run_ros2_bidirectional_closed_loop.ps1 -TimeoutMs 5000 -DryRun
```

运行外部 ROS2 双终端 Demo：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\run_ros2_external_bidirectional_runtime.ps1 -TimeoutMs 120000
```

另一个终端发布外部设备状态：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\ros2_publish_external_fleet_status.ps1
```

运行完整测试：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\test_baseline.ps1
```

## 文档入口

- `docs/full-closed-loop-demo.md`
- `docs/ros2-bidirectional-closed-loop.md`
- `docs/ros2-custom-inbound.md`
- `docs/ros2-external-two-terminal-demo.md`
- `docs/local-runtime-collaboration.md`
