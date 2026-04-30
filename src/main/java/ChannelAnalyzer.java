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

                ChannelInfo info = channelStats.computeIfAbsent(channel,
                        k -> new ChannelInfo(channel));

                info.addNetwork(signal);

            } catch (NumberFormatException e) {
            }
        }
    }

    public Map<Integer, ChannelInfo> getChannelStats() {
        Map<Integer, ChannelInfo> sortedStats = new TreeMap<>(channelStats);
        return Collections.unmodifiableMap(sortedStats);
    }

    public ChannelStatistics getStatistics() {
        return new ChannelStatistics(channelStats);
    }


}