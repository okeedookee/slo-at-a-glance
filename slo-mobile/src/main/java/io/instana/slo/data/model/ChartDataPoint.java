package io.instana.slo.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Represents a data point in the error budget chart
 */
public class ChartDataPoint {
    @SerializedName("timestamp")
    private long timestamp;

    @SerializedName("value")
    private double value;

    public ChartDataPoint() {
    }

    public ChartDataPoint(long timestamp, double value) {
        this.timestamp = timestamp;
        this.value = value;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "ChartDataPoint{" +
                "timestamp=" + timestamp +
                ", value=" + value +
                '}';
    }
}
