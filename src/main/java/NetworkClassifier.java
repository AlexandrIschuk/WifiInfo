import java.time.LocalDateTime;
import java.util.*;

public class NetworkClassifier {
    private MacVendorResolver vendorResolver;


    private Set<String> routerVendors;
    private Set<String> mobileChipVendors;
    private Set<String> iotChipVendors;
    private Set<String> enterpriseVendors;

    private Map<String, Queue<Integer>> rssiHistory = new HashMap<>();
    private static final int MAX_RSSI_HISTORY = 10;
    private static final double HIGH_VARIANCE_THRESHOLD = 40.0;

    public NetworkClassifier() {
        this.vendorResolver = MacVendorResolver.getInstance();
        initVendorCategories();
    }

    private void initVendorCategories() {
        routerVendors = new HashSet<>(Arrays.asList(
                "TP-Link", "TP-Link Technologies", "Cisco", "Cisco Systems",
                "Netgear", "D-Link", "D-Link Corporation", "D-Link International",
                "Asus", "ASUSTek", "ASUSTek COMPUTER", "Linksys", "Cisco-Linksys",
                "Belkin", "Belkin International", "Ubiquiti", "Ubiquiti Inc",
                "MikroTik", "ZyXEL", "Zyxel Communications", "Tenda", "Tenda Technology",
                "Huawei", "Huawei Technologies", "Huawei Device",
                "Xiaomi", "Xiaomi Communications", "Mi", "Mercury",
                "Keenetic", "TotoLink", "Pirelli", "Sercomm", "Sercomm Corporation",
                "Arcadyan", "Arcadyan Corporation", "Alpha Networks",
                "Actiontec", "Actiontec Electronics", "Buffalo", "Buffalo.Inc",
                "Edimax", "Edimax Technology", "TRENDnet", "Tp-Link", "TpLink",
                "Hon Hai", "Hon Hai Precision", "Foxconn", "Pegatron",
                "Wistron", "Compal", "Compal Electronics", "Gemtek", "Gemtek Technology",
                "Accton", "Accton Technology", "ZTE", "zte corporation",
                "Nokia", "Nokia Solutions", "Nokia Networks",
                "Ericsson", "Motorola Solutions", "Siemens", "Siemens AG",
                "Fiberhome", "Fiberhome Telecommunication", "Raisecom",
                "Ruijie", "Ruijie Networks", "H3C", "H3C Technologies",
                "Maipu", "Digi", "Digi International", "Lantronix",
                "Moxa", "Moxa Technologies", "Advantech", "Kontron",
                "SMC Networks", "SMC", "LevelOne", "Planet", "Planet Technology",
                "EnGenius", "Senao", "AirTies", "AirTies Wireless",
                "Comtrend", "Zhone", "Zhone Technologies", "Zyxel",
                "Calix", "Calix Inc", "Adtran", "Adtran Inc", "Telco Systems",
                "Allied Telesis", "Allied Telesyn", "Transition Networks"
        ));

        mobileChipVendors = new HashSet<>(Arrays.asList(
                "Qualcomm", "Qualcomm Inc", "MediaTek", "MediaTek Inc",
                "Broadcom", "Broadcom Limited", "Intel", "Intel Corporation",
                "Samsung", "Samsung Electronics", "Samsung Electro-Mechanics",
                "Apple", "Apple Inc", "Apple, Inc",
                "OnePlus", "OnePlus Technology", "Google", "Google Inc",
                "Motorola", "Motorola Mobility", "Sony", "Sony Corporation",
                "LG", "LG Electronics", "LG Innotek", "HTC", "HTC Corporation",
                "Nokia", "Nokia Corporation", "Xiaomi", "Xiaomi Communications",
                "Huawei", "Huawei Device", "Oppo", "Oppo Mobile", "Guangdong Oppo",
                "Vivo", "vivo Mobile", "Realme", "Realme Chongqing",
                "Honor", "Honor Device", "OnePlus", "ZTE", "zte corporation",
                "BlackBerry", "BlackBerry RTS", "Lenovo", "Lenovo Mobile",
                "ASUS", "ASUSTek", "Acer", "Acer Incorporated",
                "Panasonic", "Panasonic Mobile", "Sharp", "SHARP Corporation",
                "Fujitsu", "Fujitsu Limited", "NEC", "NEC Corporation",
                "Toshiba", "Toshiba Corporation", "Kyocera", "KYOCERA",
                "Dialog", "Dialog Semiconductor", "Cirrus Logic",
                "Maxim", "Maxim Integrated", "Analog Devices",
                "Texas Instruments", "STMicroelectronics", "NXP", "NXP Semiconductors",
                "Murata", "Murata Manufacturing", "TDK", "TDK Corporation",
                "Skyworks", "Qorvo", "RF Micro Devices"
        ));


        iotChipVendors = new HashSet<>(Arrays.asList(
                "Espressif", "Espressif Inc",
                "Texas Instruments", "TI",
                "Raspberry Pi", "Raspberry Pi Trading", "Raspberry Pi Foundation",
                "Nordic", "Nordic Semiconductor", "Silicon Labs", "Silicon Laboratories",
                "STMicroelectronics", "STMicro", "Microchip", "Microchip Technology",
                "NXP", "NXP Semiconductors", "Realtek", "Realtek Semiconductor",
                "Tuya", "Tuya Smart", "Sonoff", "Shelly", "ATH",
                "Cypress", "Cypress Semiconductor", "Infineon", "Infineon Technologies",
                "Marvell", "Marvell Semiconductor", "Renesas", "Renesas Electronics",
                "Atmel", "Atmel Corporation", "AMS", "ams AG",
                "Dialog", "Dialog Semiconductor", "WCH", "Qinheng",
                "GigaDevice", "Allwinner", "Allwinner Technology",
                "Philips Hue", "Signify", "LIFX", "Yeelight",
                "IKEA", "IKEA of Sweden", "TRADFRI",
                "Bosch", "Bosch Smart Home", "Siemens", "Siemens AG",
                "Schneider Electric", "Legrand", "ABB", "Eaton",
                "Honeywell", "Honeywell International", "Johnson Controls",
                "Ring", "Ring LLC", "Amazon", "Amazon Technologies",
                "Google Nest", "Nest Labs", "Ecobee", "Ecobee Inc",
                "Arlo", "Arlo Technologies", "Eufy", "Anker",
                "Wyze", "Wyze Labs", "Blink", "Blink by Amazon",
                "August", "August Home", "Yale", "Assa Abloy",
                "Chamberlain", "MyQ", "Axis", "Axis Communications",
                "Hikvision", "Hangzhou Hikvision", "Dahua", "Zhejiang Dahua",
                "Unifi Protect", "Ubiquiti", "Reolink", "Ezviz",
                "Garmin", "Garmin International", "Fitbit", "Fitbit Inc",
                "Tile", "Tile Inc", "Particle", "Particle Industries"
        ));

        enterpriseVendors = new HashSet<>(Arrays.asList(
                "Cisco", "Cisco Systems", "Cisco Meraki", "Meraki",
                "Aruba", "Aruba Networks", "HPE", "Hewlett Packard Enterprise",
                "Juniper", "Juniper Networks", "Ruckus", "Ruckus Wireless",
                "Fortinet", "Fortinet Inc", "Palo Alto", "Palo Alto Networks",
                "Extreme", "Extreme Networks", "Alcatel", "Alcatel-Lucent",
                "Nokia", "Nokia Networks", "Ericsson", "Ericsson AB",
                "Huawei", "Huawei Enterprise", "ZTE", "zte corporation",
                "Avaya", "Avaya Inc", "Mitel", "Mitel Networks",
                "Plantronics", "Polycom", "Poly", "GN Netcom",
                "Aerohive", "Aerohive Networks", "Xirrus", "Xirrus Inc",
                "Mist", "Mist Systems", "Mojo", "Mojo Networks",
                "WatchGuard", "WatchGuard Technologies", "SonicWall", "Sonicwall",
                "Check Point", "Check Point Software", "F5", "F5 Inc",
                "Brocade", "Brocade Communications", "Arista", "Arista Networks",
                "Dell", "Dell Inc", "Dell EMC", "EMC", "EMC Corporation",
                "IBM", "IBM Corp", "Oracle", "Oracle Corporation",
                "Microsoft", "Microsoft Corporation", "VMware", "VMware Inc",
                "NetApp", "NetApp Inc", "Pure Storage", "Pure Storage Inc",
                "Hitachi", "Hitachi Ltd", "NEC", "NEC Corporation",
                "Fujitsu", "Fujitsu Limited", "Lenovo", "Lenovo",
                "HP", "Hewlett Packard", "HPE", "Hewlett Packard Enterprise",
                "Allied Telesis", "Allied Telesyn", "ZyXEL", "Zyxel Communications",
                "TRENDnet", "TRENDnet Inc", "LevelOne", "Planet", "Planet Technology",
                "SMC Networks", "SMC", "D-Link", "D-Link Corporation",
                "Netgear", "ProSafe", "MikroTik", "Mikrotik",
                "Ubiquiti", "Ubiquiti Inc", "Cambium", "Cambium Networks",
                "Radwin", "RADWIN", "Ceragon", "Ceragon Networks",
                "Siklu", "Siklu Communication", "BridgeWave", "BridgeWave Communications",
                "ADVA", "ADVA Optical", "Ciena", "Ciena Corporation",
                "Infinera", "Infinera Inc", "Coriant", "Coriant GmbH",
                "Tellabs", "Tellabs Inc", "Calix", "Calix Inc",
                "Adtran", "Adtran Inc", "Zhone", "Zhone Technologies",
                "Keymile", "Keymile GmbH", "RAD", "RAD Data Communications"
        ));
    }

    public enum NetworkType {
        HOME_ROUTER("Домашний роутер", "Обычная домашняя сеть"),
        CORPORATE_AP("Корпоративная", "Офисная точка доступа"),
        PUBLIC_HOTSPOT("Публичная", "WiFi в кафе/аэропорту"),
        MOBILE_HOTSPOT("Мобильная", "Раздача с телефона"),
        IOT_DEVICE("IoT устройство", "Умное устройство"),
        MESH_NODE("Mesh узел", "Часть mesh-системы"),
        GUEST_NETWORK("Гостевая", "Изолированная гостевая сеть"),
        HIDDEN_NETWORK("Скрытая", "Сеть со скрытым SSID"),
        SUSPICIOUS("Подозрительная", "Потенциально опасная сеть"),
        UNKNOWN("Неизвестная", "Не удалось определить");

        public final String displayName;
        public final String description;

        NetworkType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
    }

    public static class ClassificationResult {
        public NetworkType type;
        public double confidence;
        public String reason;
        public int riskLevel;
        public LocalDateTime timestamp;

        public ClassificationResult(NetworkType type, double confidence,
                                    String reason, int riskLevel) {
            this.type = type;
            this.confidence = confidence;
            this.reason = reason;
            this.riskLevel = riskLevel;
            this.timestamp = LocalDateTime.now();
        }

        public String getRiskDescription() {
            if (riskLevel <= 2) return "Безопасно";
            if (riskLevel <= 4) return "Низкий риск";
            if (riskLevel <= 6) return "Средний риск";
            if (riskLevel <= 8) return "Высокий риск";
            return "Критический риск";
        }
    }

    public ClassificationResult classify(NetworkInfo network) {
        if (network == null || network.getSsid() == null) {
            return new ClassificationResult(NetworkType.UNKNOWN, 0,
                    "Нет данных", 0);
        }

        String ssid = network.getSsid().replace("(Подключено)", "").trim();
        String security = network.getAuthentication();
        int signal = parseIntSafe(network.getSignalStrength());
        int bssidCount = network.getBssids() != null ? network.getBssids().size() : 0;
        String vendor = getVendorFromMac(network);
        int beaconPeriod = network.getBeaconPeriod();
        String version = network.getVersion();

        double rssiVariance = updateAndGetRssiVariance(ssid, signal);

        if (ssid.isEmpty() || ssid.equals("Hidden")) {
            return new ClassificationResult(
                    NetworkType.HIDDEN_NETWORK, 95,
                    "SSID скрыт. Beacon содержит пустое имя сети",
                    5
            );
        }

        if (isSuspiciousSSID(ssid)) {
            return new ClassificationResult(
                    NetworkType.SUSPICIOUS, 90,
                    "Имя сети похоже на поддельное: " + ssid,
                    9
            );
        }

        if ((security.equals("OPEN") || security.equals("Open"))
                && !isCommonPublicSSID(ssid)) {
            return new ClassificationResult(
                    NetworkType.SUSPICIOUS, 75,
                    "Открытая сеть без шифрования. Опасно!",
                    8
            );
        }

        Map<NetworkType, Integer> scores = new LinkedHashMap<>();
        for (NetworkType type : NetworkType.values()) {
            scores.put(type, 0);
        }

        if (security.contains("Enterprise") || security.contains("802.1X")) {
            scores.merge(NetworkType.CORPORATE_AP, 50, Integer::sum);
        }
        if (security.contains("WPA3")) {
            scores.merge(NetworkType.HOME_ROUTER, 20, Integer::sum);
        }
        if (security.equals("OPEN") || security.equals("Open")) {
            scores.merge(NetworkType.PUBLIC_HOTSPOT, 15, Integer::sum);
        }

        if (routerVendors.contains(vendor)) {
            scores.merge(NetworkType.HOME_ROUTER, 30, Integer::sum);
        }
        if (enterpriseVendors.contains(vendor)) {
            scores.merge(NetworkType.CORPORATE_AP, 40, Integer::sum);
        }
        if (iotChipVendors.contains(vendor)) {
            scores.merge(NetworkType.IOT_DEVICE, 35, Integer::sum);
        }
        if (mobileChipVendors.contains(vendor)) {
            scores.merge(NetworkType.MOBILE_HOTSPOT, 25, Integer::sum);
        }

        if (beaconPeriod > 500) {
            scores.merge(NetworkType.IOT_DEVICE, 30, Integer::sum);
        } else if (beaconPeriod > 200) {
            scores.merge(NetworkType.IOT_DEVICE, 15, Integer::sum);
        }

        if (version.contains("Legacy") || version.contains("802.11b")) {
            scores.merge(NetworkType.IOT_DEVICE, 25, Integer::sum);
        }
        if (version.contains("WiFi 6") || version.contains("802.11ax")) {
            scores.merge(NetworkType.HOME_ROUTER, 15, Integer::sum);
        }
        if (version.contains("WiFi 5") || version.contains("802.11ac")) {
            scores.merge(NetworkType.HOME_ROUTER, 10, Integer::sum);
        }

        if (bssidCount >= 3 && isMeshSSID(ssid)) {
            scores.merge(NetworkType.MESH_NODE, 50, Integer::sum);
        } else if (bssidCount >= 3) {
            scores.merge(NetworkType.CORPORATE_AP, 20, Integer::sum);
            scores.merge(NetworkType.MESH_NODE, 15, Integer::sum);
        }

        if (isGuestNetwork(ssid)) {
            scores.merge(NetworkType.GUEST_NETWORK, 40, Integer::sum);
        }
        if (isCommonPublicSSID(ssid)) {
            scores.merge(NetworkType.PUBLIC_HOTSPOT, 35, Integer::sum);
        }
        if (isMobileHotspotBySSID(ssid)) {
            scores.merge(NetworkType.MOBILE_HOTSPOT, 35, Integer::sum);
        }
        if (isIoTBySSID(ssid)) {
            scores.merge(NetworkType.IOT_DEVICE, 30, Integer::sum);
        }

        if (signal > -40 && bssidCount == 1) {
            scores.merge(NetworkType.HOME_ROUTER, 20, Integer::sum);
        }
        if (signal < -75) {
            scores.merge(NetworkType.MOBILE_HOTSPOT, 15, Integer::sum);
        }

        if (rssiVariance > HIGH_VARIANCE_THRESHOLD) {
            scores.merge(NetworkType.MOBILE_HOTSPOT, 30, Integer::sum);
        } else if (rssiVariance > 0 && rssiVariance <= 10) {
            scores.merge(NetworkType.HOME_ROUTER, 10, Integer::sum);
            scores.merge(NetworkType.IOT_DEVICE, 10, Integer::sum);
        }

        NetworkType bestType = NetworkType.HOME_ROUTER;
        int bestScore = 0;
        int secondBestScore = 0;

        for (Map.Entry<NetworkType, Integer> entry : scores.entrySet()) {
            if (entry.getKey() == NetworkType.HIDDEN_NETWORK
                    || entry.getKey() == NetworkType.UNKNOWN) {
                continue;
            }
            if (entry.getValue() > bestScore) {
                secondBestScore = bestScore;
                bestScore = entry.getValue();
                bestType = entry.getKey();
            } else if (entry.getValue() > secondBestScore) {
                secondBestScore = entry.getValue();
            }
        }

        int totalScore = scores.values().stream().mapToInt(Integer::intValue).sum();
        double confidence;
        if (totalScore == 0) {
            confidence = 30;
        } else {
            double scoreRatio = (double) bestScore / totalScore;
            double gapBonus = (bestScore - secondBestScore) / (double) Math.max(bestScore, 1);
            confidence = Math.min(95, (scoreRatio * 60 + gapBonus * 35));
        }

        StringBuilder reason = new StringBuilder();
        reason.append("Скоринг: ").append(bestType.displayName)
                .append(" (").append(bestScore).append(" из ").append(totalScore).append(" баллов)");

        List<String> keyFactors = new ArrayList<>();
        if (routerVendors.contains(vendor)) keyFactors.add("производитель роутеров: " + vendor);
        if (iotChipVendors.contains(vendor)) keyFactors.add("IoT-производитель: " + vendor);
        if (mobileChipVendors.contains(vendor)) keyFactors.add("мобильный чип: " + vendor);
        if (enterpriseVendors.contains(vendor)) keyFactors.add("Enterprise-вендор: " + vendor);
        if (beaconPeriod > 500) keyFactors.add("beacon " + beaconPeriod + " мс");
        if (rssiVariance > HIGH_VARIANCE_THRESHOLD)
            keyFactors.add("нестабильный сигнал (σ²=" + String.format("%.0f", rssiVariance) + ")");
        if (!keyFactors.isEmpty()) {
            reason.append(" | ").append(String.join(", ", keyFactors));
        }

        int riskLevel;
        switch (bestType) {
            case SUSPICIOUS: riskLevel = 9; break;
            case HIDDEN_NETWORK: riskLevel = 5; break;
            case PUBLIC_HOTSPOT: riskLevel = 6; break;
            case MOBILE_HOTSPOT: riskLevel = 3; break;
            case GUEST_NETWORK: riskLevel = 3; break;
            case CORPORATE_AP: riskLevel = 2; break;
            default: riskLevel = 1;
        }

        return new ClassificationResult(bestType, confidence, reason.toString(), riskLevel);
    }

    private boolean isSuspiciousSSID(String ssid) {
        String lower = ssid.toLowerCase();
        String[] suspiciousPatterns = {
                "free", "click here", "hack",
                "phishing", "virus", "trojan", "ransomware",
                "admin", "administrator", "default", "test"
        };

        for (String pattern : suspiciousPatterns) {
            if (lower.contains(pattern)) return true;
        }

        String[] knownNetworks = {"starbucks", "mcdonalds", "airport",
                "hilton", "marriott"};
        for (String known : knownNetworks) {
            if (lower.contains(known) && !lower.equals(known)) {
                return true;
            }
        }

        return false;
    }

    private boolean isCommonPublicSSID(String ssid) {
        String lower = ssid.toLowerCase();
        String[] publicPatterns = {
                "airport", "hotel",
                "library", "museum", "metro", "subway",
                "park", "mall", "cafe", "restaurant",
                "free", "public", "open"
        };

        for (String pattern : publicPatterns) {
            if (lower.contains(pattern)) return true;
        }
        return false;
    }

    private boolean isMeshSSID(String ssid) {
        String lower = ssid.toLowerCase();
        return lower.contains("mesh") ||
                lower.contains("extender") ||
                lower.contains("repeater");
    }

    private boolean isGuestNetwork(String ssid) {
        String lower = ssid.toLowerCase();
        return lower.contains("guest") ||
                lower.contains("visitor") ||
                lower.contains("welcome");
    }

    private boolean isMobileHotspotBySSID(String ssid) {
        String lower = ssid.toLowerCase();
        String[] phonePatterns = {
                "iphone", "samsung", "galaxy", "pixel", "oneplus",
                "xiaomi", "huawei", "oppo", "nokia", "motorola"
        };

        for (String pattern : phonePatterns) {
            if (lower.contains(pattern)) return true;
        }

        if (ssid.startsWith("iPhone") || ssid.startsWith("iPad")) return true;

        return false;
    }

    private boolean isIoTBySSID(String ssid) {
        String lower = ssid.toLowerCase();
        String[] iotPatterns = {
                "camera", "cam", "doorbell", "thermostat", "sensor",
                "alexa", "echo", "google home", "nest", "ring",
                "bulb", "light", "plug", "switch", "lock",
                "tv", "roku", "chromecast", "firestick", "apple tv",
                "printer", "scanner", "speaker", "soundbar",
                "hue", "lifx", "yeelight", "smart", "home"
        };

        for (String pattern : iotPatterns) {
            if (lower.contains(pattern)) return true;
        }
        return false;
    }

    private double updateAndGetRssiVariance(String ssid, int currentRssi) {
        Queue<Integer> history = rssiHistory.computeIfAbsent(ssid, k -> new LinkedList<>());

        history.add(currentRssi);
        if (history.size() > MAX_RSSI_HISTORY) {
            history.poll();
        }

        if (history.size() < 3) {
            return 0.0;
        }

        double mean = history.stream().mapToInt(Integer::intValue).average().orElse(0);
        double variance = history.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0);

        return variance;
    }


    public void cleanupRssiHistory(Set<String> activeSsids) {
        rssiHistory.keySet().removeIf(ssid -> !activeSsids.contains(ssid));
    }

    private String getVendorFromMac(NetworkInfo network) {
        if (network.getBssids() == null || network.getBssids().isEmpty()) {
            return "";
        }
        return vendorResolver.resolve(network.getBssids().get(0).bssid);
    }

    private int parseIntSafe(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 0;
        }
    }


    public Map<String, ClassificationResult> classifyAll(
            Map<String, NetworkInfo> networks) {
        Map<String, ClassificationResult> results = new LinkedHashMap<>();
        Set<String> activeSsids = new HashSet<>();

        for (Map.Entry<String, NetworkInfo> entry : networks.entrySet()) {
            String ssid = entry.getKey();
            activeSsids.add(ssid);
            results.put(ssid, classify(entry.getValue()));
        }

        cleanupRssiHistory(activeSsids);

        return results;
    }

    public Map<NetworkType, Integer> getTypeStatistics(
            Map<String, NetworkInfo> networks) {
        Map<NetworkType, Integer> stats = new LinkedHashMap<>();

        for (NetworkType type : NetworkType.values()) {
            stats.put(type, 0);
        }

        for (NetworkInfo network : networks.values()) {
            ClassificationResult result = classify(network);
            stats.merge(result.type, 1, Integer::sum);
        }

        return stats;
    }
}