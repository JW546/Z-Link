package model;

import java.util.Map;

/**
 * DSL 实体抽象基类
 * 提供 Task、PathPlan、State 的公共接口
 */
public abstract class AbstractDSLEntity {
    
    public abstract String getName();
    
    public abstract Map<String, String> getFields();
    
    public abstract AnnotationInfo getAnnotation();
}
