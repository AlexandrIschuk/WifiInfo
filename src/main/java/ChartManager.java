import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.block.LineBorder;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class ChartManager {
    private static final Color CHART_SURFACE = new Color(255, 255, 255);
    private static final Color CHART_CANVAS = new Color(248, 250, 253);
    private static final Color CHART_GRID = new Color(220, 228, 238);
    private static final Color CHART_AXIS = new Color(97, 109, 126);
    private static final Color CHART_TEXT = new Color(31, 41, 55);
    private static final Color SIGNAL_LINE = new Color(53, 117, 246);
    private static final Color SIGNAL_FILL = new Color(53, 117, 246, 56);
    private static final Color CHANNEL_BAR = new Color(74, 137, 255);
    private static final Color CHANNEL_BAR_EDGE = new Color(38, 93, 198);
    private static final Color SELECTED_SERIES = new Color(255, 92, 92);
    private static final Color CONNECTED_SERIES = new Color(20, 168, 107);
    private static final Color OTHER_SERIES = new Color(140, 150, 165, 90);
    private static final int MAX_DATA_POINTS = 60;

    private JPanel graphContainer;
    private JPanel channelPanel24;
    private JPanel channelPanel5;
    private JPanel channelChartContainer;


    private TimeSeries signalTimeSeries;
    private JFreeChart signalChart;
    private ChartPanel signalChartPanel;

    private DefaultCategoryDataset channelDataset;
    private JFreeChart channelBarChart;

    private TimeSeries signalTimeSeries2;


    private XYSeriesCollection overlapDataset24;
    private XYSeriesCollection overlapDataset5;
    private JFreeChart overlapChart24;
    private JFreeChart overlapChart5;

    public ChartManager(JPanel graphContainer, JPanel channelPanel24, JPanel channelPanel5,
                        JPanel channelChartContainer) {
        this.graphContainer = graphContainer;
        this.channelPanel24 = channelPanel24;
        this.channelPanel5 = channelPanel5;
        this.channelChartContainer = channelChartContainer;

        initializeCharts();
    }

    private void initializeCharts() {
        initSignalChart();
        initChannelChart();
        initOverlapCharts();
    }

    private void initSignalChart() {
        signalTimeSeries = new TimeSeries("Основная точка");
        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(signalTimeSeries);

        signalTimeSeries2 = new TimeSeries("Дополнительная точка");
        dataset.addSeries(signalTimeSeries2);

        signalChart = ChartFactory.createTimeSeriesChart(
                "График уровня сигнала Wi-Fi",
                "Время",
                "Уровень сигнала (dBm)",
                dataset,
                true,
                true,
                false
        );

        configureSignalChart();

        signalChartPanel = new ChartPanel(signalChart);
        signalChartPanel.setPreferredSize(new Dimension(500, 300));
        styleChartPanel(signalChartPanel);

        if (graphContainer != null) {
            graphContainer.setOpaque(false);
            graphContainer.setLayout(new BorderLayout());
            graphContainer.add(signalChartPanel, BorderLayout.CENTER);
        }
    }

    private void configureSignalChart() {
        styleChart(signalChart);

        XYPlot plot = signalChart.getXYPlot();
        styleXYPlot(plot);
        plot.setRangeGridlinesVisible(true);
        plot.setDomainGridlinesVisible(true);
        plot.setOutlineVisible(false);

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();

        renderer.setSeriesPaint(0, SIGNAL_LINE);
        renderer.setSeriesFillPaint(0, SIGNAL_FILL);
        renderer.setSeriesStroke(0, new BasicStroke(3.0f));
        renderer.setSeriesShapesVisible(0, true);
        renderer.setSeriesShape(0, new Ellipse2D.Double(-3, -3, 6, 6));
        renderer.setSeriesShapesFilled(0, true);

        boolean hasSecondSeries = signalTimeSeries2 != null && signalTimeSeries2.getItemCount() > 0;

        if (hasSecondSeries) {
            renderer.setSeriesPaint(1, new Color(50, 207, 255));
            renderer.setSeriesFillPaint(1, new Color(50, 190, 255, 56));
            renderer.setSeriesStroke(1, new BasicStroke(3.0f));
            renderer.setSeriesShapesVisible(1, true);
            renderer.setSeriesShape(1, new Ellipse2D.Double(-3, -3, 6, 6));
            renderer.setSeriesShapesFilled(1, true);
        } else {
            renderer.setSeriesVisibleInLegend(1, false);
            renderer.setSeriesShapesVisible(1, false);
            renderer.setSeriesLinesVisible(1, false);
        }

        renderer.setUseFillPaint(true);
        plot.setRenderer(renderer);

        ValueAxis domainAxis = plot.getDomainAxis();
        domainAxis.setAutoRange(true);
        domainAxis.setFixedAutoRange(60000.0);
        styleAxis(domainAxis);

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setRange(-90.0, -10.0);
        rangeAxis.setTickUnit(new NumberTickUnit(10));
        styleAxis(rangeAxis);
    }


    private void initChannelChart() {
        channelDataset = new DefaultCategoryDataset();

        channelBarChart = ChartFactory.createBarChart(
                "Загрузка каналов Wi-Fi (%)",
                "Канал",
                "Загрузка канала (%)",
                channelDataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        configureChannelChart();

        ChartPanel chartPanel = new ChartPanel(channelBarChart);
        chartPanel.setPreferredSize(new Dimension(800, 400));
        styleChartPanel(chartPanel);

        if (channelChartContainer != null) {
            channelChartContainer.setOpaque(false);
            channelChartContainer.setLayout(new BorderLayout());
            channelChartContainer.add(chartPanel, BorderLayout.CENTER);
        }
    }

    private void configureChannelChart() {
        styleChart(channelBarChart);

        CategoryPlot plot = channelBarChart.getCategoryPlot();
        plot.setBackgroundPaint(CHART_CANVAS);
        plot.setDomainGridlinePaint(CHART_GRID);
        plot.setRangeGridlinePaint(CHART_GRID);
        plot.setRangeGridlineStroke(new BasicStroke(1f));
        plot.setDomainGridlineStroke(new BasicStroke(1f));
        plot.setRangeGridlinesVisible(true);
        plot.setDomainGridlinesVisible(true);
        plot.setOutlineVisible(false);

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setRange(0.0, 100.0);
        rangeAxis.setTickUnit(new NumberTickUnit(10));
        styleAxis(rangeAxis);

        CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
        domainAxis.setMaximumCategoryLabelWidthRatio(0.15f);
        styleCategoryAxis(domainAxis);

        BarRenderer renderer = new BarRenderer();
        renderer.setSeriesPaint(0, CHANNEL_BAR);
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setDrawBarOutline(true);
        renderer.setSeriesOutlinePaint(0, CHANNEL_BAR_EDGE);
        renderer.setSeriesOutlineStroke(0, new BasicStroke(1.25f));
        renderer.setMaximumBarWidth(0.12);
        renderer.setShadowVisible(false);
        renderer.setItemMargin(0.08);
        renderer.setDefaultItemLabelGenerator(new StandardCategoryItemLabelGenerator());
        renderer.setDefaultItemLabelsVisible(true);
        renderer.setDefaultItemLabelFont(new Font("Segoe UI", Font.BOLD, 11));
        renderer.setDefaultItemLabelPaint(CHART_TEXT);
        plot.setRenderer(renderer);
    }

    private void updateChannelChart(Map<Integer, ChannelInfo> channelStats) {
        if (channelDataset == null) {
            return;
        }

        channelDataset.clear();

        if (channelStats.isEmpty()) {
            channelDataset.addValue(0, "Загрузка %", "Нет сетей");
            return;
        }

        List<Integer> sortedChannels = new ArrayList<>(channelStats.keySet());
        Collections.sort(sortedChannels);

        for (Integer channel : sortedChannels) {
            ChannelInfo info = channelStats.get(channel);
            double loadPercentage = info.getUtilizationScore();
            String channelLabel = channel <= 14 ? channel + " (2.4G)" : channel + " (5G)";
            channelDataset.addValue(loadPercentage, "Загрузка %", channelLabel);
        }
    }

    private void initOverlapCharts() {
        overlapDataset24 = new XYSeriesCollection();
        overlapDataset5 = new XYSeriesCollection();

        overlapChart24 = createOverlapChart("Перекрытие каналов Wi-Fi (2.4 GHz)", -2, 17);
        overlapChart5 = createOverlapChart("Перекрытие каналов Wi-Fi (5 GHz)", 30, 170);



        ChartPanel panel24 = new ChartPanel(overlapChart24);
        ChartPanel panel5 = new ChartPanel(overlapChart5);
        styleChartPanel(panel24);
        styleChartPanel(panel5);


        if (channelPanel24 != null) {
            channelPanel24.setOpaque(false);
            channelPanel24.setLayout(new BorderLayout());
            channelPanel24.add(panel24, BorderLayout.CENTER);
        }

        if (channelPanel5 != null) {
            channelPanel5.setOpaque(false);
            channelPanel5.setLayout(new BorderLayout());
            channelPanel5.add(panel5, BorderLayout.CENTER);
        }
    }


    private JFreeChart createOverlapChart(String title, int min, int max) {
        JFreeChart chart = ChartFactory.createXYLineChart(
                title,
                "Канал",
                "Сигнал (dBm)",
                min <= 14 ? overlapDataset24 : overlapDataset5,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        styleChart(chart);

        XYPlot plot = chart.getXYPlot();
        styleXYPlot(plot);
        plot.setOutlineVisible(false);

        NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
        xAxis.setRange(min, max);
        xAxis.setTickUnit(max > 20 ? new NumberTickUnit(4) : new NumberTickUnit(1));
        if (max > 20) {
            xAxis.setVerticalTickLabels(true);
        }
        styleAxis(xAxis);

        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        yAxis.setRange(-95, -20);
        yAxis.setTickUnit(new NumberTickUnit(10));
        styleAxis(yAxis);

        return chart;
    }

    public void updateCharts(Collection<NetworkInfo> networks,
                             Map<Integer, ChannelInfo> channelStats,
                             String selectedSsid, String selectedBssid) {
        updateChannelChart(channelStats);
        updateOverlapCharts(networks, selectedSsid, selectedBssid);
    }

    private void updateOverlapCharts(Collection<NetworkInfo> networks,
                                     String selectedSsid, String selectedBssid) {
        overlapDataset24.removeAllSeries();
        overlapDataset5.removeAllSeries();
        String connectedSsid = "";

        for (NetworkInfo net : networks) {
            if (net.getConnectedSsid() != null && !net.getConnectedSsid().isBlank()) {
                connectedSsid = net.getConnectedSsid();
                break;
            }
        }

        List<NetworkInfo> networksToShow = new ArrayList<>();
        List<NetworkInfo> importantNetworks = new ArrayList<>();

        for (NetworkInfo net : networks) {
            String ssid = net.getSsid().replace("(Подключено)", "").trim();

            boolean isSelected = ssid.equals(selectedSsid);
            boolean isConnected = ssid.equals(connectedSsid);

            networksToShow.add(net);

            if (isSelected || isConnected) {
                importantNetworks.add(net);
            }
        }

        for (NetworkInfo net : networksToShow) {
            String ssid = net.getSsid().replace("(Подключено)", "").trim();
            boolean isImportant = ssid.equals(selectedSsid) || ssid.equals(connectedSsid);

            for (NativeWifiScanner.WifiNetwork.Bssids ap : net.getBssids()) {
                XYSeries series = createOverlapSeries(net, ap);
                if (series != null) {
                    if (ap.channel <= 14) {
                        overlapDataset24.addSeries(series);
                    } else {
                        overlapDataset5.addSeries(series);
                    }
                }
            }
        }

        highlightSelectedSeries(overlapChart24, overlapDataset24, selectedSsid, selectedBssid, connectedSsid);
        highlightSelectedSeries(overlapChart5, overlapDataset5, selectedSsid, selectedBssid, connectedSsid);

        updateLegendVisibility(overlapChart24, overlapDataset24, selectedSsid, selectedBssid, connectedSsid);
        updateLegendVisibility(overlapChart5, overlapDataset5, selectedSsid, selectedBssid, connectedSsid);
    }

    private void updateLegendVisibility(JFreeChart chart, XYSeriesCollection dataset,
                                        String selectedSsid, String selectedBssid,
                                        String connectedSsid) {
        XYPlot plot = chart.getXYPlot();
        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();

        if (renderer == null) return;

        for (int i = 0; i < dataset.getSeriesCount(); i++) {
            String seriesKey = dataset.getSeriesKey(i).toString();

            boolean isSelected = matchesSelectedBssid(seriesKey, selectedBssid) ||
                    matchesSelectedSsid(seriesKey, selectedSsid);
            boolean isConnected = matchesConnectedSsid(seriesKey, connectedSsid);

            renderer.setSeriesVisibleInLegend(i, isSelected || isConnected);

            if (!isSelected && !isConnected) {
                renderer.setSeriesPaint(i, new Color(180, 180, 180, 202));
                renderer.setSeriesStroke(i, new BasicStroke(1.5f));
            }
        }
    }

    private XYSeries createOverlapSeries(NetworkInfo net, NativeWifiScanner.WifiNetwork.Bssids ap) {
        if (ap == null || ap.channel <= 0) {
            return null;
        }

        int channel = ap.channel;
        int rssi = ap.rssi;
        int width = calculateChannelWidth(net, ap);

        XYSeries series = new XYSeries(buildSeriesKey(net.getSsid(), ap.bssid));

        double halfWidth;
        switch (width) {
            case 40 -> halfWidth = 4;
            case 80 -> halfWidth = 8;
            default -> halfWidth = 2;
        }

        double left = channel - halfWidth;
        double right = channel + halfWidth;

        double slope = halfWidth * 0.8;

        double flatLeft = channel - slope;
        double flatRight = channel + slope;


        series.add(left, -100);
        series.add(left, -100);

        series.add(flatLeft, rssi);
        series.add(flatLeft, rssi);

        series.add(flatRight, rssi);
        series.add(flatRight, rssi);

        series.add(right, -100);
        series.add(right, -100);

        return series;
    }

    private String buildSeriesKey(String ssid, String bssid) {
        return ssid + " [" + bssid + "]";
    }

    private int calculateChannelWidth(NetworkInfo net, NativeWifiScanner.WifiNetwork.Bssids ap) {
        if (ap != null && ap.channelWidth > 0) {
            return ap.channelWidth;
        }
        if (net.getBand().contains("80")) {
            return 80;
        }
        if (net.getBand().contains("40")) {
            return 40;
        }
        return 20;
    }



    private void highlightSelectedSeries(JFreeChart chart, XYSeriesCollection dataset,
                                         String selectedSsid, String selectedBssid, String connectedSsid) {
        XYPlot plot = chart.getXYPlot();
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        renderer.setDefaultShapesVisible(false);
        renderer.setDrawSeriesLineAsPath(false);


        for (int i = 0; i < dataset.getSeriesCount(); i++) {
            String seriesKey = dataset.getSeriesKey(i).toString();

            if (matchesSelectedBssid(seriesKey, selectedBssid)) {
                renderer.setSeriesPaint(i, SELECTED_SERIES);
                renderer.setSeriesStroke(i, new BasicStroke(3.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            } else if (matchesConnectedSsid(seriesKey, connectedSsid)) {
                renderer.setSeriesPaint(i, CONNECTED_SERIES);
                renderer.setSeriesStroke(i, new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            } else if (matchesSelectedSsid(seriesKey, selectedSsid)) {
                renderer.setSeriesPaint(i, new Color(255, 155, 92));
                renderer.setSeriesStroke(i, new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            } else {
                renderer.setSeriesPaint(i, OTHER_SERIES);
                renderer.setSeriesStroke(i, new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            }
        }

        plot.setRenderer(renderer);
    }

    private boolean matchesSelectedBssid(String seriesKey, String selectedBssid) {
        return selectedBssid != null && seriesKey.endsWith("[" + selectedBssid + "]");
    }

    private boolean matchesSelectedSsid(String seriesKey, String selectedSsid) {
        if (selectedSsid == null || selectedSsid.isBlank()) {
            return false;
        }
        return normalizeSsid(extractSeriesSsid(seriesKey)).equals(normalizeSsid(selectedSsid));
    }

    private boolean matchesConnectedSsid(String seriesKey, String connectedSsid) {
        if (connectedSsid == null || connectedSsid.isBlank()) {
            return false;
        }
        return normalizeSsid(extractSeriesSsid(seriesKey)).equals(normalizeSsid(connectedSsid));
    }

    private String extractSeriesSsid(String seriesKey) {
        int splitIndex = seriesKey.lastIndexOf(" [");
        return splitIndex >= 0 ? seriesKey.substring(0, splitIndex) : seriesKey;
    }

    private String normalizeSsid(String ssid) {
        if (ssid == null) {
            return "";
        }
        return ssid.replace("(РџРѕРґРєР»СЋС‡РµРЅРѕ)", "")
                .replace("(Подключено)", "")
                .trim();
    }

    public void updateSignalHistory(String selectedSsid, int signalStrength, int secondSignalStrength) {
        Second currentSecond = new Second(new Date());

        if (!signalTimeSeries.getKey().equals(selectedSsid)) {
            signalTimeSeries.clear();
            signalTimeSeries.setKey(selectedSsid);
        }
        signalTimeSeries.addOrUpdate(currentSecond, signalStrength);
        if (signalTimeSeries.getItemCount() > MAX_DATA_POINTS) {
            signalTimeSeries.delete(0, 0);
        }

        String secondKey = selectedSsid + " (доп.)";

        if (secondSignalStrength > -100) {
            if (!signalTimeSeries2.getKey().equals(secondKey)) {
                signalTimeSeries2.clear();
                signalTimeSeries2.setKey(secondKey);
            }
            signalTimeSeries2.addOrUpdate(currentSecond, secondSignalStrength);
            if (signalTimeSeries2.getItemCount() > MAX_DATA_POINTS) {
                signalTimeSeries2.delete(0, 0);
            }
        } else {
            signalTimeSeries2.clear();
            signalTimeSeries2.setKey(secondKey);
        }

        configureSignalChart();
    }



    private void styleChart(JFreeChart chart) {
        chart.setBackgroundPaint(CHART_SURFACE);
        chart.getTitle().setPaint(CHART_TEXT);
        chart.getTitle().setFont(new Font("Segoe UI", Font.BOLD, 18));

        if (chart.getLegend() != null) {
            chart.getLegend().setBackgroundPaint(CHART_SURFACE);
            chart.getLegend().setItemPaint(CHART_TEXT);
            chart.getLegend().setItemFont(new Font("Segoe UI", Font.PLAIN, 12));
            chart.getLegend().setFrame(new LineBorder());
        }
    }

    private void styleXYPlot(XYPlot plot) {
        plot.setBackgroundPaint(CHART_CANVAS);
        plot.setDomainGridlinePaint(CHART_GRID);
        plot.setRangeGridlinePaint(CHART_GRID);
        plot.setRangeGridlineStroke(new BasicStroke(1f));
        plot.setDomainGridlineStroke(new BasicStroke(1f));
    }

    private void styleAxis(ValueAxis axis) {
        axis.setAxisLinePaint(CHART_GRID);
        axis.setLabelPaint(CHART_AXIS);
        axis.setTickLabelPaint(CHART_AXIS);
        axis.setLabelFont(new Font("Segoe UI", Font.BOLD, 12));
        axis.setTickLabelFont(new Font("Segoe UI", Font.PLAIN, 11));
        axis.setTickMarkPaint(CHART_GRID);
    }

    private void styleCategoryAxis(CategoryAxis axis) {
        axis.setAxisLinePaint(CHART_GRID);
        axis.setLabelPaint(CHART_AXIS);
        axis.setTickLabelPaint(CHART_AXIS);
        axis.setLabelFont(new Font("Segoe UI", Font.BOLD, 12));
        axis.setTickLabelFont(new Font("Segoe UI", Font.PLAIN, 11));
        axis.setTickMarkPaint(CHART_GRID);
    }

    private void styleChartPanel(ChartPanel panel) {
        panel.setBackground(CHART_SURFACE);
        panel.setOpaque(true);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(214, 223, 235), 1, true),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        panel.setMouseWheelEnabled(true);
    }
}
