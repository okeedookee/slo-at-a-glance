package io.instana.slo.data.model;

/**
 * Enum representing the traffic light status of an SLO
 */
public enum TrafficLightStatus {
    /**
     * Green: SLI > SLO target and error budget is above threshold
     */
    GREEN,

    /**
     * Yellow: Error budget remaining is below the configured threshold
     */
    YELLOW,

    /**
     * Red: SLI <= SLO target (SLO is not being met)
     */
    RED;

    /**
     * Get display name for the status
     */
    public String getDisplayName() {
        switch (this) {
            case GREEN:
                return "Healthy";
            case YELLOW:
                return "Warning";
            case RED:
                return "Critical";
            default:
                return "Unknown";
        }
    }

    /**
     * Get color resource ID for the status
     */
    public int getColorResId() {
        switch (this) {
            case GREEN:
                return android.R.color.holo_green_dark;
            case YELLOW:
                return android.R.color.holo_orange_dark;
            case RED:
                return android.R.color.holo_red_dark;
            default:
                return android.R.color.darker_gray;
        }
    }
}
