# IDL/DDS 映射说明

本文件说明 `idl/urban_intent_collaboration.idl` 与项目中 DDS 主题的对应关系。

| DDS Topic | IDL Type | QoS 建议 | 发布者 | 订阅者 | 作用 |
| --- | --- | --- | --- | --- | --- |
| `cabin/passenger/intent.dsl` | `urban::MissionCommand` | Reliable + KeepLast | 乘客舱节点 | 主车、无人机、RSU、邻车 | 发布乘客自然语言意图及 DSL 契约。 |
| `urban/vehicle/telemetry.dsl` | `urban::VehicleTelemetry` | BestEffort | 主车、邻车 | 无人机、广域链路、可视化端 | 高频车辆状态流。 |
| `urban/uav/aerial_perception.dsl` | `urban::EnvironmentPerception` | BestEffort 或 Reliable | 无人机 | 主车、广域链路、可视化端 | 发布前方空中视角感知。 |
| `urban/rsu/road_event.dsl` | `urban::RsuRoadEvent` | Reliable | 路侧 RSU | 主车、广域链路、可视化端 | 发布事故、行人、信号灯、路口负载等事件。 |
| `backbone/compliant/context.dsl` | `urban::CompliantNetworkContext` | Reliable + TransientLocal | 合规广域链路节点 | 主车、可视化端 | 发布合规网络状态和广域态势。 |
| `urban/route/advice.dsl` | `urban::RouteAdvice` | Reliable | 主车、邻车、无人机、RSU、广域链路 | 主车融合器、乘客舱可视化 | 发布路线建议。 |
| `urban/route/final_decision.dsl` | `urban::RouteDecision` | Reliable | 主车融合器 | 乘客舱、可视化端、其他设备 | 发布最终路线和可解释结果。 |
| `urban/vehicle/peer_advice.dsl` | `urban::RouteAdvice` | Reliable | 邻车 | 主车 | 车车协同建议。 |

## QoS 选择原则

- 乘客意图、路线建议、最终决策属于关键消息，应优先使用 Reliable。
- 车辆状态、无人机连续感知属于高频流，可使用 BestEffort，避免阻塞。
- RSU 事故和信号灯事件对安全影响较大，应使用 Reliable。
- 广域链路上下文更新频率较低但影响决策，应使用 Reliable + TransientLocal。

## 与当前代码的关系

- `VirtualDDSScenarioDemo` 使用同名 Topic 模拟 DDS 发布订阅。
- `RouteFusionExperiment` 使用 `RouteAdvice`、`RouteDecision`、`QosObservation`、`RouteFusionExperimentResult` 的字段语义进行对比实验。
- 后续接入 ROS2、CycloneDDS 或 Fast DDS 时，可将 `idl/urban_intent_collaboration.idl` 作为消息定义基础。
