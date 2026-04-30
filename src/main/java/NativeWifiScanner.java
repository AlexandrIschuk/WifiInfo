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
        public int linkQuality;
        public int beaconPeriod;
        public boolean isEss;
        public boolean isIbss;
        public boolean privacyRequired;
        public List<Integer> supportedRates;

        public static class Bssids{

            public String bssid;
            public int rssi;

            public int channel;
            public double frequency;

            public int channelWidth;

            public String band;

            public String phyType;

            public String security;
            public int linkQuality;
            public int beaconPeriod;
            public boolean isEss;
            public boolean isIbss;
            public boolean privacyRequired;
            public List<Integer> supportedRates;

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
                int scanResult = WifiScanner.WlanApi.INSTANCE.WlanScan(
                        handle,
                        iface.InterfaceGuid,
                        null,
                        null,
                        null
                );

                if (scanResult != 0) {
                    System.err.println("WlanScan failed with error: " + scanResult);
                } else {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }

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
                            net.channelWidth = getChannelWidth(ieData, e.ulChCenterFrequency);
                            net.channel = freqToChannel(e.ulChCenterFrequency);
                            net.band = getBand(e.ulChCenterFrequency);

                            net.phy = phyToString(ieData, e.dot11BssPhyType, e.ulChCenterFrequency);
                            net.maxRate = getMaxRate(e.wlanRateSet);
                            net.wifiGeneration = wifiGeneration(ieData, e.dot11BssPhyType);
                            net.connectedSsid = getConnectedSSID();
                            net.linkQuality = e.ulLinkQuality;
                            net.beaconPeriod = e.usBeaconPeriod;

                            net.isEss = (e.usCapabilityInformation & 0x0001) != 0;
                            net.isIbss = (e.usCapabilityInformation & 0x0002) != 0;
                            net.privacyRequired = (e.usCapabilityInformation & 0x0010) != 0;

                            net.supportedRates = new ArrayList<>();
                            for (int i = 0; i < e.wlanRateSet.uRateSetLength; i++) {
                                int rate = e.wlanRateSet.usRateSet[i] & 0x7FFF;
                                net.supportedRates.add(rate / 2);
                            }


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

                        ap.phyType = phyToString(ieData, e.dot11BssPhyType, e.ulChCenterFrequency);

                        ap.security = net.security;

                        ap.channelWidth = getChannelWidth(ieData, e.ulChCenterFrequency);
                        ap.linkQuality = e.ulLinkQuality;
                        ap.beaconPeriod = e.usBeaconPeriod;
                        ap.isEss = (e.usCapabilityInformation & 0x0001) != 0;
                        ap.isIbss = (e.usCapabilityInformation & 0x0002) != 0;
                        ap.privacyRequired = (e.usCapabilityInformation & 0x0010) != 0;
                        ap.supportedRates = new ArrayList<>(net.supportedRates);

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
                            net.phy = phyToString(ieData, e.dot11BssPhyType, e.ulChCenterFrequency);
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


    private String phyToString(byte[] ieData, int dot11BssPhyType, int frequencyKHz) {
        int mhz = frequencyKHz / 1000;
        boolean is2GHz = (mhz >= 2400 && mhz <= 2500);
        boolean is5GHz = (mhz >= 4900 && mhz <= 5900);
        boolean is6GHz = (mhz >= 5925 && mhz <= 7125);

        boolean hasHE = false;
        boolean hasVHT = false;
        boolean hasHT = false;
        boolean hasERP = false;
        boolean hasDSSS = false;

        int i = 0;
        while (i < ieData.length - 2) {
            int id = ieData[i] & 0xFF;
            int len = ieData[i + 1] & 0xFF;

            if (i + 2 + len > ieData.length) break;

            if (id == 255 && len > 0) {
                int extId = ieData[i + 2] & 0xFF;
                if (extId == 35) hasHE = true;
            }
            if (id == 191) hasVHT = true;
            if (id == 45) hasHT = true;
            if (id == 42) hasERP = true;
            if (id == 3) hasDSSS = true;

            i += 2 + len;
        }

        if (hasHE) {
            if (is6GHz) return "802.11ax (6 GHz)";
            if (is5GHz) return "802.11ax (5 GHz)";
            if (is2GHz) return "802.11ax (2.4 GHz)";
        }

        if (hasVHT) {
            return "802.11ac (5 GHz)";
        }

        if (hasHT) {
            if (is5GHz) return "802.11n (5 GHz)";
            if (is2GHz) return "802.11n (2.4 GHz)";
        }

        if (is5GHz && hasERP) return "802.11a";
        if (is2GHz && hasERP) return "802.11g";
        if (is2GHz && hasDSSS) return "802.11b";

        if (is5GHz) return "802.11a";
        if (is2GHz) return "802.11g";

        return "unknown";
    }

    private int getChannelWidth(byte[] ieData, int frequencyKHz) {
        int width = 20;
        int mhz = frequencyKHz / 1000;
        boolean is2GHz = (mhz >= 2400 && mhz <= 2500);

        int i = 0;
        while (i < ieData.length - 2) {
            int id = ieData[i] & 0xFF;
            int len = ieData[i + 1] & 0xFF;

            if (i + 2 + len > ieData.length) break;

            if (id == 61 && len >= 2) {
                int htInfo1 = ieData[i + 2] & 0xFF;
                int htInfo2 = ieData[i + 3] & 0xFF;

                int secondaryOffset = htInfo2 & 0x03;

                if (secondaryOffset == 1 || secondaryOffset == 3) {
                    width = 40;
                } else {
                    width = 20;
                }


                boolean supports40MHz = ((htInfo1 >> 2) & 0x01) == 1;
            }

            if (id == 192 && len >= 3) {
                int vhtOpInfoChWidth = ieData[i + 2] & 0xFF;
                int vhtOpInfoSeg0 = ieData[i + 3] & 0xFF;
                int vhtOpInfoSeg1 = ieData[i + 4] & 0xFF;

                switch (vhtOpInfoChWidth) {
                    case 0:
                        break;
                    case 1:
                        width = 80;
                        break;
                    case 2:
                        width = 160;
                        break;
                    case 3:
                        width = 160;
                        break;
                }
            }

            if (id == 255 && len >= 1) {
                int extId = ieData[i + 2] & 0xFF;

                if (extId == 36 && len >= 6) {
                    int heOpParams0 = ieData[i + 3] & 0xFF;
                    int heOpParams1 = ieData[i + 4] & 0xFF;
                    int heOpParams2 = ieData[i + 5] & 0xFF;

                    int chWidth = (heOpParams1 >> 2) & 0x03;

                    switch (chWidth) {
                        case 0:
                            width = 20;
                            break;
                        case 1:
                            width = 40;
                            break;
                        case 2:
                            width = 80;
                            break;
                        case 3:
                            width = 160;
                            break;
                    }
                }
            }

            i += 2 + len;
        }

        if (is2GHz && width > 40) {
            width = 40;
        }

        return width;
    }
    private String wifiGeneration(byte[] ieData, int dot11BssPhyType) {
        boolean hasHE = false;
        boolean hasVHT = false;
        boolean hasHT = false;

        int i = 0;
        while (i < ieData.length - 2) {
            int id = ieData[i] & 0xFF;
            int len = ieData[i + 1] & 0xFF;

            if (i + 2 + len > ieData.length) break;

            if (id == 255 && len > 0) {
                int extId = ieData[i + 2] & 0xFF;
                if (extId == 35) {
                    hasHE = true;
                }
            }

            if (id == 191) {
                hasVHT = true;
            }

            if (id == 45) {
                hasHT = true;
            }

            i += 2 + len;
        }

        if (hasHE) return "WiFi 6 (802.11ax)";
        if (hasVHT) return "WiFi 5 (802.11ac)";
        if (hasHT) return "WiFi 4 (802.11n)";

        switch (dot11BssPhyType) {
            case 4: return "WiFi 4 (802.11n)";
            case 7: return "WiFi 5 (802.11ac)";
            case 8: return "WiFi 6 (802.11ax)";
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