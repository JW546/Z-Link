package utils;

import java.util.HashMap;
import java.util.Map;

/**
 * 黑板(Blackboard) - 行为树节点间的全局共享存储区
 * 
 * 核心功能:
 * 1. 全局数据共享 - 允许所有行为树节点访问和修改共享状态
 * 2. 类型安全存储 - 支持多种数据类型
 * 3. 观察者模式 - 支持数据变化监听
 * 
 * 场景: 民用无人交通系统的行为树节点间数据共享
 *       例如: 传感器数据、决策状态、环境信息等
 */
public class Blackboard {
    
    // 单例模式
    private static Blackboard instance;
    
    // 数据存储
    private Map<String, Object> data;
    
    // 数据类型映射 (用于类型检查)
    private Map<String, Class<?>> typeMap;
    
    // 观察者回调
    private Map<String, Runnable> observers;
    
    /**
     * 私有构造函数
     */
    private Blackboard() {
        this.data = new HashMap<>();
        this.typeMap = new HashMap<>();
        this.observers = new HashMap<>();
    }
    
    /**
     * 获取黑板单例
     */
    public static Blackboard getInstance() {
        if (instance == null) {
            instance = new Blackboard();
        }
        return instance;
    }
    
    /**
     * 写入数据
     * 
     * @param key 键
     * @param value 值
     */
    public void write(String key, Object value) {
        Object oldValue = data.get(key);
        data.put(key, value);
        typeMap.put(key, value.getClass());
        
        System.out.println("[黑板写入] " + key + " = " + value);
        
        // 触发观察者
        if (observers.containsKey(key)) {
            observers.get(key).run();
        }
    }
    
    /**
     * 读取数据
     * 
     * @param key 键
     * @return 值,如果不存在则返回null
     */
    public Object read(String key) {
        return data.get(key);
    }
    
    /**
     * 读取指定类型的数据
     * 
     * @param key 键
     * @param type 期望的类型
     * @return 值,如果不存在或类型不匹配则返回null
     */
    @SuppressWarnings("unchecked")
    public <T> T read(String key, Class<T> type) {
        Object value = data.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }
    
    /**
     * 检查键是否存在
     */
    public boolean contains(String key) {
        return data.containsKey(key);
    }
    
    /**
     * 删除数据
     */
    public void remove(String key) {
        data.remove(key);
        typeMap.remove(key);
        observers.remove(key);
        System.out.println("[黑板删除] " + key);
    }
    
    /**
     * 添加观察者
     * 当指定键的数据发生变化时触发回调
     */
    public void addObserver(String key, Runnable observer) {
        observers.put(key, observer);
        System.out.println("[黑板观察者] 添加观察者: " + key);
    }
    
    /**
     * 清空黑板
     */
    public void clear() {
        data.clear();
        typeMap.clear();
        observers.clear();
        System.out.println("[黑板清空] 所有数据已清空");
    }
    
    /**
     * 打印黑板所有内容
     */
    public void printAll() {
        System.out.println("\n========== 黑板内容 ==========");
        if (data.isEmpty()) {
            System.out.println("(空)");
        } else {
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                System.out.println("  " + entry.getKey() + " = " + entry.getValue() + 
                                 " [" + typeMap.get(entry.getKey()).getSimpleName() + "]");
            }
        }
        System.out.println("================================\n");
    }
    
    /**
     * 获取所有键
     */
    public String[] getAllKeys() {
        return data.keySet().toArray(new String[0]);
    }
    
    /**
     * 获取数据数量
     */
    public int size() {
        return data.size();
    }
}
