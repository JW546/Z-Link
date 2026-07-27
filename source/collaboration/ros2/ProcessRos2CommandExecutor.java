package collaboration.ros2;

import java.io.IOException;
import java.util.List;

public class ProcessRos2CommandExecutor implements Ros2CommandExecutor {
    public int execute(List<String> command) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.inheritIO();
        Process process = builder.start();
        return process.waitFor();
    }
}
