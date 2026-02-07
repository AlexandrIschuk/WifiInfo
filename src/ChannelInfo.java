import java.util.Date;

public class ChannelInfo {
    private int channel;
    private int networkCount;
    private double averageSignal; // Средний сигнал в %
    private Date lastUpdated;

    public ChannelInfo(int channel) {
        this.channel = channel;
        this.networkCount = 0;
        this.averageSignal = 0;
        this.lastUpdated = new Date();
    }

    // Геттеры и сеттеры
    public int getChannel() { return channel; }
    public int getNetworkCount() { return networkCount; }
    public double getAverageSignal() { return averageSignal; }
    public Date getLastUpdated() { return lastUpdated; }

    public void setNetworkCount(int networkCount) { this.networkCount = networkCount; }
    public void setAverageSignal(double averageSignal) { this.averageSignal = averageSignal; }
    public void setLastUpdated(Date lastUpdated) { this.lastUpdated = lastUpdated; }
}