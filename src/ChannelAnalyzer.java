import java.util.*;
import java.util.stream.Collectors;

public class ChannelAnalyzer {
    private Map<Integer, ChannelInfo> channelStats;

    public ChannelAnalyzer() {
        this.channelStats = new HashMap<>();
    }

    public void updateFromNetworks(List<NetworkInfo> networks) {
        channelStats.clear();

        for (NetworkInfo network : networks) {
            if (network.getChannel().isEmpty()) continue;

            try {
                int channel = Integer.parseInt(network.getChannel());
                int signal = Integer.parseInt(network.getSignalStrength());

                // Получаем или создаём информацию о канале
                ChannelInfo info = channelStats.computeIfAbsent(channel,
                        k -> new ChannelInfo(channel));

                // Добавляем сеть на этот канал (только один раз)
                info.addNetwork(signal);

            } catch (NumberFormatException e) {
                // Skip invalid data
            }
        }
    }

    public Map<Integer, ChannelInfo> getChannelStats() {
        // Сортируем каналы по возрастанию
        Map<Integer, ChannelInfo> sortedStats = new TreeMap<>(channelStats);
        return Collections.unmodifiableMap(sortedStats);
    }

    public ChannelStatistics getStatistics() {
        return new ChannelStatistics(channelStats);
    }

    // Получить список каналов, отсортированных по загрузке (от лучшего к худшему)
    public List<Integer> getRecommendedChannels() {
        return channelStats.entrySet().stream()
                .sorted(Comparator.comparingInt(e -> e.getValue().getUtilizationScore()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    // Получить только 2.4 GHz каналы
    public List<Integer> getChannels24() {
        return channelStats.keySet().stream()
                .filter(ch -> ch <= 14)
                .sorted()
                .collect(Collectors.toList());
    }

    // Получить только 5 GHz каналы
    public List<Integer> getChannels5() {
        return channelStats.keySet().stream()
                .filter(ch -> ch > 14)
                .sorted()
                .collect(Collectors.toList());
    }
}