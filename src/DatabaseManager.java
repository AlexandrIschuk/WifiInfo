/*
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class DatabaseManager {
    private static final String URL = "jdbc:postgresql://192.168.1.68:5432/wifimonitor";
    private static final String USER = "postgres";
    private static final String PASSWORD = "1234";

    private Connection connection;

    public DatabaseManager() {
        try {
            Class.forName("org.postgresql.Driver");
            Properties props = new Properties();
            props.setProperty("user", USER);
            props.setProperty("password", PASSWORD);
            props.setProperty("ssl", "false");
            this.connection = DriverManager.getConnection(URL, props);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    public void saveNetworkInfo(NetworkInfo info) throws SQLException {
        // Сохраняем сеть
        String networkSql = """
            INSERT INTO networks (ssid, security_type, frequency_band, network_type)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (ssid) DO UPDATE SET
                security_type = EXCLUDED.security_type,
                frequency_band = EXCLUDED.frequency_band,
                network_type = EXCLUDED.network_type""";

        try (PreparedStatement ps = connection.prepareStatement(networkSql)) {
            ps.setString(1, info.getSsid());
            ps.setString(2, info.getAuthentication());
            ps.setString(3, info.getBand());

            ps.executeUpdate();
        }

        // Для каждой точки доступа
        for (String bssid : info.getBssids()) {
            // Сохраняем точку доступа
            String apSql = """
                INSERT INTO access_points (
                    bssid, ssid, channel, radio_type, 
                    basic_rates, other_rates, band
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (bssid) DO UPDATE SET
                    channel = EXCLUDED.channel,
                    radio_type = EXCLUDED.radio_type,
                    basic_rates = EXCLUDED.basic_rates,
                    other_rates = EXCLUDED.other_rates,
                    band = EXCLUDED.band""";

            try (PreparedStatement ps = connection.prepareStatement(apSql)) {
                ps.setString(1, bssid);
                ps.setString(2, info.getSsid());
                ps.setInt(3, info.getChannel().isEmpty() ? 0 : Integer.parseInt(info.getChannel()));
                ps.setString(4, info.getRadioType());
                ps.setString(5, info.getBasicRates());
                ps.setString(6, info.getOtherRates());
                ps.setString(7, info.getBand());
                ps.executeUpdate();
            }

            // Сохраняем измерение сигнала
            String signalSql = """
                INSERT INTO signal_measurements (
                    bssid, signal_strength, signal_quality, 
                    authentication, cipher
                ) VALUES (?, ?, ?, ?, ?)""";

            try (PreparedStatement ps = connection.prepareStatement(signalSql)) {
                ps.setString(1, bssid);
                ps.setInt(2, info.getSignalStrength().isEmpty() ? 0 : Integer.parseInt(info.getSignalStrength()));
                ps.setInt(3, info.getSignalStrength().isEmpty() ? 0 :
                        (Integer.parseInt(info.getSignalStrength()) / 2) - 100);
                ps.setString(4, info.getAuthentication());
                ps.setString(5, info.getCipher());
                ps.executeUpdate();
            }
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}*/
