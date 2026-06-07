package integration;

import generated.TailorLexer;
import generated.TailorParser;
import model.DSLVisitor;
import nlu.DSLGenerator;
import nlu.NLUProcessor;
import nlu.NLUResult;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import utils.AmapRealAPI;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.HashMap;
import java.util.Scanner;

/**
 * 閿洏鑷劧璇█娴嬭瘯锛氳緭鍏ョ洰鐨勫湴/绱ф€ョ▼搴?璐圭敤鍋忓ソ锛岀敓鎴怐SL浠ｇ爜銆?
 */
public class NLKeyboardToDSLTest {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final Random RANDOM = new Random();

    private static Path saveJavaFromDsl(String dsl, String ts) throws IOException {
        TailorLexer lexer = new TailorLexer(CharStreams.fromString(dsl));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        TailorParser parser = new TailorParser(tokens);
        DSLVisitor visitor = new DSLVisitor();
        visitor.visitTailorProgram(parser.tailorProgram());
        String javaCode = visitor.getGeneratedJavaCode();

        Path outDir = Paths.get("generated-java");
        Files.createDirectories(outDir);
        Path outFile = outDir.resolve("nl_keyboard_to_dsl_" + ts + ".java");
        Files.writeString(outFile, javaCode, StandardCharsets.UTF_8);
        return outFile;
    }

    private static String ask(Scanner scanner, String prompt, String fallback) {
        System.out.print(prompt);
        String value = scanner.nextLine().trim();
        return value.isEmpty() ? fallback : value;
    }

    private static void configureOrigin(Scanner scanner) {
        String currentOrigin = DSLGenerator.getDefaultOrigin();
        String originInput = ask(scanner,
            "璇疯緭鍏ヨ捣鐐癸紙鍥炶溅浣跨敤榛樿锛? + currentOrigin + "锛夛細",
            currentOrigin);

        if (originInput.equals(currentOrigin)) {
            System.out.println("[ORIGIN] 浣跨敤榛樿璧风偣: " + currentOrigin);
            return;
        }

        try {
            AmapRealAPI amap = new AmapRealAPI();
            AmapRealAPI.AmapGeoResult geo = amap.geocode(originInput);
            if (geo != null && geo.isSuccess()) {
                DSLGenerator.setDefaultOrigin(originInput);
                DSLGenerator.setDefaultOriginCoords(geo.getLocationString());
                System.out.println("[ORIGIN] 宸茶缃捣鐐? " + originInput + " (" + geo.getLocationString() + ")");
            } else {
                System.out.println("[ORIGIN] 鏈兘瑙ｆ瀽璧风偣锛岀户缁娇鐢ㄩ粯璁よ捣鐐? " + currentOrigin);
            }
        } catch (Exception e) {
            System.out.println("[ORIGIN] 璧风偣璁剧疆寮傚父锛岀户缁娇鐢ㄩ粯璁よ捣鐐? " + currentOrigin);
        }
    }

    private static String buildFreeformInput(String intentHint, String target, String urgency, String cost) {
        String intent = intentHint == null ? "" : intentHint.toLowerCase(Locale.ROOT);
        List<String> clauses = new ArrayList<>();

        if (intent.contains("閰嶉€?)) {
            clauses.add(pick("鏃犱汉鏈烘墽琛岄厤閫?, "瀹夋帓閰嶉€佷换鍔?, "鍙戣捣绌哄湴閰嶉€?));
        } else if (intent.contains("鍗忓悓")) {
            clauses.add(pick("鍙戣捣璺ㄨ澶囧崗鍚?, "鍚姩杞︽満鑱斿姩", "寮€鍚叏鍩熷崗鍚?));
        } else if (intent.contains("搴旀€?)) {
            clauses.add(pick("瑙﹀彂搴旀€ュ崗鍚?, "杩涘叆搴旀€ュ缃?, "鍙戣捣绱ф€ヨ仈鍔?));
        } else if (intent.contains("鏌ヨ")) {
            clauses.add(pick("鏌ヨ褰撳墠绯荤粺鐘舵€?, "鎷夊彇杞︽満杩愯鐘舵€?, "姹囨€诲綋鍓嶄换鍔℃€佸娍"));
        } else if (intent.contains("鎺у埗")) {
            clauses.add(pick("鎵ц杞﹁締鎺у埗鍔ㄤ綔", "涓嬪彂杞﹁締鎺у埗鎸囦护", "瑙﹀彂鎺у埗閾捐矾"));
        } else {
            clauses.add(pick("鎵ц瀵艰埅浠诲姟", "鍙戣捣琛岀▼瑙勫垝", "瀹夋帓鍓嶅線鐩爣"));
        }

        if (target != null && !target.isBlank()) {
            clauses.add("鐩爣=" + target);
        }
        if (urgency != null && !urgency.isBlank()) {
            clauses.add("绱ф€ョ▼搴?" + urgency);
        }
        if (cost != null && !cost.isBlank()) {
            clauses.add("璐圭敤鍋忓ソ=" + cost);
        }

        clauses.add("璇风粰鍑哄崗鍚屽缓璁苟杈撳嚭DSL");
        return String.join("锛?, clauses);
    }

    private static String pick(String... options) {
        if (options == null || options.length == 0) {
            return "鎵ц浠诲姟";
        }
        return options[Math.max(0, RANDOM.nextInt(options.length))];
    }

    private static String normalizePlaceText(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text.trim();
        normalized = normalized.replaceAll("^[鎴戜亢鍜盷\\s*(鍦▅浠巪浜巪浣嶄簬)", "");
        normalized = normalized.replaceAll("^(褰撳墠浣嶇疆|鐜板湪鍦▅鐩墠鍦?\\s*", "");
        normalized = normalized.replaceAll("[锛屻€傦紒锛?.!?].*$", "");
        return normalized.trim();
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> map = new HashMap<>();
        if (args == null) {
            return map;
        }
        for (String arg : args) {
            if (arg == null) {
                continue;
            }
            if (arg.startsWith("--origin=")) {
                map.put("origin", arg.substring("--origin=".length()));
            } else if (arg.startsWith("--input=")) {
                map.put("input", arg.substring("--input=".length()));
            }
        }
        return map;
    }

    private static void configureOriginByText(String originInput) {
        String currentOrigin = DSLGenerator.getDefaultOrigin();
        if (originInput == null || originInput.isBlank()) {
            System.out.println("[ORIGIN] 浣跨敤榛樿璧风偣: " + currentOrigin);
            return;
        }
        String normalizedOrigin = normalizePlaceText(originInput);
        if (normalizedOrigin.isBlank()) {
            normalizedOrigin = originInput;
        }
        try {
            AmapRealAPI amap = new AmapRealAPI();
            AmapRealAPI.AmapGeoResult geo = amap.geocode(normalizedOrigin);
            if (geo != null && geo.isSuccess()) {
                DSLGenerator.setDefaultOrigin(normalizedOrigin);
                DSLGenerator.setDefaultOriginCoords(geo.getLocationString());
                System.out.println("[ORIGIN] 宸茶缃捣鐐? " + normalizedOrigin + " (" + geo.getLocationString() + ")");
            } else {
                System.out.println("[ORIGIN] 鏈兘瑙ｆ瀽璧风偣锛岀户缁娇鐢ㄩ粯璁よ捣鐐? " + currentOrigin);
            }
        } catch (Exception e) {
            System.out.println("[ORIGIN] 璧风偣璁剧疆寮傚父锛岀户缁娇鐢ㄩ粯璁よ捣鐐? " + currentOrigin);
        }
    }

    public static void main(String[] args) {
        try {
            Map<String, String> argMap = parseArgs(args);
            String argInput = argMap.get("input");
            String argOrigin = argMap.get("origin");

            Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);

            System.out.println("============================================================");
            System.out.println("  閿洏鑷劧璇█ -> DSL 娴嬭瘯");
            System.out.println("============================================================");

            if (argInput != null && !argInput.isBlank()) {
                configureOriginByText(argOrigin);
            } else {
                configureOrigin(scanner);
            }

            String directSentence;
            if (argInput != null && !argInput.isBlank()) {
                directSentence = argInput;
                System.out.println("鍙洿鎺ヨ緭鍏ュ畬鏁村彞瀛愶紙鍥炶溅璺宠繃锛屼緥锛氬惎鍔ㄦ棤浜烘満閰嶉€佸埌瑙傞煶妗?/ 鎴戣鍘昏タ鍗楀ぇ瀛︼級锛? + directSentence);
            } else {
                directSentence = ask(scanner,
                    "鍙洿鎺ヨ緭鍏ュ畬鏁村彞瀛愶紙鍥炶溅璺宠繃锛屼緥锛氬惎鍔ㄦ棤浜烘満閰嶉€佸埌瑙傞煶妗?/ 鎴戣鍘昏タ鍗楀ぇ瀛︼級锛?,
                    "");
            }

            String naturalLanguage;
            if (!directSentence.isEmpty()) {
                naturalLanguage = directSentence;
            } else {
                String intentHint = ask(scanner,
                    "璇疯緭鍏ョ敤閫旂被鍨嬶紙瀵艰埅/閰嶉€?鍗忓悓/搴旀€?鏌ヨ/鎺у埗锛岄粯璁ゅ鑸級锛?,
                    "瀵艰埅");
                String target = ask(scanner,
                    "璇疯緭鍏ョ洰鏍囷紙鍦扮偣鎴栧璞★紝榛樿瑗垮崡澶у锛夛細",
                    "瑗垮崡澶у");
                String urgency = ask(scanner,
                    "璇疯緭鍏ョ揣鎬ョ▼搴︽弿杩帮紙渚嬶細鎴戝緢鐫€鎬?/ 姝ｅ父锛夛細",
                    "姝ｅ父");
                String cost = ask(scanner,
                    "璇疯緭鍏ヨ垂鐢ㄥ亸濂斤紙渚嬶細璐圭敤涓嶆槸闂 / 鐪侀挶锛夛細",
                    "閮藉彲浠?);

                naturalLanguage = buildFreeformInput(intentHint, target, urgency, cost);
            }

            System.out.println("\n[杈撳叆璇彞] " + naturalLanguage);

            NLUProcessor processor = new NLUProcessor();
            NLUResult result = processor.processToResult(naturalLanguage);
            String dsl = new DSLGenerator().generateDSL(result);

            processor.printResultDetails(result);

            System.out.println("\n---------------- 鐢熸垚DSL ----------------");
            System.out.println(dsl);
            System.out.println("----------------------------------------");

            Path outDir = Paths.get("dsl-output");
            Files.createDirectories(outDir);
            String ts = LocalDateTime.now().format(TS);
            Path outFile = outDir.resolve("nl_keyboard_to_dsl_" + ts + ".dsl");
            Files.writeString(outFile, dsl, StandardCharsets.UTF_8);
            System.out.println("[SAVE] DSL鏂囦欢宸插啓鍏? " + outFile);

            Path javaOut = saveJavaFromDsl(dsl, ts);
            System.out.println("[SAVE] Java鏂囦欢宸插啓鍏? " + javaOut);

            if (result.getPrimaryIntent() == null) {
                System.err.println("[FAIL] 鏈瘑鍒埌涓绘剰鍥撅紝DSL浠诲姟鏃犳晥銆?);
                System.exit(1);
            }

            if (dsl.contains("estimated_distance_km = 0")
                || dsl.contains("estimated_time_min = 0")) {
                System.err.println("[FAIL] 璺緞浼扮畻鏃犳晥锛堝嚭鐜?璺濈/0鏃堕棿锛夛紝璇锋鏌ヨ捣鐐规垨鐩殑鍦拌瘑鍒€?);
                System.exit(1);
            }

            System.out.println("[PASS] 閿洏鑷劧璇█杞珼SL娴嬭瘯閫氳繃銆?);
            System.exit(0);
        } catch (IOException e) {
            System.err.println("[FAIL] DSL鍐欐枃浠跺け璐? " + e.getMessage());
            System.exit(1);
        } catch (Throwable t) {
            t.printStackTrace();
            System.err.println("[FAIL] 閿洏鑷劧璇█杞珼SL娴嬭瘯澶辫触銆?);
            System.exit(1);
        }
    }
}
