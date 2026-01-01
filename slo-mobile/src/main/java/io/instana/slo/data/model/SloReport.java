package io.instana.slo.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents an SLO report with detailed metrics
 */
public class SloReport {
    @SerializedName("sli")
    private double sli;

    @SerializedName("slo")
    private double sloTarget;

    @SerializedName("totalErrorBudget")
    private double totalErrorBudget;

    @SerializedName("errorBudgetRemaining")
    private double errorBudgetRemaining;

    @SerializedName("errorBudgetRemainChart")
    private Map<String, Double> errorBudgetRemainChart;

    @SerializedName("fromTimestamp")
    private long fromTimestamp;

    @SerializedName("toTimestamp")
    private long toTimestamp;

    @SerializedName("timeWindow")
    private TimeWindow timeWindow;

    public SloReport() {
    }

    public double getSli() {
        return sli;
    }

    public void setSli(double sli) {
        this.sli = sli;
    }

    public double getSloTarget() {
        return sloTarget;
    }

    public void setSloTarget(double sloTarget) {
        this.sloTarget = sloTarget;
    }

    public double getTotalErrorBudget() {
        return totalErrorBudget;
    }

    public void setTotalErrorBudget(double totalErrorBudget) {
        this.totalErrorBudget = totalErrorBudget;
    }

    public double getErrorBudgetRemaining() {
        return errorBudgetRemaining;
    }

    public void setErrorBudgetRemaining(double errorBudgetRemaining) {
        this.errorBudgetRemaining = errorBudgetRemaining;
    }

    public Map<String, Double> getErrorBudgetRemainChartRaw() {
        return errorBudgetRemainChart;
    }

    public void setErrorBudgetRemainChart(Map<String, Double> errorBudgetRemainChart) {
        this.errorBudgetRemainChart = errorBudgetRemainChart;
    }

    public long getFromTimestamp() {
        return fromTimestamp;
    }

    public void setFromTimestamp(long fromTimestamp) {
        this.fromTimestamp = fromTimestamp;
    }

    public long getToTimestamp() {
        return toTimestamp;
    }

    public void setToTimestamp(long toTimestamp) {
        this.toTimestamp = toTimestamp;
    }

    public TimeWindow getTimeWindow() {
        return timeWindow;
    }

    public void setTimeWindow(TimeWindow timeWindow) {
        this.timeWindow = timeWindow;
    }

    /**
     * Convert the map-based chart data to a list of ChartDataPoint objects
     * with actual timestamps calculated from fromTimestamp and toTimestamp
     */
    public List<ChartDataPoint> getErrorBudgetRemainChart() {
        if (errorBudgetRemainChart == null || errorBudgetRemainChart.isEmpty()) {
            return new ArrayList<>();
        }

        List<ChartDataPoint> dataPoints = new ArrayList<>();
        List<Integer> sortedKeys = new ArrayList<>();
        
        // Parse string keys to integers and sort them
        for (String key : errorBudgetRemainChart.keySet()) {
            try {
                sortedKeys.add(Integer.parseInt(key));
            } catch (NumberFormatException e) {
                // Skip invalid keys
            }
        }
        Collections.sort(sortedKeys);

        // Calculate time interval between data points
        int numPoints = sortedKeys.size();
        if (numPoints == 0) {
            return dataPoints;
        }
        
        long timeInterval = 0;
        if (numPoints > 1 && toTimestamp > fromTimestamp) {
            timeInterval = (toTimestamp - fromTimestamp) / (numPoints - 1);
        }

        // Convert to ChartDataPoint objects with actual timestamps
        for (int i = 0; i < sortedKeys.size(); i++) {
            Integer index = sortedKeys.get(i);
            Double value = errorBudgetRemainChart.get(String.valueOf(index));
            if (value != null) {
                // Calculate actual timestamp for this data point
                long timestamp = fromTimestamp + (i * timeInterval);
                dataPoints.add(new ChartDataPoint(timestamp, value));
            }
        }

        return dataPoints;
    }

    /**
     * Calculate the percentage of error budget remaining
     */
    public double getErrorBudgetRemainingPercentage() {
        if (totalErrorBudget == 0) {
            return 0.0;
        }
        return (errorBudgetRemaining / totalErrorBudget) * 100.0;
    }

    /**
     * Calculate the percentage of error budget consumed
     */
    public double getErrorBudgetConsumedPercentage() {
        return 100.0 - getErrorBudgetRemainingPercentage();
    }

    @Override
    public String toString() {
        return "SloReport{" +
                "sli=" + sli +
                ", sloTarget=" + sloTarget +
                ", totalErrorBudget=" + totalErrorBudget +
                ", errorBudgetRemaining=" + errorBudgetRemaining +
                ", chartDataPoints=" + (errorBudgetRemainChart != null ? errorBudgetRemainChart.size() : 0) +
                '}';
    }
}
