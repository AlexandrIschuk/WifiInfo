import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

public class NativeWifiScanner {

    public static class WifiNetwork {

        public String ssid;
        public String security;
        public String band;

        public ArrayList<Bssids> bssids = new ArrayList<>();

        public int rssi;
        public double frequency;
        public int channel;

        public String phy;

        public double maxRate;
        public int channelWidth;
        public String wifiGeneration;
        public String cipher;
        public String connectedSsid;

        public static class Bssids{

            public String bssid;
            public int rssi;

            public int channel;
            public double frequency;

            public int channelWidth;

            public String band;

            public String phyType;

            public String security;

        }
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
                        byte[] ieData = new byte[0];
                        if (net == null) {

                            net = new WifiNetwork();
                            net.ssid = ssid;

                            Pointer entryPtr = e.getPointer();
                            Pointer iePtr = entryPtr.share(e.ulIeOffset);

                            ieData = iePtr.getByteArray(0, e.ulIeSize);

                            net.security = SecurityParser.parseSecurity(ieData);
                            net.cipher = SecurityParser.parseCipher(ieData);

                            net.rssi = e.lRssi;
                            net.frequency = e.ulChCenterFrequency;
                            net.channelWidth = getChannelWidth(ieData);
                            net.channel = freqToChannel(e.ulChCenterFrequency);
                            //net.channel = freqToChannel(e.ulChCenterFrequency);
                            net.band = getBand(e.ulChCenterFrequency);

                            net.phy = phyToString(e.dot11BssPhyType);
                            net.maxRate = getMaxRate(e.wlanRateSet);
                            net.wifiGeneration = wifiGeneration(e.dot11BssPhyType);

                            net.connectedSsid = getConnectedSSID();


                            networks.put(ssid, net);
                        }

                        WifiNetwork.Bssids ap = new WifiNetwork.Bssids();

                        ap.bssid = String.format(
                                "%02X:%02X:%02X:%02X:%02X:%02X",
                                e.dot11Bssid[0],
                                e.dot11Bssid[1],
                                e.dot11Bssid[2],
                                e.dot11Bssid[3],
                                e.dot11Bssid[4],
                                e.dot11Bssid[5]
                        );

                        ap.rssi = e.lRssi;

                        ap.frequency = e.ulChCenterFrequency;

                        ap.channel = freqToChannel(e.ulChCenterFrequency);

                        ap.band = getBand(e.ulChCenterFrequency);

                        ap.phyType = phyToString(e.dot11BssPhyType);

                        ap.security = net.security;

                        ap.channelWidth = getChannelWidth(ieData);

                        boolean exists = false;

                        for (WifiNetwork.Bssids b : net.bssids) {
                            if (b.bssid.equals(ap.bssid)) {
                                exists = true;
                                break;
                            }
                        }

                        if (!exists) {
                            net.bssids.add(ap);
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
    private int getSecondaryChannel(int primary, byte[] ieData) {

        int i = 0;

        while (i < ieData.length - 2) {

            int id = ieData[i] & 0xFF;
            int len = ieData[i + 1] & 0xFF;

            // HT Operation (802.11n)
            if (id == 61 && len >= 2) {

                int offset = ieData[i + 2] & 0x03;

                if (offset == 1) { // above
                    return primary + 4;
                } else if (offset == 3) { // below
                    return primary - 4;
                }
            }

            i += 2 + len;
        }

        return -1;
    }

    private String buildChannelDisplay(int primary, int width, byte[] ieData) {

        if (width <= 20) {
            return String.valueOf(primary);
        }

        // Попробуем определить направление (выше или ниже)
        int secondary = getSecondaryChannel(primary, ieData);

        if (width == 40) {

            if (secondary != -1)
                return primary + " + " + secondary;

            // fallback
            return primary + " + " + (primary + 4);
        }

        if (width == 80) {
            return primary + " + " + (primary + 4) + " + " + (primary + 8) + " + " + (primary + 12);
        }

        if (width == 160) {
            return primary + " + ..."; // можно позже расширить
        }

        return String.valueOf(primary);
    }

    public String getConnectedSSID() {

        try {

            Process p = Runtime.getRuntime().exec("netsh wlan show interfaces");

            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line;

            while ((line = reader.readLine()) != null) {

                if (line.trim().startsWith("SSID")) {

                    if (!line.contains("BSSID")) {
                        return line.split(":")[1].trim();
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
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
    private int getChannelWidth(byte[] ieData) {
        int width = 20;
        int maxSupported = 20;

        int i = 0;
        while (i < ieData.length - 2) {
            int id = ieData[i] & 0xFF;
            int len = ieData[i + 1] & 0xFF;

            // HT Operation
            if (id == 61 && len >= 2) {
                int htInfo = ieData[i + 2] & 0xFF;
                int secondaryOffset = htInfo & 0x03;

                if (secondaryOffset == 1 || secondaryOffset == 2) {
                    width = 40;
                }
            }

            // VHT Operation
            if (id == 192 && len >= 3) {
                int channelWidth = ieData[i + 2] & 0xFF;

                switch (channelWidth) {
                    case 1: // 80 МГц
                        width = 80;
                        break;
                    case 2: // 160 МГц
                    case 3: // 80+80 МГц
                        width = 160;
                        break;
                    // case 0: 20/40 - оставляем текущее значение
                }
            }

            // HE Operation
            if (id == 255 && len >= 1) {
                int extId = ieData[i + 2] & 0xFF;

                if (extId == 36 && len >= 6) {
                    int heInfo = ieData[i + 3] & 0xFF;
                    int heWidth = (heInfo >> 2) & 0x03;

                    switch (heWidth) {
                        case 1: width = 40; break;
                        case 2: width = 80; break;
                        case 3: width = 160; break;
                        // case 0: 20 - оставляем
                    }
                }
            }

            i += 2 + len;
        }


        return width;
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