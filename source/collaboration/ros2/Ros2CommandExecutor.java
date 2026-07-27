package collaboration.ros2;

import java.io.IOException;
import java.util.List;

public interface Ros2CommandExecutor {
    int execute(List<String> command) throws IOException, InterruptedException;
}
