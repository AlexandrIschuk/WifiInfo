import java.time.LocalDateTime;
import java.util.*;

public class NetworkClassifier {
    private MacVendorResolver vendorResolver;
    public NetworkClassifier() {
        this.vendorResolver = MacVendorResolver.getInstance();
    }

    public enum NetworkType {
        HOME_ROUTER("Домашний роутер", "Обычная домашняя сеть"),
        CORPORATE_AP("Корпоративная", "Офисная точка доступа"),
        PUBLIC_HOTSPOT("Публичная", "WiFi в кафе/аэропорту"),
        MOBILE_HOTSPOT("Мобильная", "Раздача с телефона"),
        IOT_DEVICE("IoT устройство", "Умное устройство"),
        MESH_NODE("🕸Mesh узел", "Часть mesh-системы"),
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
        String channel = network.getChannel();
        int channelNum = parseIntSafe(channel);
        int signal = parseIntSafe(network.getSignalStrength());
        int bssidCount = network.getBssids() != null ? network.getBssids().size() : 0;
        String vendor = getVendorFromMac(network);
        int beaconPeriod = network.getBeaconPeriod();
        String version = network.getVersion();


        if (ssid.isEmpty() || ssid.equals("Hidden")) {
            return new ClassificationResult(
                    NetworkType.HIDDEN_NETWORK, 95,
                    "SSID скрыт. Beacon содержит пустое имя сети",
                    5
            );
        }


        if (isSuspiciousSSID(ssid)) {
            return new ClassificationResult(
                    NetworkType.SUSPICIOUS, 85,
                    "Имя сети похоже на поддельное: " + ssid,
                    9
            );
        }

        if (security.equals("OPEN") || security.equals("Open")) {
            if (isCommonPublicSSID(ssid)) {
                return new ClassificationResult(
                        NetworkType.PUBLIC_HOTSPOT, 80,
                        "Открытая публичная сеть: " + ssid,
                        6
                );
            } else {
                return new ClassificationResult(
                        NetworkType.SUSPICIOUS, 70,
                        "Открытая сеть без шифрования. Опасно!",
                        8
                );
            }
        }

        if (security.contains("Enterprise") || security.contains("802.1X")) {
            return new ClassificationResult(
                    NetworkType.CORPORATE_AP, 90,
                    "Использует корпоративную аутентификацию (802.1X)",
                    2
            );
        }

        if (bssidCount >= 3 && isMeshSSID(ssid)) {
            return new ClassificationResult(
                    NetworkType.MESH_NODE, 85,
                    "Обнаружено " + bssidCount + " точек доступа. Mesh-система",
                    1
            );
        }

        if (isGuestNetwork(ssid)) {
            return new ClassificationResult(
                    NetworkType.GUEST_NETWORK, 90,
                    "Имя сети указывает на гостевую (Guest)",
                    3
            );
        }

        if (isMobileHotspot(ssid, channelNum, signal, vendor)) {
            return new ClassificationResult(
                    NetworkType.MOBILE_HOTSPOT, 75,
                    "Признаки мобильной точки доступа: " +
                            (vendor.isEmpty() ? "характерное имя" : "производитель " + vendor),
                    3
            );
        }

        if (isIoTDevice(ssid, beaconPeriod, version)) {
            return new ClassificationResult(
                    NetworkType.IOT_DEVICE, 80,
                    "Характеристики IoT устройства: " +
                            (beaconPeriod > 200 ? "длинный beacon период" : "старое поколение WiFi"),
                    1
            );
        }

        if (isKnownRouterVendor(vendor) && security.contains("WPA")) {
            return new ClassificationResult(
                    NetworkType.HOME_ROUTER, 75,
                    "Производитель роутера: " + vendor + ", защита WPA",
                    1
            );
        }

        if (signal > -40 && bssidCount == 1) {
            return new ClassificationResult(
                    NetworkType.HOME_ROUTER, 60,
                    "Очень сильный сигнал, одна точка доступа. Вероятно домашний роутер",
                    1
            );
        }

        return new ClassificationResult(
                NetworkType.HOME_ROUTER, 40,
                "Стандартная WiFi сеть с защитой",
                2
        );
    }


    private boolean isSuspiciousSSID(String ssid) {
        String lower = ssid.toLowerCase();
        String[] suspiciousPatterns = {
                "free wifi", "free internet", "click here", "hack",
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
                "starbucks", "mcdonalds", "airport", "hotel",
                "library", "museum", "metro", "subway",
                "park", "mall", "cafe", "restaurant",
                "free", "public", "open", "guest"
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

    private boolean isMobileHotspot(String ssid, int channel, int signal, String vendor) {
        String lower = ssid.toLowerCase();

        String[] phonePatterns = {
                "iphone", "samsung", "galaxy", "pixel", "oneplus",
                "xiaomi", "huawei", "oppo", "nokia", "motorola"
        };

        for (String pattern : phonePatterns) {
            if (lower.contains(pattern)) return true;
        }

        if (ssid.startsWith("iPhone") || ssid.startsWith("iPad")) return true;

        if (signal < -70) return true;

        if (vendor.contains("Qualcomm") || vendor.contains("Apple")
                || vendor.contains("Samsung") || vendor.contains("Xiaomi")
                || vendor.contains("OnePlus") || vendor.contains("Google")) return true;

        return false;
    }

    private boolean isIoTDevice(String ssid, int beaconPeriod, String version) {
        String lower = ssid.toLowerCase();

        String[] iotPatterns = {
                "camera", "cam", "doorbell", "thermostat", "sensor",
                "alexa", "echo", "google home", "nest", "ring",
                "bulb", "light", "plug", "switch", "lock",
                "tv", "roku", "chromecast", "firestick", "apple tv",
                "printer", "scanner", "speaker", "soundbar"
        };

        for (String pattern : iotPatterns) {
            if (lower.contains(pattern)) return true;
        }

        if (beaconPeriod > 500) return true;

        if (version.contains("Legacy") || version.contains("802.11b")) return true;

        return false;
    }


    private String getVendorFromMac(NetworkInfo network) {
        if (network.getBssids() == null || network.getBssids().isEmpty()) {
            return "";
        }
        return vendorResolver.resolve(network.getBssids().get(0).bssid);
    }

    private boolean isKnownRouterVendor(String vendor) {
        String[] routerVendors = {
                "Cisco", "Netgear", "D-Link", "TP-Link", "Asus",
                "Linksys", "Ubiquiti", "Aruba", "Belkin", "MikroTik"
        };

        for (String rv : routerVendors) {
            if (vendor.contains(rv)) return true;
        }
        return false;
    }

    private int parseIntSafe(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Классифицирует все сети в списке
     */
    public Map<String, ClassificationResult> classifyAll(
            Map<String, NetworkInfo> networks) {
        Map<String, ClassificationResult> results = new LinkedHashMap<>();

        for (Map.Entry<String, NetworkInfo> entry : networks.entrySet()) {
            results.put(entry.getKey(), classify(entry.getValue()));
        }

        return results;
    }

    /**
     * Получает статистику по типам сетей
     */
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

    /**
     * Находит подозрительные сети
     */
    public List<Map.Entry<String, ClassificationResult>> findSuspicious(
            Map<String, NetworkInfo> networks) {
        Map<String, ClassificationResult> results = classifyAll(networks);

        return results.entrySet().stream()
                .filter(e -> e.getValue().riskLevel >= 7)
                .sorted((a, b) -> Integer.compare(b.getValue().riskLevel,
                        a.getValue().riskLevel))
                .collect(java.util.stream.Collectors.toList());
    }
}