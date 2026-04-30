import com.formdev.flatlaf.FlatIntelliJLaf;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.List;

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
    private Map<Integer, Double> lastForecast;

    private JPanel mainPanel;
    private JTable networksTable;
    private JTextArea detailsArea;
    private JPanel graphContainerPanel;
    private JPanel channelPanel;
    private JPanel channelPanel5;
    private JPanel filterPanel;
    private JPanel channelChartPanelContainer;
    private JPanel channelStatsPanel;
    private boolean flag = false;

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
    private JTabbedPane tabbedPane1;
    private JTabbedPane tabbedPane2;
    private JTabbedPane tabbedPane3;
    private JButton trainButton;
    private JButton predictButton;
    private JLabel predictionLabel;
    private JRadioButton signalLessRadio;
    private JRadioButton signalGreaterRadio;
    private JLabel networksCount;
    private JButton connectButton;
    private JButton disconnectButton;
    private Timer predictionTimer;
    private Timer recordingUiTimer;
    private Instant recordingStartedAt;
    private ClassificationPanel classificationPanel;

    public TableMonitor() {
        initializeComponents();
        setupUI();
        startRefreshTimer();
    }

    private void initializeComponents() {
        if (networksTable == null) {
            networksTable = new JTable();
        }
        if (mainPanel == null) {
            mainPanel = new JPanel(new BorderLayout());
        }
        scanner = new NetworkScanner();
        channelAnalyzer = new ChannelAnalyzer();
        classificationPanel = new ClassificationPanel();
        tableManager = new NetworkTableManager(networksTable);
        chartManager = new ChartManager(graphContainerPanel, channelPanel, channelPanel5,
                channelChartPanelContainer);
        selectionManager = new SelectionManager(tableManager, chartManager, detailsArea,
                channelAnalyzer, bssidArea, bssidComboBox);
        setupFilterListeners();
    }

    private void setupUI() {
        setContentPane(mainPanel);
        setTitle("Мониторинг беспроводных сетей");
        setSize(1600, 1200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setupReportButtons();
        applyModernPanelStyles();
        selectionManager.bssidsListener();
        if (tabbedPane3 != null) {
            tabbedPane3.addTab("Классификация", classificationPanel);
        }
        connectButton.addActionListener(e -> connectButtonListener());
        disconnectButton.addActionListener(e -> WifiConnector.disconnect());


        styleButton(trainButton, false);
        styleButton(predictButton, false);
        styleLabels(predictionLabel);
    }

    private void connectButtonListener() {
        String selectedSsid = tableManager.getSelectedSsid();
        NetworkInfo selectedNetwork = selectionManager.getSelectedNetwork();
        if (selectedSsid == null || selectedSsid.isEmpty() || selectedNetwork == null) {
            JOptionPane.showMessageDialog(this, "Сначала выберите сеть в таблице.");
            return;
        }
        String security = selectedNetwork.getAuthentication();

        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        panel.add(new JLabel("SSID:"));
        JLabel ssidLabel = new JLabel(selectedSsid);
        panel.add(ssidLabel);
        JPasswordField passwordField = null;
        panel.add(new JLabel("Пароль:"));
        if (!security.equals("OPEN")) {
            passwordField = new JPasswordField(20);
            panel.add(passwordField);
        } else {
            panel.add(new JLabel("Открытая сеть"));
        }
        styleLabels(ssidLabel);

        int option = JOptionPane.showConfirmDialog(
                this,
                panel,
                "Подключение к " + selectedSsid,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (option == JOptionPane.OK_OPTION) {
            String password;
            if (!security.equals("OPEN")) {
                password = new String(passwordField.getPassword());
            } else {
                password = null;
            }

            SwingWorker<Integer, Void> worker = new SwingWorker<>() {
                @Override
                protected Integer doInBackground() {
                    return WifiConnector.connectToWifi(selectedSsid, password, security);
                }

                @Override
                protected void done() {
                    try {
                        int result = get();
                        if (result == 0) {
                            JOptionPane.showMessageDialog(TableMonitor.this,
                                    "Подключение установлено!");
                        } else {
                            JOptionPane.showMessageDialog(TableMonitor.this,
                                    "Ошибка подключения. Код ошибки: " + result,
                                    "Ошибка", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            };
            worker.execute();
        }
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
                channel5Value, busiestChannelValue, bestChannelValue, bssidsCount, avgNetworksValue, recordingTimerLabel, logLabel, networksCount
        );

        styleTextArea(detailsArea, false);
        styleTextArea(bssidArea, false);
        styleTextArea(channelRecommendationArea, true);

        styleInput(channelFilterField);
        styleInput(signalFilterField);
        styleComboBox(securityFilterComboBox);
        styleComboBox(bssidComboBox);
        styleButton(applyFilterButton, true);
        styleButton(connectButton, true);
        styleButton(disconnectButton, false);
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
            label.setFont(new Font("Segoe UI", Font.PLAIN, 17));
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

        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primary ? ACCENT_COLOR : new Color(180, 190, 200), 1, true),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)
        ));

        button.setBackground(primary ? ACCENT_COLOR : new Color(233, 239, 248));
        button.setForeground(primary ? Color.WHITE : TITLE_COLOR);
    }

    private void startRefreshTimer() {
        refreshTimer = new Timer(1500, e -> loadNetworks());
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
        networksCount.setText("Количество сетей: " + networks.size());
        classificationPanel.updateClassification(networks);

        if (!flag) selectConnectedNetwork(networks);

        flag = true;
        channelAnalyzer.updateFromNetworks(new ArrayList<>(networks.values()));
        reportRecorder.record(networks, channelAnalyzer.getChannelStats());

        updateChannelStats();
        updateSelection();
        updateLabels();

        chartManager.updateCharts(networks.values(), channelAnalyzer.getChannelStats(),
                selectionManager.getSelectedSsid(), selectionManager.getSelectedBssid());

    }


    private void highlightRecommendedChannel(double[] prediction) {
        int[] channels = {1, 6, 11, 36, 40, 44, 48};
        int bestChannel = channels[0];
        double minLoad = prediction[0];

        for (int i = 1; i < channels.length; i++) {
            if (prediction[i] < minLoad) {
                minLoad = prediction[i];
                bestChannel = channels[i];
            }
        }

        if (channelLabel != null) {
            channelLabel.setForeground(bestChannel <= 14 ? new Color(0, 150, 0) : Color.BLUE);
            channelLabel.setToolTipText("Рекомендуемый канал: " + bestChannel + " (прогноз загрузки " +
                    String.format("%.0f", minLoad) + "%)");
        }
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
        fileChooser.setSelectedFile(new File(reportRecorder.buildDefaultFileName()));

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
        boolean signalGreater = signalGreaterRadio != null && signalGreaterRadio.isSelected();
        boolean signalLess = signalLessRadio != null && signalLessRadio.isSelected();

        tableManager.applyFilters(
                (String) securityFilterComboBox.getSelectedItem(),
                channelFilterField.getText(),
                signalFilterField.getText(),
                signalGreater,
                signalLess
        );
    }

    private void selectConnectedNetwork(Map<String, NetworkInfo> networks) {
        String connectedSsid = findConnectedSsid(networks);

        if (connectedSsid != null && selectionManager != null) {
            NetworkInfo connectedNetwork = networks.get(connectedSsid);
            if (connectedNetwork != null) {
                selectionManager.setSelectedNetwork(connectedSsid, connectedNetwork);
                tableManager.selectNetworkBySsid(connectedSsid);
            }
        }
    }

    private String findConnectedSsid(Map<String, NetworkInfo> networks) {
        for (NetworkInfo network : networks.values()) {
            if (network.getSsid() != null && network.getSsid().contains("(Подключено)")) {
                return network.getSsid();
            }
        }

        for (NetworkInfo network : networks.values()) {
            String connected = network.getConnectedSsid();
            if (connected != null && !connected.isEmpty()) {
                for (NetworkInfo net : networks.values()) {
                    String cleanName = net.getSsid().replace("(Подключено)", "").trim();
                    if (cleanName.equals(connected)) {
                        return net.getSsid();
                    }
                }
            }
        }
        return null;
    }


    private void resetFilters() {
        securityFilterComboBox.setSelectedIndex(0);
        channelFilterField.setText("");
        signalFilterField.setText("");

        if (signalGreaterRadio != null) {
            signalGreaterRadio.setSelected(true);
        }

        tableManager.resetFilters();
    }

    @Override
    public void dispose() {
        if (refreshTimer != null) {
            refreshTimer.stop();
        }
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

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayoutManager(14, 4, new Insets(15, 15, 15, 15), -1, -1));
        mainPanel.setMaximumSize(new Dimension(419, 188));
        mainPanel.setMinimumSize(new Dimension(322, 513));
        mainPanel.setPreferredSize(new Dimension(550, 513));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(5, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel1, new GridConstraints(0, 0, 13, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(-2104859)), null, TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION, this.$$$getFont$$$(null, -1, -1, panel1.getFont()), new Color(-2104859)));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel1.add(scrollPane1, new GridConstraints(2, 0, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(700, 250), null, null, 0, false));
        networksTable = new JTable();
        networksTable.setAutoCreateColumnsFromModel(true);
        networksTable.setAutoCreateRowSorter(false);
        networksTable.setAutoResizeMode(2);
        Font networksTableFont = this.$$$getFont$$$("JetBrains Mono SemiBold", Font.PLAIN, 12, networksTable.getFont());
        if (networksTableFont != null) networksTable.setFont(networksTableFont);
        scrollPane1.setViewportView(networksTable);
        filterPanel = new JPanel();
        filterPanel.setLayout(new GridLayoutManager(2, 6, new Insets(2, 2, 2, 2), -1, -1));
        panel1.add(filterPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, new Dimension(-1, 80), 0, false));
        channelFilterField = new JTextField();
        filterPanel.add(channelFilterField, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        signalFilterField = new JTextField();
        filterPanel.add(signalFilterField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        securityFilterComboBox = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement("Все");
        defaultComboBoxModel1.addElement("WPA2-Personal");
        defaultComboBoxModel1.addElement("WPA");
        defaultComboBoxModel1.addElement("OPEN");
        defaultComboBoxModel1.addElement("WPA3-Personal");
        securityFilterComboBox.setModel(defaultComboBoxModel1);
        filterPanel.add(securityFilterComboBox, new GridConstraints(1, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        applyFilterButton = new JButton();
        applyFilterButton.setActionCommand("Button");
        applyFilterButton.setLabel("Применить");
        applyFilterButton.setText("Применить");
        filterPanel.add(applyFilterButton, new GridConstraints(1, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        resetFilterButton = new JButton();
        resetFilterButton.setActionCommand("Button");
        resetFilterButton.setLabel("Отменить");
        resetFilterButton.setText("Отменить");
        filterPanel.add(resetFilterButton, new GridConstraints(1, 5, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        channelFilterLabel = new JLabel();
        channelFilterLabel.setText("Канал");
        filterPanel.add(channelFilterLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        signalFilterLabel = new JLabel();
        signalFilterLabel.setText("Уровень сигнала");
        filterPanel.add(signalFilterLabel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        securityFilterLabel = new JLabel();
        securityFilterLabel.setText("Безопасность");
        filterPanel.add(securityFilterLabel, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        filterPanel.add(panel2, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        signalLessRadio = new JRadioButton();
        signalLessRadio.setText("Сигнал меньше");
        panel2.add(signalLessRadio, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        signalGreaterRadio = new JRadioButton();
        signalGreaterRadio.setText("Сигнал больше");
        panel2.add(signalGreaterRadio, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        recordingPanel = new JPanel();
        recordingPanel.setLayout(new GridLayoutManager(1, 4, new Insets(5, 5, 5, 5), -1, -1));
        panel1.add(recordingPanel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        startRecordingButton = new JButton();
        startRecordingButton.setText("Начать запись");
        recordingPanel.add(startRecordingButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        stopAndSaveRecordingButton = new JButton();
        stopAndSaveRecordingButton.setText("Остановить и сохранить");
        recordingPanel.add(stopAndSaveRecordingButton, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        recordingPanel.add(panel3, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        recordingTimerLabel = new JLabel();
        recordingTimerLabel.setText("Label");
        panel3.add(recordingTimerLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        logLabel = new JLabel();
        logLabel.setText("Запись отчета:");
        panel3.add(logLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        recordingPanel.add(panel4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        networksCount = new JLabel();
        networksCount.setText("Количество сетей:");
        panel4.add(networksCount, new GridConstraints(0, 0, 2, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        tabbedPane3 = new JTabbedPane();
        panel1.add(tabbedPane3, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(-1, 500), new Dimension(200, 200), null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(4, 6, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane3.addTab("Подробная информация о сети", panel5);
        channelRecommendationArea = new JTextArea();
        channelRecommendationArea.setEditable(false);
        Font channelRecommendationAreaFont = this.$$$getFont$$$("JetBrains Mono SemiBold", -1, 14, channelRecommendationArea.getFont());
        if (channelRecommendationAreaFont != null) channelRecommendationArea.setFont(channelRecommendationAreaFont);
        channelRecommendationArea.setMargin(new Insets(5, 5, 5, 5));
        panel5.add(channelRecommendationArea, new GridConstraints(0, 2, 4, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), new Dimension(250, -1), 0, false));
        final JScrollPane scrollPane2 = new JScrollPane();
        panel5.add(scrollPane2, new GridConstraints(0, 0, 4, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        detailsArea = new JTextArea();
        detailsArea.setEditable(false);
        Font detailsAreaFont = this.$$$getFont$$$("JetBrains Mono SemiBold", -1, 14, detailsArea.getFont());
        if (detailsAreaFont != null) detailsArea.setFont(detailsAreaFont);
        detailsArea.setMaximumSize(new Dimension(2147483647, 200));
        scrollPane2.setViewportView(detailsArea);
        bssidComboBox = new JComboBox();
        panel5.add(bssidComboBox, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel5.add(spacer1, new GridConstraints(1, 5, 3, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel5.add(spacer2, new GridConstraints(0, 3, 4, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        panel5.add(spacer3, new GridConstraints(0, 1, 4, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        bssidArea = new JTextArea();
        bssidArea.setEditable(false);
        Font bssidAreaFont = this.$$$getFont$$$("JetBrains Mono SemiBold", -1, 14, bssidArea.getFont());
        if (bssidAreaFont != null) bssidArea.setFont(bssidAreaFont);
        bssidArea.setMargin(new Insets(5, 5, 5, 5));
        panel5.add(bssidArea, new GridConstraints(1, 4, 3, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), new Dimension(200, -1), 0, false));
        tabbedPane1 = new JTabbedPane();
        mainPanel.add(tabbedPane1, new GridConstraints(0, 3, 12, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(4, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane1.addTab("Информация о сети", panel6);
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new BorderLayout(0, 0));
        panel6.add(panel7, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        channelStatsPanel = new JPanel();
        channelStatsPanel.setLayout(new GridLayoutManager(7, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel7.add(channelStatsPanel, BorderLayout.WEST);
        channelStatsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLoweredBevelBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        channelTotalValue = new JLabel();
        Font channelTotalValueFont = this.$$$getFont$$$("JetBrains Mono SemiBold", Font.BOLD, 16, channelTotalValue.getFont());
        if (channelTotalValueFont != null) channelTotalValue.setFont(channelTotalValueFont);
        channelTotalValue.setText("Сеть не выбрана");
        channelStatsPanel.add(channelTotalValue, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        channel24Value = new JLabel();
        Font channel24ValueFont = this.$$$getFont$$$("JetBrains Mono SemiBold", Font.BOLD, 16, channel24Value.getFont());
        if (channel24ValueFont != null) channel24Value.setFont(channel24ValueFont);
        channel24Value.setText("Сеть не выбрана");
        channelStatsPanel.add(channel24Value, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        channel5Value = new JLabel();
        Font channel5ValueFont = this.$$$getFont$$$("JetBrains Mono SemiBold", Font.BOLD, 16, channel5Value.getFont());
        if (channel5ValueFont != null) channel5Value.setFont(channel5ValueFont);
        channel5Value.setText("Сеть не выбрана");
        channelStatsPanel.add(channel5Value, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        busiestChannelValue = new JLabel();
        Font busiestChannelValueFont = this.$$$getFont$$$("JetBrains Mono SemiBold", Font.BOLD, 16, busiestChannelValue.getFont());
        if (busiestChannelValueFont != null) busiestChannelValue.setFont(busiestChannelValueFont);
        busiestChannelValue.setText("Сеть не выбрана");
        channelStatsPanel.add(busiestChannelValue, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        bestChannelValue = new JLabel();
        Font bestChannelValueFont = this.$$$getFont$$$("JetBrains Mono SemiBold", Font.BOLD, 16, bestChannelValue.getFont());
        if (bestChannelValueFont != null) bestChannelValue.setFont(bestChannelValueFont);
        bestChannelValue.setText("Сеть не выбрана");
        channelStatsPanel.add(bestChannelValue, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        avgNetworksValue = new JLabel();
        Font avgNetworksValueFont = this.$$$getFont$$$("JetBrains Mono SemiBold", Font.BOLD, 16, avgNetworksValue.getFont());
        if (avgNetworksValueFont != null) avgNetworksValue.setFont(avgNetworksValueFont);
        avgNetworksValue.setText("Сеть не выбрана");
        channelStatsPanel.add(avgNetworksValue, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        networkPanel = new JPanel();
        networkPanel.setLayout(new GridLayoutManager(7, 1, new Insets(10, 30, 10, 30), -1, -1));
        networkPanel.putClientProperty("html.disable", Boolean.FALSE);
        panel7.add(networkPanel, BorderLayout.CENTER);
        networkPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLoweredBevelBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        ssidLabel = new JLabel();
        Font ssidLabelFont = this.$$$getFont$$$("JetBrains Mono SemiBold", Font.BOLD, 16, ssidLabel.getFont());
        if (ssidLabelFont != null) ssidLabel.setFont(ssidLabelFont);
        ssidLabel.setText("Сеть не выбрана");
        networkPanel.add(ssidLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        signalLabel = new JLabel();
        Font signalLabelFont = this.$$$getFont$$$("JetBrains Mono SemiBold", Font.BOLD, 16, signalLabel.getFont());
        if (signalLabelFont != null) signalLabel.setFont(signalLabelFont);
        signalLabel.setText("Сеть не выбрана");
        networkPanel.add(signalLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        securityLabel = new JLabel();
        Font securityLabelFont = this.$$$getFont$$$("JetBrains Mono SemiBold", Font.BOLD, 16, securityLabel.getFont());
        if (securityLabelFont != null) securityLabel.setFont(securityLabelFont);
        securityLabel.setText("Сеть не выбрана");
        networkPanel.add(securityLabel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        bssidLabel = new JLabel();
        Font bssidLabelFont = this.$$$getFont$$$("JetBrains Mono SemiBold", Font.BOLD, 16, bssidLabel.getFont());
        if (bssidLabelFont != null) bssidLabel.setFont(bssidLabelFont);
        bssidLabel.setText("Сеть не выбрана");
        networkPanel.add(bssidLabel, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        channelLabel = new JLabel();
        Font channelLabelFont = this.$$$getFont$$$("JetBrains Mono SemiBold", Font.BOLD, 16, channelLabel.getFont());
        if (channelLabelFont != null) channelLabel.setFont(channelLabelFont);
        channelLabel.setText("Сеть не выбрана");
        networkPanel.add(channelLabel, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        bssidsCount = new JLabel();
        Font bssidsCountFont = this.$$$getFont$$$("JetBrains Mono SemiBold", Font.BOLD, 16, bssidsCount.getFont());
        if (bssidsCountFont != null) bssidsCount.setFont(bssidsCountFont);
        bssidsCount.setText("Сеть не выбрана");
        networkPanel.add(bssidsCount, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        tabbedPane2 = new JTabbedPane();
        panel6.add(tabbedPane2, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        graphContainerPanel = new JPanel();
        graphContainerPanel.setLayout(new BorderLayout(0, 0));
        tabbedPane2.addTab("График уровня сигнала", graphContainerPanel);
        channelChartPanelContainer = new JPanel();
        channelChartPanelContainer.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane2.addTab("График загрузки каналов", channelChartPanelContainer);
        connectButton = new JButton();
        connectButton.setText("Подключиться");
        panel6.add(connectButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        disconnectButton = new JButton();
        disconnectButton.setText("Отключиться");
        panel6.add(disconnectButton, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane1.addTab("Графики перекрытий", panel8);
        channelPanel = new JPanel();
        channelPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel8.add(channelPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(600, -1), null, null, 0, false));
        channelPanel5 = new JPanel();
        channelPanel5.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel8.add(channelPanel5, new GridConstraints(1, 0, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(600, -1), null, null, 1, false));
        ButtonGroup buttonGroup;
        buttonGroup = new ButtonGroup();
        buttonGroup.add(signalGreaterRadio);
        buttonGroup.add(signalLessRadio);
    }

    /**
     * @noinspection ALL
     */
    private Font $$$getFont$$$(String fontName, int style, int size, Font currentFont) {
        if (currentFont == null) return null;
        String resultName;
        if (fontName == null) {
            resultName = currentFont.getName();
        } else {
            Font testFont = new Font(fontName, Font.PLAIN, 10);
            if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
                resultName = fontName;
            } else {
                resultName = currentFont.getName();
            }
        }
        Font font = new Font(resultName, style >= 0 ? style : currentFont.getStyle(), size >= 0 ? size : currentFont.getSize());
        boolean isMac = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH).startsWith("mac");
        Font fontWithFallback = isMac ? new Font(font.getFamily(), font.getStyle(), font.getSize()) : new StyleContext().getFont(font.getFamily(), font.getStyle(), font.getSize());
        return fontWithFallback instanceof FontUIResource ? fontWithFallback : new FontUIResource(fontWithFallback);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

}