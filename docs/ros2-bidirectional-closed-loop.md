# ROS2 实时双向闭环

这个 Demo 把前面的完整闭环接到 ROS2 形态：

```text
外部 ROS2 /agent/status
-> Ros2InboundBridge
-> 厂商协议 Adapter 归一化
-> 本地 DDS-compatible Topic
-> Coordinator Agent 冲突检测与重规划
-> Ros2LiveBridge
-> ROS2 /coordination/final_decision
```

先运行 dry-run 验证，不需要真正发 ROS2 进程：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\run_ros2_bidirectional_closed_loop.ps1 -TimeoutMs 5000 -DryRun
```

成功时会看到：

```text
[ROS2-BIDIR] localMessages=11
[ROS2-BIDIR] finalDecision={... selectedStrategy=cooperative_score_policy, selectedRoute=route_D_emergency_green_wave ...}
[ROS2-BIDIR] outboundFinalDecisionCommand=[ros2, topic, pub, ..., /coordination/final_decision, ...]
```

这说明系统已经完成：

- ROS2 inbound 形态消息进入后端；
- DJI / ROS2-DDS / 私有 JSON 三类设备经 Adapter 统一；
- 本地 DDS-compatible 发布订阅传递给 Coordinator；
- Coordinator 检测自车和 peer car 的路线冲突；
- 系统输出动态重规划后的 `route_D_emergency_green_wave`；
- 最终决策再被 outbound bridge 发布回 ROS2 `/coordination/final_decision`。

如果要运行真实 ROS2 发布路径，先确保已经在项目里配置好 ROS2 Humble 环境，然后运行：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\run_ros2_bidirectional_closed_loop.ps1 -TimeoutMs 5000
```

真实模式会调用 `ros2 topic pub --once`。如果只是检查系统逻辑，优先用 `-DryRun`，因为它更快，也不会受 ROS2 discovery 等待影响。
