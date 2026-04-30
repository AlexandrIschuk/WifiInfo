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
        String[] columnNames = {"SSID","Производитель", "Сигнал (dBm)", "Канал", "Безопасность", "Точек доступа", "MAC-адрес"};
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

                if (ssid != null && connectedSsid != null && !connectedSsid.isEmpty()
                        && ssid.startsWith(connectedSsid)) {
                    c.setBackground(new Color(180, 255, 180));
                } else {
                    if (!isSelected)
                        c.setBackground(Color.WHITE);
                }

                if (column == 5 && value instanceof Integer) {
                    int apCount = (Integer) value;
                    if (apCount > 1 && !isSelected) {
                        c.setBackground(new Color(255, 255, 200));
                    }
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
        columnModel.getColumn(1).setPreferredWidth(120);
        columnModel.getColumn(2).setPreferredWidth(100);
        columnModel.getColumn(3).setPreferredWidth(50);
        columnModel.getColumn(4).setPreferredWidth(100);
        columnModel.getColumn(5).setPreferredWidth(40);
        columnModel.getColumn(6).setPreferredWidth(150);
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

        for (NetworkInfo info : networks.values()) {
            if (!existingSsids.contains(info.getSsid())) {
                connectedSsid = info.getConnectedSsid();
                addNetworkRow(info);
            } else {
                updateNetworkRow(info);
            }
        }

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
        int apCount = info.getBssids() != null ? info.getBssids().size() : 0;
        String macAddress = info.getBssids() != null && !info.getBssids().isEmpty()
                ? info.getBssids().get(0).bssid : "N/A";

        tableModel.addRow(new Object[]{
                info.getSsid(),
                info.getVendor(),
                info.getSignalStrength(),
                info.getChannel(),
                info.getAuthentication(),
                apCount,
                macAddress
        });
    }

    private void updateNetworkRow(NetworkInfo info) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (info.getSsid().equals(tableModel.getValueAt(i, 0))) {
                int apCount = info.getBssids() != null ? info.getBssids().size() : 0;
                String macAddress = info.getBssids() != null && !info.getBssids().isEmpty()
                        ? info.getBssids().get(0).bssid : "N/A";

                tableModel.setValueAt(info.getVendor(), i, 1);
                tableModel.setValueAt(info.getSignalStrength(), i, 2);
                tableModel.setValueAt(info.getChannel(), i, 3);
                tableModel.setValueAt(info.getAuthentication(), i, 4);
                tableModel.setValueAt(apCount, i, 5);
                tableModel.setValueAt(macAddress, i, 6);
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

    public void applyFilters(String security, String channelText, String signalText,
                             boolean signalGreater, boolean signalLess) {
        List<RowFilter<DefaultTableModel, Integer>> filters = new ArrayList<>();

        if (security != null && !security.equals("Все")) {
            filters.add(RowFilter.regexFilter("(?i)" + security, 4));
        }

        if (!channelText.isEmpty()) {
            try {
                int channel = Integer.parseInt(channelText);
                filters.add(RowFilter.regexFilter("^" + channel + "$", 3));
            } catch (NumberFormatException ignored) {}
        }

        if (!signalText.isEmpty()) {
            try {
                int minSignal = Integer.parseInt(signalText);
                filters.add(createSignalFilter(minSignal, signalGreater, signalLess));
            } catch (NumberFormatException ignored) {}
        }

        sorter.setRowFilter(filters.isEmpty() ? null : RowFilter.andFilter(filters));
    }

    private RowFilter<DefaultTableModel, Integer> createSignalFilter(int threshold,
                                                                     boolean greater,
                                                                     boolean less) {
        return new RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                String signalStr = (String) entry.getValue(2);
                try {
                    int signalValue = Integer.parseInt(signalStr);
                    if (greater) {
                        return signalValue >= threshold;
                    } else if (less) {
                        return signalValue <= threshold;
                    }
                    return true;
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
    public void selectNetworkBySsid(String ssid) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String currentSsid = (String) tableModel.getValueAt(i, 0);
            if (currentSsid != null && currentSsid.equals(ssid)) {
                int viewRow = table.convertRowIndexToView(i);
                table.setRowSelectionInterval(viewRow, viewRow);
                table.scrollRectToVisible(table.getCellRect(viewRow, 0, true));
                break;
            }
        }
    }

}


interface TableSelectionListener {
    void onSelectionChanged(String selectedSsid);
}
