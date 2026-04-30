import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Guid;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.Memory;
import com.sun.jna.Native;

public class WifiConnector {




    private static String getWpa3TransitionProfile(String ssid, String password) {
        return "<?xml version=\"1.0\"?>\n" +
                "<WLANProfile xmlns=\"http://www.microsoft.com/networking/WLAN/profile/v1\">\n" +
                "    <name>" + ssid + "</name>\n" +
                "    <SSIDConfig>\n" +
                "        <SSID>\n" +
                "            <name>" + ssid + "</name>\n" +
                "        </SSID>\n" +
                "    </SSIDConfig>\n" +
                "    <connectionType>ESS</connectionType>\n" +
                "    <connectionMode>manual</connectionMode>\n" +
                "    <MSM>\n" +
                "        <security>\n" +
                "            <authEncryption>\n" +
                "                <authentication>WPA3SAE</authentication>\n" +
                "                <encryption>AES</encryption>\n" +
                "                <useOneX>false</useOneX>\n" +
                "                <transitionMode xmlns=\"http://www.microsoft.com/networking/WLAN/profile/v4\">true</transitionMode>\n" +
                "            </authEncryption>\n" +
                "            <sharedKey>\n" +
                "                <keyType>passPhrase</keyType>\n" +
                "                <protected>false</protected>\n" +
                "                <keyMaterial>" + password + "</keyMaterial>\n" +
                "            </sharedKey>\n" +
                "        </security>\n" +
                "    </MSM>\n" +
                "</WLANProfile>";
    }

    private static String getOpenProfile(String ssid, String password) {
        return  "<?xml version=\"1.0\"?>\n" +
                "<WLANProfile xmlns=\"http://www.microsoft.com/networking/WLAN/profile/v1\">\n" +
                "    <name>"+ ssid +"</name>\n" +
                "    <SSIDConfig>\n" +
                "        <SSID>\n" +
                "            <name>"+ ssid +"</name>\n" +
                "        </SSID>\n" +
                "    </SSIDConfig>\n" +
                "    <connectionType>ESS</connectionType>\n" +
                "    <connectionMode>manual</connectionMode>\n" +
                "    <MSM>\n" +
                "        <security>\n" +
                "            <authEncryption>\n" +
                "                <authentication>open</authentication>\n" +
                "                <encryption>none</encryption>\n" +
                "                <useOneX>false</useOneX>\n" +
                "            </authEncryption>\n" +
                "        </security>\n" +
                "    </MSM>\n" +
                "</WLANProfile>";
    }

    public static int connectToWifi(String ssid, String password, String security) {
        Pointer handle = null;
        PointerByReference handleRef = new PointerByReference();
        IntByReference negotiatedVersion = new IntByReference();

        int result = WifiScanner.WlanApi.INSTANCE.WlanOpenHandle(
                2, null, negotiatedVersion, handleRef
        );
        if (result != 0) return result;
        handle = handleRef.getValue();

        try {
            WifiScanner.GUID.ByReference interfaceGuid = getFirstWifiInterfaceGuid(handle);
            if (interfaceGuid == null) return -1;

            String profileXml;
            if(security == "OPEN"){
                profileXml = getOpenProfile(ssid,null);
            }else{
                profileXml = getWpa3TransitionProfile(ssid, password);

            }


            int charCount = profileXml.length() + 1;
            Pointer profileMemory = new Memory(charCount * Native.WCHAR_SIZE);
            profileMemory.setWideString(0, profileXml);

            WifiScanner.WLAN_CONNECTION_PARAMETERS params = new WifiScanner.WLAN_CONNECTION_PARAMETERS();
            params.wlanConnectionMode = WifiScanner.WLAN_CONNECTION_MODE_TEMPORARY_PROFILE;
            params.strProfile = profileMemory;
            params.pDot11Ssid = null;
            params.pDesiredBssidList = null;
            params.dot11BssType = WifiScanner.DOT11_BSS_TYPE_INFRASTRUCTURE;
            params.dwFlags = 0;

            result = WifiScanner.WlanApi.INSTANCE.WlanConnect(
                    handle,
                    interfaceGuid,
                    params,
                    Pointer.NULL
            );

            return result;
        } finally {
            if (handle != null) {
                WifiScanner.WlanApi.INSTANCE.WlanCloseHandle(handle, null);
            }
        }
    }


    public static int disconnect() {
        Pointer handle = null;
        PointerByReference handleRef = new PointerByReference();
        IntByReference negotiatedVersion = new IntByReference();

        int result = WifiScanner.WlanApi.INSTANCE.WlanOpenHandle(
                2, null, negotiatedVersion, handleRef
        );
        if (result != 0) return result;
        handle = handleRef.getValue();

        try {
            WifiScanner.GUID.ByReference ifaceGuid = getFirstWifiInterfaceGuid(handle);
            if (ifaceGuid == null) return -1;

            System.out.println("Отключение от текущей сети...");
            return WifiScanner.WlanApi.INSTANCE.WlanDisconnect(
                    handle, ifaceGuid, null
            );
        } finally {
            if (handle != null) {
                WifiScanner.WlanApi.INSTANCE.WlanCloseHandle(handle, null);
            }
        }
    }

    private static WifiScanner.GUID.ByReference getFirstWifiInterfaceGuid(Pointer handle) {
        PointerByReference ifaceListRef = new PointerByReference();
        int result = WifiScanner.WlanApi.INSTANCE.WlanEnumInterfaces(
                handle, null, ifaceListRef
        );
        if (result != 0) return null;

        WifiScanner.WLAN_INTERFACE_INFO_LIST ifaceList =
                new WifiScanner.WLAN_INTERFACE_INFO_LIST(ifaceListRef.getValue());
        WifiScanner.WLAN_INTERFACE_INFO[] interfaces = ifaceList.getInterfaces();

        if (interfaces.length == 0) return null;

        Guid.GUID.ByReference guidRef = new Guid.GUID.ByReference();
        guidRef.Data1 = interfaces[0].InterfaceGuid.Data1;
        guidRef.Data2 = interfaces[0].InterfaceGuid.Data2;
        guidRef.Data3 = interfaces[0].InterfaceGuid.Data3;
        guidRef.Data4 = interfaces[0].InterfaceGuid.Data4.clone();
        return guidRef;
    }


}