# ROS2 自定义消息反向接入

ROS2 进入系统现在支持两种方式：

```text
std_msgs/msg/String
urban_runtime_msgs/msg/RuntimeAgentStatus
```

`std_msgs/msg/String` 适合快速联调，`data` 使用 `key=value|key=value` 形式。

结构化接入使用项目自定义消息：

```text
urban_runtime_msgs/msg/RuntimeAgentStatus
```

对应的订阅脚本是：

```text
tools/ros2_runtime_agent_status_subscriber.py
```

当 `Ros2DdsAdapter` 处于 custom message 模式时，`Ros2InboundBridge` 会对 `/agent/status` 自动使用这个脚本。脚本会把结构化字段转换成统一入口格式：

```text
topic=runtime/agent/status
source=<source_id>
logicalAgent=<agent_id>
protocol=<protocol>
role=<role>
route=<route>
x=<position_x_m>
y=<position_y_m>
z=<position_z_m>
speed=<speed_mps>
battery=<battery_pct>
rateHz=<publish_rate_hz>
missionStatus=<mission_status>
```

然后系统继续执行：

```text
协议 Adapter 归一化
-> 本地 DDS-compatible Topic
-> Coordinator Agent
-> ROS2 outbound final decision
```

这意味着 ROS2 自定义结构化消息现在不只支持“系统输出到 ROS2”，也支持“ROS2 外部设备状态进入系统”。
