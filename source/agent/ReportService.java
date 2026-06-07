package agent;

import java.util.List;
import java.util.Map;

public class ReportService {
    public String buildMarkdown(Map<String, Object> plan, Map<String, Object> execution) {
        if (plan == null) return "# 任务报告\n\n未找到任务计划。";
        StringBuilder report = new StringBuilder();
        report.append("# 无人系统意图协同任务报告\n\n");
        report.append("## 任务概述\n\n");
        report.append("- missionId: ").append(plan.get("missionId")).append("\n");
        report.append("- originalText: ").append(plan.get("input")).append("\n");
        report.append("- decisionSummary: ").append(plan.get("decisionSummary")).append("\n\n");
        appendSection(report, "结构化 DSL", plan.get("structuredDsl"));
        appendSection(report, "参与设备", plan.get("deviceCatalog"));
        appendSection(report, "DDS Topic / QoS", plan.get("qosPolicies"));
        appendSection(report, "路径规划结果", plan.get("routePlan"));
        appendSection(report, "任务调度结果", plan.get("taskSchedule"));
        appendSection(report, "风险评估", plan.get("riskAssessment"));
        appendSection(report, "Fallback 策略", plan.get("fallbackStrategies"));
        if (execution != null) appendSection(report, "执行时间线摘要", execution.get("timelineSummary"));
        appendSection(report, "通信日志摘要", plan.get("communicationLogs"));
        report.append("\n> 本报告由实验型原型自动生成，适用于实验报告和答辩展示。\n");
        return report.toString();
    }

    private void appendSection(StringBuilder report, String title, Object value) {
        report.append("## ").append(title).append("\n\n");
        report.append("```text\n");
        report.append(value == null ? "无数据" : String.valueOf(value));
        report.append("\n```\n\n");
    }
}
