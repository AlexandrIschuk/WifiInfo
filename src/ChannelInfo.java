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
    private int utilizationScore;  // Загрузка канала в процентах

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

        // Рассчитываем загрузку канала на основе количества сетей и их сигнала
        updateUtilizationScore();
    }

    private void updateUtilizationScore() {

        double countFactor = Math.min(100, networkCount * 10); // 10 сетей = 100%

        // Сигнал: -30 dBm (отлично) = 100%, -90 dBm (плохо) = 0%
        double signalFactor = 0;
        if (averageSignal != 0) {
            signalFactor = Math.min(100, Math.max(0,
                    (100+averageSignal) / 0.6));
        }

        // Итоговая загрузка: 70% от количества сетей, 30% от силы сигнала
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