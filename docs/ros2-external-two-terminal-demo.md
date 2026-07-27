# ROS2 外部双终端闭环 Demo

这个 Demo 用于验证“外部 ROS2 Topic 真的进入系统”，不是 Java 测试里直接注入数据。

终端 A：启动系统，等待外部 `/agent/status`。

```powershell
cd D:\DSLCODE\无人系统意图协同平台_免安装版
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\run_ros2_external_bidirectional_runtime.ps1 -TimeoutMs 120000
```

终端 B：发布三台外部设备状态。

```powershell
cd D:\DSLCODE\无人系统意图协同平台_免安装版
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\ros2_publish_external_fleet_status.ps1
```

链路如下：

```text
终端 B ros2 topic pub /agent/status
-> 终端 A Ros2InboundBridge
-> 厂商协议 Adapter
-> 本地 DDS-compatible Topic
-> CoordinatorAgent
-> ConflictResolver
-> Ros2LiveBridge
-> ros2 topic pub /coordination/final_decision
```

终端 A 成功时会输出：

```text
[ROS2-EXTERNAL-BIDIR] localMessages=...
[ROS2-EXTERNAL-BIDIR] forwardedToRos2=...
[ROS2-EXTERNAL-BIDIR] finalDecision={... selectedRoute=route_D_emergency_green_wave ...}
```

如果要查看最终 ROS2 Topic，可以再开一个终端：

```powershell
cd D:\DSLCODE\无人系统意图协同平台_免安装版
. .\tools\setup_ros2_humble_env.ps1
ros2 topic echo /coordination/final_decision std_msgs/msg/String
```
