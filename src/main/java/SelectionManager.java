import lombok.Getter;

import javax.swing.*;
import javax.swing.text.DefaultCaret;

public class SelectionManager implements TableSelectionListener {
    private ChartManager chartManager;
    private JTextArea detailsArea;
    private JTextArea bssidArea;
    private ChannelAnalyzer channelAnalyzer;
    private JComboBox bssidComboBox;
    private boolean updatingCombo = false;


    @Getter
    private String selectedSsid;
    @Getter
    private NetworkInfo selectedNetwork;
    @Getter
    private String selectedBssid;

    public SelectionManager(NetworkTableManager tableManager, ChartManager chartManager, JTextArea detailsArea, ChannelAnalyzer channelAnalyzer, JTextArea bssidArea, JComboBox bssidComboBox) {
        this.chartManager = chartManager;
        this.detailsArea = detailsArea;
        this.bssidArea = bssidArea;
        this.bssidComboBox = bssidComboBox;
        this.channelAnalyzer = channelAnalyzer;

        DefaultCaret caret = (DefaultCaret) detailsArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        tableManager.addSelectionListener(this);
    }

    @Override
    public void onSelectionChanged(String selectedSsid) {
        this.selectedSsid = selectedSsid;
    }

    public void bssidsListener(){
        bssidComboBox.addActionListener(e -> {

            if (updatingCombo)
                return;

            String mac = extractMac((String) bssidComboBox.getSelectedItem());

            if (mac == null)
                return;

            selectedBssid = mac;
            updateBssids(mac);
        });
    }


    public void setSelectedNetwork(String ssid, NetworkInfo network) {
        if (ssid.equals(selectedSsid)) {
            this.selectedNetwork = network;
            updateDetails();
            updateSignalHistory();
            updateBssidComboBox();

            String mac = extractMac((String) bssidComboBox.getSelectedItem());

            if (mac != null) {
                selectedBssid = mac;
                updateBssids(mac);
            }

        }
    }

    private void updateBssidComboBox() {

        updatingCombo = true;

        String selectedMac = selectedBssid != null
                ? selectedBssid
                : extractMac((String) bssidComboBox.getSelectedItem());

        bssidComboBox.removeAllItems();

        if (selectedNetwork != null) {

            for (NativeWifiScanner.WifiNetwork.Bssids ap : selectedNetwork.getBssids()) {
                bssidComboBox.addItem(ap.bssid + " " + "(" + ap.rssi + " dBm" + ")");
            }
        }

        if (selectedNetwork != null && !selectedNetwork.getBssids().isEmpty()) {
            String fallbackMac = selectedNetwork.getBssids().get(0).bssid;
            String macToSelect = findAvailableMac(selectedMac, fallbackMac);
            selectedBssid = macToSelect;
            selectComboItemByMac(macToSelect);
        } else {
            selectedBssid = null;
        }

        updatingCombo = false;
    }

    private void selectComboItemByMac(String mac) {
        if (mac == null) {
            return;
        }

        ComboBoxModel model = bssidComboBox.getModel();
        for (int i = 0; i < model.getSize(); i++) {
            Object item = model.getElementAt(i);
            if (mac.equals(extractMac((String) item))) {
                bssidComboBox.setSelectedIndex(i);
                return;
            }
        }
    }

    private String findAvailableMac(String preferredMac, String fallbackMac) {
        if (preferredMac == null) {
            return fallbackMac;
        }

        for (NativeWifiScanner.WifiNetwork.Bssids ap : selectedNetwork.getBssids()) {
            if (preferredMac.equals(ap.bssid)) {
                return preferredMac;
            }
        }

        return fallbackMac;
    }

    private String extractMac(String comboValue) {
        if (comboValue == null || comboValue.length() < 17) {
            return null;
        }
        return comboValue.substring(0, 17);
    }

    private void updateDetails() {
        if (detailsArea == null || selectedNetwork == null) return;

        String details = selectedNetwork.getDetailedInfo();
        details += getChannelAnalysisText();
        detailsArea.setText(details);
    }
    private void updateBssids(String mac) {
        if (selectedNetwork == null) return;

        String details = selectedNetwork.getBssidsInfo(mac);
        bssidArea.setText(details);
    }


    private String getChannelAnalysisText() {
        if (selectedNetwork == null || selectedNetwork.getChannel().isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        try {
            int channel = Integer.parseInt(selectedNetwork.getChannel());
            sb.append("\n\nАНАЛИЗ КАНАЛА ").append(channel).append(":\n");

            ChannelInfo channelInfo = channelAnalyzer.getChannelStats().get(channel);
            if (channelInfo != null) {
                sb.append(String.format("Загрузка канала: %d%% (%s)\n",
                        channelInfo.getUtilizationScore(),
                        channelInfo.getLoadDescription()));
                sb.append(String.format("Количество сетей: %d\n", channelInfo.getNetworkCount()));
                sb.append(String.format("Средний уровень сигнала: %.0f dBm\n\n", channelInfo.getAverageSignal()));

            } else {
                sb.append("Канал свободен!\n");
            }
        } catch (NumberFormatException e) {
        }

        return sb.toString();
    }




    private void updateSignalHistory() {
        if (selectedNetwork != null && !selectedNetwork.getSignalStrength().isEmpty()) {
            try {
                int mainSignal = Integer.parseInt(selectedNetwork.getSignalStrength());

                int secondSignal = -100;
                if (selectedNetwork.getBssids() != null && selectedNetwork.getBssids().size() > 1) {
                    for (NativeWifiScanner.WifiNetwork.Bssids bssid : selectedNetwork.getBssids()) {
                        int signal = bssid.rssi;
                        if (signal > secondSignal && signal != mainSignal) {
                            secondSignal = signal;
                        }
                    }
                }

                chartManager.updateSignalHistory(selectedSsid, mainSignal, secondSignal);
            } catch (NumberFormatException e) {
            }
        }
    }

}
