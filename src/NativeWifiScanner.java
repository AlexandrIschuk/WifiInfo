import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import java.util.*;

public class NativeWifiScanner {

    public static class WifiNetwork {

        public String ssid;
        public String security;
        public String band;

        public ArrayList<String> bssids = new ArrayList<>();

        public int rssi;
        public double frequency;
        public int channel;

        public String phy;

        public double maxRate;
        public int channelWidth;
        public String wifiGeneration;
        public String cipher;
    }

    public List<WifiNetwork> scan() throws Exception {

        Map<String, WifiNetwork> networks = new LinkedHashMap<>();

        PointerByReference handleRef = new PointerByReference();
        IntByReference version = new IntByReference();

        WifiScanner.WlanApi.INSTANCE.WlanOpenHandle(
                2,
                null,
                version,
                handleRef
        );

        Pointer handle = handleRef.getValue();

        try {

            PointerByReference ifaceRef = new PointerByReference();

            WifiScanner.WlanApi.INSTANCE.WlanEnumInterfaces(
                    handle,
                    null,
                    ifaceRef
            );

            WifiScanner.WLAN_INTERFACE_INFO_LIST list =
                    new WifiScanner.WLAN_INTERFACE_INFO_LIST(ifaceRef.getValue());

            for (WifiScanner.WLAN_INTERFACE_INFO iface : list.getInterfaces()) {

                PointerByReference bssRef = new PointerByReference();

                WifiScanner.WlanApi.INSTANCE.WlanGetNetworkBssList(
                        handle,
                        iface.InterfaceGuid,
                        null,
                        3,
                        false,
                        null,
                        bssRef
                );

                try {

                    WifiScanner.WLAN_BSS_LIST bssList =
                            new WifiScanner.WLAN_BSS_LIST(bssRef.getValue());

                    for (WifiScanner.WLAN_BSS_ENTRY e : bssList.getEntries()) {

                        String ssid = e.dot11Ssid.getSSID();

                        if (ssid == null || ssid.isEmpty())
                            continue;

                        WifiNetwork net = networks.get(ssid);

                        if (net == null) {

                            net = new WifiNetwork();
                            net.ssid = ssid;

                            Pointer entryPtr = e.getPointer();
                            Pointer iePtr = entryPtr.share(e.ulIeOffset);

                            byte[] ieData = iePtr.getByteArray(0, e.ulIeSize);

                            net.security = SecurityParser.parseSecurity(ieData);
                            net.cipher = SecurityParser.parseCipher(ieData);

                            net.rssi = e.lRssi;
                            net.frequency = e.ulChCenterFrequency;

                            net.channel = freqToChannel(e.ulChCenterFrequency);
                            net.band = getBand(e.ulChCenterFrequency);

                            net.phy = phyToString(e.dot11BssPhyType);
                            net.maxRate = getMaxRate(e.wlanRateSet);
                            net.wifiGeneration = wifiGeneration(e.dot11BssPhyType);
                            net.channelWidth = getChannelWidth(ieData);

                            networks.put(ssid, net);
                        }

                        String bssid = String.format(
                                "%02X:%02X:%02X:%02X:%02X:%02X",
                                e.dot11Bssid[0],
                                e.dot11Bssid[1],
                                e.dot11Bssid[2],
                                e.dot11Bssid[3],
                                e.dot11Bssid[4],
                                e.dot11Bssid[5]
                        );

                        if (!net.bssids.contains(bssid)) {
                            net.bssids.add(bssid);
                        }

                        if (e.lRssi > net.rssi) {
                            net.rssi = e.lRssi;
                            net.channel = freqToChannel(e.ulChCenterFrequency);
                            net.frequency = e.ulChCenterFrequency;
                            net.band = getBand(e.ulChCenterFrequency);
                            net.phy = phyToString(e.dot11BssPhyType);
                        }
                    }

                } finally {

                    WifiScanner.WlanApi.INSTANCE.WlanFreeMemory(bssRef.getValue());
                }
            }

            WifiScanner.WlanApi.INSTANCE.WlanFreeMemory(ifaceRef.getValue());

        } finally {

            WifiScanner.WlanApi.INSTANCE.WlanCloseHandle(handle, null);
        }

        return new ArrayList<>(networks.values());
    }

    private double getMaxRate(WifiScanner.WLAN_RATE_SET rateSet) {

        int max = 0;

        for (int i = 0; i < rateSet.usRateSet.length; i++) {

            int rate = rateSet.usRateSet[i] & 0x7FFF;

            if(rate > max)
                max = rate;
        }

        return max * 0.5;
    }

    private int freqToChannel(int freq) {

        int mhz = freq / 1000;

        if (mhz >= 2412 && mhz <= 2472)
            return (mhz - 2407) / 5;

        if (mhz == 2484)
            return 14;

        if (mhz >= 5000)
            return (mhz - 5000) / 5;

        return 0;
    }

    private String phyToString(int phy) {

        switch (phy) {

            case 0: return "802.11a";
            case 1: return "802.11b";
            case 2: return "802.11g";
            case 4: return "802.11n";
            case 7: return "802.11ac";
            case 8: return "802.11ax";

            default: return "unknown";
        }
    }
    private int getChannelWidth(byte[] ie) {

        for(int i=0;i<ie.length-2;i++) {

            int id = ie[i] & 0xFF;
            int len = ie[i+1] & 0xFF;

            if(id == 192) { // VHT Operation

                int width = ie[i+2] & 0xFF;

                if(width == 0) return 20;
                if(width == 1) return 80;
                if(width == 2) return 160;
            }

            if(id == 61) { // HT Operation

                int htInfo = ie[i+2] & 0xFF;

                if((htInfo & 0x04) != 0)
                    return 40;
                else
                    return 20;
            }

            i += len + 1;
        }

        return 20;
    }
    private String wifiGeneration(int phy) {

        switch (phy) {

            case 4: return "WiFi 4 (802.11n)";
            case 7: return "WiFi 5 (802.11ac)";
            case 8: return "WiFi 6 (802.11ax)";
            case 9: return "WiFi 7 (802.11be)";

            default: return "Legacy";
        }
    }

    private String getBand(int freqKHz) {

        int mhz = freqKHz / 1000;

        if (mhz >= 2400 && mhz <= 2500)
            return "2.4 GHz";

        if (mhz >= 4900 && mhz <= 5900)
            return "5 GHz";

        if (mhz >= 5925 && mhz <= 7125)
            return "6 GHz";

        return "Unknown";
    }
}