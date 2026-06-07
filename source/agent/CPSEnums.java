package agent;

/**
 * CPS Agent 通用枚举定义
 * 
 * 解决模块间的循环依赖问题
 */
public class CPSEnums {
    
    /**
     * 设备角色
     */
    public enum DeviceRole {
        VEHICLE,      // 车辆
        DRONE,        // 无人机
        CONTROL_CENTER // 控制中心
    }
    
    /**
     * 任务阶段
     */
    public enum TaskPhase {
        IDLE,         // 空闲
        PLANNING,     // 规划中
        EXECUTING,    // 执行中
        MONITORING    // 监控中
    }
}
