import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@Setter
public class NetworkInfo {
    private String ssid;
    private List<String> bssids = new ArrayList<>();
    private String signalStrength = "";
    private String channel = "";
    private String radioType = "";
    private String basicRates = "";
    private String version = "";
    private String channelWidth = "";
    private String authentication = "";
    private String cipher = "";
    private String frequency = "";

    private String mode = "";
    private String connectionMode = "";
    private String band = "";


    public NetworkInfo(String ssid) {
        this.ssid = ssid;
    }

    public void addBssid(List<String> bssid) {
        this.bssids = bssid;
    }



    public String getDetailedInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Подробная информация о сети: ").append(ssid).append("\n\n");
        sb.append("Уровень сигнала: ").append(signalStrength).append("%\n");
        sb.append("Тип безопасности: ").append(authentication).append("\n");
        sb.append("Канал: ").append(channel).append("(").append(frequency).append(" ГГц").append(")").append("\n");
        sb.append("Доступные точки доступа (").append(bssids.size()).append("):\n");
        for (String bssid : bssids) {
            sb.append(" - ").append(bssid).append("\n\n");
        }
        sb.append("Тип радио: ").append(radioType).append("\n");
        sb.append("Шифрование: ").append(cipher).append("\n");
        sb.append("Cкорость: ").append(basicRates).append(" Mbps\n");
        sb.append("Версия Wi-Fi: ").append(version).append("\n");
        sb.append("Ширина канала: ").append(channelWidth).append(" Mhz\n");
        sb.append("Диапазон: ").append(band).append("\n");

        return sb.toString();
    }
}