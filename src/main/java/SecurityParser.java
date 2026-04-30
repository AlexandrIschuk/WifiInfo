import java.util.Arrays;

public class SecurityParser {

    public static String parseSecurity(byte[] ies) {

        byte[] rsn = findElement(ies, 48);
        byte[] wpa = findVendorWpa(ies);

        if (rsn != null)
            return parseRsn(rsn);

        if (wpa != null)
            return "WPA";

        return "OPEN";
    }

    private static byte[] findElement(byte[] ies, int id) {

        int pos = 0;

        while (pos + 2 <= ies.length) {

            int elementId = ies[pos] & 0xFF;
            int len = ies[pos + 1] & 0xFF;

            if (pos + 2 + len > ies.length)
                break;

            if (elementId == id) {
                return Arrays.copyOfRange(
                        ies,
                        pos + 2,
                        pos + 2 + len
                );
            }

            pos += 2 + len;
        }

        return null;
    }

    private static byte[] findVendorWpa(byte[] ies) {

        int pos = 0;

        while (pos + 2 <= ies.length) {

            int id = ies[pos] & 0xFF;
            int len = ies[pos + 1] & 0xFF;

            if (pos + 2 + len > ies.length)
                break;

            if (id == 221) {

                if (len >= 4 &&
                        ies[pos + 2] == 0x00 &&
                        ies[pos + 3] == 0x50 &&
                        ies[pos + 4] == (byte) 0xF2 &&
                        ies[pos + 5] == 1) {

                    return Arrays.copyOfRange(
                            ies,
                            pos + 2,
                            pos + 2 + len
                    );
                }
            }

            pos += 2 + len;
        }

        return null;
    }

    private static String parseRsn(byte[] rsn) {

        if (rsn.length < 8)
            return "WPA2-Personal";

        int pos = 2;

        pos += 4;

        int pairwiseCount =
                (rsn[pos] & 0xFF) |
                        ((rsn[pos + 1] & 0xFF) << 8);

        pos += 2 + pairwiseCount * 4;

        int akmCount =
                (rsn[pos] & 0xFF) |
                        ((rsn[pos + 1] & 0xFF) << 8);

        pos += 2;

        boolean wpa2 = false;
        boolean wpa3 = false;
        boolean enterprise = false;

        for (int i = 0; i < akmCount; i++) {

            int type = rsn[pos + 3] & 0xFF;

            if (type == 1)
                enterprise = true;

            if (type == 2)
                wpa2 = true;

            if (type == 8)
                wpa3 = true;

            pos += 4;
        }

        if (enterprise)
            return "WPA2-Enterprise";

        if (wpa2 && wpa3)
            return "WPA2/WPA3-Personal";

        if (wpa3)
            return "WPA3-Personal";

        if (wpa2)
            return "WPA2-Personal";

        return "WPA2-Personal";
    }
    public static String parseCipher(byte[] ie) {

        int i = 0;

        while(i < ie.length - 2) {

            int id = ie[i] & 0xFF;
            int len = ie[i + 1] & 0xFF;

            if(id == 48) {

                int pos = i + 4;

                pos += 4;

                int pairwiseCount =
                        (ie[pos] & 0xFF) |
                                ((ie[pos + 1] & 0xFF) << 8);

                pos += 2;

                if(pairwiseCount > 0) {

                    int type = ie[pos + 3] & 0xFF;

                    switch(type) {

                        case 2: return "TKIP";
                        case 4: return "CCMP";
                        case 8: return "GCMP";

                        default: return "Unknown";
                    }
                }
            }

            i += len + 2;
        }

        return "Open";
    }
}