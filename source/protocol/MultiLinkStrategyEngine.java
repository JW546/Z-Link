package protocol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 多链路策略引擎。
 * 用于在竞赛/工程场景中快速给出“主链路-备链路-广域感知链路”的推荐组合。
 */
public class MultiLinkStrategyEngine {

    public enum LinkType {
        ADHOC_MESH,
        V2X_5G,
        DDS_ROUTER_WAN,
        CN_LEO_SATCOM,
        BEIDOU_SHORT_MESSAGE,
        PRIVATE_5GA_EDGE,
        COMPLIANT_BACKBONE,
    }

    public static class LinkContext {
        public boolean compliantBackboneAllowedInRegion;
        public boolean compliantBackboneRuntimeAvailable;
        public boolean chinaRegion = true;
        public boolean cnLeoSatcomAvailable;
        public boolean beidouShortMessageAvailable;
        public boolean private5GAvailable = true;
        public boolean edgeNodeAvailable;
        public boolean missionCritical;
        public boolean longDistanceCoordination;
    }

    public static class PlanResult {
        public LinkType primaryBackbone;
        public LinkType secondaryBackbone;
        public LinkType globalPerceptionLink;
        public boolean edgeNodeRequired;
        public List<String> rationale = new ArrayList<>();
    }

    public PlanResult plan(LinkContext ctx) {
        PlanResult result = new PlanResult();

        // 主链路优先保证本地自治与可落地性。
        result.primaryBackbone = LinkType.ADHOC_MESH;
        result.secondaryBackbone = ctx.private5GAvailable ? LinkType.PRIVATE_5GA_EDGE : LinkType.V2X_5G;
        result.globalPerceptionLink = LinkType.DDS_ROUTER_WAN;
        result.edgeNodeRequired = ctx.edgeNodeAvailable || ctx.missionCritical;

        result.rationale.add("主链路优先采用 AdHoc Mesh，确保弱网/断网条件下近场自治协同");
        result.rationale.add("备链路使用 V2X/5G 或 5G-A 专网边缘，承担城市道路和运营商网络覆盖场景");

        if (ctx.longDistanceCoordination) {
            result.globalPerceptionLink = LinkType.DDS_ROUTER_WAN;
            result.rationale.add("远距协同时启用 DDS Router 跨域桥接，降低直连复杂度");
        }

        if (ctx.chinaRegion && ctx.cnLeoSatcomAvailable) {
            result.globalPerceptionLink = LinkType.CN_LEO_SATCOM;
            result.rationale.add("中国境内演示优先使用国产低轨卫星互联网预留接口，避免把境外商用卫星通信链路写成依赖");
        } else if (ctx.chinaRegion && ctx.beidouShortMessageAvailable && ctx.missionCritical) {
            result.globalPerceptionLink = LinkType.BEIDOU_SHORT_MESSAGE;
            result.rationale.add("任务关键且宽带链路不可用时，可用北斗短报文承载低频告警和最小控制意图");
        } else if (!ctx.chinaRegion && ctx.compliantBackboneAllowedInRegion && ctx.compliantBackboneRuntimeAvailable) {
            result.globalPerceptionLink = LinkType.COMPLIANT_BACKBONE;
            result.rationale.add("境外合规环境下，COMPLIANT_BACKBONE 可作为全局感知与跨域辅助链路，不作为唯一总路");
        } else {
            result.rationale.add("境内默认不依赖 COMPLIANT_BACKBONE，自动回退到地面广域链路或国产卫星互联网预留接口");
        }

        if (result.edgeNodeRequired) {
            result.rationale.add("轻边缘节点承载协议转换、缓存重放、链路健康探测与切换决策");
        } else {
            result.rationale.add("当前可无边缘运行，但不满足高可靠赛事与工程验收目标");
        }

        return result;
    }

    public String toSummary(PlanResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("[LINK-PLAN] primary=").append(result.primaryBackbone)
          .append(", secondary=").append(result.secondaryBackbone)
          .append(", global=").append(result.globalPerceptionLink)
          .append(", edgeRequired=").append(result.edgeNodeRequired)
          .append('\n');
        for (String r : result.rationale) {
            sb.append("  - ").append(r).append('\n');
        }
        return sb.toString();
    }

    public List<LinkType> recommendedFailoverOrder(PlanResult result) {
        List<LinkType> order = new ArrayList<>();
        order.add(result.primaryBackbone);
        order.add(result.secondaryBackbone);
        if (!order.contains(result.globalPerceptionLink)) {
            order.add(result.globalPerceptionLink);
        }
        return Collections.unmodifiableList(order);
    }
}
