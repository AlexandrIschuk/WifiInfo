import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class HtmlReportRecorder {
    private static final String[] BSSID_COLORS = {
            "#2f6de5", "#0f9d58", "#d9534f", "#f39c12", "#8e44ad",
            "#16a085", "#e67e22", "#34495e", "#c0392b", "#1abc9c"
    };
    private static final DateTimeFormatter FILE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter DISPLAY_TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter SHORT_TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    private boolean recording;
    private Instant startedAt;
    private Instant stoppedAt;
    private Set<String> trackedSsids = new LinkedHashSet<>();
    private final Map<String, List<Snapshot>> snapshotsBySsid = new LinkedHashMap<>();
    private final List<ChannelSnapshot> channelSnapshots = new ArrayList<>();

    public void start(Collection<String> ssids) {
        recording = true;
        startedAt = Instant.now();
        stoppedAt = null;
        trackedSsids = new LinkedHashSet<>(ssids);
        snapshotsBySsid.clear();
        channelSnapshots.clear();

        for (String ssid : trackedSsids) {
            snapshotsBySsid.put(ssid, new ArrayList<>());
        }
    }

    public boolean isRecording() {
        return recording;
    }

    public boolean hasData() {
        return startedAt != null;
    }

    public Set<String> getTrackedSsids() {
        return new LinkedHashSet<>(trackedSsids);
    }

    public String buildDefaultFileName() {
        Instant baseTime = stoppedAt != null ? stoppedAt : Instant.now();
        return "wifi-report-" + FILE_TIME_FORMAT.format(baseTime) + ".html";
    }

    public void record(Map<String, NetworkInfo> networks, Map<Integer, ChannelInfo> channelStats) {
        if (!recording) {
            return;
        }

        Instant now = Instant.now();
        for (String ssid : trackedSsids) {
            List<Snapshot> snapshots = snapshotsBySsid.computeIfAbsent(ssid, key -> new ArrayList<>());
            NetworkInfo network = networks.get(ssid);
            snapshots.add(network != null ? Snapshot.from(now, network) : Snapshot.missing(now));
        }

        if (channelStats != null && !channelStats.isEmpty()) {
            for (ChannelInfo channelInfo : channelStats.values()) {
                channelSnapshots.add(ChannelSnapshot.from(now, channelInfo));
            }
        }
    }

    public void stop() {
        if (!recording) {
            return;
        }
        recording = false;
        stoppedAt = Instant.now();
    }

    public void writeHtmlReport(Path path) throws IOException {
        Files.writeString(path, buildHtml(), StandardCharsets.UTF_8);
    }

    private String buildHtml() {
        Instant reportEnd = stoppedAt != null ? stoppedAt : Instant.now();
        StringBuilder html = new StringBuilder();
        html.append("""
                <!DOCTYPE html>
                <html lang="ru">
                <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Wi-Fi отчет</title>
                <style>
                :root {
                    --bg: #eef3f8;
                    --card: #ffffff;
                    --border: #d7e1ed;
                    --text: #1f2937;
                    --muted: #617083;
                    --accent: #2f6de5;
                    --ok: #14a86b;
                    --warn: #f47c3f;
                }
                * { box-sizing: border-box; }
                body {
                    margin: 0;
                    padding: 32px;
                    background: linear-gradient(180deg, #f7faff 0%, var(--bg) 100%);
                    color: var(--text);
                    font: 14px/1.5 "Segoe UI", sans-serif;
                }
                h1, h2, h3 { margin: 0; }
                .wrap {
                    max-width: 1200px;
                    margin: 0 auto;
                    display: grid;
                    gap: 20px;
                }
                .card {
                    background: var(--card);
                    border: 1px solid var(--border);
                    border-radius: 18px;
                    padding: 20px 24px;
                    box-shadow: 0 10px 30px rgba(31, 41, 55, 0.06);
                }
                .hero {
                    display: grid;
                    gap: 14px;
                }
                .hero p {
                    margin: 0;
                    color: var(--muted);
                }
                .stats {
                    display: grid;
                    grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
                    gap: 12px;
                }
                .stat {
                    padding: 14px 16px;
                    border: 1px solid var(--border);
                    border-radius: 14px;
                    background: #fbfdff;
                }
                .stat .label {
                    color: var(--muted);
                    font-size: 12px;
                    text-transform: uppercase;
                    letter-spacing: 0.04em;
                }
                .stat .value {
                    margin-top: 6px;
                    font-size: 20px;
                    font-weight: 700;
                }
                .network-header {
                    display: flex;
                    justify-content: space-between;
                    gap: 16px;
                    align-items: center;
                    margin-bottom: 14px;
                }
                .badge {
                    display: inline-flex;
                    align-items: center;
                    gap: 8px;
                    padding: 6px 10px;
                    border-radius: 999px;
                    background: #eff6ff;
                    color: var(--accent);
                    font-weight: 600;
                }
                .chart-card {
                    margin-top: 16px;
                    padding: 16px;
                    border: 1px solid var(--border);
                    border-radius: 16px;
                    background: #fbfdff;
                }
                .chart-title {
                    margin-bottom: 10px;
                    color: var(--muted);
                    font-size: 12px;
                    text-transform: uppercase;
                    letter-spacing: 0.04em;
                }
                .chart {
                    width: 100%;
                    height: auto;
                    display: block;
                }
                .channel-grid {
                    display: grid;
                    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
                    gap: 14px;
                    margin-top: 12px;
                }
                .channel-card {
                    border: 1px solid var(--border);
                    border-radius: 16px;
                    background: #fbfdff;
                    padding: 16px;
                }
                .channel-card h3 {
                    font-size: 18px;
                }
                .channel-meta {
                    margin-top: 4px;
                    color: var(--muted);
                    font-size: 13px;
                }
                .channel-stats {
                    display: grid;
                    grid-template-columns: repeat(2, minmax(0, 1fr));
                    gap: 10px;
                    margin-top: 12px;
                }
                .channel-stat {
                    padding: 10px 12px;
                    border-radius: 12px;
                    border: 1px solid var(--border);
                    background: #ffffff;
                }
                .channel-stat .label {
                    color: var(--muted);
                    font-size: 11px;
                    text-transform: uppercase;
                    letter-spacing: 0.04em;
                }
                .channel-stat .value {
                    margin-top: 4px;
                    font-size: 16px;
                    font-weight: 700;
                }
                details.measurements {
                    margin-top: 16px;
                    border: 1px solid var(--border);
                    border-radius: 16px;
                    background: #fbfdff;
                    overflow: hidden;
                }
                details.measurements summary {
                    cursor: pointer;
                    list-style: none;
                    padding: 14px 16px;
                    font-weight: 600;
                    color: var(--text);
                    background: #f7faff;
                }
                details.measurements summary::-webkit-details-marker {
                    display: none;
                }
                details.measurements .content {
                    padding: 0 0 4px;
                }
                table {
                    width: 100%;
                    border-collapse: collapse;
                    overflow: hidden;
                    border-radius: 14px;
                }
                th, td {
                    padding: 10px 12px;
                    border-bottom: 1px solid var(--border);
                    text-align: left;
                    vertical-align: top;
                }
                th {
                    background: #f7faff;
                    color: var(--muted);
                    font-size: 12px;
                    text-transform: uppercase;
                    letter-spacing: 0.04em;
                }
                tr:last-child td { border-bottom: none; }
                .missing {
                    color: #b45309;
                    font-weight: 600;
                }
                .ok {
                    color: var(--ok);
                    font-weight: 600;
                }
                .muted {
                    color: var(--muted);
                }
                </style>
                </head>
                <body>
                <div class="wrap">
                """);

        html.append("<section class=\"card hero\">");
        html.append("<h1>Отчет по мониторингу Wi-Fi</h1>");
        html.append("<p>Отчет создан автоматически на основе записанных обновлений списка сетей.</p>");
        html.append("<div class=\"stats\">");
        appendStat(html, "Начало записи", formatInstant(startedAt));
        appendStat(html, "Конец записи", formatInstant(reportEnd));
        appendStat(html, "Длительность", formatDuration(startedAt, reportEnd));
        appendStat(html, "Сетей в отчете", String.valueOf(trackedSsids.size()));
        html.append("</div></section>");

        appendChannelStatisticsSection(html);

        for (String ssid : trackedSsids) {
            List<Snapshot> snapshots = snapshotsBySsid.getOrDefault(ssid, List.of());
            long foundCount = snapshots.stream().filter(snapshot -> !snapshot.missing).count();
            Snapshot bestSignal = snapshots.stream()
                    .filter(snapshot -> !snapshot.missing)
                    .max((left, right) -> Integer.compare(left.signal, right.signal))
                    .orElse(null);

            html.append("<section class=\"card\">");
            html.append("<div class=\"network-header\">");
            html.append("<div>");
            html.append("<h2>").append(escapeHtml(ssid)).append("</h2>");
            html.append("<div class=\"muted\">Записей: ").append(snapshots.size())
                    .append(" | Обнаружено: ").append(foundCount).append("</div>");
            html.append("</div>");
            html.append("<span class=\"badge\">")
                    .append(foundCount > 0 ? "Есть данные" : "Не обнаружена")
                    .append("</span>");
            html.append("</div>");

            html.append("<div class=\"stats\">");
            appendStat(html, "Лучший сигнал", bestSignal != null ? bestSignal.signal + " dBm" : "нет данных");
            appendStat(html, "Последний канал", findLastValue(snapshots, snapshot -> snapshot.channel));
            appendStat(html, "Последние BSSID", findLastValue(snapshots,
                    snapshot -> snapshot.bssids.isEmpty() ? "-" : String.join(", ", snapshot.bssids)));
            appendStat(html, "Безопасность", findLastValue(snapshots, snapshot -> snapshot.security));
            html.append("</div>");

            html.append("<div class=\"chart-card\">");
            html.append("<div class=\"chart-title\">График сигнала</div>");
            html.append(buildSignalChartSvg(snapshots));
            html.append("</div>");

            html.append("<details class=\"measurements\">");
            html.append("<summary>Список измерений</summary>");
            html.append("<div class=\"content\" style=\"overflow:auto;\">");
            html.append("<table>");
            html.append("""
                    <thead>
                    <tr>
                        <th>Время</th>
                        <th>Статус</th>
                        <th>Сигнал</th>
                        <th>Канал</th>
                        <th>BSSID</th>
                        <th>Безопасность</th>
                        <th>Диапазон</th>
                        <th>Точек доступа</th>
                    </tr>
                    </thead>
                    <tbody>
                    """);

            for (Snapshot snapshot : snapshots) {
                html.append("<tr>");
                html.append("<td>").append(escapeHtml(formatInstant(snapshot.capturedAt))).append("</td>");
                if (snapshot.missing) {
                    html.append("<td class=\"missing\">Не обнаружена</td>");
                    html.append("<td colspan=\"6\" class=\"muted\">Сеть отсутствовала в этом обновлении</td>");
                } else {
                    html.append("<td class=\"ok\">Обнаружена</td>");
                    html.append("<td>").append(snapshot.signal).append(" dBm</td>");
                    html.append("<td>").append(escapeHtml(snapshot.channel)).append("</td>");
                    html.append("<td>").append(formatBssidsHtml(snapshot.bssids)).append("</td>");
                    html.append("<td>").append(escapeHtml(snapshot.security)).append("</td>");
                    html.append("<td>").append(escapeHtml(snapshot.band)).append("</td>");
                    html.append("<td>").append(snapshot.bssidsCount).append("</td>");
                }
                html.append("</tr>");
            }

            html.append("</tbody></table></div></details></section>");
        }

        html.append("</div></body></html>");
        return html.toString();
    }

    private void appendStat(StringBuilder html, String label, String value) {
        html.append("<div class=\"stat\">");
        html.append("<div class=\"label\">").append(escapeHtml(label)).append("</div>");
        html.append("<div class=\"value\">").append(escapeHtml(value)).append("</div>");
        html.append("</div>");
    }

    private void appendChannelStat(StringBuilder html, String label, String value) {
        html.append("<div class=\"channel-stat\">");
        html.append("<div class=\"label\">").append(escapeHtml(label)).append("</div>");
        html.append("<div class=\"value\">").append(escapeHtml(value)).append("</div>");
        html.append("</div>");
    }

    private String formatInstant(Instant instant) {
        return instant == null ? "-" : DISPLAY_TIME_FORMAT.format(instant);
    }

    private String formatDuration(Instant start, Instant end) {
        if (start == null || end == null) {
            return "-";
        }
        Duration duration = Duration.between(start, end);
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        return String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds);
    }

    private String buildSignalChartSvg(List<Snapshot> snapshots) {
        List<Snapshot> visibleSnapshots = snapshots.stream()
                .filter(snapshot -> !snapshot.missing)
                .toList();

        if (visibleSnapshots.isEmpty()) {
            return "<div class=\"muted\">Недостаточно данных для построения графика сигнала.</div>";
        }

        final int width = 960;
        final int height = 260;
        final int left = 56;
        final int right = 20;
        final int top = 20;
        final int bottom = 34;
        final int plotWidth = width - left - right;
        final int plotHeight = height - top - bottom;
        final int minSignal = -100;
        final int maxSignal = -20;

        StringBuilder svg = new StringBuilder();
        svg.append("<svg class=\"chart\" viewBox=\"0 0 ").append(width).append(' ').append(height)
                .append("\" xmlns=\"http://www.w3.org/2000/svg\" role=\"img\" aria-label=\"График сигнала сети\">");
        svg.append("<rect x=\"0\" y=\"0\" width=\"").append(width).append("\" height=\"").append(height)
                .append("\" rx=\"14\" fill=\"#ffffff\"/>");

        for (int signal = minSignal; signal <= maxSignal; signal += 20) {
            double normalized = (double) (signal - minSignal) / (maxSignal - minSignal);
            int y = top + plotHeight - (int) Math.round(normalized * plotHeight);
            svg.append("<line x1=\"").append(left).append("\" y1=\"").append(y)
                    .append("\" x2=\"").append(width - right).append("\" y2=\"").append(y)
                    .append("\" stroke=\"#d7e1ed\" stroke-width=\"1\"/>");
            svg.append("<text x=\"").append(left - 10).append("\" y=\"").append(y + 4)
                    .append("\" text-anchor=\"end\" font-size=\"11\" fill=\"#617083\">")
                    .append(signal).append("</text>");
        }

        svg.append("<line x1=\"").append(left).append("\" y1=\"").append(top)
                .append("\" x2=\"").append(left).append("\" y2=\"").append(top + plotHeight)
                .append("\" stroke=\"#b9c7d8\" stroke-width=\"1.2\"/>");
        svg.append("<line x1=\"").append(left).append("\" y1=\"").append(top + plotHeight)
                .append("\" x2=\"").append(width - right).append("\" y2=\"").append(top + plotHeight)
                .append("\" stroke=\"#b9c7d8\" stroke-width=\"1.2\"/>");

        Map<String, List<SignalPoint>> pointsByBssid = new LinkedHashMap<>();
        for (int i = 0; i < visibleSnapshots.size(); i++) {
            Snapshot snapshot = visibleSnapshots.get(i);
            for (Map.Entry<String, Integer> entry : snapshot.bssidSignals.entrySet()) {
                pointsByBssid.computeIfAbsent(entry.getKey(), key -> new ArrayList<>())
                        .add(new SignalPoint(i, entry.getValue(), snapshot.capturedAt));
            }
        }

        if (pointsByBssid.isEmpty()) {
            return "<div class=\"muted\">Нет данных по точкам доступа для построения графика.</div>";
        }

        int maxXAxisLabels = Math.max(2, plotWidth / 90);
        int xAxisLabelStep = Math.max(1,
                (int) Math.ceil((double) (visibleSnapshots.size() - 1) / (maxXAxisLabels - 1)));

        int bssidIndex = 0;
        int legendRow = 0;
        for (Map.Entry<String, List<SignalPoint>> entry : pointsByBssid.entrySet()) {
            String bssid = entry.getKey();
            List<SignalPoint> points = entry.getValue();
            String color = BSSID_COLORS[bssidIndex % BSSID_COLORS.length];
            StringBuilder path = new StringBuilder();

            for (SignalPoint point : points) {
                int x = visibleSnapshots.size() == 1
                        ? left + (plotWidth / 2)
                        : left + (int) Math.round((double) point.snapshotIndex / (visibleSnapshots.size() - 1) * plotWidth);
                int y = top + plotHeight - (int) Math.round((double) (point.signal - minSignal) / (maxSignal - minSignal) * plotHeight);
                if (path.isEmpty()) {
                    path.append("M ").append(x).append(' ').append(y).append(' ');
                } else {
                    path.append("L ").append(x).append(' ').append(y).append(' ');
                }

                svg.append("<circle cx=\"").append(x).append("\" cy=\"").append(y)
                        .append("\" r=\"3.5\" fill=\"").append(color).append("\" stroke=\"#ffffff\" stroke-width=\"1.5\">")
                        .append("<title>")
                        .append(escapeHtml(bssid)).append(" | ")
                        .append(escapeHtml(formatInstant(point.capturedAt)))
                        .append(" | ").append(point.signal).append(" dBm</title></circle>");
            }

            svg.append("<path d=\"").append(path).append("\" fill=\"none\" stroke=\"").append(color)
                    .append("\" stroke-width=\"2.2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"/>");

            int legendY = top + 14 + (legendRow * 14);
            svg.append("<line x1=\"").append(width - right - 210).append("\" y1=\"").append(legendY - 4)
                    .append("\" x2=\"").append(width - right - 194).append("\" y2=\"").append(legendY - 4)
                    .append("\" stroke=\"").append(color).append("\" stroke-width=\"2.2\"/>");
            svg.append("<text x=\"").append(width - right - 190).append("\" y=\"").append(legendY)
                    .append("\" font-size=\"10\" fill=\"#617083\">")
                    .append(escapeHtml(bssid)).append("</text>");

            bssidIndex++;
            legendRow++;
        }

        for (int i = 0; i < visibleSnapshots.size(); i++) {
            Snapshot snapshot = visibleSnapshots.get(i);
            int x = visibleSnapshots.size() == 1
                    ? left + (plotWidth / 2)
                    : left + (int) Math.round((double) i / (visibleSnapshots.size() - 1) * plotWidth);
            if (i % xAxisLabelStep == 0 || i == visibleSnapshots.size() - 1) {
                svg.append("<text x=\"").append(x).append("\" y=\"").append(height - 10)
                        .append("\" text-anchor=\"middle\" font-size=\"10\" fill=\"#617083\">")
                        .append(escapeHtml(formatShortInstant(snapshot.capturedAt))).append("</text>");
            }
        }

        svg.append("</svg>");
        return svg.toString();
    }

    private void appendChannelStatisticsSection(StringBuilder html) {
        if (channelSnapshots.isEmpty()) {
            return;
        }

        Map<Integer, List<ChannelSnapshot>> snapshotsByChannel = new LinkedHashMap<>();
        for (ChannelSnapshot snapshot : channelSnapshots) {
            snapshotsByChannel.computeIfAbsent(snapshot.channel, key -> new ArrayList<>()).add(snapshot);
        }

        html.append("<section class=\"card\">");
        html.append("<div class=\"network-header\">");
        html.append("<div><h2>Статистика загрузки каналов</h2>");
        html.append("<div class=\"muted\">Отдельная карточка и история измерений для каждого канала</div></div>");
        html.append("<span class=\"badge\">").append(snapshotsByChannel.size()).append(" каналов</span>");
        html.append("</div>");
        html.append("<div class=\"channel-grid\">");

        for (Map.Entry<Integer, List<ChannelSnapshot>> entry : snapshotsByChannel.entrySet()) {
            List<ChannelSnapshot> snapshots = entry.getValue();
            ChannelSnapshot latest = snapshots.get(snapshots.size() - 1);
            int peakLoad = snapshots.stream().mapToInt(snapshot -> snapshot.utilization).max().orElse(0);
            double avgLoad = snapshots.stream().mapToInt(snapshot -> snapshot.utilization).average().orElse(0);
            int peakNetworks = snapshots.stream().mapToInt(snapshot -> snapshot.networkCount).max().orElse(0);

            html.append("<div class=\"channel-card\">");
            html.append("<h3>Канал ").append(entry.getKey()).append("</h3>");
            html.append("<div class=\"channel-meta\">Последнее измерение: ")
                    .append(escapeHtml(formatInstant(latest.capturedAt))).append("</div>");
            html.append("<div class=\"channel-stats\">");
            appendChannelStat(html, "Текущая загрузка", latest.utilization + "%");
            appendChannelStat(html, "Средняя загрузка", String.format(Locale.ROOT, "%.0f%%", avgLoad));
            appendChannelStat(html, "Пиковая загрузка", peakLoad + "%");
            appendChannelStat(html, "Оценка", latest.loadDescription);
            appendChannelStat(html, "Сетей сейчас", String.valueOf(latest.networkCount));
            appendChannelStat(html, "Макс. сетей", String.valueOf(peakNetworks));
            appendChannelStat(html, "Средний сигнал", String.format(Locale.ROOT, "%.0f dBm", latest.averageSignal));
            appendChannelStat(html, "Измерений", String.valueOf(snapshots.size()));
            html.append("</div>");
            html.append("<details class=\"measurements\">");
            html.append("<summary>История измерений канала</summary>");
            html.append("<div class=\"content\" style=\"overflow:auto;\">");
            html.append("<table>");
            html.append("""
                    <thead>
                    <tr>
                        <th>Время</th>
                        <th>Загрузка</th>
                        <th>Описание</th>
                        <th>Сетей</th>
                        <th>Средний сигнал</th>
                    </tr>
                    </thead>
                    <tbody>
                    """);

            for (ChannelSnapshot snapshot : snapshots) {
                html.append("<tr>");
                html.append("<td>").append(escapeHtml(formatInstant(snapshot.capturedAt))).append("</td>");
                html.append("<td>").append(snapshot.utilization).append("%</td>");
                html.append("<td>").append(escapeHtml(snapshot.loadDescription)).append("</td>");
                html.append("<td>").append(snapshot.networkCount).append("</td>");
                html.append("<td>").append(String.format(Locale.ROOT, "%.0f dBm", snapshot.averageSignal)).append("</td>");
                html.append("</tr>");
            }

            html.append("</tbody></table></div></details></div>");
        }

        html.append("</div></section>");
    }

    private String findLastValue(List<Snapshot> snapshots, SnapshotValueExtractor extractor) {
        for (int i = snapshots.size() - 1; i >= 0; i--) {
            Snapshot snapshot = snapshots.get(i);
            if (!snapshot.missing) {
                return extractor.extract(snapshot);
            }
        }
        return "нет данных";
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String formatShortInstant(Instant instant) {
        return instant == null ? "" : SHORT_TIME_FORMAT.format(instant);
    }

    private String formatBssidsHtml(List<String> bssids) {
        if (bssids == null || bssids.isEmpty()) {
            return "-";
        }

        StringBuilder value = new StringBuilder();
        for (int i = 0; i < bssids.size(); i++) {
            if (i > 0) {
                value.append("<br>");
            }
            value.append(escapeHtml(bssids.get(i)));
        }
        return value.toString();
    }

    private interface SnapshotValueExtractor {
        String extract(Snapshot snapshot);
    }

    private static class SignalPoint {
        private final int snapshotIndex;
        private final int signal;
        private final Instant capturedAt;

        private SignalPoint(int snapshotIndex, int signal, Instant capturedAt) {
            this.snapshotIndex = snapshotIndex;
            this.signal = signal;
            this.capturedAt = capturedAt;
        }
    }

    private static class Snapshot {
        private final Instant capturedAt;
        private final boolean missing;
        private final int signal;
        private final String channel;
        private final List<String> bssids;
        private final Map<String, Integer> bssidSignals;
        private final String security;
        private final String band;
        private final int bssidsCount;

        private Snapshot(Instant capturedAt, boolean missing, int signal, String channel, List<String> bssids,
                         Map<String, Integer> bssidSignals, String security, String band, int bssidsCount) {
            this.capturedAt = capturedAt;
            this.missing = missing;
            this.signal = signal;
            this.channel = channel;
            this.bssids = bssids;
            this.bssidSignals = bssidSignals;
            this.security = security;
            this.band = band;
            this.bssidsCount = bssidsCount;
        }

        private static Snapshot from(Instant capturedAt, NetworkInfo network) {
            List<String> bssids = new ArrayList<>();
            Map<String, Integer> bssidSignals = new LinkedHashMap<>();
            for (NativeWifiScanner.WifiNetwork.Bssids bssid : network.getBssids()) {
                if (bssid != null && bssid.bssid != null && !bssid.bssid.isBlank()) {
                    bssids.add(bssid.bssid);
                    bssidSignals.put(bssid.bssid, bssid.rssi);
                }
            }
            int signalValue;
            try {
                signalValue = Integer.parseInt(network.getSignalStrength());
            } catch (NumberFormatException e) {
                signalValue = 0;
            }

            return new Snapshot(
                    capturedAt,
                    false,
                    signalValue,
                    network.getChannel(),
                    bssids,
                    bssidSignals,
                    network.getAuthentication(),
                    network.getBand(),
                    bssids.size()
            );
        }

        private static Snapshot missing(Instant capturedAt) {
            return new Snapshot(capturedAt, true, 0, "", List.of(), Map.of(), "", "", 0);
        }
    }

    private static class ChannelSnapshot {
        private final Instant capturedAt;
        private final int channel;
        private final int utilization;
        private final String loadDescription;
        private final int networkCount;
        private final double averageSignal;

        private ChannelSnapshot(Instant capturedAt, int channel, int utilization, String loadDescription,
                                int networkCount, double averageSignal) {
            this.capturedAt = capturedAt;
            this.channel = channel;
            this.utilization = utilization;
            this.loadDescription = loadDescription;
            this.networkCount = networkCount;
            this.averageSignal = averageSignal;
        }

        private static ChannelSnapshot from(Instant capturedAt, ChannelInfo channelInfo) {
            return new ChannelSnapshot(
                    capturedAt,
                    channelInfo.getChannel(),
                    channelInfo.getUtilizationScore(),
                    channelInfo.getLoadDescription(),
                    channelInfo.getNetworkCount(),
                    channelInfo.getAverageSignal()
            );
        }
    }
}
