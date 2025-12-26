package io.instana.slo.util;

import io.instana.slo.data.model.TrafficLightStatus;

/**
 * Utility class for calculating traffic light status based on SLO metrics
 */
public class TrafficLightCalculator {

    /**
     * Calculate the traffic light status for an SLO
     * 
     * Priority order:
     * 1. RED: If SLI <= SLO target (SLO is not being met)
     * 2. YELLOW: If error budget remaining is below threshold
     * 3. GREEN: Otherwise (SLO is being met and error budget is healthy)
     * 
     * @param sli Current SLI value (Service Level Indicator)
     * @param sloTarget Target SLO value
     * @param errorBudgetRemaining Remaining error budget
     * @param totalErrorBudget Total error budget
     * @param yellowThreshold Threshold percentage for yellow status (0-100)
     * @return TrafficLightStatus (GREEN, YELLOW, or RED)
     */
    public static TrafficLightStatus calculate(
            double sli,
            double sloTarget,
            double errorBudgetRemaining,
            double totalErrorBudget,
            double yellowThreshold) {

        // Priority 1: Check if SLO is being met
        // If SLI <= SLO target, the SLO is not being met -> RED
        if (sli <= sloTarget) {
            return TrafficLightStatus.RED;
        }

        // Priority 2: Check error budget health
        // Calculate the percentage of error budget remaining
        double remainingPercentage = 0.0;
        if (totalErrorBudget > 0) {
            remainingPercentage = (errorBudgetRemaining / totalErrorBudget) * 100.0;
        }

        // If remaining error budget is below threshold -> YELLOW
        if (remainingPercentage <= yellowThreshold) {
            return TrafficLightStatus.YELLOW;
        }

        // Otherwise, everything is healthy -> GREEN
        return TrafficLightStatus.GREEN;
    }

    /**
     * Calculate the traffic light status with default yellow threshold (50%)
     */
    public static TrafficLightStatus calculate(
            double sli,
            double sloTarget,
            double errorBudgetRemaining,
            double totalErrorBudget) {
        return calculate(sli, sloTarget, errorBudgetRemaining, totalErrorBudget, 50.0);
    }

    /**
     * Get a human-readable description of the status
     */
    public static String getStatusDescription(TrafficLightStatus status, double sli, double sloTarget, double errorBudgetRemainingPercentage) {
        switch (status) {
            case GREEN:
                return String.format("Healthy - SLI: %.2f%% (Target: %.2f%%), Error Budget: %.1f%% remaining",
                        sli * 100, sloTarget * 100, errorBudgetRemainingPercentage);
            case YELLOW:
                return String.format("Warning - Error budget low: %.1f%% remaining (SLI: %.2f%%)",
                        errorBudgetRemainingPercentage, sli * 100);
            case RED:
                return String.format("Critical - SLO not met: SLI %.2f%% below target %.2f%%",
                        sli * 100, sloTarget * 100);
            default:
                return "Unknown status";
        }
    }
}
