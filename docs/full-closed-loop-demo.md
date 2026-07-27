# 完整闭环协同 Demo

这个 Demo 对应完整系统链路，不依赖前端：

```text
外部设备原生报文
-> 协议 Adapter 归一化
-> 本地 DDS-compatible Topic
-> Coordinator Agent 冲突检测
-> 动态重规划
-> 可解释最终路线决策
-> 策略评估报告
```

运行命令：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\run_full_closed_loop_demo.ps1 -TimeoutMs 5000
```

这个 Demo 会模拟三种不同来源的外部设备：

- DJI 风格 UAV 原生遥测；
- ROS2/DDS 风格自车状态；
- 第三方私有 JSON 风格 peer car 状态。

三种报文先进入 `ExternalDeviceStatusIngress`，再通过 `ProtocolAdapterRegistry` 选择对应 Adapter，转换为统一 `RuntimeAgentStatus`。之后 Coordinator 不再关心设备来自哪个厂商，只根据统一字段判断路线冲突，并输出：

- `runtime/protocol/native_frame`：厂商原始报文与统一报文的映射证据；
- `runtime/conflict/event`：路线占用冲突；
- `runtime/replanning/request`：动态重规划请求；
- `runtime/decision/final`：最终路线 `route_D_emergency_green_wave`；
- `runtime/policy/evaluation`：Dijkstra、Greedy 与协同评分策略对比。

这一步把“不同品牌设备接入”和“实时协同决策”连成了一个闭环。接入真实设备时，只需要把 Demo 中构造的原生报文替换成真实设备 SDK、ROS2 Topic、CAN/V2X 或其他厂商链路收到的报文，后面的 Adapter、DDS Topic、Coordinator 和决策逻辑保持不变。
