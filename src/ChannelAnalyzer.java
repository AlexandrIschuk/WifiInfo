import java.util.*;

public class ChannelAnalyzer {
    private Map<Integer, ChannelInfo> channelStats = new HashMap<>();

    public void updateFromNetworks(List<NetworkInfo> networks) {
        // Группировка сетей по каналам
        Map<Integer, List<NetworkInfo>> networksByChannel = new HashMap<>();

        for (NetworkInfo network : networks) {
            try {
                String channelStr = network.getChannel();
                if (channelStr != null && !channelStr.isEmpty()) {
                    int channel = Integer.parseInt(channelStr);
                    networksByChannel.computeIfAbsent(channel, k -> new ArrayList<>())
                            .add(network);
                }
            } catch (NumberFormatException e) {
                // Пропускаем сети без канала
            }
        }

        // Обновление статистики для каждого канала
        for (Map.Entry<Integer, List<NetworkInfo>> entry : networksByChannel.entrySet()) {
            updateChannelStats(entry.getKey(), entry.getValue());
        }

        // Обновление каналов без сетей
        for (int channel : getAllPossibleChannels()) {
            if (!networksByChannel.containsKey(channel)) {
                ChannelInfo info = channelStats.getOrDefault(channel, new ChannelInfo(channel));
                info.setNetworkCount(0);
                info.setAverageSignal(0);
                info.setLastUpdated(new Date());
                channelStats.put(channel, info);
            }
        }
    }

    private void updateChannelStats(int channel, List<NetworkInfo> networks) {
        ChannelInfo info = channelStats.getOrDefault(channel, new ChannelInfo(channel));
        info.setNetworkCount(networks.size());

        // Расчет среднего сигнала
        double totalSignal = 0;
        int countWithSignal = 0;

        for (NetworkInfo network : networks) {
            try {
                int signal = Integer.parseInt(network.getSignalStrength());
                totalSignal += signal;
                countWithSignal++;
            } catch (NumberFormatException e) {
                // Пропускаем
            }
        }

        info.setAverageSignal(countWithSignal > 0 ? totalSignal / countWithSignal : 0);
        info.setLastUpdated(new Date());
        channelStats.put(channel, info);
    }

    public Map<Integer, ChannelInfo> getChannelStats() {
        return new HashMap<>(channelStats);
    }

    private List<Integer> getAllPossibleChannels() {
        List<Integer> channels = new ArrayList<>();
        // 2.4GHz каналы
        for (int i = 1; i <= 14; i++) channels.add(i);
        // Основные 5GHz каналы
        int[] channels5GHz = {36, 40, 44, 48, 52, 56, 60, 64, 100, 104, 108, 112,
                116, 120, 124, 128, 132, 136, 140, 149, 153, 157, 161, 165};
        for (int channel : channels5GHz) channels.add(channel);
        return channels;
    }
}