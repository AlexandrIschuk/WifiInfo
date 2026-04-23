// NetworkTableManager.java
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.RowFilter;
import java.awt.*;
import java.util.*;
import java.util.List;

public class NetworkTableManager {
    private final JTable table;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;
    private final List<TableSelectionListener> selectionListeners = new ArrayList<>();
    private String currentSelectedSsid;
    private String connectedSsid;

    public NetworkTableManager(JTable table) {
        this.table = table;
        setupTable();
    }

    private void setupTable() {
        String[] columnNames = {"SSID", "Сигнал (dBm)", "Канал", "Безопасность", "MAC-адрес"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {

            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value,
                    boolean isSelected, boolean hasFocus,
                    int row, int column) {

                Component c = super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);

                int modelRow = table.convertRowIndexToModel(row);
                String ssid = (String) tableModel.getValueAt(modelRow, 0);



                if (ssid.startsWith(connectedSsid)) {

                    c.setBackground(new Color(180, 255, 180)); // зелёный

                } else {

                    if (!isSelected)
                        c.setBackground(Color.WHITE);
                }

                return c;
            }
        });

        table.setModel(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);

        setupTableHeader();
        setupColumnWidths();
        setupSelectionListener();
    }

    private void setupTableHeader() {
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("SansSerif", Font.BOLD, 14));
        header.setBackground(new Color(70, 130, 180));
        header.setForeground(Color.WHITE);
    }

    private void setupColumnWidths() {
        TableColumnModel columnModel = table.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(200);
        columnModel.getColumn(1).setPreferredWidth(80);
        columnModel.getColumn(2).setPreferredWidth(60);
        columnModel.getColumn(3).setPreferredWidth(120);
        columnModel.getColumn(4).setPreferredWidth(150);
    }

    private void setupSelectionListener() {
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = table.getSelectedRow();
                if (selectedRow >= 0) {
                    int modelRow = table.convertRowIndexToModel(selectedRow);
                    currentSelectedSsid = (String) tableModel.getValueAt(modelRow, 0);
                    notifySelectionListeners();
                }
            }
        });
    }


    public void updateTable(Map<String, NetworkInfo> networks) {
        Set<String> existingSsids = getExistingSsids();

        // Добавляем и обновляем сети
        for (NetworkInfo info : networks.values()) {
            if (!existingSsids.contains(info.getSsid())) {
                connectedSsid = info.getConnectedSsid();
                addNetworkRow(info);
            } else {
                updateNetworkRow(info);
            }
        }

        // Удаляем отсутствующие сети
        removeMissingNetworks(networks);

    }

    private Set<String> getExistingSsids() {
        Set<String> ssids = new HashSet<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            ssids.add((String) tableModel.getValueAt(i, 0));
        }
        return ssids;
    }

    private void addNetworkRow(NetworkInfo info) {
        tableModel.addRow(new Object[]{
                info.getSsid(),
                info.getSignalStrength(),
                info.getChannel(),
                info.getAuthentication(),
                info.getBssids().get(0).bssid
        });
    }

    private void updateNetworkRow(NetworkInfo info) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (info.getSsid().equals(tableModel.getValueAt(i, 0))) {
                tableModel.setValueAt(info.getSignalStrength(), i, 1);
                tableModel.setValueAt(info.getChannel(), i, 2);
                tableModel.setValueAt(info.getAuthentication(), i, 3);
                tableModel.setValueAt(info.getBssids().get(0).bssid, i, 4);
                break;
            }
        }
    }

    private void removeMissingNetworks(Map<String, NetworkInfo> networks) {
        for (int i = tableModel.getRowCount() - 1; i >= 0; i--) {
            String ssid = (String) tableModel.getValueAt(i, 0);
            if (!networks.containsKey(ssid)) {
                tableModel.removeRow(i);
            }
        }
    }

    public void applyFilters(String security, String channelText, String signalText) {
        List<RowFilter<DefaultTableModel, Integer>> filters = new ArrayList<>();

        if (security != null && !security.equals("Все")) {
            filters.add(RowFilter.regexFilter("(?i)" + security, 3));
        }

        if (!channelText.isEmpty()) {
            try {
                int channel = Integer.parseInt(channelText);
                filters.add(RowFilter.regexFilter("^" + channel + "$", 2));
            } catch (NumberFormatException ignored) {}
        }

        if (!signalText.isEmpty()) {
            try {
                int minSignal = Integer.parseInt(signalText);
                filters.add(createSignalFilter(minSignal));
            } catch (NumberFormatException ignored) {}
        }

        sorter.setRowFilter(filters.isEmpty() ? null : RowFilter.andFilter(filters));

    }

    private RowFilter<DefaultTableModel, Integer> createSignalFilter(int minSignal) {
        return new RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                String signalStr = (String) entry.getValue(1);
                try {
                    int signalValue = Integer.parseInt(signalStr);
                    return signalValue >= minSignal;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        };
    }

    public void resetFilters() {
        sorter.setRowFilter(null);
    }


    public String getSelectedSsid() {
        return currentSelectedSsid;
    }

    public void addSelectionListener(TableSelectionListener listener) {
        selectionListeners.add(listener);
    }

    private void notifySelectionListeners() {
        for (TableSelectionListener listener : selectionListeners) {
            listener.onSelectionChanged(currentSelectedSsid);
        }
    }

}

interface TableSelectionListener {
    void onSelectionChanged(String selectedSsid);
}
