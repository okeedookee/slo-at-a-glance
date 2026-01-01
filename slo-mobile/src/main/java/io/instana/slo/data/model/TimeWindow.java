package io.instana.slo.data.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

/**
 * Represents the time window configuration for an SLO
 */
public class TimeWindow implements Serializable {
    private static final long serialVersionUID = 1L;
    @SerializedName("type")
    private String type;

    @SerializedName("duration")
    private int duration;

    @SerializedName("durationUnit")
    private String durationUnit;

    @SerializedName("timezone")
    private String timezone;

    @SerializedName("startTimestamp")
    private Long startTimestamp;

    public TimeWindow() {
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getDurationUnit() {
        return durationUnit;
    }

    public void setDurationUnit(String durationUnit) {
        this.durationUnit = durationUnit;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public Long getStartTimestamp() {
        return startTimestamp;
    }

    public void setStartTimestamp(Long startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

    /**
     * Get formatted time window size (e.g., "1 week", "7 days")
     */
    public String getFormattedSize() {
        if (durationUnit == null) {
            return String.valueOf(duration);
        }
        return duration + " " + durationUnit + (duration > 1 ? "s" : "");
    }

    /**
     * Get capitalized type for display
     */
    public String getFormattedType() {
        if (type == null || type.isEmpty()) {
            return "Unknown";
        }
        return type.substring(0, 1).toUpperCase() + type.substring(1);
    }

    @Override
    public String toString() {
        return "TimeWindow{" +
                "type='" + type + '\'' +
                ", duration=" + duration +
                ", durationUnit='" + durationUnit + '\'' +
                ", timezone='" + timezone + '\'' +
                ", startTimestamp=" + startTimestamp +
                '}';
    }
}
