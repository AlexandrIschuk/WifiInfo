import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class NetworkInfo {
    private String ssid;
    private List<String> bssids = new ArrayList<>();
    private String signalStrength = "";
    private String channel = "";
    private String radioType = "";
    private String basicRates = "";
    private String otherRates = "";
    private String networkType = "";
    private String authentication = "";
    private String cipher = "";
    private String mode = "";
    private String connectionMode = "";
    private String band = "";


    public NetworkInfo(String ssid) {
        this.ssid = ssid;
    }

    public void addBssid(String bssid) {
        this.bssids.add(bssid);
    }

    public void setSignalStrength(String signalStrength) {
        this.signalStrength = signalStrength;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getSsid() {
        return ssid;
    }

    public String getSignalStrength() {
        return signalStrength;
    }

    public String getChannel() {
        return channel;
    }

    public List<String> getBssids() {
        return bssids;
    }

    public String getRadioType() {
        return radioType;
    }

    public void setRadioType(String radioType) {
        this.radioType = radioType;
    }

    public String getBasicRates() {
        return basicRates;
    }

    public void setBasicRates(String basicRates) {
        this.basicRates = basicRates;
    }

    public String getOtherRates() {
        return otherRates;
    }

    public void setOtherRates(String otherRates) {
        this.otherRates = otherRates;
    }

    public String getNetworkType() {return networkType;}

    public void setNetworkType(String networkType) {this.networkType = networkType;}

    public String getAuthentication() {
        return authentication;
    }

    public void setAuthentication(String authentication) {
        this.authentication = authentication;
    }

    public String getCipher() {
        return cipher;
    }

    public void setCipher(String cipher) {
        this.cipher = cipher;
    }

    public String getBand() {
        return band;
    }

    public void setBand(String band) {
        this.band = band;
    }


    public String getDetailedInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Подробная информация о сети: ").append(ssid).append("\n\n");
        sb.append("Уровень сигнала: ").append(signalStrength).append("%\n");
        sb.append("Тип безопасности: ").append(authentication).append("\n");
        sb.append("Канал: ").append(channel).append("\n");
        sb.append("Доступные точки доступа (").append(bssids.size()).append("):\n");
        for (String bssid : bssids) {
            sb.append(" - ").append(bssid).append("\n\n");
        }
        sb.append("Тип радио: ").append(radioType).append("\n");
        sb.append("Шифрование: ").append(cipher).append("\n");
        sb.append("Базовая скорость: ").append(basicRates).append("\n");
        sb.append("Другие скорости: ").append(otherRates).append("\n");
        sb.append("Тип сети: ").append(networkType).append("\n");
        sb.append("Диапазон: ").append(band).append("\n");

        return sb.toString();
    }
}