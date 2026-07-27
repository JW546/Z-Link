cmake_minimum_required(VERSION 3.8)
project(urban_runtime_msgs)

find_package(ament_cmake REQUIRED)
find_package(rosidl_default_generators REQUIRED)

rosidl_generate_interfaces(${PROJECT_NAME}
  "msg/RuntimeIntentContract.msg"
  "msg/RuntimeAgentStatus.msg"
  "msg/ProtocolNativeFrame.msg"
  "msg/ConflictEvent.msg"
  "msg/ReplanningRequest.msg"
  "msg/RouteDecision.msg"
  "msg/PolicyEvaluationReport.msg"
)

ament_export_dependencies(rosidl_default_runtime)
ament_package()
