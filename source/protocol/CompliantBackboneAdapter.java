package protocol;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * 合规广域链路(COMPLIANT_BACKBONE)卫星通信协议适配器
 * 
 * 功能：
 * - 国内批准系统或预留卫星移动通信链路接入与管理
 * - 全球广域感知数据收发
 * - 纳秒级授时同步
 * - 与地面DDS/MAVLink网络的无缝桥接
 * 
 * 支持两种模式：
 * - 模拟模式 (Mock): 默认，用于开发和测试
 * - 真实模式 (Real): 连接真实合规广域链路网络，需要合规广域链路终端和API密钥
 * 
 * @since 论文第2章 - 系统架构
 */
public class CompliantBackboneAdapter {
    
    // ==================== 运行模式 ====================
    
    /**
     * 运行模式
     */
    public enum RunMode {
        MOCK,   // 模拟模式（默认）
        REAL    // 真实合规广域链路模式
    }
    
    /** 当前运行模式 */
    private RunMode runMode = RunMode.MOCK;
    
    /** 真实合规广域链路客户端（仅在REAL模式下使用） */
    private CompliantBackboneRealClient realClient;
    
    /** API密钥（REAL模式必需） */
    private String apiKey;
    
    /** 真实终端ID */
    private String realTerminalId;
    
    // 合规广域链路终端状态
    public enum TerminalStatus {
        DISCONNECTED,    // 未连接
        CONNECTING,      // 连接中
        CONNECTED,       // 已连接
        SYNCING,         // 授时同步中
        ACTIVE           // 活跃运行
    }
    
    // 卫星轨道类型
    public enum OrbitType {
        LEO_550KM,       // Approved LEO Satcom Reserved Orbit (550km)
        LEO_340KM,       // Approved LEO Satcom Reserved Orbit (340km)
        LEO_1200KM,      // 其他LEO轨道
        GEO              // 地球同步轨道
    }
    
    // 终端配置
    private String terminalId;
    private TerminalStatus status;
    private OrbitType orbitType;
    private String gatewayUrl;
    private long connectionLatencyNs;  // 连接延迟(纳秒)
    private long lastSyncTimestamp;    // 最后同步时间戳
    private boolean timeSyncEnabled;
    
    // 卫星/广域链路缓存
    private static final Map<String, CompliantBackboneAdapter> activeTerminals = new ConcurrentHashMap<>();
    private final List<SatelliteLink> satelliteLinks = new ArrayList<>();
    
    /**
     * 卫星/广域链路信息
     */
    public static class SatelliteLink {
        private String satelliteId;
        private OrbitType orbitType;
        private double elevation;      // 仰角(度)
        private double azimuth;          // 方位角(度)
        private long rttNs;             // 往返时延(纳秒)
        private long timestamp;
        private double snr;             // 信噪比
        
        public SatelliteLink(String satelliteId, OrbitType orbitType) {
            this.satelliteId = satelliteId;
            this.orbitType = orbitType;
            this.timestamp = System.nanoTime();
        }
        
        // Getters and Setters
        public String getSatelliteId() { return satelliteId; }
        public OrbitType getOrbitType() { return orbitType; }
        public double getElevation() { return elevation; }
        public void setElevation(double elevation) { this.elevation = elevation; }
        public double getAzimuth() { return azimuth; }
        public void setAzimuth(double azimuth) { this.azimuth = azimuth; }
        public long getRttNs() { return rttNs; }
        public void setRttNs(long rttNs) { this.rttNs = rttNs; }
        public long getTimestamp() { return timestamp; }
        public double getSnr() { return snr; }
        public void setSnr(double snr) { this.snr = snr; }
        
        /**
         * 计算单向传播延迟(纳秒)
         */
        public long getOneWayDelayNs() {
            return rttNs / 2;
        }
    }
    
    /**
     * 合规广域链路时间同步包
     */
    public static class TimeSyncPacket {
        private long localSendTime;     // 本地发送时间(纳秒)
        private long satelliteRecvTime; // 卫星接收时间(纳秒)
        private long satelliteSendTime; // 卫星发送时间(纳秒)
        private long localRecvTime;     // 本地接收时间(纳秒)
        private long correctedTime;      // 校正后时间(纳秒)
        private double clockOffset;      // 时钟偏移量(毫秒)
        private double clockDrift;       // 时钟漂移率(ppm)
        
        public TimeSyncPacket() {}
        
        public long calculateCorrectedTime() {
            // Two-way timing (TWSTFT) 算法
            // 假设对称链路: Tp = ((T4-T1) - (T3-T2)) / 2
            long t1 = localSendTime;
            long t2 = satelliteRecvTime;
            long t3 = satelliteSendTime;
            long t4 = localRecvTime;
            
            long propagationDelay = ((t4 - t1) - (t3 - t2)) / 2;
            correctedTime = t2 + propagationDelay;
            
            // 计算时钟偏移
            clockOffset = (double) (t2 - t1 + t3 - t4) / 2 / 1_000_000; // 转换为毫秒
            
            return correctedTime;
        }
        
        // Getters and Setters
        public long getLocalSendTime() { return localSendTime; }
        public void setLocalSendTime(long localSendTime) { this.localSendTime = localSendTime; }
        public long getSatelliteRecvTime() { return satelliteRecvTime; }
        public void setSatelliteRecvTime(long satelliteRecvTime) { this.satelliteRecvTime = satelliteRecvTime; }
        public long getSatelliteSendTime() { return satelliteSendTime; }
        public void setSatelliteSendTime(long satelliteSendTime) { this.satelliteSendTime = satelliteSendTime; }
        public long getLocalRecvTime() { return localRecvTime; }
        public void setLocalRecvTime(long localRecvTime) { this.localRecvTime = localRecvTime; }
        public long getCorrectedTime() { return correctedTime; }
        public double getClockOffset() { return clockOffset; }
        public double getClockDrift() { return clockDrift; }
        public void setClockDrift(double clockDrift) { this.clockDrift = clockDrift; }
    }
    
    /**
     * 全球态势感知数据包
     */
    public static class GlobalPerceptionData {
        private long timestamp;
        private double latitude;
        private double longitude;
        private double altitude;
        private String dataSource;       // 数据来源: "satellite", "terrestrial", "aerial"
        private String perceptionType;    // 感知类型: "terrain", "weather", "obstacle", "traffic"
        private String content;           // 感知内容(JSON)
        private int confidence;          // 置信度(0-100)
        private double coverageRadius;   // 覆盖半径(km)
        
        public GlobalPerceptionData() {
            this.timestamp = System.nanoTime();
        }
        
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        
        // Getters and Setters
        public long getTimestamp() { return timestamp; }
        public double getLatitude() { return latitude; }
        public void setLatitude(double latitude) { this.latitude = latitude; }
        public double getLongitude() { return longitude; }
        public void setLongitude(double longitude) { this.longitude = longitude; }
        public double getAltitude() { return altitude; }
        public void setAltitude(double altitude) { this.altitude = altitude; }
        public String getDataSource() { return dataSource; }
        public void setDataSource(String dataSource) { this.dataSource = dataSource; }
        public String getPerceptionType() { return perceptionType; }
        public void setPerceptionType(String perceptionType) { this.perceptionType = perceptionType; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public int getConfidence() { return confidence; }
        public void setConfidence(int confidence) { this.confidence = confidence; }
        public double getCoverageRadius() { return coverageRadius; }
        public void setCoverageRadius(double coverageRadius) { this.coverageRadius = coverageRadius; }
        
        /**
         * 转换为标准格式
         */
        public String toStandardFormat() {
            return String.format(
                "COMPLIANT_BACKBONE:timestamp=%d,lat=%.6f,lon=%.6f,alt=%.1f,source=%s,type=%s,confidence=%d",
                timestamp, latitude, longitude, altitude, dataSource, perceptionType, confidence
            );
        }
    }
    
    /**
     * 创建合规广域链路适配器实例
     */
    public CompliantBackboneAdapter(String terminalId, OrbitType orbitType) {
        this.terminalId = terminalId;
        this.status = TerminalStatus.DISCONNECTED;
        this.orbitType = orbitType;
        this.timeSyncEnabled = true;
        this.gatewayUrl = "https://approved-satcom.example.cn/api/v1/";
    }
    
    /**
     * 连接到合规广域链路网络
     */
    public boolean connect() {
        System.out.println("[COMPLIANT_BACKBONE] 终端 " + terminalId + " 正在连接...");
        
        status = TerminalStatus.CONNECTING;
        
        // 模拟终端注册过程
        try {
            // 实际场景中需要与合规广域链路地面站建立连接
            // 1. 扫描可见卫星
            scanSatellites();
            
            // 2. 选择最佳链路
            SatelliteLink bestLink = selectBestLink();
            if (bestLink == null) {
                System.out.println("[COMPLIANT_BACKBONE] 错误: 未发现可用卫星/广域链路");
                status = TerminalStatus.DISCONNECTED;
                return false;
            }
            
            // 3. 建立连接
            establishLink(bestLink);
            
            // 4. 启动授时同步
            if (timeSyncEnabled) {
                status = TerminalStatus.SYNCING;
                startTimeSync();
            }
            
            status = TerminalStatus.ACTIVE;
            activeTerminals.put(terminalId, this);
            
            System.out.println("[COMPLIANT_BACKBONE] ✓ 终端 " + terminalId + " 已连接");
            System.out.println("  轨道类型: " + orbitType);
            System.out.println("  连接卫星: " + bestLink.getSatelliteId());
            System.out.println("  链路延迟: " + (bestLink.getRttNs() / 1_000_000) + " ms");
            
            return true;
            
        } catch (Exception e) {
            System.out.println("[COMPLIANT_BACKBONE] 连接失败: " + e.getMessage());
            status = TerminalStatus.DISCONNECTED;
            return false;
        }
    }
    
    /**
     * 扫描可见卫星
     */
    private void scanSatellites() {
        System.out.println("[COMPLIANT_BACKBONE] 扫描可见卫星...");
        
        satelliteLinks.clear();
        
        // 模拟扫描到的卫星
        String[] satelliteIds = {
            "COMPLIANT_BACKBONE-1234", "COMPLIANT_BACKBONE-2345", "COMPLIANT_BACKBONE-3456"
        };
        
        for (int i = 0; i < satelliteIds.length; i++) {
            SatelliteLink link = new SatelliteLink(satelliteIds[i], orbitType);
            link.setElevation(45 + i * 10);    // 仰角
            link.setAzimuth(90 + i * 30);       // 方位角
            link.setRttNs(20_000_000L + i * 5_000_000L);  // 20-30ms RTT
            
            satelliteLinks.add(link);
            System.out.println("  发现卫星: " + satelliteIds[i] + 
                             " (仰角:" + link.getElevation() + "°, RTT:" + (link.getRttNs()/1_000_000) + "ms)");
        }
    }
    
    /**
     * 选择最佳卫星/广域链路(基于仰角和延迟)
     */
    private SatelliteLink selectBestLink() {
        if (satelliteLinks.isEmpty()) {
            return null;
        }
        
        // 选择仰角最高、延迟最低的链路
        SatelliteLink best = satelliteLinks.get(0);
        for (SatelliteLink link : satelliteLinks) {
            // 综合评分: 仰角权重0.6, 延迟权重0.4
            double bestScore = best.getElevation() * 0.6 + (100 - best.getRttNs()/1_000_000) * 0.4;
            double linkScore = link.getElevation() * 0.6 + (100 - link.getRttNs()/1_000_000) * 0.4;
            
            if (linkScore > bestScore) {
                best = link;
            }
        }
        
        return best;
    }
    
    /**
     * 建立卫星/广域链路
     */
    private void establishLink(SatelliteLink link) {
        System.out.println("[COMPLIANT_BACKBONE] 建立链路到 " + link.getSatelliteId() + "...");
        
        // 模拟链路建立过程
        // 1. 发送注册请求
        // 2. 接收网关分配
        // 3. 建立加密隧道
        
        connectionLatencyNs = link.getRttNs();
        System.out.println("[COMPLIANT_BACKBONE] 链路建立成功, 延迟: " + (connectionLatencyNs/1_000_000) + "ms");
    }
    
    /**
     * 启动纳秒级授时同步
     */
    public void startTimeSync() {
        System.out.println("[COMPLIANT_BACKBONE] 启动纳秒级授时同步...");
        
        // 执行多次授时以提高精度
        for (int i = 0; i < 5; i++) {
            TimeSyncPacket syncPacket = performTimeSync();
            
            if (syncPacket != null) {
                double offsetMs = syncPacket.getClockOffset();
                System.out.printf("[COMPLIANT_BACKBONE] 授时 #%d: 时钟偏移 = %.6f ms, 校正时间 = %d ns%n",
                                i + 1, offsetMs, syncPacket.getCorrectedTime());
                
                // 校正本地时钟
                applyTimeCorrection(syncPacket);
            }
            
            try {
                Thread.sleep(100); // 间隔100ms
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        lastSyncTimestamp = System.nanoTime();
        System.out.println("[COMPLIANT_BACKBONE] ✓ 授时同步完成");
    }
    
    /**
     * 执行单次授时同步
     */
    public TimeSyncPacket performTimeSync() {
        TimeSyncPacket packet = new TimeSyncPacket();
        
        // T1: 本地发送时间
        packet.setLocalSendTime(System.nanoTime());
        
        // 模拟卫星处理延迟和转发
        // 实际场景中数据包需要经过卫星网络传输到地面站再返回
        long processingDelay = 5_000_000L; // 5ms处理延迟
        
        // T2: 卫星接收时间
        packet.setSatelliteRecvTime(packet.getLocalSendTime() + processingDelay);
        
        // T3: 卫星发送时间(包含地面站校正)
        packet.setSatelliteSendTime(packet.getSatelliteRecvTime() + 15_000_000L); // 15ms卫星处理
        
        // T4: 本地接收时间
        packet.setLocalRecvTime(System.nanoTime());
        
        // 计算校正时间
        packet.calculateCorrectedTime();
        
        return packet;
    }
    
    /**
     * 应用时间校正
     */
    public void applyTimeCorrection(TimeSyncPacket packet) {
        // 实际应用中需要调整系统时钟
        // Java中通常通过NTP或PTP协议实现
        // 这里仅记录校正值
        
        System.out.printf("[COMPLIANT_BACKBONE] 时间校正值: %.6f ms (漂移率: %.3f ppm)%n",
            packet.getClockOffset(), packet.getClockDrift());
    }
    
    /**
     * 发送全球态势感知数据
     */
    public boolean sendGlobalPerceptionData(GlobalPerceptionData data) {
        if (status != TerminalStatus.ACTIVE) {
            System.out.println("[COMPLIANT_BACKBONE] 错误: 终端未连接");
            return false;
        }
        
        // 1. 添加时间戳和位置信息
        data.setTimestamp(System.nanoTime());
        
        // 2. 编码为合规广域链路协议格式
        String encoded = encodeForTransmission(data);
        
        // 3. 通过卫星/广域链路发送
        System.out.println("[COMPLIANT_BACKBONE] 发送全球态势数据:");
        System.out.println("  类型: " + data.getPerceptionType());
        System.out.println("  位置: (" + data.getLatitude() + ", " + data.getLongitude() + ")");
        System.out.println("  覆盖半径: " + data.getCoverageRadius() + " km");
        System.out.println("  编码: " + encoded.substring(0, Math.min(80, encoded.length())) + "...");
        
        // 4. 模拟发送成功
        return true;
    }
    
    /**
     * 接收全球态势感知数据
     */
    public GlobalPerceptionData receiveGlobalPerceptionData() {
        if (status != TerminalStatus.ACTIVE) {
            return null;
        }
        
        // 模拟从卫星网络接收数据
        // 实际应用中需要实现数据接收和解析逻辑
        
        GlobalPerceptionData data = new GlobalPerceptionData();
        data.setLatitude(29.5582);  // 重庆区域示例
        data.setLongitude(106.5549);
        data.setAltitude(300);
        data.setDataSource("satellite");
        data.setPerceptionType("weather");
        data.setContent("{\"visibility\":450,\"fog_density\":0.65}");
        data.setConfidence(92);
        data.setCoverageRadius(50);
        
        return data;
    }
    
    /**
     * 编码为合规广域链路传输协议
     */
    private String encodeForTransmission(GlobalPerceptionData data) {
        return String.format(
            "COMPLIANT_BACKBONE_FRAME:id=%s,ts=%d,payload=%s",
            terminalId, data.getTimestamp(), data.toStandardFormat()
        );
    }
    
    /**
     * 将合规广域链路数据转换为DDS格式
     */
    public String toDDSFormat(GlobalPerceptionData data) {
        return String.format(
            "DDS:topic=global/perception/COMPLIANT_BACKBONE,type=COMPLIANT_BACKBONEPerception,id=%s,lat=%.6f,lon=%.6f,alt=%.1f,perception_type=%s,source=%s,confidence=%d,timestamp=%d,coverage_radius=%.1f",
            terminalId, data.getLatitude(), data.getLongitude(), 
            data.getAltitude(), data.getPerceptionType(), data.getDataSource(), data.getConfidence(), data.getTimestamp(), data.getCoverageRadius()
        );
    }
    
    /**
     * 将DDS数据转换为合规广域链路格式
     */
    public GlobalPerceptionData fromDDSFormat(String ddsData) {
        GlobalPerceptionData data = new GlobalPerceptionData();
        Map<String, String> fields = parseKeyValueFields(ddsData);

        if (fields.containsKey("lat")) {
            data.setLatitude(parseDouble(fields.get("lat"), 0.0));
        }
        if (fields.containsKey("lon")) {
            data.setLongitude(parseDouble(fields.get("lon"), 0.0));
        }
        if (fields.containsKey("alt")) {
            data.setAltitude(parseDouble(fields.get("alt"), 0.0));
        }
        if (fields.containsKey("perception_type")) {
            data.setPerceptionType(fields.get("perception_type"));
        } else if (fields.containsKey("type")) {
            data.setPerceptionType(fields.get("type"));
        }
        if (fields.containsKey("source")) {
            data.setDataSource(fields.get("source"));
        } else if (fields.containsKey("id")) {
            data.setDataSource(fields.get("id"));
        } else {
            data.setDataSource("COMPLIANT_BACKBONE_converted");
        }
        if (fields.containsKey("confidence")) {
            data.setConfidence(parseInt(fields.get("confidence"), 0));
        }
        if (fields.containsKey("timestamp")) {
            data.setTimestamp(parseLong(fields.get("timestamp"), System.nanoTime()));
        }
        if (fields.containsKey("coverage_radius")) {
            data.setCoverageRadius(parseDouble(fields.get("coverage_radius"), 0.0));
        }

        return data;
    }

    private Map<String, String> parseKeyValueFields(String text) {
        Map<String, String> fields = new java.util.HashMap<>();
        if (text == null || text.isBlank()) {
            return fields;
        }

        String cleaned = text.trim();
        int braceStart = cleaned.indexOf('{');
        int braceEnd = cleaned.lastIndexOf('}');
        if (braceStart >= 0 && braceEnd > braceStart) {
            cleaned = cleaned.substring(braceStart + 1, braceEnd);
        }

        String[] parts = cleaned.split(",");
        for (String part : parts) {
            String item = part.trim();
            if (item.isEmpty() || !item.contains("=")) {
                continue;
            }
            String[] kv = item.split("=", 2);
            if (kv.length == 2) {
                fields.put(kv[0].trim(), kv[1].trim().replace("\"", ""));
            }
        }
        return fields;
    }

    private double parseDouble(String value, double fallback) {
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return fallback;
        }
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return fallback;
        }
    }

    private long parseLong(String value, long fallback) {
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            return fallback;
        }
    }
    
    /**
     * 断开合规广域链路连接
     */
    public void disconnect() {
        System.out.println("[COMPLIANT_BACKBONE] 终端 " + terminalId + " 断开连接...");
        
        status = TerminalStatus.DISCONNECTED;
        activeTerminals.remove(terminalId);
        satelliteLinks.clear();
        
        System.out.println("[COMPLIANT_BACKBONE] ✓ 已断开连接");
    }
    
    /**
     * 获取终端状态
     */
    public TerminalStatus getStatus() {
        return status;
    }
    
    /**
     * 获取连接延迟(纳秒)
     */
    public long getConnectionLatencyNs() {
        return connectionLatencyNs;
    }
    
    /**
     * 获取终端ID
     */
    public String getTerminalId() {
        return terminalId;
    }
    
    /**
     * 获取所有活跃终端
     */
    public static Map<String, CompliantBackboneAdapter> getActiveTerminals() {
        return activeTerminals;
    }
    
    /**
     * 获取当前可见卫星列表
     */
    public List<SatelliteLink> getSatelliteLinks() {
        return satelliteLinks;
    }
    
    /**
     * 获取最后同步时间
     */
    public long getLastSyncTimestamp() {
        return lastSyncTimestamp;
    }
    
    /**
     * 检查授时同步状态
     */
    public boolean isTimeSynced() {
        if (!timeSyncEnabled) return false;
        
        // 检查最后同步时间是否在1秒内
        long timeSinceSync = System.nanoTime() - lastSyncTimestamp;
        return timeSinceSync < 1_000_000_000L; // 1秒
    }
    
    /**
     * 工厂方法: 创建标准LEO合规广域链路适配器
     */
    public static CompliantBackboneAdapter createLEOAdapter(String terminalId) {
        return new CompliantBackboneAdapter(terminalId, OrbitType.LEO_550KM);
    }
    
    /**
     * 工厂方法: 创建高轨合规广域链路适配器
     */
    public static CompliantBackboneAdapter createGEOBridge(String terminalId) {
        return new CompliantBackboneAdapter(terminalId, OrbitType.LEO_1200KM);
    }
    
    // ==================== 真实合规广域链路接入接口 ====================
    
    /**
     * 切换到真实合规广域链路模式
     * 
     * 需要条件：
     * 1. 拥有真实的合规广域链路终端设备
     * 2. 从国内批准系统或境内运营服务方获取API访问权限
     * 3. 配置有效的API密钥
     * 
     * @param apiKey 合规广域链路API密钥
     * @param terminalId 合规广域链路终端硬件ID
     * @return 是否切换成功
     */
    public boolean enableRealMode(String apiKey, String terminalId) {
        System.out.println("[COMPLIANT_BACKBONE] 切换到真实合规广域链路模式...");
        System.out.println("  API密钥: " + (apiKey != null ? "已配置" : "未配置"));
        System.out.println("  终端ID: " + terminalId);
        
        this.apiKey = apiKey;
        this.realTerminalId = terminalId;
        this.realClient = CompliantBackboneRealClient.create(apiKey, terminalId);
        
        if (realClient == null) {
            System.out.println("[COMPLIANT_BACKBONE] ✗ 真实模式初始化失败");
            return false;
        }
        
        // 执行真实认证
        CompliantBackboneRealClient.AuthResponse auth = realClient.authenticate();
        if (auth != null && auth.isValid()) {
            // 建立实时数据流
            if (realClient.connect()) {
                this.runMode = RunMode.REAL;
                this.status = TerminalStatus.ACTIVE;
                System.out.println("[COMPLIANT_BACKBONE] ✓ 已切换到真实合规广域链路模式");
                return true;
            }
        }
        
        System.out.println("[COMPLIANT_BACKBONE] ✗ 无法连接到真实合规广域链路网络");
        return false;
    }
    
    /**
     * 从环境变量配置真实合规广域链路模式
     * 
     * 需要设置环境变量：
     * - COMPLIANT_BACKBONE_API_KEY: 合规广域链路API密钥
     * - COMPLIANT_BACKBONE_TERMINAL_ID: 合规广域链路终端硬件ID
     * 
     * @return 是否切换成功
     */
    public boolean enableRealModeFromEnv() {
        String apiKey = System.getenv("COMPLIANT_BACKBONE_API_KEY");
        String terminalId = System.getenv("COMPLIANT_BACKBONE_TERMINAL_ID");
        return enableRealMode(apiKey, terminalId);
    }
    
    /**
     * 切换回模拟模式
     */
    public void disableRealMode() {
        if (runMode == RunMode.REAL && realClient != null) {
            realClient.disconnect();
            realClient = null;
        }
        this.runMode = RunMode.MOCK;
        System.out.println("[COMPLIANT_BACKBONE] 已切换回模拟模式");
    }
    
    /**
     * 检查是否运行在真实模式
     */
    public boolean isRealMode() {
        return runMode == RunMode.REAL;
    }
    
    /**
     * 获取当前运行模式
     */
    public RunMode getRunMode() {
        return runMode;
    }
    
    /**
     * 获取真实合规广域链路客户端
     */
    public CompliantBackboneRealClient getRealClient() {
        return realClient;
    }
    
    /**
     * 真实模式：获取终端状态
     */
    public CompliantBackboneRealClient.TerminalStatus getRealTerminalStatus() {
        if (runMode != RunMode.REAL || realClient == null) {
            return null;
        }
        return realClient.getTerminalStatus();
    }
    
    /**
     * 真实模式：获取可见卫星
     */
    public CompliantBackboneRealClient.SatelliteStatus[] getRealVisibleSatellites() {
        if (runMode != RunMode.REAL || realClient == null) {
            return null;
        }
        return realClient.getVisibleSatellites();
    }
    
    /**
     * 真实模式：发送遥测数据
     */
    public boolean sendRealTelemetry(double lat, double lon, double alt, 
                                     double speed, double heading, 
                                     double battery, double signal) {
        if (runMode != RunMode.REAL || realClient == null) {
            return false;
        }
        
        CompliantBackboneRealClient.TelemetryData telemetry = new CompliantBackboneRealClient.TelemetryData();
        telemetry.timestamp = System.currentTimeMillis();
        telemetry.latitude = lat;
        telemetry.longitude = lon;
        telemetry.altitude = alt;
        telemetry.speed = speed;
        telemetry.heading = heading;
        telemetry.batteryPercent = battery;
        telemetry.signalStrength = signal;
        
        return realClient.sendTelemetry(telemetry);
    }
    
    /**
     * 真实模式：获取真实连接延迟
     */
    public long getRealConnectionLatencyNs() {
        if (runMode != RunMode.REAL || realClient == null) {
            return connectionLatencyNs;
        }
        
        CompliantBackboneRealClient.TerminalStatus status = realClient.getTerminalStatus();
        if (status != null) {
            return status.pingMs * 1_000_000L; // 转换为纳秒
        }
        return connectionLatencyNs;
    }
}
