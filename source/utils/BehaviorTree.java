package utils;

import java.util.ArrayList;
import java.util.List;

/**
 * 行为树节点类型
 */
enum NodeType {
    SEQUENCE("序列", "顺序执行所有子节点，全部成功才返回成功"),
    SELECTOR("选择", "顺序执行子节点，任一成功就返回成功"),
    PARALLEL("并行", "同时执行所有子节点"),
    ACTION("动作", "执行具体动作"),
    CONDITION("条件", "判断条件是否满足");

    private String name;
    private String description;

    NodeType(String name, String description) {
        this.name = name;
        this.description = description;
    }
}

/**
 * 行为树节点基类
 */
abstract class BTNode {
    protected NodeType type;
    protected String name;
    protected List<BTNode> children;
    protected boolean status;

    public BTNode(NodeType type, String name) {
        this.type = type;
        this.name = name;
        this.children = new ArrayList<>();
        this.status = false;
    }

    public abstract boolean execute();

    public void addChild(BTNode child) {
        children.add(child);
    }

    public String getName() { return name; }
    public boolean getStatus() { return status; }

    public void printTree(String indent) {
        System.out.println(indent + name + " [" + type.name() + "] " + (status ? "✓" : "✗"));
        for (BTNode child : children) {
            child.printTree(indent + "  ");
        }
    }
}

/**
 * 序列节点
 */
class SequenceNode extends BTNode {
    public SequenceNode(String name) {
        super(NodeType.SEQUENCE, name);
    }

    @Override
    public boolean execute() {
        System.out.println("\n[序列节点] 执行: " + name);
        boolean allSuccess = true;
        for (BTNode child : children) {
            boolean result = child.execute();
            if (!result) {
                allSuccess = false;
                break;
            }
        }
        status = allSuccess;
        System.out.println("[序列节点] " + name + " 结果: " + (allSuccess ? "成功" : "失败"));
        return allSuccess;
    }
}

/**
 * 选择节点
 */
class SelectorNode extends BTNode {
    public SelectorNode(String name) {
        super(NodeType.SELECTOR, name);
    }

    @Override
    public boolean execute() {
        System.out.println("\n[选择节点] 执行: " + name);
        boolean anySuccess = false;
        for (BTNode child : children) {
            boolean result = child.execute();
            if (result) {
                anySuccess = true;
                break;
            }
        }
        status = anySuccess;
        System.out.println("[选择节点] " + name + " 结果: " + (anySuccess ? "成功" : "失败"));
        return anySuccess;
    }
}

/**
 * 动作节点
 */
class ActionNode extends BTNode {
    private Runnable action;

    public ActionNode(String name, Runnable action) {
        super(NodeType.ACTION, name);
        this.action = action;
    }

    @Override
    public boolean execute() {
        System.out.println("[动作节点] 执行: " + name);
        try {
            action.run();
            status = true;
            System.out.println("[动作节点] " + name + " 成功");
            return true;
        } catch (Exception e) {
            status = false;
            System.out.println("[动作节点] " + name + " 失败: " + e.getMessage());
            return false;
        }
    }
}

/**
 * 条件节点
 */
class ConditionNode extends BTNode {
    private java.util.function.BooleanSupplier condition;

    public ConditionNode(String name, java.util.function.BooleanSupplier condition) {
        super(NodeType.CONDITION, name);
        this.condition = condition;
    }

    @Override
    public boolean execute() {
        System.out.println("[条件节点] 检查: " + name);
        boolean result = condition.getAsBoolean();
        status = result;
        System.out.println("[条件节点] " + name + " " + (result ? "满足" : "不满足"));
        return result;
    }
}

/**
 * 行为树主类
 * 场景：民用无人交通系统的动态决策
 */
public class BehaviorTree {
    private BTNode root;
    private static boolean VERBOSE = false;

    public BehaviorTree(BTNode root) {
        this.root = root;
    }

    public static void setVerbose(boolean verbose) {
        VERBOSE = verbose;
    }

    public boolean execute() {
        if (VERBOSE) System.out.println("\n========== 行为树开始执行 ==========");
        boolean result = root.execute();
        if (VERBOSE) System.out.println("\n========== 行为树执行结束 ==========\n");
        return result;
    }

    public void printTree() {
        System.out.println("\n行为树结构:");
        root.printTree("");
    }

    /**
     * 创建民用无人交通任务行为树示例
     * 场景：智能网联汽车与大疆无人机的协同配送
     * 装饰器应用说明：
     * - RetryDecorator: 通信初始化失败时重试
     * - TimeoutDecorator: 限制关键操作的超时时间
     * - ConditionalDecorator: 根据条件决定是否执行某些任务
     * - DelayDecorator: 模拟无人机发射前的安全延迟
     * - InvertDecorator: 反转某些条件判断
     * - RepeatDecorator: 重复执行某些监控动作
     * - SuccessDecorator: 关键清理操作必须成功执行
     * - FailureDecorator: 紧急情况下强制终止流程
     */
    public static BehaviorTree createDroneMissionBT() {
        // 根节点：序列节点
        SequenceNode root = new SequenceNode("民用无人交通协同任务");

        // ==================== 第一阶段：任务准备 ====================
        SequenceNode preparationPhase = new SequenceNode("任务准备阶段");

        // 条件节点：检查设备状态
        ConditionNode deviceCheck = new ConditionNode("检查车机和无人机状态",
            () -> true); // 始终成功，以便演示完整流程

        // [装饰器应用1: RetryDecorator] DDS通信初始化可能失败，需要重试
        ActionNode initDDS = new ActionNode("初始化DDS通信",
            () -> System.out.println("  -> 通过DDS-Standard建立车机与无人机通信链路"));
        RetryDecorator retryInitDDS = new RetryDecorator("重试初始化DDS通信(最多3次)", 3);
        retryInitDDS.addChild(initDDS);

        // [装饰器应用2: TimeoutDecorator] IDL配置不应超过5秒
        ActionNode configureIDL = new ActionNode("配置IDL接口",
            () -> {
                // 模拟配置可能耗时
                try { Thread.sleep(100); } catch (Exception e) {}
                System.out.println("  -> 配置DDS-IDL-v2.0接口定义");
            });
        TimeoutDecorator timeoutConfigureIDL = new TimeoutDecorator("配置IDL接口(5秒超时)", 5000);
        timeoutConfigureIDL.addChild(configureIDL);

        // ==================== 第二阶段：环境感知 ====================
        SequenceNode sensingPhase = new SequenceNode("环境感知阶段");

        // 动作节点：收集车机数据
        ActionNode collectVehicleData = new ActionNode("收集车机传感器数据",
            () -> System.out.println("  -> 车机收集车速、GPS、摄像头数据"));

        // 动作节点：收集无人机数据
        ActionNode collectDroneData = new ActionNode("收集无人机传感器数据",
            () -> System.out.println("  -> 无人机收集飞行高度、航向、障碍物数据"));

        // [装饰器应用3: RepeatDecorator] 持续监控预警状态
        ActionNode monitorAlert = new ActionNode("持续监控预警状态",
            () -> System.out.println("  -> 扫描道路和灾害预警信息"));
        RepeatDecorator repeatMonitor = new RepeatDecorator("持续监控预警(执行3次)", 3);
        repeatMonitor.addChild(monitorAlert);

        // 条件节点：检测到预警
        ConditionNode alertCheck = new ConditionNode("检测道路/灾害预警",
            () -> false); // 始终返回false，演示正常配送流程（如果有预警会走应急救援分支）

        // ==================== 第三阶段：任务选择 ====================
        SelectorNode missionSelector = new SelectorNode("任务模式选择");

        // 选择1：正常配送任务
        SequenceNode normalDelivery = new SequenceNode("正常配送任务");

        // [装饰器应用4: TimeoutDecorator] 路径规划应在3秒内完成
        ActionNode routePlanning = new ActionNode("路径规划",
            () -> {
                try { Thread.sleep(500); } catch (Exception e) {}
                System.out.println("  -> 配送车规划最优路径，避开拥堵区域");
            });
        TimeoutDecorator timeoutRoutePlanning = new TimeoutDecorator("路径规划(3秒超时)", 3000);
        timeoutRoutePlanning.addChild(routePlanning);

        ActionNode startDelivery = new ActionNode("开始配送",
            () -> System.out.println("  -> 车机按规划路径执行配送"));

        // 选择2：无人机辅助配送
        SequenceNode droneAssistedDelivery = new SequenceNode("无人机辅助配送");

        ActionNode assessSituation = new ActionNode("评估拥堵/灾害情况",
            () -> System.out.println("  -> 评估地面交通状况，决定无人机介入"));

        // [装饰器应用5: DelayDecorator] 无人机发射前需要2秒安全检查延迟
        ActionNode launchDrone = new ActionNode("发射无人机",
            () -> System.out.println("  -> 大疆无人机起飞，进入配送空域"));
        DelayDecorator delayLaunch = new DelayDecorator("安全延迟后发射无人机(2秒)", 2000);
        delayLaunch.addChild(launchDrone);

        ActionNode droneDelivery = new ActionNode("执行空中配送",
            () -> System.out.println("  -> 无人机执行最后1公里配送"));

        // 选择3：应急救援模式
        SequenceNode emergencyMode = new SequenceNode("应急救援模式");

        ActionNode alertResponse = new ActionNode("响应灾害预警",
            () -> System.out.println("  -> 车机和无人机响应道路塌方/事故预警"));

        // [装饰器应用6: TimeoutDecorator] 空中侦察受时间限制
        ActionNode aerialReconnaissance = new ActionNode("空中侦察",
            () -> {
                try { Thread.sleep(800); } catch (Exception e) {}
                System.out.println("  -> 无人机进行空中侦察，评估灾情");
            });
        TimeoutDecorator timeoutRecon = new TimeoutDecorator("空中侦察(10秒超时)", 10000);
        timeoutRecon.addChild(aerialReconnaissance);

        ActionNode coordinateRescue = new ActionNode("协调救援",
            () -> System.out.println("  -> 通过V2X协议协调应急车辆和资源"));

        // ==================== 第四阶段：数据协同 ====================
        SequenceNode dataCoordination = new SequenceNode("数据协同与协议适配");

        // [装饰器应用7: ConditionalDecorator] 只在有预警时才触发紧急数据同步
        ActionNode emergencySync = new ActionNode("紧急数据同步",
            () -> System.out.println("  -> 触发紧急模式下的高频数据同步"));
        ConditionalDecorator emergencyCondition = new ConditionalDecorator("预警时紧急同步",
            () -> false); // 正常配送模式下不触发
        emergencyCondition.addChild(emergencySync);

        ActionNode convertVehicleProtocol = new ActionNode("转换车机协议",
            () -> System.out.println("  -> BYD-Protocol数据转换为DDS-Standard格式"));

        // [装饰器应用8: RetryDecorator] 协议转换可能失败需要重试
        ActionNode convertDroneProtocol = new ActionNode("转换无人机协议",
            () -> {
                System.out.println("  -> DJI-Protocol数据转换为DDS-Standard格式");
            });
        RetryDecorator retryConvert = new RetryDecorator("重试协议转换(最多2次)", 2);
        retryConvert.addChild(convertDroneProtocol);

        ActionNode applySynonymMapping = new ActionNode("同义词语义对齐",
            () -> System.out.println("  -> 将'车速'和'飞行速度'映射到统一术语'speed'"));

        ActionNode applyUnitConversion = new ActionNode("单位转换",
            () -> System.out.println("  -> 车速km/h与无人机速度m/s自动转换"));

        // [装饰器应用9: TimeoutDecorator] DDS发布有严格时间要求
        ActionNode publishToDDS = new ActionNode("发布到DDS",
            () -> {
                try { Thread.sleep(300); } catch (Exception e) {}
                System.out.println("  -> 通过DDS数据分发服务发布实时状态");
            });
        TimeoutDecorator timeoutPublish = new TimeoutDecorator("DDS发布(2秒超时)", 2000);
        timeoutPublish.addChild(publishToDDS);

        ActionNode syncWithV2X = new ActionNode("V2X车路协同",
            () -> System.out.println("  -> 与路侧单元同步交通信号和预警信息"));

        // ==================== 第五阶段：任务完成 ====================
        SequenceNode completionPhase = new SequenceNode("任务完成阶段");

        // [装饰器应用10: ConditionalDecorator] 电量充足时跳过充电
        ActionNode chargeBattery = new ActionNode("充电操作",
            () -> System.out.println("  -> 无人机返回充电站，车辆插电充电"));
        ConditionalDecorator chargeCondition = new ConditionalDecorator("电量不足时充电",
            () -> false); // 电量充足，不执行充电
        chargeCondition.addChild(chargeBattery);

        ActionNode reportStatus = new ActionNode("报告任务状态",
            () -> System.out.println("  -> 向调度中心报告配送完成情况和设备状态"));

        // [装饰器应用11: SuccessDecorator] 关键状态报告必须成功记录
        ActionNode recordLog = new ActionNode("记录任务日志",
            () -> {
                System.out.println("  -> 将任务数据持久化到日志系统");
            });
        SuccessDecorator forceRecord = new SuccessDecorator("强制记录日志(忽略失败)");
        forceRecord.addChild(recordLog);

        ActionNode returnToBase = new ActionNode("返航/充电",
            () -> System.out.println("  -> 车辆返航配送站，无人机返回充电点"));

        // 构建完整行为树
        // 第一阶段
        preparationPhase.addChild(deviceCheck);
        preparationPhase.addChild(retryInitDDS);
        preparationPhase.addChild(timeoutConfigureIDL);

        // 第二阶段
        sensingPhase.addChild(collectVehicleData);
        sensingPhase.addChild(collectDroneData);
        sensingPhase.addChild(repeatMonitor);

        // 第三阶段
        normalDelivery.addChild(timeoutRoutePlanning);
        normalDelivery.addChild(startDelivery);

        droneAssistedDelivery.addChild(assessSituation);
        droneAssistedDelivery.addChild(delayLaunch);
        droneAssistedDelivery.addChild(droneDelivery);

        emergencyMode.addChild(alertResponse);
        emergencyMode.addChild(timeoutRecon);
        emergencyMode.addChild(coordinateRescue);

        missionSelector.addChild(normalDelivery);
        missionSelector.addChild(droneAssistedDelivery);
        missionSelector.addChild(emergencyMode);

        // 第四阶段
        dataCoordination.addChild(emergencyCondition);
        dataCoordination.addChild(convertVehicleProtocol);
        dataCoordination.addChild(retryConvert);
        dataCoordination.addChild(applySynonymMapping);
        dataCoordination.addChild(applyUnitConversion);
        dataCoordination.addChild(timeoutPublish);
        dataCoordination.addChild(syncWithV2X);

        // 第五阶段
        completionPhase.addChild(chargeCondition);
        completionPhase.addChild(reportStatus);
        completionPhase.addChild(forceRecord);
        completionPhase.addChild(returnToBase);

        // 组合所有阶段
        root.addChild(preparationPhase);
        root.addChild(sensingPhase);
        root.addChild(missionSelector);  // SELECTOR会根据是否有预警自动选择合适的任务模式
        root.addChild(dataCoordination);
        root.addChild(completionPhase);

        return new BehaviorTree(root);
    }
}
