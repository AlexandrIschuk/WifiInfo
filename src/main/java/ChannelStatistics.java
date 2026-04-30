import lombok.Getter;
import java.util.*;
import java.util.stream.Collectors;

public class ChannelStatistics {
    @Getter
    private int totalChannels;
    @Getter
    private int channels24;
    @Getter
    private int channels5;
    private int busiestChannel;
    private int bestChannel;
    private double avgLoad;
    private Map<Integer, ChannelInfo> channelStats;

    public ChannelStatistics(Map<Integer, ChannelInfo> channelStats) {
        this.channelStats = channelStats;
        calculateStatistics();
    }

    private void calculateStatistics() {
        totalChannels = channelStats.size();
        channels24 = 0;
        channels5 = 0;
        busiestChannel = -1;
        bestChannel = -1;
        int maxLoad = 0;
        int minLoad = 100;
        double totalNetworks = 0;
        double totalLoad = 0;

        for (Map.Entry<Integer, ChannelInfo> entry : channelStats.entrySet()) {
            int channel = entry.getKey();
            ChannelInfo info = entry.getValue();
            int count = info.getNetworkCount();
            int load = info.getUtilizationScore();

            if (channel <= 14) {
                channels24++;
            } else {
                channels5++;
            }

            totalNetworks += count;
            totalLoad += load;

            if (load > maxLoad) {
                maxLoad = load;
                busiestChannel = channel;
            }

            if (load < minLoad) {
                minLoad = load;
                bestChannel = channel;
            }
        }

        avgLoad = totalChannels > 0 ? totalLoad / totalChannels : 0;
    }

    public String getBusiestChannelDisplay() {
        if (busiestChannel > 0) {
            ChannelInfo info = channelStats.get(busiestChannel);
            return String.format("Канал %d (%d%%)", busiestChannel, info.getUtilizationScore());
        }
        return "-";
    }

    public String getBestChannelDisplay() {
        if (bestChannel > 0) {
            ChannelInfo info = channelStats.get(bestChannel);
            return String.format("Канал %d (%d%%)", bestChannel, info.getUtilizationScore());
        }
        return "-";
    }


    public String getAvgLoadFormatted() {
        return String.format("%.1f%%", avgLoad);
    }

    public String getRecommendations() {
        StringBuilder rec = new StringBuilder();
        rec.append("АНАЛИЗ ЗАГРУЗКИ КАНАЛОВ\n");

        if (channelStats.isEmpty()) {
            rec.append("Сети не обнаружены\n");
            return rec.toString();
        }



        rec.append("\n").append(getTopRecommendations());
        rec.append("\n").append(getWorstRecommendations());
        return rec.toString();
    }

    private String getWorstRecommendations() {

        StringBuilder rec = new StringBuilder();
        rec.append("Наиболее загруженные каналы:\n");

        List<Map.Entry<Integer, ChannelInfo>> worstChannels = channelStats.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue().getUtilizationScore(),
                        e1.getValue().getUtilizationScore()))
                .limit(3)
                .collect(Collectors.toList());

        if (worstChannels.isEmpty()) {
            rec.append("  Нет доступных каналов\n");
        } else {
            int rank = 1;
            for (Map.Entry<Integer, ChannelInfo> entry : worstChannels) {
                int channel = entry.getKey();
                ChannelInfo info = entry.getValue();

                rec.append(String.format("Канал %d - загрузка %d%% (%d сетей)\n",
                        channel, info.getUtilizationScore(), info.getNetworkCount()));
                rank++;
            }
        }


        return rec.toString();
    }


    private String getTopRecommendations() {
        StringBuilder rec = new StringBuilder();
        rec.append("Наименее загруженные каналы:\n");

        List<Map.Entry<Integer, ChannelInfo>> bestChannels = channelStats.entrySet().stream()
                .sorted(Comparator.comparingInt(e -> e.getValue().getUtilizationScore()))
                .limit(3)
                .collect(Collectors.toList());

        if (bestChannels.isEmpty()) {
            rec.append("  Нет доступных каналов\n");
        } else {
            int rank = 1;
            for (Map.Entry<Integer, ChannelInfo> entry : bestChannels) {
                int channel = entry.getKey();
                ChannelInfo info = entry.getValue();

                rec.append(String.format("Канал %d - загрузка %d%% (%d сетей)\n",
                         channel, info.getUtilizationScore(), info.getNetworkCount()));
                rank++;
            }
        }


        return rec.toString();
    }
}