import sys

import rclpy
from rclpy.node import Node
from std_msgs.msg import String


class StringSubscriber(Node):
    def __init__(self, topic):
        super().__init__("codex_ros2_inbound_string_subscriber")
        self.create_subscription(String, topic, self.handle_message, 10)
        print(f"[ROS2-STRING-SUB] listening {topic}", flush=True)

    def handle_message(self, message):
        print(f"data: {message.data}", flush=True)


def main():
    topic = sys.argv[1] if len(sys.argv) > 1 else "/agent/status"
    rclpy.init()
    node = StringSubscriber(topic)
    try:
        rclpy.spin(node)
    finally:
        node.destroy_node()
        rclpy.shutdown()


if __name__ == "__main__":
    main()
