import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.Map;

public class ClassificationPanel extends JPanel {

    private JTable classificationTable;
    private DefaultTableModel tableModel;
    private JLabel statisticsLabel;
    private JLabel suspiciousLabel;
    private NetworkClassifier classifier;

    private static final Color COLOR_HOME = new Color(46, 204, 113);
    private static final Color COLOR_CORPORATE = new Color(52, 152, 219);
    private static final Color COLOR_PUBLIC = new Color(149, 165, 166);
    private static final Color COLOR_MOBILE = new Color(230, 126, 34);
    private static final Color COLOR_IOT = new Color(155, 89, 182);
    private static final Color COLOR_SUSPICIOUS = new Color(231, 76, 60);
    private static final Color COLOR_OTHER = new Color(52, 73, 94);

    public ClassificationPanel() {
        this.classifier = new NetworkClassifier();
        initializeUI();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setOpaque(true);
        setBackground(Color.WHITE);


        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("Классификация сетей");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(new Color(31, 41, 55));
        headerPanel.add(titleLabel, BorderLayout.WEST);

        statisticsLabel = new JLabel();
        statisticsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statisticsLabel.setForeground(new Color(75, 85, 99));
        headerPanel.add(statisticsLabel, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        String[] columns = {"SSID", "Тип сети", "Уверенность", "Уровень риска", "Обоснование"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        classificationTable = new JTable(tableModel);
        classificationTable.setRowHeight(30);
        classificationTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        classificationTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        classificationTable.setShowGrid(true);
        classificationTable.setGridColor(new Color(230, 235, 240));
        classificationTable.setSelectionBackground(new Color(235, 245, 255));

        classificationTable.setDefaultRenderer(Object.class,
                new ClassificationTableCellRenderer());

        classificationTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        classificationTable.getColumnModel().getColumn(1).setPreferredWidth(130);
        classificationTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        classificationTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        classificationTable.getColumnModel().getColumn(4).setPreferredWidth(250);

        JScrollPane scrollPane = new JScrollPane(classificationTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(
                new Color(214, 223, 235), 1, true));
        add(scrollPane, BorderLayout.CENTER);

        suspiciousLabel = new JLabel(" ");
        suspiciousLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        suspiciousLabel.setForeground(new Color(231, 76, 60));
        suspiciousLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        add(suspiciousLabel, BorderLayout.SOUTH);

        add(createLegendPanel(), BorderLayout.SOUTH);
    }

    private JPanel createLegendPanel() {
        JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        legendPanel.setOpaque(false);
        legendPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        addLegendItem(legendPanel, COLOR_HOME, "Домашняя");
        addLegendItem(legendPanel, COLOR_CORPORATE, "Корпоративная");
        addLegendItem(legendPanel, COLOR_PUBLIC, "Публичная");
        addLegendItem(legendPanel, COLOR_MOBILE, "Мобильная");
        addLegendItem(legendPanel, COLOR_IOT, "IoT");
        addLegendItem(legendPanel, COLOR_SUSPICIOUS, "Опасная");

        return legendPanel;
    }

    private void addLegendItem(JPanel panel, Color color, String text) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        item.setOpaque(false);

        JPanel colorBox = new JPanel();
        colorBox.setPreferredSize(new Dimension(12, 12));
        colorBox.setBackground(color);
        colorBox.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        item.add(colorBox);

        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        item.add(label);

        panel.add(item);
    }

    public void updateClassification(Map<String, NetworkInfo> networks) {
        tableModel.setRowCount(0);

        Map<String, NetworkClassifier.ClassificationResult> results =
                classifier.classifyAll(networks);

        for (Map.Entry<String, NetworkClassifier.ClassificationResult> entry :
                results.entrySet()) {

            NetworkClassifier.ClassificationResult result = entry.getValue();

            tableModel.addRow(new Object[]{
                    entry.getKey(),
                    result.type.displayName,
                    (int)result.confidence + "%",
                    result.getRiskDescription(),
                    result.reason
            });
        }

        updateStatistics(networks);
    }

    private void updateStatistics(Map<String, NetworkInfo> networks) {
        Map<NetworkClassifier.NetworkType, Integer> stats =
                classifier.getTypeStatistics(networks);

        int total = networks.size();
        long suspicious = stats.getOrDefault(
                NetworkClassifier.NetworkType.SUSPICIOUS, 0);

        statisticsLabel.setText(String.format(
                "Всего: %d",
                total
        ));

        if (suspicious > 0) {
            suspiciousLabel.setText(String.format(
                    "Внимание: Обнаружено %d подозрительных сетей!", suspicious));
            suspiciousLabel.setVisible(true);
        } else {
            suspiciousLabel.setVisible(false);
        }
    }

    private class ClassificationTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {

            Component c = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);

            if (!isSelected) {
                String typeName = (String) table.getValueAt(row, 1);
                Color bgColor = getColorForType(typeName);
                setBackground(bgColor);
                setForeground(Color.WHITE);
            }

            setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));

            return c;
        }

        private Color getColorForType(String typeName) {
            if (typeName.contains("Домашний")) return COLOR_HOME;
            if (typeName.contains("Корпоративная")) return COLOR_CORPORATE;
            if (typeName.contains("Публичная")) return COLOR_PUBLIC;
            if (typeName.contains("Мобильная")) return COLOR_MOBILE;
            if (typeName.contains("IoT")) return COLOR_IOT;
            if (typeName.contains("Подозрительная")) return COLOR_SUSPICIOUS;
            return COLOR_OTHER;
        }
    }
}