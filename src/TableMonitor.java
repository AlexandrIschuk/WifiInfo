// TableMonitor.java
import com.formdev.flatlaf.FlatIntelliJLaf;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TableMonitor extends JFrame {
    private static final Color APP_BACKGROUND = new Color(242, 246, 251);
    private static final Color CARD_BACKGROUND = Color.WHITE;
    private static final Color CARD_BORDER = new Color(214, 223, 235);
    private static final Color TITLE_COLOR = new Color(31, 41, 55);
    private static final Color BODY_TEXT = new Color(75, 85, 99);
    private static final Color ACCENT_COLOR = new Color(47, 109, 229);
    private static final Color TEXTAREA_BACKGROUND = new Color(247, 250, 255);
    private NetworkTableManager tableManager;
    private NetworkScanner scanner;
    private ChartManager chartManager;
    private ChannelAnalyzer channelAnalyzer;
    private SelectionManager selectionManager;
    private ChannelStatistics channelStatistics;
    private Timer refreshTimer;
    private final HtmlReportRecorder reportRecorder = new HtmlReportRecorder();
    private final Map<String, NetworkInfo> latestNetworks = new LinkedHashMap<>();

    private JPanel mainPanel;
    private JTable networksTable;
    private JTextArea detailsArea;
    private JPanel graphContainerPanel;
    private JPanel channelPanel;
    private JPanel channelPanel5;
    private JPanel filterPanel;
    private JPanel channelChartPanelContainer;
    private JPanel channelStatsPanel;

    private JLabel ssidLabel;
    private JLabel channelFilterLabel;
    private JLabel signalFilterLabel;
    private JLabel securityFilterLabel;
    private JLabel signalLabel;
    private JLabel securityLabel;
    private JLabel bssidLabel;
    private JLabel channelLabel;
    private JLabel channelTotalValue;
    private JLabel channel24Value;
    private JLabel channel5Value;
    private JLabel busiestChannelValue;
    private JLabel bestChannelValue;
    private JLabel bssidsCount;
    private JLabel avgNetworksValue;
    private JTextArea channelRecommendationArea;

    private JButton applyFilterButton;
    private JButton resetFilterButton;
    private JTextField channelFilterField;
    private JTextField signalFilterField;
    private JComboBox<String> securityFilterComboBox;
    private JTextArea bssidArea;
    private JComboBox bssidComboBox;
    private JPanel networkPanel;
    private JPanel recordingPanel;
    private JButton startRecordingButton;
    private JButton stopAndSaveRecordingButton;
    private String connectedSSID;
    private JMenuItem startReportMenuItem;
    private JMenuItem stopReportMenuItem;
    private JButton startReportButton;
    private JButton stopReportButton;
    private JLabel recordingTimerLabel;
    private JLabel logLabel;
    private Timer recordingUiTimer;
    private Instant recordingStartedAt;

    public TableMonitor() {
        initializeComponents();
        setupUI();
        startRefreshTimer();
    }

    private void initializeComponents() {
        scanner = new NetworkScanner();
        channelAnalyzer = new ChannelAnalyzer(); // Создаем один экземпляр
        tableManager = new NetworkTableManager(networksTable);
        chartManager = new ChartManager(graphContainerPanel, channelPanel, channelPanel5,
                channelChartPanelContainer, channelStatsPanel);
        selectionManager = new SelectionManager(tableManager, chartManager, detailsArea,
                channelAnalyzer, bssidArea, bssidComboBox); // Передаем анализатор
        setupFilterListeners();
    }

    private void setupUI() {
        setContentPane(mainPanel);
        setTitle("Мониторинг беспроводных сетей");
        setSize(2100, 1200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setupReportMenu();
        setupReportButtons();
        applyModernPanelStyles();
        selectionManager.bssidsListener();
    }


    private void setupFilterListeners() {
        applyFilterButton.addActionListener(e -> applyFilters());
        resetFilterButton.addActionListener(e -> resetFilters());
        channelFilterField.addActionListener(e -> applyFilters());
        signalFilterField.addActionListener(e -> applyFilters());
    }

    private void applyModernPanelStyles() {
        if (mainPanel != null) {
            mainPanel.setBackground(APP_BACKGROUND);
        }

        styleCardPanel(networkPanel, "Информация о сети");
        styleCardPanel(channelStatsPanel, "Статистика каналов");

        styleLabels(
                ssidLabel, channelFilterLabel, signalFilterLabel, securityFilterLabel, signalLabel,
                securityLabel, bssidLabel, channelLabel, channelTotalValue, channel24Value,
                channel5Value, busiestChannelValue, bestChannelValue, bssidsCount, avgNetworksValue, recordingTimerLabel, logLabel
        );

        styleTextArea(detailsArea, false);
        styleTextArea(bssidArea, false);
        styleTextArea(channelRecommendationArea, true);

        styleInput(channelFilterField);
        styleInput(signalFilterField);
        styleComboBox(securityFilterComboBox);
        styleComboBox(bssidComboBox);
        styleButton(applyFilterButton, true);
        styleButton(resetFilterButton, false);
    }

    private void styleCardPanel(JPanel panel, String title) {
        if (panel == null) {
            return;
        }

        panel.setOpaque(true);
        panel.setBackground(CARD_BACKGROUND);
        panel.setBorder(createCardBorder(title));

        for (Component component : panel.getComponents()) {
            styleComponent(component);
        }
    }

    private Border createCardBorder(String title) {
        Border outline = BorderFactory.createLineBorder(CARD_BORDER, 1, true);
        Border shellPadding = BorderFactory.createEmptyBorder(8, 10, 10, 10);
        TitledBorder titledBorder = BorderFactory.createTitledBorder(
                new CompoundBorder(outline, shellPadding),
                title,
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 16),
                TITLE_COLOR
        );
        Border innerPadding = BorderFactory.createEmptyBorder(16, 16, 16, 16);
        return new CompoundBorder(titledBorder, innerPadding);
    }

    private void styleComponent(Component component) {
        if (component instanceof JPanel nestedPanel) {
            nestedPanel.setOpaque(false);
            for (Component child : nestedPanel.getComponents()) {
                styleComponent(child);
            }
            return;
        }

        if (component instanceof JLabel label) {
            label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            label.setForeground(BODY_TEXT);
            return;
        }

        if (component instanceof JTextArea textArea) {
            styleTextArea(textArea, textArea == channelRecommendationArea);
            return;
        }

        if (component instanceof JTextField textField) {
            styleInput(textField);
            return;
        }

        if (component instanceof JScrollPane scrollPane) {
            styleScrollPane(scrollPane);
            Component view = scrollPane.getViewport().getView();
            if (view != null) {
                styleComponent(view);
            }
            return;
        }

        if (component instanceof JComboBox<?> comboBox) {
            styleComboBox(comboBox);
            return;
        }

        if (component instanceof JButton button) {
            styleButton(button, button == applyFilterButton);
        }
    }

    private void styleLabels(JLabel... labels) {
        for (JLabel label : labels) {
            if (label == null) {
                continue;
            }
            label.setFont(new Font("Segoe UI", Font.PLAIN, 15));
            label.setForeground(BODY_TEXT);
        }
    }

    private void styleTextArea(JTextArea textArea, boolean highlight) {
        if (textArea == null) {
            return;
        }

        textArea.setFont(new Font("Segoe UI", Font.PLAIN, highlight ? 14 : 13));
        textArea.setForeground(BODY_TEXT);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setBackground(highlight ? TEXTAREA_BACKGROUND : CARD_BACKGROUND);
        textArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(highlight ? new Color(206, 220, 244) : CARD_BORDER, 1, true),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
    }

    private void styleInput(JTextField textField) {
        if (textField == null) {
            return;
        }

        textField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        textField.setBackground(new Color(250, 252, 255));
        textField.setForeground(TITLE_COLOR);
        textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER, 1, true),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        textField.setMargin(new Insets(4, 6, 4, 6));
    }

    private void styleComboBox(JComboBox<?> comboBox) {
        if (comboBox == null) {
            return;
        }

        comboBox.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        comboBox.setBackground(new Color(250, 252, 255));
        comboBox.setForeground(TITLE_COLOR);
        comboBox.setBorder(BorderFactory.createLineBorder(CARD_BORDER, 1, true));
    }

    private void styleScrollPane(JScrollPane scrollPane) {
        scrollPane.setBorder(BorderFactory.createLineBorder(CARD_BORDER, 1, true));
        scrollPane.getViewport().setBackground(CARD_BACKGROUND);
        scrollPane.setBackground(CARD_BACKGROUND);
    }

    private void styleButton(JButton button, boolean primary) {
        if (button == null) {
            return;
        }

        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setFocusPainted(false);

        // Используем LineBorder с параметром rounded = true
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primary ? ACCENT_COLOR : new Color(180, 190, 200), 1, true),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)
        ));

        button.setBackground(primary ? ACCENT_COLOR : new Color(233, 239, 248));
        button.setForeground(primary ? Color.WHITE : TITLE_COLOR);
    }

    private void startRefreshTimer() {
        refreshTimer = new Timer(3000, e -> loadNetworks());
        refreshTimer.setInitialDelay(0);
        refreshTimer.start();
    }

    private void loadNetworks() {
        new SwingWorker<Map<String, NetworkInfo>, Void>() {
            @Override
            protected Map<String, NetworkInfo> doInBackground() {
                return scanner.scanNetworks();
            }

            @Override
            protected void done() {
                try {
                    Map<String, NetworkInfo> networks = get();
                    updateUI(networks);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.execute();
    }

    private void updateUI(Map<String, NetworkInfo> networks) {
        latestNetworks.clear();
        latestNetworks.putAll(networks);

        tableManager.updateTable(networks);
        channelAnalyzer.updateFromNetworks(new ArrayList<>(networks.values()));
        reportRecorder.record(networks, channelAnalyzer.getChannelStats());

        updateChannelStats();
        updateSelection();
        updateLabels();

        chartManager.updateCharts(networks.values(), channelAnalyzer.getChannelStats(),
                selectionManager.getSelectedSsid(), selectionManager.getSelectedBssid());
    }

    private void setupReportMenu() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        JMenu reportMenu = new JMenu("Отчет");
        reportMenu.setFont(new Font("Segoe UI", Font.BOLD, 13));

        startReportMenuItem = new JMenuItem("Начать запись");
        startReportMenuItem.addActionListener(e -> startReportRecording());

        stopReportMenuItem = new JMenuItem("Стоп и создать отчет");
        stopReportMenuItem.addActionListener(e -> stopAndCreateReport());
        stopReportMenuItem.setEnabled(false);

        reportMenu.add(startReportMenuItem);
        reportMenu.add(stopReportMenuItem);
        menuBar.add(reportMenu);
        setJMenuBar(menuBar);
    }

    private void setupReportButtons() {
        if (recordingPanel == null) {
            return;
        }
        ensureRecordingTimerLabel();
        startRecordingButton.addActionListener(e -> startReportRecording());


        stopAndSaveRecordingButton.addActionListener(e -> stopAndCreateReport());


        styleButton(startRecordingButton, true);
        styleButton(stopAndSaveRecordingButton, false);
        updateReportMenuState();

    }

    private void startReportRecording() {
        if (latestNetworks.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Сначала дождитесь обновления списка сетей.",
                    "Нет данных",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        List<String> selectedNetworks = promptNetworksForReport();
        if (selectedNetworks == null || selectedNetworks.isEmpty()) {
            return;
        }

        reportRecorder.start(selectedNetworks);
        reportRecorder.record(latestNetworks, channelAnalyzer.getChannelStats());
        recordingStartedAt = Instant.now();
        startRecordingTimer();
        updateReportMenuState();

        JOptionPane.showMessageDialog(
                this,
                "Запись отчета запущена. В отчет попадут выбранные сети: " + String.join(", ", selectedNetworks),
                "Запись начата",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void stopAndCreateReport() {
        if (!reportRecorder.isRecording()) {
            return;
        }

        reportRecorder.stop();
        reportRecorder.record(latestNetworks, channelAnalyzer.getChannelStats());
        stopRecordingTimer();

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Сохранить HTML отчет");
        fileChooser.setSelectedFile(new java.io.File(reportRecorder.buildDefaultFileName()));

        int result = fileChooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            updateReportMenuState();
            return;
        }

        Path filePath = fileChooser.getSelectedFile().toPath();
        try {
            reportRecorder.writeHtmlReport(filePath);
            JOptionPane.showMessageDialog(
                    this,
                    "HTML отчет сохранен:\n" + filePath,
                    "Отчет создан",
                    JOptionPane.INFORMATION_MESSAGE
            );
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Не удалось сохранить отчет:\n" + ex.getMessage(),
                    "Ошибка сохранения",
                    JOptionPane.ERROR_MESSAGE
            );
        } finally {
            updateReportMenuState();
        }
    }

    private List<String> promptNetworksForReport() {
        JPanel checkboxesPanel = new JPanel();
        checkboxesPanel.setLayout(new BoxLayout(checkboxesPanel, BoxLayout.Y_AXIS));
        checkboxesPanel.setOpaque(false);

        List<JCheckBox> checkBoxes = new ArrayList<>();
        for (String ssid : latestNetworks.keySet()) {
            JCheckBox checkBox = new JCheckBox(ssid, false);
            checkBox.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            checkBox.setOpaque(false);
            checkBoxes.add(checkBox);
            checkboxesPanel.add(checkBox);
        }

        JScrollPane scrollPane = new JScrollPane(checkboxesPanel);
        scrollPane.setPreferredSize(new Dimension(420, 240));

        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.add(new JLabel("Выберите сети, которые нужно включить в HTML отчет:"), BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        int option = JOptionPane.showConfirmDialog(
                this,
                panel,
                "Выбор сетей для отчета",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (option != JOptionPane.OK_OPTION) {
            return null;
        }

        List<String> selectedNetworks = new ArrayList<>();
        for (JCheckBox checkBox : checkBoxes) {
            if (checkBox.isSelected()) {
                selectedNetworks.add(checkBox.getText());
            }
        }

        if (selectedNetworks.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Нужно выбрать хотя бы одну сеть для отчета.",
                    "Нет выбранных сетей",
                    JOptionPane.WARNING_MESSAGE
            );
            return null;
        }

        return selectedNetworks;
    }

    private void updateReportMenuState() {
        boolean recording = reportRecorder.isRecording();
        if (startReportMenuItem != null) {
            startReportMenuItem.setEnabled(!recording);
        }
        if (stopReportMenuItem != null) {
            stopReportMenuItem.setEnabled(recording);
        }
        if (startReportButton != null) {
            startReportButton.setEnabled(!recording);
        }
        if (stopReportButton != null) {
            stopReportButton.setEnabled(recording);
        }
        if (startRecordingButton != null) {
            startRecordingButton.setEnabled(!recording);
        }
        if (stopAndSaveRecordingButton != null) {
            stopAndSaveRecordingButton.setEnabled(recording);
        }
        updateRecordingTimerLabel();
    }

    private void ensureRecordingTimerLabel() {
        if (recordingTimerLabel != null || recordingPanel == null) {
            return;
        }

        recordingTimerLabel = new JLabel("Таймер: 00:00:00");
        recordingTimerLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        recordingTimerLabel.setForeground(TITLE_COLOR);
        recordingPanel.add(recordingTimerLabel);
        recordingPanel.revalidate();
        recordingPanel.repaint();
    }

    private void startRecordingTimer() {
        ensureRecordingTimerLabel();
        updateRecordingTimerLabel();

        if (recordingUiTimer != null) {
            recordingUiTimer.stop();
        }

        recordingUiTimer = new Timer(1000, e -> updateRecordingTimerLabel());
        recordingUiTimer.setInitialDelay(0);
        recordingUiTimer.start();
    }

    private void stopRecordingTimer() {
        if (recordingUiTimer != null) {
            recordingUiTimer.stop();
            recordingUiTimer = null;
        }
        recordingStartedAt = null;
        updateRecordingTimerLabel();
    }

    private void updateRecordingTimerLabel() {
        if (recordingTimerLabel == null) {
            return;
        }

        if (recordingStartedAt == null || !reportRecorder.isRecording()) {
            recordingTimerLabel.setText("Таймер: 00:00:00");
            return;
        }

        Duration duration = Duration.between(recordingStartedAt, Instant.now());
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        recordingTimerLabel.setText(String.format("Таймер: %02d:%02d:%02d", hours, minutes, seconds));
    }

    private void updateChannelStats() {
        ChannelStatistics stats = channelAnalyzer.getStatistics();
        if (channelTotalValue != null) {
            int activeChannels = stats.getTotalChannels();
            channelTotalValue.setText("Активных каналов: " + activeChannels);
        }
        if (channel24Value != null)
            channel24Value.setText("2.4 GHz: " + stats.getChannels24());
        if (channel5Value != null)
            channel5Value.setText("5 GHz: " + stats.getChannels5());
        if (busiestChannelValue != null)
            busiestChannelValue.setText("Макс. загрузка: " + stats.getBusiestChannelDisplay());
        if (bestChannelValue != null)
            bestChannelValue.setText("Мин. загрузка: " + stats.getBestChannelDisplay());
        if (avgNetworksValue != null)
            avgNetworksValue.setText("Сред. загрузка: " + stats.getAvgLoadFormatted());
        if (channelRecommendationArea != null)
            channelRecommendationArea.setText(stats.getRecommendations());
    }

    private void updateSelection() {
        String selectedSsid = tableManager.getSelectedSsid();
        if (selectedSsid != null) {
            selectionManager.setSelectedNetwork(selectedSsid, scanner.getNetwork(selectedSsid));
        }
    }

    private void updateLabels() {
        NetworkInfo selected = selectionManager.getSelectedNetwork();
        if (selected != null) {
            ssidLabel.setText("Сеть: " + selected.getSsid());
            signalLabel.setText("Сигнал: " + selected.getSignalStrength() + " dBm");
            securityLabel.setText("Безопасность: " + selected.getAuthentication());
            bssidLabel.setText("MAC-адрес: " + selected.getBssids().get(0).bssid);
            channelLabel.setText("Канал: " + selected.getChannel());
            bssidsCount.setText("Точек доступа: " + selected.getBssids().size());
        }
    }

    private void applyFilters() {
        tableManager.applyFilters(
                (String) securityFilterComboBox.getSelectedItem(),
                channelFilterField.getText(),
                signalFilterField.getText()
        );
    }

    private void resetFilters() {
        securityFilterComboBox.setSelectedIndex(0);
        channelFilterField.setText("");
        signalFilterField.setText("");
        tableManager.resetFilters();
    }

    @Override
    public void dispose() {
        if (refreshTimer != null) {
            refreshTimer.stop();
        }
        scanner.close();
        super.dispose();
    }

    public static void main(String[] args) {
        try {
            FlatIntelliJLaf.setup();
            UIManager.put("Button.arc", 10);
            UIManager.put("Component.arc", 10);
            UIManager.put("TextComponent.arc", 10);
            UIManager.put("Table.showHorizontalLines", true);
            UIManager.put("Table.showVerticalLines", true);
            UIManager.put("Table.gridColor", new Color(200, 200, 200));


        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            new TableMonitor().setVisible(true);
        });
    }
}
