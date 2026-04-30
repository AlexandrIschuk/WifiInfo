import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.stream.Collectors;

@Getter
@Setter
public class NetworkInfo {
    private String ssid;
    private List<NativeWifiScanner.WifiNetwork.Bssids> bssids = new ArrayList<>();
    private String signalStrength = "";
    private String channel = "";
    private String radioType = "";
    private String basicRates = "";
    private String version = "";
    private String channelWidth = "";
    private String authentication = "";
    private String cipher = "";
    private String frequency = "";
    private String connectedSsid = "";
    private int linkQuality;
    private int beaconPeriod;
    private boolean inRegDomain;
    private boolean isEss;
    private boolean isIbss;
    private boolean privacyRequired;
    private List<Integer> supportedRates;
    private String vendor = "";

    private String mode = "";
    private String connectionMode = "";
    private String band = "";


    public NetworkInfo(String ssid) {
        this.ssid = ssid;
    }

    public String getSupportedRatesDisplay() {
        if (supportedRates == null || supportedRates.isEmpty()) {
            return "-";
        }
        return supportedRates.stream()
                .map(r -> r + " Mbps")
                .collect(Collectors.joining(", "));
    }

    public String getDetailedInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Подробная информация о сети:\n").append(ssid).append("\n\n");
        if (!vendor.isEmpty()) {
            sb.append("Производитель: ").append(vendor).append("\n");
        }
        sb.append("Уровень сигнала: ").append(signalStrength).append(" dBm\n");
        sb.append("Тип безопасности: ").append(authentication).append("\n");
        sb.append("Канал: ").append(channel).append("(").append(frequency).append(" ГГц").append(")").append("\n");
        sb.append("Тип радио: ").append(radioType).append("\n");
        sb.append("Шифрование: ").append(cipher).append("\n");
        sb.append("Поддерживаемые скорости: ").append(getSupportedRatesDisplay()).append("\n");
        int maxRate = supportedRates.stream()
                .max(Integer::compare).orElse(0);
        sb.append(String.format("Максимальная скорость: %d Mbps\n", maxRate));
        sb.append("Версия Wi-Fi: ").append(version).append("\n");
        sb.append("Ширина канала: ").append(channelWidth).append(" Mhz\n");
        sb.append("Диапазон: ").append(band).append("\n");
        sb.append(String.format("Интервал маяка: %s мс\n", beaconPeriod * 1.024));
        sb.append(String.format("Качество канала: %d%%\n", linkQuality));
        if (isEss) {
            sb.append("Тип сети: Инфраструктурная (роутер)\n");
        } else if (isIbss) {
            sb.append("Тип сети: Ad-hoc (компьютер-компьютер)\n");
        }

        return sb.toString();
    }
    public String getBssidsInfo(String mac) {
        StringBuilder sb = new StringBuilder();
        for (NativeWifiScanner.WifiNetwork.Bssids bssid : bssids) {
            if(bssid.bssid.equals(mac)) {
                sb.append("MAC: ").append(bssid.bssid).append("\n");
                sb.append("Сигнал: ").append(bssid.rssi).append("dBm\n");
                sb.append("Безопасность: ").append(bssid.security).append("\n");
                sb.append("Канал: ").append(bssid.channel).append("(").append(bssid.frequency / 1000000).append(" ГГц)").append("\n");
                sb.append("Тип радио: ").append(bssid.phyType).append("\n");
                sb.append("Ширина канала: ").append(bssid.channelWidth).append(" MHz\n");
                sb.append("Диапазон: ").append(bssid.band).append("\n\n");
            }
        }
        return sb.toString();
    }


}