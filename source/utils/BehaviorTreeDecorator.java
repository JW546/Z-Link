package utils;

import java.util.function.BooleanSupplier;

/**
 * 
 * 装饰器节点用于修改子节点的执行行为
 * 
 * 场景: 民用无人交通系统的复杂行为控制
 */

/**
 * 反转装饰器 - 反转子节点的返回值
 * 例如: 子节点返回true,则装饰器返回false
 */
class InvertDecorator extends BTNode {
    public InvertDecorator(String name) {
        super(NodeType.SEQUENCE, name); // 装饰器使用SEQUENCE类型标识
    }
    
    @Override
    public boolean execute() {
        System.out.println("[反转装饰器] 执行: " + name);
        if (children.isEmpty()) {
            System.out.println("  警告: 没有子节点");
            return false;
        }
        
        boolean childResult = children.get(0).execute();
        boolean inverted = !childResult;
        
        status = inverted;
        System.out.println("[反转装饰器] " + name + " 子节点结果: " + childResult + 
                          ", 反转后: " + inverted);
        return inverted;
    }
}

/**
 * 重复装饰器 - 重复执行子节点指定次数
 */
class RepeatDecorator extends BTNode {
    private int repeatCount;
    private boolean repeatUntilFail; // 是否重复直到失败

    public RepeatDecorator(String name, int repeatCount) {
        super(NodeType.SEQUENCE, name);
        this.repeatCount = repeatCount;
        this.repeatUntilFail = false;
    }
    
    public RepeatDecorator(String name, boolean repeatUntilFail) {
        super(NodeType.SEQUENCE, name);
        this.repeatCount = -1;
        this.repeatUntilFail = repeatUntilFail;
    }
    
    @Override
    public boolean execute() {
        System.out.println("[重复装饰器] 执行: " + name);
        if (children.isEmpty()) {
            System.out.println("  警告: 没有子节点");
            return false;
        }
        
        boolean allSuccess = true;
        int actualCount = 0;
        
        if (repeatUntilFail) {
            // 重复直到失败
            while (true) {
                actualCount++;
                boolean result = children.get(0).execute();
                if (!result) {
                    allSuccess = false;
                    break;
                }
                if (actualCount >= 100) { // 安全限制
                    System.out.println("  警告: 达到最大重复次数(100)");
                    break;
                }
            }
            System.out.println("  重复执行 " + actualCount + " 次直到失败");
        } else {
            // 固定次数重复
            for (int i = 0; i < repeatCount; i++) {
                actualCount++;
                boolean result = children.get(0).execute();
                if (!result) {
                    allSuccess = false;
                    break;
                }
            }
            System.out.println("  重复执行 " + actualCount + " 次");
        }
        
        status = allSuccess;
        System.out.println("[重复装饰器] " + name + " 结果: " + (allSuccess ? "成功" : "失败"));
        return allSuccess;
    }
}

/**
 * 超时装饰器 - 限制子节点执行时间
 */
class TimeoutDecorator extends BTNode {
    private long timeoutMillis;

    public TimeoutDecorator(String name, long timeoutMillis) {
        super(NodeType.SEQUENCE, name);
        this.timeoutMillis = timeoutMillis;
    }
    
    @Override
    public boolean execute() {
        System.out.println("[超时装饰器] 执行: " + name + " (超时: " + timeoutMillis + "ms)");
        if (children.isEmpty()) {
            System.out.println("  警告: 没有子节点");
            return false;
        }
        
        long startTime = System.currentTimeMillis();
        boolean result = children.get(0).execute();
        long elapsedTime = System.currentTimeMillis() - startTime;
        
        boolean timedOut = elapsedTime > timeoutMillis;
        
        status = result && !timedOut;
        System.out.println("[超时装饰器] " + name + " 执行时间: " + elapsedTime + "ms" +
                          (timedOut ? " (超时!)" : "") + 
                          ", 结果: " + (status ? "成功" : "失败"));
        return status;
    }
}

/**
 * 条件装饰器 - 满足条件时才执行子节点
 */
class ConditionalDecorator extends BTNode {
    private BooleanSupplier condition;

    public ConditionalDecorator(String name, BooleanSupplier condition) {
        super(NodeType.SEQUENCE, name);
        this.condition = condition;
    }
    
    @Override
    public boolean execute() {
        System.out.println("[条件装饰器] 检查: " + name);
        if (!condition.getAsBoolean()) {
            System.out.println("  条件不满足,跳过子节点");
            status = true;  // 条件不满足时跳过，但不视为失败
            return true;
        }

        System.out.println("  条件满足,执行子节点");
        if (children.isEmpty()) {
            System.out.println("  警告: 没有子节点");
            status = false;
            return false;
        }

        boolean result = children.get(0).execute();
        status = result;
        System.out.println("[条件装饰器] " + name + " 结果: " + (result ? "成功" : "失败"));
        return result;
    }
}

/**
 * 失败时重试装饰器 - 子节点失败时重试指定次数
 */
class RetryDecorator extends BTNode {
    private int maxRetries;

    public RetryDecorator(String name, int maxRetries) {
        super(NodeType.SEQUENCE, name);
        this.maxRetries = maxRetries;
    }
    
    @Override
    public boolean execute() {
        System.out.println("[重试装饰器] 执行: " + name + " (最大重试: " + maxRetries + ")");
        if (children.isEmpty()) {
            System.out.println("  警告: 没有子节点");
            return false;
        }
        
        boolean result = false;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            if (attempt > 0) {
                System.out.println("  重试第 " + attempt + " 次...");
            }
            result = children.get(0).execute();
            if (result) {
                System.out.println("  第 " + (attempt + 1) + " 次尝试成功");
                break;
            }
        }
        
        status = result;
        System.out.println("[重试装饰器] " + name + " 结果: " + (result ? "成功" : "失败"));
        return result;
    }
}

/**
 * 延迟装饰器 - 延迟指定时间后执行子节点
 */
class DelayDecorator extends BTNode {
    private long delayMillis;

    public DelayDecorator(String name, long delayMillis) {
        super(NodeType.SEQUENCE, name);
        this.delayMillis = delayMillis;
    }
    
    @Override
    public boolean execute() {
        System.out.println("[延迟装饰器] 执行: " + name + " (延迟: " + delayMillis + "ms)");
        
        if (delayMillis > 0) {
            try {
                Thread.sleep(delayMillis);
                System.out.println("  延迟完成");
            } catch (InterruptedException e) {
                System.out.println("  延迟被中断");
                status = false;
                return false;
            }
        }
        
        if (children.isEmpty()) {
            System.out.println("  警告: 没有子节点");
            status = false;
            return false;
        }
        
        boolean result = children.get(0).execute();
        status = result;
        System.out.println("[延迟装饰器] " + name + " 结果: " + (result ? "成功" : "失败"));
        return result;
    }
}

/**
 * 成功装饰器 - 始终返回成功,不管子节点结果
 */
class SuccessDecorator extends BTNode {
    public SuccessDecorator(String name) {
        super(NodeType.SEQUENCE, name);
    }
    
    @Override
    public boolean execute() {
        System.out.println("[成功装饰器] 执行: " + name);
        if (children.isEmpty()) {
            System.out.println("  警告: 没有子节点");
            status = true;
            return true;
        }
        
        children.get(0).execute();
        status = true; // 始终返回成功
        System.out.println("[成功装饰器] " + name + " 结果: 成功 (忽略子节点结果)");
        return true;
    }
}

/**
 * 失败装饰器 - 始终返回失败,不管子节点结果
 */
class FailureDecorator extends BTNode {
    public FailureDecorator(String name) {
        super(NodeType.SEQUENCE, name);
    }
    
    @Override
    public boolean execute() {
        System.out.println("[失败装饰器] 执行: " + name);
        if (children.isEmpty()) {
            System.out.println("  警告: 没有子节点");
            status = false;
            return false;
        }
        
        children.get(0).execute();
        status = false; // 始终返回失败
        System.out.println("[失败装饰器] " + name + " 结果: 失败 (忽略子节点结果)");
        return false;
    }
}
