# DSL 数据与指令契约说明

本项目中的 DSL 不是简单的自然语言摘要，而是无人设备之间协同通信的语义契约。它规定：

- 谁发布消息
- 谁订阅消息
- 交换什么数据
- 交换什么指令
- 数据映射到哪个 IDL 类型
- 使用什么 DDS Topic 和 QoS

## 1. 车辆之间交换的数据

无人车之间不是交换任意文本，而是交换结构化数据。

| 数据类别 | Topic | IDL 类型 | 发布者 | 订阅者 | 关键字段 |
|---|---|---|---|---|---|
| 车辆遥测 | `urban/vehicle/telemetry.dsl` | `urban.VehicleTelemetry` | `CAR-CQ-001`、`CAR-CQ-002` | `UAV-CQ-007` 等 | `vehicle_id`、`position_x_m`、`speed_mps`、`heading_deg`、`road_event`、`traffic_signal` |
| 无人机感知 | `urban/uav/aerial_perception.dsl` | `urban.EnvironmentPerception` | `UAV-CQ-007` | `CAR-CQ-001`、`CAR-CQ-002` | `event_type`、`confidence`、`risk_score`、`visibility_m`、`evidence_summary` |
| 路侧事件 | `urban/rsu/road_event.dsl` | `urban.RsuRoadEvent` | `RSU-SCHOOL-009` | `CAR-CQ-001`、`CAR-CQ-002` | `road_event`、`traffic_signal`、`pedestrian_count`、`recommended_route`、`risk_score` |
| 合规态势 | `backbone/compliant/context.dsl` | `urban.CompliantNetworkContext` | `CN-BACKBONE-CQ` | `CAR-CQ-001`、`CAR-CQ-002` | `link_mode`、`link_health`、`private_5g_signal`、`expected_latency_ms`、`recommended_route` |

## 2. 车辆之间交换的指令

指令不是控制中心派单，而是设备自主协作中的语义触发。

| 指令类别 | Topic | IDL 类型 | 发布者 | 订阅者 | 作用 |
|---|---|---|---|---|---|
| 乘客意图 | `cabin/passenger/intent.dsl` | `urban.MissionCommand` | `CABIN-CAR-CQ-001` | 本车、邻车、无人机、RSU | 触发设备自主选择订阅/发布任务 |
| 邻车建议 | `urban/vehicle/peer_advice.dsl` | `urban.RouteAdvice` | `CAR-CQ-002` | `CAR-CQ-001` | 给本车提供路线建议 |
| 多源路线建议 | `urban/route/advice.dsl` | `urban.RouteAdvice` | 本车、邻车、无人机 | 本车、座舱、合规态势节点 | 进入路线融合算法 |
| 最终路线决策 | `urban/route/final_decision.dsl` | `urban.RouteDecision` | `CAR-CQ-001` | 乘客座舱 | 向乘客解释最终路线 |

## 3. DSL 中如何表达

DSL 现在包含三层契约：

```dsl
state dataExchangeContract {
    string data_1 = "topic=urban/vehicle/telemetry.dsl; idl=urban.VehicleTelemetry; publisher=CAR-CQ-001|CAR-CQ-002; subscriber=UAV-CQ-007; fields=vehicle_id,position_x_m,speed_mps,road_event; qos=BEST_EFFORT_sensor_stream";
}
```

这表示设备之间交换的数据。

```dsl
state commandExchangeContract {
    string command_1 = "topic=cabin/passenger/intent.dsl; idl=urban.MissionCommand; publisher=CABIN-CAR-CQ-001; subscriber=CAR-CQ-001|CAR-CQ-002|UAV-CQ-007|RSU-SCHOOL-009; kind=PASSENGER_INTENT";
}
```

这表示设备之间交换的指令或协同触发。

```dsl
state ddsIdlBinding {
    string binding_1 = "cabin/passenger/intent.dsl -> urban.MissionCommand";
}
```

这表示 DSL Topic 与 IDL 消息类型之间的映射关系。

## 4. 可向导师说明的话

本项目的 DSL 不是只描述“我要去哪”，而是把乘客自然语言转成无人设备之间可执行的通信契约。它明确规定车辆、无人机、RSU 和合规态势链路之间交换哪些数据、哪些建议和哪些最终决策，并把每个语义 Topic 映射到 IDL 类型，最后通过 DDS 的发布/订阅机制完成设备协同。
