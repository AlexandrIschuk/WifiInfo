import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import javax.swing.RowFilter;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;


public class TableMonitor extends JFrame {
    // Существующие поля
    private DatabaseManager dbManager;
    private JLabel ssidLabel;
    private JPanel mainPanel;
    private JLabel signalLabel;
    private JLabel securityLabel;
    private JLabel bssidLabel;
    private JLabel channelLabel;
    private JTable networksTable;
    private JTextArea detailsArea;
    private JPanel graphContainerPanel;
    private DefaultTableModel tableModel;
    private Timer refreshTimer;
    private Map<String, NetworkInfo> networkCache = new LinkedHashMap<>();
    private String currentlySelectedSsid = null;

    // Поля для графика
    private TimeSeries signalTimeSeries;
    private JFreeChart signalChart;

    private ChartPanel chartPanel;
    private static final int MAX_DATA_POINTS = 60;

    private ChannelAnalyzer channelAnalyzer;
    private JFreeChart channelBarChart;
    private ChartPanel channelChartPanel;
    private DefaultCategoryDataset channelDataset;

    private JButton applyFilterButton;
    private JButton resetFilterButton;
    private JPanel filterPanel;
    private JTextField channelFilterField;
    private JTextField signalFilterField;
    private JComboBox securityFilterComboBox;
    private JLabel channelFilterLabel;
    private JLabel signalFilterLabel;
    private JLabel securityFilterLabel;
    private JPanel channelChartPanelContainer;
    private JPanel channelStatsPanel;
    private JLabel channelTotalValue;
    private JLabel channel24Value;
    private JLabel channel5Value;
    private JLabel busiestChannelValue;
    private JLabel bestChannelValue;
    private JLabel avgNetworksValue;
    private JTextArea channelRecommendationArea;

    private ListSelectionListener selectionListener;

    private TableRowSorter<DefaultTableModel> sorter;
    private JLabel networksCountLabel;

    public TableMonitor() {
        DefaultCaret caret = (DefaultCaret)detailsArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);

        // Настраиваем таблицу и фильтры
        setupTableAndFilters();

        // Настраиваем график
        initChartPanel();
        channelAnalyzer = new ChannelAnalyzer();

        // Инициализация компонентов анализа каналов (если они есть в форме)
        initChannelAnalysisComponents();
    }
    private void initChannelAnalysisComponents() {
        // Проверяем, существуют ли компоненты в форме
        if (channelChartPanelContainer != null) {
            initChannelChart();
        }
    }


    private void initChannelChart() {
        // Создаем dataset для гистограммы
        channelDataset = new DefaultCategoryDataset();

        // Создаем гистограмму
        channelBarChart = ChartFactory.createBarChart(
                "Загруженность каналов Wi-Fi",
                "Канал",
                "Количество сетей",
                channelDataset,
                PlotOrientation.VERTICAL,
                true,  // Легенда
                true,  // Tooltips
                false  // URLs
        );

        // Настраиваем внешний вид
        CategoryPlot plot = channelBarChart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        rangeAxis.setAutoRangeIncludesZero(true);

        // Опционально: устанавливаем шаг 1
        rangeAxis.setTickUnit(new NumberTickUnit(1));

        // Запрещаем дробные значения

        // Настраиваем рендерер для столбцов
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setMaximumBarWidth(0.1);
        renderer.setDrawBarOutline(true);
        renderer.setSeriesPaint(0, new Color(70, 130, 180));

        // Создаем панель для графика
        channelChartPanel = new ChartPanel(channelBarChart);
        channelChartPanel.setPreferredSize(new Dimension(800, 400));

        // Добавляем график в контейнер формы
        channelChartPanelContainer.setLayout(new BorderLayout());
        channelChartPanelContainer.add(channelChartPanel, BorderLayout.CENTER);
    }

    private void updateChannelAnalysis() {
        // Получаем статистику каналов
        Map<Integer, ChannelInfo> channelStats = channelAnalyzer.getChannelStats();

        if (channelDataset != null) {
            // Очищаем dataset
            channelDataset.clear();

            int totalChannels = 0;
            int channels24 = 0;
            int channels5 = 0;
            int busiestChannel = -1;
            int maxNetworks = 0;
            int bestChannel = -1;
            int minNetworks = Integer.MAX_VALUE;
            double totalNetworks = 0;

            // Сортируем каналы по номеру
            List<Integer> sortedChannels = new ArrayList<>(channelStats.keySet());
            Collections.sort(sortedChannels);

            // Обновляем график и собираем статистику
            for (Integer channel : sortedChannels) {
                ChannelInfo info = channelStats.get(channel);

                // Добавляем в график
                channelDataset.addValue(info.getNetworkCount(), "Сети", channel.toString());

                // Собираем статистику
                totalChannels++;
                totalNetworks += info.getNetworkCount();

                if (channel <= 14) {
                    channels24++;
                } else {
                    channels5++;
                }

                // Самый загруженный канал
                if (info.getNetworkCount() > maxNetworks) {
                    maxNetworks = info.getNetworkCount();
                    busiestChannel = channel;
                }

                // Наименее загруженный канал (рекомендуемый)
                if (info.getNetworkCount() < minNetworks && info.getNetworkCount() >= 0) {
                    minNetworks = info.getNetworkCount();
                    bestChannel = channel;
                }
            }

            // Обновляем статистические метки в форме
            updateChannelStatsLabels(totalChannels, channels24, channels5,
                    busiestChannel, bestChannel, totalNetworks / totalChannels);

            // Обновляем рекомендации
            updateChannelRecommendations(channelStats, busiestChannel, bestChannel);

            // Обновляем цвета на графике
            //updateChannelChartColors(channelStats);
        }
    }


    private void updateChannelStatsLabels(int totalChannels, int channels24, int channels5,
                                          int busiestChannel, int bestChannel, double avgNetworks) {
        if (channelTotalValue != null) channelTotalValue.setText("Всего каналов: " + totalChannels);
        if (channel24Value != null) channel24Value.setText("2.4 GHz каналы: " + channels24);
        if (channel5Value != null) channel5Value.setText("5 GHz каналы: " + channels5);

        if (busiestChannelValue != null) {
            busiestChannelValue.setText("Самый Загруженный: " + (busiestChannel > 0 ?
                    "Канал " + busiestChannel : "-"));
        }

        if (bestChannelValue != null) {
            bestChannelValue.setText("Рекомендуемый: " + (bestChannel > 0 ?
                    "Канал " + bestChannel : "-"));
        }

        if (avgNetworksValue != null) {
            avgNetworksValue.setText("Среднее сетей/канал: " + String.format("%.1f", avgNetworks));
        }
    }

    private void updateChannelRecommendations(Map<Integer, ChannelInfo> channelStats,
                                              int busiestChannel, int bestChannel) {
        if (channelRecommendationArea != null) {
            StringBuilder rec = new StringBuilder();
            rec.append("📡 РЕКОМЕНДАЦИИ ПО КАНАЛАМ:\n\n");

            if (bestChannel > 0) {
                rec.append("✓ Рекомендуемый канал: ").append(bestChannel).append("\n");
                rec.append("   Нагрузка: ").append(channelStats.get(bestChannel).getNetworkCount())
                        .append(" сетей\n");
            }

            if (busiestChannel > 0 && busiestChannel != bestChannel) {
                rec.append("✗ Избегайте канала: ").append(busiestChannel).append("\n");
                rec.append("   Нагрузка: ").append(channelStats.get(busiestChannel).getNetworkCount())
                        .append(" сетей\n");
            }

            // Анализ 2.4GHz диапазона
            List<Integer> channels24 = new ArrayList<>();
            for (Integer channel : channelStats.keySet()) {
                if (channel <= 14) channels24.add(channel);
            }

            if (!channels24.isEmpty()) {
                rec.append("\n📶 2.4GHz диапазон:\n");
                for (Integer channel : Arrays.asList(1, 6, 11)) { // Стандартные каналы
                    ChannelInfo info = channelStats.get(channel);
                    if (info != null) {
                        rec.append("   Канал ").append(channel).append(": ")
                                .append(info.getNetworkCount()).append(" сетей\n");
                    }
                }
            }

            channelRecommendationArea.setText(rec.toString());
        }
    }

    private void setupTableAndFilters() {
        String[] columnNames = {"SSID", "Сигнал (%)", "Канал", "Безопасность", "MAC-адрес"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        networksTable.setModel(tableModel);
        networksTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Настраиваем сортировщик строк для фильтрации
        sorter = new TableRowSorter<>(tableModel);
        networksTable.setRowSorter(sorter);

        JTableHeader header = networksTable.getTableHeader();
        header.setFont(new Font("SansSerif", Font.BOLD, 14));
        header.setBackground(new Color(70, 130, 180));
        header.setForeground(Color.WHITE);

        TableColumnModel columnModel = networksTable.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(200);
        columnModel.getColumn(1).setPreferredWidth(80);
        columnModel.getColumn(2).setPreferredWidth(60);
        columnModel.getColumn(3).setPreferredWidth(120);
        columnModel.getColumn(4).setPreferredWidth(150);

        // Настраиваем обработчики для кнопок фильтров
        setupFilterListeners();

        // Создаем метку для счетчика сетей
        networksCountLabel = new JLabel("Найдено сетей: 0");
        networksCountLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        // Добавляем счетчик в интерфейс
        if (mainPanel != null) {
            // Ищем контейнер для таблицы
            Component[] components = mainPanel.getComponents();
            for (Component comp : components) {
                if (comp instanceof JScrollPane) {
                    JScrollPane scrollPane = (JScrollPane) comp;
                    // Создаем панель для таблицы с фильтрами и счетчиком
                    JPanel tableWrapper = new JPanel(new BorderLayout());
                    tableWrapper.add(filterPanel, BorderLayout.NORTH);
                    tableWrapper.add(scrollPane, BorderLayout.CENTER);
                    tableWrapper.add(networksCountLabel, BorderLayout.SOUTH);

                    // Заменяем ScrollPane на новую панель
                    mainPanel.remove(scrollPane);
                    mainPanel.add(tableWrapper, BorderLayout.CENTER);
                    break;
                }
            }
        }
    }

    private void setupFilterListeners() {
        // Обработчик для кнопки применения фильтров
        applyFilterButton.addActionListener(e -> applyFilters());

        // Обработчик для кнопки сброса фильтров
        resetFilterButton.addActionListener(e -> resetFilters());

        // Обработчики для полей ввода (применяем фильтры при нажатии Enter)
        channelFilterField.addActionListener(e -> applyFilters());
        signalFilterField.addActionListener(e -> applyFilters());
    }

    private void applyFilters() {
        List<RowFilter<DefaultTableModel, Integer>> filters = new ArrayList<>();

        String[] items = {"Все","WPA2","WPA","WEP","Open","WPA3"};


        // Фильтр по типу безопасности
        String selectedSecurity = (String) securityFilterComboBox.getSelectedItem();
        if (selectedSecurity != null && !selectedSecurity.equals("Все")) {
            filters.add(RowFilter.regexFilter("(?i)" + selectedSecurity, 3)); // Колонка "Безопасность"
        }

        // Фильтр по каналу
        String channelText = channelFilterField.getText().trim();
        if (!channelText.isEmpty()) {
            try {
                int channel = Integer.parseInt(channelText);
                filters.add(RowFilter.regexFilter("^" + channel + "$", 2)); // Колонка "Канал"
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this,
                        "Некорректный номер канала. Введите число.",
                        "Ошибка фильтра", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        // Фильтр по уровню сигнала
        String signalText = signalFilterField.getText().trim();
        if (!signalText.isEmpty()) {
            try {
                int minSignal = Integer.parseInt(signalText);
                if (minSignal < 0 || minSignal > 100) {
                    JOptionPane.showMessageDialog(this,
                            "Уровень сигнала должен быть от 0 до 100%",
                            "Ошибка фильтра", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Создаем фильтр для сигнала больше указанного значения
                filters.add(new RowFilter<DefaultTableModel, Integer>() {
                    @Override
                    public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                        String signalStr = (String) entry.getValue(1); // Колонка "Сигнал (%)"
                        if (signalStr != null && signalStr.endsWith("%")) {
                            try {
                                int signalValue = Integer.parseInt(signalStr.replace("%", "").trim());
                                return signalValue >= minSignal;
                            } catch (NumberFormatException e) {
                                return false;
                            }
                        }
                        return false;
                    }
                });
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this,
                        "Некорректный уровень сигнала. Введите число от 0 до 100.",
                        "Ошибка фильтра", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        // Применяем все фильтры
        if (!filters.isEmpty()) {
            sorter.setRowFilter(RowFilter.andFilter(filters));
        } else {
            sorter.setRowFilter(null);
        }

        // Обновляем счетчик сетей
        updateNetworksCount();
    }

    private void resetFilters() {
        securityFilterComboBox.setSelectedIndex(0); // "Все"
        channelFilterField.setText("");
        signalFilterField.setText("");
        sorter.setRowFilter(null);
        updateNetworksCount();
    }

    private void updateNetworksCount() {
        if (networksCountLabel != null && sorter != null) {
            int visibleCount = sorter.getViewRowCount();
            networksCountLabel.setText("Найдено сетей: " + visibleCount);
        }
    }

    private void initChartPanel() {
        // Создаем временной ряд для графика сигнала
        signalTimeSeries = new TimeSeries("Уровень сигнала");

        // Создаем датасет
        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(signalTimeSeries);

        // Создаем график
        signalChart = ChartFactory.createTimeSeriesChart(
                "График уровня сигнала Wi-Fi",
                "Время",
                "Уровень сигнала (%)",
                dataset,
                true,
                true,
                false
        );

        // Настраиваем внешний вид графика
        signalChart.setBackgroundPaint(Color.WHITE);

        XYPlot plot = signalChart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesPaint(0, new Color(44, 102, 230));
        renderer.setSeriesStroke(0, new BasicStroke(2.0f));
        renderer.setSeriesShapesVisible(0, true);
        renderer.setSeriesShape(0, new java.awt.geom.Ellipse2D.Double(-3, -3, 6, 6));
        plot.setRenderer(renderer);

        ValueAxis domainAxis = plot.getDomainAxis();
        domainAxis.setAutoRange(true);
        domainAxis.setFixedAutoRange(60000.0);

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setRange(0.0, 100.0);

        // Создаем панель для графика
        chartPanel = new ChartPanel(signalChart);
        chartPanel.setPreferredSize(new Dimension(500, 300));
        chartPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                "График уровня сигнала",
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                new Font("SansSerif", Font.BOLD, 12),
                Color.BLUE
        ));

        // Добавляем панель графика в интерфейс
        if (graphContainerPanel != null) {
            graphContainerPanel.add(chartPanel, BorderLayout.CENTER);
        }
    }



    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TableMonitor wf = new TableMonitor();
            wf.setContentPane(wf.mainPanel);
            wf.setTitle("Мониторинг беспроводных сетей");
            wf.setSize(1800, 1200);
            wf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            wf.setLocationRelativeTo(null);


            wf.setVisible(true);
            wf.setupTimer();
            wf.loadNetworks();
        });
    }

    private void setupTimer() {
        refreshTimer = new Timer(500, e -> { // 2 секунды вместо 100 мс
            try {
                // Принудительное сканирование перед обновлением
                new ProcessBuilder("netsh", "wlan", "scan","force=true").start().waitFor();
                loadNetworks();
                if (currentlySelectedSsid != null) {
                    analyzeSelectedNetwork();
                    updateSignalHistory();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        refreshTimer.setInitialDelay(0);
        refreshTimer.start();
    }

    private void loadNetworks() {
        new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                List<String> networks = scanAvailableNetworks();
                channelAnalyzer.updateFromNetworks(new ArrayList<>(networkCache.values()));
                return networks;
            }

            @Override
            protected void done() {
                try {
                    List<String> networks = get();

                    // Получаем текущее выделение ДО обновления
                    int selectedViewRow = networksTable.getSelectedRow();
                    String selectedSsid = null;

                    if (selectedViewRow >= 0) {
                        // Конвертируем индекс представления в индекс модели
                        int modelRow = networksTable.convertRowIndexToModel(selectedViewRow);
                        selectedSsid = (String) tableModel.getValueAt(modelRow, 0);
                    }

                    // Обновляем таблицу БЕЗ полной очистки
                    updateTableModel();

                    // Восстанавливаем выделение
                    if (selectedSsid != null) {
                        restoreSelection(selectedSsid);
                    }

                    updateChannelAnalysis();
                    updateNetworksCount();

                } catch (Exception e) {
                    JOptionPane.showMessageDialog(TableMonitor.this,
                            "Ошибка сканирования: " + e.getMessage(),
                            "Ошибка", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void updateTableModel() {
        // Временно отключаем слушатели выделения

        networksTable.getSelectionModel().removeListSelectionListener(selectionListener);

        // Сохраняем текущий набор строк
        Set<String> existingSsids = new HashSet<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            existingSsids.add((String) tableModel.getValueAt(i, 0));
        }

        // Добавляем новые сети
        for (NetworkInfo info : networkCache.values()) {
            String ssid = info.getSsid();
            if (!existingSsids.contains(ssid)) {
                addNetworkToTable(info);
            } else {
                // Обновляем существующую строку
                updateNetworkRow(info);
            }
        }

        // Удаляем сети, которых больше нет
        removeMissingNetworks();

        // Включаем слушатели обратно
        networksTable.getSelectionModel().addListSelectionListener(selectionListener);
    }

    private void addNetworkToTable(NetworkInfo info) {
        String primaryBssid = info.getBssids().isEmpty() ? "" : info.getBssids().get(0);
        tableModel.addRow(new Object[]{
                info.getSsid(),
                info.getSignalStrength() + "%",
                info.getChannel(),
                info.getAuthentication(),
                primaryBssid
        });
    }

    private void updateNetworkRow(NetworkInfo info) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (info.getSsid().equals(tableModel.getValueAt(i, 0))) {
                String primaryBssid = info.getBssids().isEmpty() ? "" : info.getBssids().get(0);
                tableModel.setValueAt(info.getSignalStrength() + "%", i, 1);
                tableModel.setValueAt(info.getChannel(), i, 2);
                tableModel.setValueAt(info.getAuthentication(), i, 3);
                tableModel.setValueAt(primaryBssid, i, 4);
                break;
            }
        }
    }

    private void removeMissingNetworks() {
        for (int i = tableModel.getRowCount() - 1; i >= 0; i--) {
            String ssid = (String) tableModel.getValueAt(i, 0);
            if (!networkCache.containsKey(ssid)) {
                tableModel.removeRow(i);
            }
        }
    }

    private void restoreSelection(String selectedSsid) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (selectedSsid.equals(tableModel.getValueAt(i, 0))) {
                int viewIndex = networksTable.convertRowIndexToView(i);
                if (viewIndex >= 0) {
                    // Отключаем автоматическое скроллирование
                    networksTable.setRowSelectionInterval(viewIndex, viewIndex);

                    // Плавный скроллинг без скачков
                    Rectangle cellRect = networksTable.getCellRect(viewIndex, 0, true);
                    networksTable.scrollRectToVisible(cellRect);

                    currentlySelectedSsid = selectedSsid;
                }
                break;
            }
        }
    }

    private List<String> scanAvailableNetworks() throws IOException, InterruptedException {
        Map<String, NetworkInfo> newCache = new LinkedHashMap<>();
        Process process = new ProcessBuilder("netsh", "wlan", "show", "networks", "mode=bssid").start();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), "CP866"))) {

            String currentSsid = null;
            NetworkInfo currentInfo = null;
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.startsWith("SSID") && line.contains(":")) {
                    if (currentSsid != null && currentInfo != null) {
                        newCache.put(currentSsid, currentInfo);
                    }
                    String[] parts = line.split(":", 2);
                    currentSsid = parts.length > 1 ? parts[1].trim() : "";
                    if (!currentSsid.isEmpty()) {
                        currentInfo = new NetworkInfo(currentSsid);
                    }
                }
                else if (currentSsid != null && currentInfo != null) {
                    if (line.startsWith("BSSID")  && line.contains(":")) {
                        String[] parts = line.split(":", 2);
                        if (parts.length > 1) currentInfo.addBssid(parts[1].trim().toUpperCase());
                    }
                    else if ((line.startsWith("Сигнал") || line.startsWith("Signal")) && line.contains(":")) {
                        String[] parts = line.split(":");
                        if (parts.length > 1) {
                            String signalStr = parts[1].replace("%", "").trim();
                            currentInfo.setSignalStrength(signalStr);
                        }
                    }
                    else if ((line.startsWith("Проверка") || line.startsWith("Authentication")) && line.contains(":")) {
                        String[] parts = line.split(":", 2);
                        if (parts.length > 1) currentInfo.setAuthentication(parts[1].trim());
                    }
                    else if ((line.startsWith("Шифр") || line.startsWith("Cipher")) && line.contains(":")) {
                        String[] parts = line.split(":", 2);
                        if (parts.length > 1) currentInfo.setCipher(parts[1].trim());
                    }
                    else if ((line.startsWith("Канал") || line.startsWith("Channel")) && line.contains(":")) {
                        String[] parts = line.split(":");
                        if (parts.length > 1) currentInfo.setChannel(parts[1].trim());
                    }
                    else if ((line.startsWith("Тип радио") || line.startsWith("Radio type")) && line.contains(":")) {
                        String[] parts = line.split(":", 2);
                        if (parts.length > 1) currentInfo.setRadioType(parts[1].trim());
                    }
                    else if ((line.startsWith("Диапазон") || line.startsWith("Band")) && line.contains(":")) {
                        String[] parts = line.split(":", 2);
                        if (parts.length > 1) currentInfo.setBand(parts[1].trim());
                    }
                    else if ((line.startsWith("Базовая скорость") || line.startsWith("Basic rates")) && line.contains(":")) {
                        String[] parts = line.split(":", 2);
                        if (parts.length > 1) currentInfo.setBasicRates(parts[1].trim());
                    }
                    else if ((line.startsWith("Другие скорости") || line.startsWith("Other rates")) && line.contains(":")) {
                        String[] parts = line.split(":", 2);
                        if (parts.length > 1) currentInfo.setOtherRates(parts[1].trim());
                    }
                    else if ((line.startsWith("Тип сети") || line.startsWith("Network type")) && line.contains(":")) {
                        String[] parts = line.split(":", 2);
                        if (parts.length > 1) currentInfo.setNetworkType(parts[1].trim());
                    }
                }
            }
            if (currentSsid != null && currentInfo != null) {
                newCache.put(currentSsid, currentInfo);
            }
        }
        networkCache = newCache;
        return null;
    }

    private void analyzeSelectedNetwork() {
        if (currentlySelectedSsid == null) return;

        NetworkInfo info = networkCache.get(currentlySelectedSsid);
        if (info == null) return;

        SwingUtilities.invokeLater(() -> {
            if (ssidLabel != null) ssidLabel.setText("Сеть: " + info.getSsid());
            if (!info.getSignalStrength().isEmpty()) {
                try {
                    int percent = Integer.parseInt(info.getSignalStrength());
                    int dbm = (percent / 2) - 100;
                    if (signalLabel != null) signalLabel.setText(String.format("Сигнал: %d%% (~%d dBm)", percent, dbm));
                } catch (NumberFormatException e) {
                    if (signalLabel != null) signalLabel.setText("Сигнал: " + info.getSignalStrength() + "%");
                }
            } else {
                if (signalLabel != null) signalLabel.setText("Сигнал: -");
            }
            if (securityLabel != null) securityLabel.setText("Безопасность: " +
                    (info.getAuthentication().isEmpty() ? "-" : info.getAuthentication()));
            if (bssidLabel != null) bssidLabel.setText("MAC-адрес: " +
                    (info.getBssids().isEmpty() ? "-" : info.getBssids().get(0)));
            if (channelLabel != null) channelLabel.setText("Канал: " +
                    (info.getChannel().isEmpty() ? "-" : info.getChannel()));
            if (detailsArea != null) detailsArea.setText(info.getDetailedInfo());

            if (detailsArea != null) {
                String details = info.getDetailedInfo();

                // Добавляем информацию о канале
                if (!info.getChannel().isEmpty()) {
                    try {
                        int channel = Integer.parseInt(info.getChannel());
                        details += "\n\n📶 АНАЛИЗ КАНАЛА " + channel + ":\n";

                        ChannelInfo channelInfo = channelAnalyzer.getChannelStats().get(channel);
                        if (channelInfo != null) {
                            details += "• Сетей на канале: " + channelInfo.getNetworkCount() + "\n";
                            details += String.format("• Средний сигнал: %.1f%%\n", channelInfo.getAverageSignal());

                            // Рекомендация
                            if (channelInfo.getNetworkCount() == 0) {
                                details += "✅ Канал свободен\n";
                            } else if (channelInfo.getNetworkCount() < 3) {
                                details += "🟡 Канал слабо загружен\n";
                            } else if (channelInfo.getNetworkCount() < 6) {
                                details += "🟠 Канал умеренно загружен\n";
                            } else {
                                details += "🔴 Канал сильно загружен\n";
                            }
                        }
                    } catch (NumberFormatException e) {
                        // Пропускаем
                    }
                }

                detailsArea.setText(details);
            }
        });
    }

    private void updateSignalHistory() {
        if (currentlySelectedSsid == null) return;

        NetworkInfo info = networkCache.get(currentlySelectedSsid);
        if (info == null || info.getSignalStrength().isEmpty()) return;

        try {
            int currentSignal = Integer.parseInt(info.getSignalStrength());

            if (signalTimeSeries == null) {
                signalTimeSeries = new TimeSeries(currentlySelectedSsid);
                TimeSeriesCollection dataset = new TimeSeriesCollection();
                dataset.addSeries(signalTimeSeries);
                if (signalChart != null) {
                    signalChart.getXYPlot().setDataset(dataset);
                }
            } else {
                if (!signalTimeSeries.getKey().equals(currentlySelectedSsid)) {
                    signalTimeSeries.clear();
                    signalTimeSeries.setKey(currentlySelectedSsid);
                }
            }

            Second currentSecond = new Second(new java.util.Date());
            signalTimeSeries.addOrUpdate(currentSecond, currentSignal);

            if (signalTimeSeries.getItemCount() > MAX_DATA_POINTS) {
                signalTimeSeries.delete(0, 0);
            }

        } catch (NumberFormatException e) {
            // Игнорируем некорректные значения
        }
    }

    private String getBandInfo(int channel) {
        if (channel <= 14) {
            return "2.4GHz (стандартный диапазон)";
        } else if (channel <= 64) {
            return "5GHz (UNII-1)";
        } else if (channel <= 140) {
            return "5GHz (UNII-2)";
        } else {
            return "5GHz (UNII-3)";
        }
    }

    @Override
    public void dispose() {
        if (refreshTimer != null) {
            refreshTimer.stop();
        }
        super.dispose();
    }
}


