package io.instana.slo.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashSet;
import java.util.Set;

/**
 * Manager for application preferences including secure storage for API credentials
 */
public class PreferencesManager {
    private static final String PREFS_NAME = "slo_preferences";
    private static final String ENCRYPTED_PREFS_NAME = "slo_encrypted_preferences";
    
    // Preference keys
    private static final String KEY_API_ENDPOINT = "api_endpoint";
    private static final String KEY_API_TOKEN = "api_token";
    private static final String KEY_YELLOW_THRESHOLD = "yellow_threshold";
    private static final String KEY_FIRST_RUN = "first_run";
    private static final String KEY_SELECTED_SLO_IDS = "selected_slo_ids";
    
    // Default values
    private static final String DEFAULT_API_ENDPOINT = "https://instana.io";
    private static final double DEFAULT_YELLOW_THRESHOLD = 50.0;

    private final SharedPreferences preferences;
    private final SharedPreferences encryptedPreferences;

    public PreferencesManager(Context context) {
        // Regular preferences for non-sensitive data
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        // Encrypted preferences for sensitive data (API token)
        encryptedPreferences = createEncryptedPreferences(context);
    }

    /**
     * Create encrypted shared preferences for secure storage
     */
    private SharedPreferences createEncryptedPreferences(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            return EncryptedSharedPreferences.create(
                    context,
                    ENCRYPTED_PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            // Fallback to regular preferences if encryption fails
            return context.getSharedPreferences(ENCRYPTED_PREFS_NAME, Context.MODE_PRIVATE);
        }
    }

    // API Endpoint
    public String getApiEndpoint() {
        return preferences.getString(KEY_API_ENDPOINT, DEFAULT_API_ENDPOINT);
    }

    public void setApiEndpoint(String endpoint) {
        preferences.edit().putString(KEY_API_ENDPOINT, endpoint).apply();
    }

    // API Token (stored encrypted)
    public String getApiToken() {
        return encryptedPreferences.getString(KEY_API_TOKEN, "");
    }

    public void setApiToken(String token) {
        encryptedPreferences.edit().putString(KEY_API_TOKEN, token).apply();
    }

    // Yellow Threshold
    public double getYellowThreshold() {
        return Double.longBitsToDouble(
                preferences.getLong(KEY_YELLOW_THRESHOLD, 
                        Double.doubleToLongBits(DEFAULT_YELLOW_THRESHOLD))
        );
    }

    public void setYellowThreshold(double threshold) {
        preferences.edit()
                .putLong(KEY_YELLOW_THRESHOLD, Double.doubleToLongBits(threshold))
                .apply();
    }

    // First run flag
    public boolean isFirstRun() {
        return preferences.getBoolean(KEY_FIRST_RUN, true);
    }

    public void setFirstRun(boolean firstRun) {
        preferences.edit().putBoolean(KEY_FIRST_RUN, firstRun).apply();
    }

    /**
     * Check if API credentials are configured
     */
    public boolean isConfigured() {
        String endpoint = getApiEndpoint();
        String token = getApiToken();
        return endpoint != null && !endpoint.isEmpty() && 
               token != null && !token.isEmpty();
    }

    /**
     * Clear all preferences
     */
    public void clearAll() {
        preferences.edit().clear().apply();
        encryptedPreferences.edit().clear().apply();
    }

    /**
     * Get default API endpoint
     */
    public static String getDefaultApiEndpoint() {
        return DEFAULT_API_ENDPOINT;
    }

    /**
     * Get default yellow threshold
     */
    public static double getDefaultYellowThreshold() {
        return DEFAULT_YELLOW_THRESHOLD;
    }

    // Selected SLO IDs
    /**
     * Get the set of selected SLO IDs
     * @return Set of SLO IDs that should be displayed, or empty set if none selected
     */
    public Set<String> getSelectedSloIds() {
        // Always return a new HashSet to avoid SharedPreferences mutation issues
        Set<String> stored = preferences.getStringSet(KEY_SELECTED_SLO_IDS, new HashSet<>());
        return stored != null ? new HashSet<>(stored) : new HashSet<>();
    }

    /**
     * Set the selected SLO IDs
     * @param sloIds Set of SLO IDs to display
     */
    public void setSelectedSloIds(Set<String> sloIds) {
        // Create a new HashSet and commit synchronously to ensure it's saved
        Set<String> newSet = sloIds != null ? new HashSet<>(sloIds) : new HashSet<>();
        android.util.Log.d("PreferencesManager", "Saving selected SLO IDs: " + newSet);
        preferences.edit()
                .remove(KEY_SELECTED_SLO_IDS)  // Remove first to avoid mutation issues
                .putStringSet(KEY_SELECTED_SLO_IDS, newSet)
                .commit();  // Use commit() instead of apply() to ensure immediate save
        
        // Verify it was saved
        Set<String> verified = preferences.getStringSet(KEY_SELECTED_SLO_IDS, new HashSet<>());
        android.util.Log.d("PreferencesManager", "Verified saved SLO IDs: " + verified);
    }

    /**
     * Check if a specific SLO is selected
     * @param sloId The SLO ID to check
     * @return true if the SLO is selected
     */
    public boolean isSloSelected(String sloId) {
        Set<String> selectedIds = getSelectedSloIds();
        return selectedIds.contains(sloId);
    }

    /**
     * Check if SLO selection filter is active
     * @return true if specific SLOs are selected, false if none selected
     */
    public boolean hasSelectedSlos() {
        Set<String> selectedIds = getSelectedSloIds();
        boolean hasSelected = !selectedIds.isEmpty();
        android.util.Log.d("PreferencesManager", "hasSelectedSlos() = " + hasSelected + ", IDs: " + selectedIds);
        return hasSelected;
    }
}
