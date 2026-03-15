import com.sun.jna.*;
import com.sun.jna.ptr.*;
import java.util.*;

public class WifiScanner {

    public interface WlanApi extends Library {

        WlanApi INSTANCE = Native.load("wlanapi", WlanApi.class);

        int WlanOpenHandle(
                int dwClientVersion,
                Pointer pReserved,
                IntByReference pdwNegotiatedVersion,
                PointerByReference phClientHandle
        );
        int WlanCloseHandle(
                Pointer hClientHandle,
                Pointer pReserved
        );

        int WlanEnumInterfaces(
                Pointer hClientHandle,
                Pointer pReserved,
                PointerByReference ppInterfaceList
        );

        int WlanGetNetworkBssList(
                Pointer hClientHandle,
                GUID pInterfaceGuid,
                Pointer pDot11Ssid,
                int dot11BssType,
                boolean securityEnabled,
                Pointer pReserved,
                PointerByReference ppWlanBssList
        );

        int WlanGetAvailableNetworkList(
                Pointer hClientHandle,
                GUID pInterfaceGuid,
                int dwFlags,
                Pointer pReserved,
                PointerByReference ppAvailableNetworkList
        );

        void WlanFreeMemory(Pointer pMemory);
    }

    public static class GUID extends Structure {

        public int Data1;
        public short Data2;
        public short Data3;
        public byte[] Data4 = new byte[8];

        protected List<String> getFieldOrder() {
            return Arrays.asList("Data1","Data2","Data3","Data4");
        }
    }

    public static class DOT11_SSID extends Structure {

        public int uSSIDLength;
        public byte[] ucSSID = new byte[32];

        protected List<String> getFieldOrder() {
            return Arrays.asList("uSSIDLength","ucSSID");
        }

        public String getSSID() {

            int len = Math.min(uSSIDLength,32);

            if(len <= 0) return "";

            return new String(ucSSID,0,len);
        }
    }

    public static class WLAN_RATE_SET extends Structure {

        public int uRateSetLength;
        public short[] usRateSet = new short[126];

        protected List<String> getFieldOrder() {
            return Arrays.asList("uRateSetLength","usRateSet");
        }
    }

    public static class WLAN_BSS_ENTRY extends Structure {

        public DOT11_SSID dot11Ssid;

        public int uPhyId;

        public byte[] dot11Bssid = new byte[6];

        public int dot11BssType;
        public int dot11BssPhyType;

        public int lRssi;

        public int ulLinkQuality;

        public boolean bInRegDomain;

        public short usBeaconPeriod;

        public long ullTimestamp;
        public long ullHostTimestamp;

        public short usCapabilityInformation;

        public int ulChCenterFrequency;

        public WLAN_RATE_SET wlanRateSet;

        public int ulIeOffset;
        public int ulIeSize;

        protected List<String> getFieldOrder() {

            return Arrays.asList(
                    "dot11Ssid",
                    "uPhyId",
                    "dot11Bssid",
                    "dot11BssType",
                    "dot11BssPhyType",
                    "lRssi",
                    "ulLinkQuality",
                    "bInRegDomain",
                    "usBeaconPeriod",
                    "ullTimestamp",
                    "ullHostTimestamp",
                    "usCapabilityInformation",
                    "ulChCenterFrequency",
                    "wlanRateSet",
                    "ulIeOffset",
                    "ulIeSize"
            );
        }
    }

    public static class WLAN_BSS_LIST extends Structure {

        public int dwTotalSize;
        public int dwNumberOfItems;
        public WLAN_BSS_ENTRY wlanBssEntries;

        public WLAN_BSS_LIST() {}

        public WLAN_BSS_LIST(Pointer p) {
            super(p);
            read();
        }

        protected List<String> getFieldOrder() {
            return Arrays.asList(
                    "dwTotalSize",
                    "dwNumberOfItems",
                    "wlanBssEntries"
            );
        }

        public WLAN_BSS_ENTRY[] getEntries() {

            if(dwNumberOfItems <= 0)
                return new WLAN_BSS_ENTRY[0];

            return (WLAN_BSS_ENTRY[])
                    wlanBssEntries.toArray(dwNumberOfItems);
        }
    }

    public static class WLAN_INTERFACE_INFO extends Structure {

        public GUID InterfaceGuid;
        public char[] strInterfaceDescription = new char[256];
        public int isState;

        protected List<String> getFieldOrder() {
            return Arrays.asList(
                    "InterfaceGuid",
                    "strInterfaceDescription",
                    "isState"
            );
        }
    }

    public static class WLAN_INTERFACE_INFO_LIST extends Structure {

        public int dwNumberOfItems;
        public int dwIndex;
        public WLAN_INTERFACE_INFO InterfaceInfo;

        public WLAN_INTERFACE_INFO_LIST() {}

        public WLAN_INTERFACE_INFO_LIST(Pointer p) {
            super(p);
            read();
        }

        protected List<String> getFieldOrder() {
            return Arrays.asList(
                    "dwNumberOfItems",
                    "dwIndex",
                    "InterfaceInfo"
            );
        }

        public WLAN_INTERFACE_INFO[] getInterfaces() {

            if(dwNumberOfItems <= 0)
                return new WLAN_INTERFACE_INFO[0];

            return (WLAN_INTERFACE_INFO[])
                    InterfaceInfo.toArray(dwNumberOfItems);
        }
    }
    public static class WLAN_AVAILABLE_NETWORK extends Structure {

        public char[] strProfileName = new char[256];

        public DOT11_SSID dot11Ssid;

        public int dot11BssType;

        public int uNumberOfBssids;

        public boolean bNetworkConnectable;

        public int wlanNotConnectableReason;

        public int uNumberOfPhyTypes;

        public int[] dot11PhyTypes = new int[8];

        public boolean bMorePhyTypes;

        public int wlanSignalQuality;

        public boolean bSecurityEnabled;

        public int dot11DefaultAuthAlgorithm;

        public int dot11DefaultCipherAlgorithm;

        public int dwFlags;

        public int dwReserved;

        @Override
        protected List<String> getFieldOrder() {

            return Arrays.asList(
                    "strProfileName",
                    "dot11Ssid",
                    "dot11BssType",
                    "uNumberOfBssids",
                    "bNetworkConnectable",
                    "wlanNotConnectableReason",
                    "uNumberOfPhyTypes",
                    "dot11PhyTypes",
                    "bMorePhyTypes",
                    "wlanSignalQuality",
                    "bSecurityEnabled",
                    "dot11DefaultAuthAlgorithm",
                    "dot11DefaultCipherAlgorithm",
                    "dwFlags",
                    "dwReserved"
            );
        }
    }
    public static class WLAN_AVAILABLE_NETWORK_LIST extends Structure {

        public int dwNumberOfItems;
        public int dwIndex;

        public WLAN_AVAILABLE_NETWORK Network;

        public WLAN_AVAILABLE_NETWORK_LIST() {}

        public WLAN_AVAILABLE_NETWORK_LIST(Pointer p) {
            super(p);
            read();
        }

        @Override
        protected List<String> getFieldOrder() {

            return Arrays.asList(
                    "dwNumberOfItems",
                    "dwIndex",
                    "Network"
            );
        }

        public WLAN_AVAILABLE_NETWORK[] getNetworks() {

            if (dwNumberOfItems <= 0)
                return new WLAN_AVAILABLE_NETWORK[0];

            return (WLAN_AVAILABLE_NETWORK[])
                    Network.toArray(dwNumberOfItems);
        }
    }
}