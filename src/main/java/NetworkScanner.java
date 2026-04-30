
import java.util.*;

public class NetworkScanner {
    private NativeWifiScanner scanner;
    private Map<String, NetworkInfo> lastScan;
    private MacVendorResolver vendorResolver;

    public NetworkScanner() {
        this.scanner = new NativeWifiScanner();
        this.lastScan = new LinkedHashMap<>();
        this.vendorResolver = MacVendorResolver.getInstance();
    }

    public Map<String, NetworkInfo> scanNetworks() {
        Map<String, NetworkInfo> newCache = new LinkedHashMap<>();

        try {
            List<NativeWifiScanner.WifiNetwork> nets = scanner.scan();
            for (NativeWifiScanner.WifiNetwork n : nets) {
                if (n.ssid == null || n.ssid.isEmpty()) continue;

                NetworkInfo info = createNetworkInfo(n);
                newCache.put(n.ssid, info);
            }

            lastScan = newCache;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return newCache;
    }

    private NetworkInfo createNetworkInfo(NativeWifiScanner.WifiNetwork n) {
        if(n.ssid.equals(n.connectedSsid)){
            n.ssid = n.ssid + "(Подключено)";
        }
        NetworkInfo info = new NetworkInfo(n.ssid);

        info.setSignalStrength(String.valueOf(n.rssi));
        info.setChannel(String.valueOf(n.channel));
        info.setRadioType(n.phy);
        info.setBand(n.band);
        info.setAuthentication(n.security);
        info.setBssids(n.bssids);
        info.setBasicRates(String.valueOf(n.maxRate));
        info.setChannelWidth(String.valueOf(n.channelWidth));
        info.setVersion(n.wifiGeneration);
        info.setFrequency(String.valueOf(n.frequency / 1000000));
        info.setCipher(n.cipher);
        info.setConnectedSsid(n.connectedSsid);
        info.setLinkQuality(n.linkQuality);
        info.setBeaconPeriod(n.beaconPeriod);
        info.setEss(n.isEss);
        info.setIbss(n.isIbss);
        info.setPrivacyRequired(n.privacyRequired);
        info.setSupportedRates(n.supportedRates);

        if (!n.bssids.isEmpty()) {
            info.setVendor(vendorResolver.resolve(n.bssids.get(0).bssid));
        }


        return info;
    }

    public NetworkInfo getNetwork(String ssid) {
        return lastScan.get(ssid);
    }


}