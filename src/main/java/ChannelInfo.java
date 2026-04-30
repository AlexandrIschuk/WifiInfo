import lombok.Getter;

public class ChannelInfo {
    @Getter
    private int channel;
    @Getter
    private int networkCount;
    private int totalSignal;
    @Getter
    private double averageSignal;
    @Getter
    private int utilizationScore;

    public ChannelInfo(int channel) {
        this.channel = channel;
        this.networkCount = 0;
        this.totalSignal = 0;
        this.averageSignal = 0;
        this.utilizationScore = 0;
    }

    public void addNetwork(int signalStrength) {
        networkCount++;
        totalSignal += signalStrength;
        averageSignal = networkCount > 0 ? (double) totalSignal / networkCount : 0;

        updateUtilizationScore();
    }

    private void updateUtilizationScore() {

        double countFactor = Math.min(100, networkCount * 10); // 10 сетей = 100%

        double signalFactor = 0;
        if (averageSignal != 0) {
            signalFactor = Math.min(100, Math.max(0,
                    (100+averageSignal) / 0.6));
        }

        utilizationScore = (int)((countFactor * 0.7) + (signalFactor * 0.3));
        utilizationScore = Math.min(100, Math.max(0, utilizationScore));
    }

    public String getLoadDescription() {
        if (utilizationScore < 20) return "Отлично";
        if (utilizationScore < 40) return "Хорошо";
        if (utilizationScore < 60) return "Умеренно";
        if (utilizationScore < 80) return "Плохо";
        return "Критично";
    }

}