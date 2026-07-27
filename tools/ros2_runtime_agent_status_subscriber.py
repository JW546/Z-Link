import sys

import rclpy
from rclpy.node import Node
from urban_runtime_msgs.msg import RuntimeAgentStatus


class RuntimeAgentStatusSubscriber(Node):
    def __init__(self, topic):
        super().__init__("codex_ros2_inbound_runtime_agent_status_subscriber")
        self.create_subscription(RuntimeAgentStatus, topic, self.handle_message, 10)
        print(f"[ROS2-RUNTIME-STATUS-SUB] listening {topic}", flush=True)

    def handle_message(self, message):
        data = "|".join([
            "topic=runtime/agent/status",
            f"source={message.source_id}",
            f"logicalAgent={message.agent_id}",
            f"protocol={message.protocol}",
            f"role={message.role}",
            f"route={message.route}",
            f"x={message.position_x_m}",
            f"y={message.position_y_m}",
            f"z={message.position_z_m}",
            f"speed={message.speed_mps}",
            f"battery={message.battery_pct}",
            f"rateHz={message.publish_rate_hz}",
            f"missionStatus={message.mission_status}",
            f"stamp={message.timestamp_ms}",
        ])
        print(f"data: {data}", flush=True)


def main():
    topic = sys.argv[1] if len(sys.argv) > 1 else "/agent/status"
    rclpy.init()
    node = RuntimeAgentStatusSubscriber(topic)
    try:
        rclpy.spin(node)
    finally:
        node.destroy_node()
        rclpy.shutdown()


if __name__ == "__main__":
    main()
