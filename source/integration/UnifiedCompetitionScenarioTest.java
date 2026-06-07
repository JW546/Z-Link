package integration;

import agent.CPSAgent;
import nlu.Intent;
import nlu.NLUProcessor;
import nlu.NLUResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 绔炶禌/璁烘枃缁熶竴鍦烘櫙娴嬭瘯锛氶噸搴嗗北鍦板簲鎬ュ尰鐤楀崗鍚岄厤閫併€?
 *
 * 鐩爣锛氱敤涓€涓畬鏁翠换鍔￠摼楠岃瘉鏍稿績鑳藉姏锛?
 * 1) 鍗忓悓璇箟璇嗗埆
 * 2) 鍓嶆柟璺彛涓诲姩璁㈤槄
 * 3) 澶氭簮铻嶅悎涓庨噸瑙勫垝瑙﹀彂
 * 4) 閰嶉€佹剰鍥炬墽琛?
 * 5) 鍚屽湴鍒拌揪闆舵椂寤?
 */
public class UnifiedCompetitionScenarioTest {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private static class KPI {
        int nluTotal;
        int nluCorrect;
        int agentTotal;
        int agentCorrect;
        int requiredSubscriptions;
        int hitSubscriptions;
        int requiredPublishes;
        int hitPublishes;
        double fusionScore;
        boolean replanTriggered;
        int phase2EtaMinutes;
        int phase3EtaMinutes;
        int phase4EtaMinutes;
        long endToEndLatencyMs;

        double nluAccuracy() {
            return nluTotal == 0 ? 0.0 : (double) nluCorrect / nluTotal;
        }

        double agentAccuracy() {
            return agentTotal == 0 ? 0.0 : (double) agentCorrect / agentTotal;
        }

        double subscriptionCoverage() {
            return requiredSubscriptions == 0 ? 0.0 : (double) hitSubscriptions / requiredSubscriptions;
        }

        double publishCoverage() {
            return requiredPublishes == 0 ? 0.0 : (double) hitPublishes / requiredPublishes;
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void saveReports(KPI kpi) {
        try {
            Path reportDir = Paths.get("z", "r");
            Files.createDirectories(reportDir);

            String stamp = LocalDateTime.now().format(TS);
            Path csvPath = reportDir.resolve("unified_competition_kpi_" + stamp + ".csv");
            Path mdPath = reportDir.resolve("unified_competition_kpi_" + stamp + ".md");

            List<String> csv = new ArrayList<>();
            csv.add("metric,value");
            csv.add("nlu_intent_accuracy," + String.format("%.4f", kpi.nluAccuracy()));
            csv.add("agent_intent_accuracy," + String.format("%.4f", kpi.agentAccuracy()));
            csv.add("subscription_coverage," + String.format("%.4f", kpi.subscriptionCoverage()));
            csv.add("publish_coverage," + String.format("%.4f", kpi.publishCoverage()));
            csv.add("fusion_score_phase2," + String.format("%.4f", kpi.fusionScore));
            csv.add("replan_triggered," + kpi.replanTriggered);
            csv.add("eta_phase2_minutes," + kpi.phase2EtaMinutes);
            csv.add("eta_phase3_minutes," + kpi.phase3EtaMinutes);
            csv.add("eta_phase4_minutes," + kpi.phase4EtaMinutes);
            csv.add("end_to_end_latency_ms," + kpi.endToEndLatencyMs);
            Files.write(csvPath, csv, StandardCharsets.UTF_8);

            List<String> md = new ArrayList<>();
            md.add("# Unified Competition Scenario KPI Report");
            md.add("");
            md.add("## Scenario");
            md.add("Chongqing mountain emergency medical collaborative delivery");
            md.add("");
            md.add("## KPI Summary");
            md.add("| KPI | Value |");
            md.add("| --- | --- |");
            md.add("| NLU intent accuracy | " + String.format("%.2f%%", kpi.nluAccuracy() * 100) + " |");
            md.add("| Agent intent accuracy | " + String.format("%.2f%%", kpi.agentAccuracy() * 100) + " |");
            md.add("| Subscription coverage | " + String.format("%.2f%%", kpi.subscriptionCoverage() * 100) + " |");
            md.add("| Publish coverage | " + String.format("%.2f%%", kpi.publishCoverage() * 100) + " |");
            md.add("| Fusion score (Phase2) | " + String.format("%.4f", kpi.fusionScore) + " |");
            md.add("| Replan triggered | " + (kpi.replanTriggered ? "Yes" : "No") + " |");
            md.add("| ETA Phase2 (min) | " + kpi.phase2EtaMinutes + " |");
            md.add("| ETA Phase3 (min) | " + kpi.phase3EtaMinutes + " |");
            md.add("| ETA Phase4 (min) | " + kpi.phase4EtaMinutes + " |");
            md.add("| End-to-end latency (ms) | " + kpi.endToEndLatencyMs + " |");
            Files.write(mdPath, md, StandardCharsets.UTF_8);

            System.out.println("[REPORT] CSV: " + csvPath);
            System.out.println("[REPORT] MD : " + mdPath);
        } catch (IOException e) {
            System.err.println("[WARN] KPI report export failed: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        try {
            long start = System.currentTimeMillis();
            KPI kpi = new KPI();

            NLUProcessor nlu = new NLUProcessor();
            CPSAgent agent = new CPSAgent();

            System.out.println("============================================================");
            System.out.println("  Unified Competition Scenario Test");
            System.out.println("  Scene: Chongqing emergency medical collaborative delivery");
            System.out.println("============================================================");

        // Phase 1: 鎺у埗涓績鍗忓悓璋冨害
        NLUResult phase1Nlu = nlu.processToResult("鎺у埗涓績鎵ц璺ㄥ搧鐗屽崗鍚岃皟搴︼紝鑱斿姩鏄熼摼閾捐矾");
        kpi.nluTotal++;
        if (phase1Nlu.getPrimaryIntent() == Intent.COORDINATION) kpi.nluCorrect++;
        require(phase1Nlu.getPrimaryIntent() == Intent.COORDINATION, "Phase1 NLU鏈瘑鍒獵OORDINATION");

        CPSAgent.AgentResult phase1 = agent.processInput("鎺у埗涓績鎵ц璺ㄥ搧鐗屽崗鍚岃皟搴︼紝鑱斿姩鏄熼摼閾捐矾");
        kpi.agentTotal++;
        if ("COORDINATION".equals(phase1.intent)) kpi.agentCorrect++;
        require("COORDINATION".equals(phase1.intent), "Phase1 Agent鎰忓浘閿欒");

        kpi.requiredPublishes += 1;
        if (phase1.publishTopics.contains("CompliantBackboneSync")) kpi.hitPublishes++;
        require(phase1.publishTopics.contains("CompliantBackboneSync"), "Phase1 缂哄皯CompliantBackboneSync");

        kpi.requiredSubscriptions += 1;
        if (phase1.subscribeTopics.contains("GlobalPerception")) kpi.hitSubscriptions++;
        require(phase1.subscribeTopics.contains("GlobalPerception"), "Phase1 缂哄皯GlobalPerception");

        // Phase 2: 杞﹁締鍓嶅悜璺彛鍗忓悓瀵艰埅
        CPSAgent.AgentResult phase2 = agent.processInput("鎴戝湪娌欏潽鍧濓紝鍘昏タ鍗楀ぇ瀛﹂檮灞炲尰闄紝鎻愬墠璁㈤槄鍓嶆柟璺彛鎷ュ牭淇℃伅");
        kpi.agentTotal++;
        if ("NAVIGATION".equals(phase2.intent)) kpi.agentCorrect++;
        require("NAVIGATION".equals(phase2.intent), "Phase2 Agent鎰忓浘搴斾负NAVIGATION");

        kpi.requiredPublishes += 1;
        if (phase2.publishTopics.contains("SubscriptionIntent")) kpi.hitPublishes++;
        require(phase2.publishTopics.contains("SubscriptionIntent"), "Phase2 缂哄皯璁㈤槄鎰忓浘鍙戝竷");

        kpi.requiredSubscriptions += 3;
        if (phase2.subscribeTopics.contains("Traffic/Intersection/Forward")) kpi.hitSubscriptions++;
        if (phase2.subscribeTopics.contains("UAV/Perception/ForwardCorridor")) kpi.hitSubscriptions++;
        if (phase2.subscribeTopics.contains("Route/Replan/Trigger")) kpi.hitSubscriptions++;
        require(phase2.subscribeTopics.contains("Traffic/Intersection/Forward"), "Phase2 缂哄皯鍓嶆柟璺彛浜ら€氳闃?);
        require(phase2.subscribeTopics.contains("UAV/Perception/ForwardCorridor"), "Phase2 缂哄皯鏃犱汉鏈哄墠鏂规劅鐭ヨ闃?);
        require(phase2.subscribeTopics.contains("Route/Replan/Trigger"), "Phase2 缂哄皯閲嶈鍒掕Е鍙戣闃?);
        require(phase2.fusionScore > 0.0, "Phase2 铻嶅悎璇勫垎搴斿ぇ浜?");
        require(phase2.replanTriggered, "Phase2 搴旇Е鍙戦噸瑙勫垝");
        require(phase2.optimizedRoute != null, "Phase2 搴斾骇鍑轰紭鍖栬矾绾?);
        kpi.fusionScore = phase2.fusionScore;
        kpi.replanTriggered = phase2.replanTriggered;
        kpi.phase2EtaMinutes = phase2.optimizedRoute.estimatedTime;

        // Phase 3: 鏃犱汉鏈烘帴鍔涢厤閫?
        NLUResult phase3Nlu = nlu.processToResult("鍚姩鏃犱汉鏈洪厤閫佷换鍔″埌瑗垮崡澶у闄勫睘鍖婚櫌锛岃繍閫佸尰鐤楃墿璧?);
        kpi.nluTotal++;
        if (phase3Nlu.getPrimaryIntent() == Intent.DELIVERY) kpi.nluCorrect++;
        require(phase3Nlu.getPrimaryIntent() == Intent.DELIVERY, "Phase3 NLU鏈瘑鍒獶ELIVERY");

        CPSAgent.AgentResult phase3 = agent.processInput("鍚姩鏃犱汉鏈洪厤閫佷换鍔″埌瑗垮崡澶у闄勫睘鍖婚櫌锛岃繍閫佸尰鐤楃墿璧?);
        kpi.agentTotal++;
        if ("DELIVERY".equals(phase3.intent)) kpi.agentCorrect++;
        require("DELIVERY".equals(phase3.intent), "Phase3 Agent鎰忓浘閿欒");

        kpi.requiredPublishes += 2;
        if (phase3.publishTopics.contains("DroneStatus")) kpi.hitPublishes++;
        if (phase3.publishTopics.contains("DeliveryStatus")) kpi.hitPublishes++;
        require(phase3.publishTopics.contains("DroneStatus"), "Phase3 缂哄皯DroneStatus鍙戝竷");
        require(phase3.publishTopics.contains("DeliveryStatus"), "Phase3 缂哄皯DeliveryStatus鍙戝竷");
        if (phase3.optimizedRoute != null) {
            kpi.phase3EtaMinutes = phase3.optimizedRoute.estimatedTime;
        }

        // Phase 4: 鍒拌揪纭锛堝悓鍦伴浂鏃跺欢锛?
        CPSAgent.AgentResult phase4 = agent.processInput("鎴戝氨鍦ㄨタ鍗楀ぇ瀛﹂檮灞炲尰闄紝浣犲憡璇夋垜鎴戝幓瑗垮崡澶у闄勫睘鍖婚櫌瑕佸涔?);
        require(phase4.optimizedRoute != null, "Phase4 搴旇繑鍥炶矾绾跨粨鏋?);
        require(phase4.optimizedRoute.estimatedTime == 0, "Phase4 鍚屽湴鍦烘櫙棰勮鏃堕棿搴斾负0");
        kpi.phase4EtaMinutes = phase4.optimizedRoute.estimatedTime;

        kpi.endToEndLatencyMs = System.currentTimeMillis() - start;

        System.out.println("---------------- KPI Summary ----------------");
        System.out.printf("NLU intent accuracy      : %.2f%%%n", kpi.nluAccuracy() * 100);
        System.out.printf("Agent intent accuracy    : %.2f%%%n", kpi.agentAccuracy() * 100);
        System.out.printf("Subscription coverage    : %.2f%%%n", kpi.subscriptionCoverage() * 100);
        System.out.printf("Publish coverage         : %.2f%%%n", kpi.publishCoverage() * 100);
        System.out.printf("Fusion score (Phase2)    : %.4f%n", kpi.fusionScore);
        System.out.printf("Replan triggered         : %s%n", kpi.replanTriggered ? "YES" : "NO");
        System.out.printf("ETA P2/P3/P4 (minutes)   : %d / %d / %d%n", kpi.phase2EtaMinutes, kpi.phase3EtaMinutes, kpi.phase4EtaMinutes);
        System.out.printf("End-to-end latency (ms)  : %d%n", kpi.endToEndLatencyMs);
        System.out.println("---------------------------------------------");

        saveReports(kpi);

            System.out.println("[PASS] UnifiedCompetitionScenarioTest passed.");
            System.exit(0);
        } catch (Throwable t) {
            t.printStackTrace();
            System.err.println("[FAIL] UnifiedCompetitionScenarioTest failed.");
            System.exit(1);
        }
    }
}
