import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MacVendorResolver {

    private static MacVendorResolver instance;
    private Map<String, String> macToVendor = new HashMap<>();

    private static final String MANUF_FILE = "manuf.txt";

    private MacVendorResolver() {
        loadFromFile();
    }

    public static MacVendorResolver getInstance() {
        if (instance == null) {
            instance = new MacVendorResolver();
        }
        return instance;
    }

    private void loadFromFile() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("manuf.txt")) {
            if (is != null) {
                loadFromStream(is);
                return;
            }
        } catch (IOException e) {
        }

        File file = new File("manuf.txt");
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
                parseLines(reader);
            } catch (IOException e) {}
        }
    }

    private void parseLines(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("#") || line.trim().isEmpty()) {
                continue;
            }

            String[] parts = line.split("\t");
            if (parts.length >= 2) {
                String macPrefix = parts[0].trim().toUpperCase();
                String vendor = parts[1].trim();

                if (macPrefix.contains(":")) {
                    String oui = macPrefix.length() >= 8 ? macPrefix.substring(0, 8) : macPrefix;
                    macToVendor.put(oui, vendor);
                }
            }
        }
    }

    private void loadFromStream(InputStream is) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            parseLines(reader);
        } catch (IOException e) {
        }
    }

        public String resolve(String mac) {
        if (mac == null || mac.isEmpty()) return "";

        String cleanMac = mac.toUpperCase().replaceAll("[^0-9A-F]", "");
        if (cleanMac.length() < 6) return "";

        String oui = cleanMac.substring(0, 2) + ":" +
                cleanMac.substring(2, 4) + ":" +
                cleanMac.substring(4, 6);

        return macToVendor.getOrDefault(oui, "");
    }
}