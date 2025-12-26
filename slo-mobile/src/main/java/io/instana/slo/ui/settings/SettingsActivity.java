package io.instana.slo.ui.settings;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.JsonObject;
import io.instana.slo.R;
import io.instana.slo.data.api.ApiClient;
import io.instana.slo.data.api.InstanaApiService;
import io.instana.slo.data.model.Slo;
import io.instana.slo.data.repository.SloRepository;
import io.instana.slo.ui.slolist.SloListViewModel;
import io.instana.slo.util.PreferencesManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Settings activity for configuring API credentials and app preferences
 */
public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.settings_title);
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, new SettingsFragment())
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        private PreferencesManager preferencesManager;
        private SloListViewModel viewModel;
        private List<Slo> availableSlos = new ArrayList<>();

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);

            preferencesManager = new PreferencesManager(requireContext());
            viewModel = new ViewModelProvider(this).get(SloListViewModel.class);

            // Set up API endpoint preference
            EditTextPreference endpointPref = findPreference("api_endpoint");
            if (endpointPref != null) {
                endpointPref.setSummary(preferencesManager.getApiEndpoint());
                endpointPref.setOnPreferenceChangeListener((preference, newValue) -> {
                    String endpoint = newValue.toString().trim();
                    if (endpoint.isEmpty()) {
                        Toast.makeText(requireContext(), R.string.error_empty_endpoint, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    preferencesManager.setApiEndpoint(endpoint);
                    preference.setSummary(endpoint);
                    resetApiClient();
                    return true;
                });
            }

            // Set up API token preference
            EditTextPreference tokenPref = findPreference("api_token");
            if (tokenPref != null) {
                // Mask the token in summary
                String token = preferencesManager.getApiToken();
                tokenPref.setSummary(token.isEmpty() ? getString(R.string.not_configured) : "••••••••");
                
                // Set input type to password
                tokenPref.setOnBindEditTextListener(editText -> {
                    editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                });

                tokenPref.setOnPreferenceChangeListener((preference, newValue) -> {
                    String newToken = newValue.toString().trim();
                    if (newToken.isEmpty()) {
                        Toast.makeText(requireContext(), R.string.error_empty_token, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    preferencesManager.setApiToken(newToken);
                    preference.setSummary("••••••••");
                    resetApiClient();
                    return true;
                });
            }

            // Set up yellow threshold preference
            EditTextPreference thresholdPref = findPreference("yellow_threshold");
            if (thresholdPref != null) {
                double threshold = preferencesManager.getYellowThreshold();
                thresholdPref.setSummary(String.format("%.0f%%", threshold));
                
                thresholdPref.setOnBindEditTextListener(editText -> {
                    editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                });

                thresholdPref.setOnPreferenceChangeListener((preference, newValue) -> {
                    try {
                        double value = Double.parseDouble(newValue.toString());
                        if (value < 0 || value > 100) {
                            Toast.makeText(requireContext(), R.string.error_invalid_threshold, Toast.LENGTH_SHORT).show();
                            return false;
                        }
                        preferencesManager.setYellowThreshold(value);
                        preference.setSummary(String.format("%.0f%%", value));
                        return true;
                    } catch (NumberFormatException e) {
                        Toast.makeText(requireContext(), R.string.error_invalid_number, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                });
            }

            // Set up reset to defaults preference
            Preference resetPref = findPreference("reset_defaults");
            if (resetPref != null) {
                resetPref.setOnPreferenceClickListener(preference -> {
                    showResetConfirmationDialog();
                    return true;
                });
            }

            // Set up clear data preference
            Preference clearDataPref = findPreference("clear_data");
            if (clearDataPref != null) {
                clearDataPref.setOnPreferenceClickListener(preference -> {
                    showClearDataConfirmationDialog();
                    return true;
                });
            }

            // Set up SLO selection preference
            Preference sloSelectionPref = findPreference("slo_selection");
            if (sloSelectionPref != null) {
                updateSloSelectionSummary(sloSelectionPref);
                sloSelectionPref.setOnPreferenceClickListener(preference -> {
                    if (!preferencesManager.isConfigured()) {
                        Toast.makeText(requireContext(), R.string.error_not_configured, Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    
                    // Load SLOs only when user clicks to select
                    if (availableSlos.isEmpty()) {
                        loadSlosForSelection();
                    } else {
                        showSloSelectionDialog();
                    }
                    return true;
                });
            }
        }

        @Override
        public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            
            // Set up the validate button click listener
            RecyclerView recyclerView = getListView();
            recyclerView.post(() -> {
                // Find the button in the preference layout
                for (int i = 0; i < recyclerView.getChildCount(); i++) {
                    View child = recyclerView.getChildAt(i);
                    Button validateButton = child.findViewById(R.id.validate_button);
                    if (validateButton != null) {
                        validateButton.setOnClickListener(v -> validateApiConfiguration());
                        break;
                    }
                }
            });
        }

        private void resetApiClient() {
            // Reset the API client when credentials change
            SloRepository.resetInstance();
            Toast.makeText(requireContext(), R.string.settings_saved, Toast.LENGTH_SHORT).show();
        }

        private void showResetConfirmationDialog() {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.reset_defaults_title)
                    .setMessage(R.string.reset_defaults_message)
                    .setPositiveButton(R.string.reset, (dialog, which) -> {
                        // Reset to default values
                        preferencesManager.setApiEndpoint(PreferencesManager.getDefaultApiEndpoint());
                        preferencesManager.setYellowThreshold(PreferencesManager.getDefaultYellowThreshold());
                        
                        // Update UI
                        EditTextPreference endpointPref = findPreference("api_endpoint");
                        if (endpointPref != null) {
                            endpointPref.setSummary(PreferencesManager.getDefaultApiEndpoint());
                        }
                        
                        EditTextPreference thresholdPref = findPreference("yellow_threshold");
                        if (thresholdPref != null) {
                            thresholdPref.setSummary(String.format("%.0f%%", PreferencesManager.getDefaultYellowThreshold()));
                        }
                        
                        resetApiClient();
                        Toast.makeText(requireContext(), R.string.defaults_restored, Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        }

        private void showClearDataConfirmationDialog() {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.clear_data_title)
                    .setMessage(R.string.clear_data_message)
                    .setPositiveButton(R.string.clear, (dialog, which) -> {
                        preferencesManager.clearAll();
                        
                        // Update UI
                        EditTextPreference endpointPref = findPreference("api_endpoint");
                        if (endpointPref != null) {
                            endpointPref.setSummary(PreferencesManager.getDefaultApiEndpoint());
                        }
                        
                        EditTextPreference tokenPref = findPreference("api_token");
                        if (tokenPref != null) {
                            tokenPref.setSummary(getString(R.string.not_configured));
                        }
                        
                        EditTextPreference thresholdPref = findPreference("yellow_threshold");
                        if (thresholdPref != null) {
                            thresholdPref.setSummary(String.format("%.0f%%", PreferencesManager.getDefaultYellowThreshold()));
                        }
                        
                        resetApiClient();
                        Toast.makeText(requireContext(), R.string.data_cleared, Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        }

        private void showSloSelectionDialog() {
            if (!preferencesManager.isConfigured()) {
                Toast.makeText(requireContext(), R.string.error_not_configured, Toast.LENGTH_SHORT).show();
                return;
            }

            if (availableSlos.isEmpty()) {
                Toast.makeText(requireContext(), R.string.slo_selection_error, Toast.LENGTH_SHORT).show();
                return;
            }

            // Inflate custom dialog layout
            View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_slo_selection, null);
            EditText searchEditText = dialogView.findViewById(R.id.search_edit_text);
            ListView listView = dialogView.findViewById(R.id.slo_list_view);

            // Prepare data
            List<Slo> filteredSlos = new ArrayList<>(availableSlos);
            Set<String> selectedIds = preferencesManager.getSelectedSloIds();
            Set<String> currentSelection = new HashSet<>(selectedIds);

            // Create adapter
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    requireContext(),
                    android.R.layout.simple_list_item_multiple_choice,
                    filteredSlos.stream().map(Slo::getName).collect(Collectors.toList())
            );
            listView.setAdapter(adapter);

            // Set initial checked state
            for (int i = 0; i < filteredSlos.size(); i++) {
                listView.setItemChecked(i, currentSelection.contains(filteredSlos.get(i).getId()));
            }

            // Handle item clicks
            listView.setOnItemClickListener((parent, view, position, id) -> {
                String sloId = filteredSlos.get(position).getId();
                if (listView.isItemChecked(position)) {
                    currentSelection.add(sloId);
                } else {
                    currentSelection.remove(sloId);
                }
            });

            // Handle search
            searchEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String query = s.toString().toLowerCase().trim();
                    filteredSlos.clear();
                    
                    if (query.isEmpty()) {
                        filteredSlos.addAll(availableSlos);
                    } else {
                        filteredSlos.addAll(availableSlos.stream()
                                .filter(slo -> slo.getName().toLowerCase().contains(query))
                                .collect(Collectors.toList()));
                    }
                    
                    // Update adapter
                    adapter.clear();
                    adapter.addAll(filteredSlos.stream().map(Slo::getName).collect(Collectors.toList()));
                    
                    // Restore checked state
                    for (int i = 0; i < filteredSlos.size(); i++) {
                        listView.setItemChecked(i, currentSelection.contains(filteredSlos.get(i).getId()));
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            // Show dialog
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.slo_selection_dialog_title)
                    .setView(dialogView)
                    .setPositiveButton(R.string.ok, (dialog, which) -> {
                        preferencesManager.setSelectedSloIds(currentSelection);
                        
                        // Update summary
                        Preference sloSelectionPref = findPreference("slo_selection");
                        if (sloSelectionPref != null) {
                            updateSloSelectionSummary(sloSelectionPref);
                        }
                        
                        Toast.makeText(requireContext(), R.string.settings_saved, Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .setNeutralButton("Clear All", (dialog, which) -> {
                        // Clear all selections
                        preferencesManager.setSelectedSloIds(new HashSet<>());
                        
                        Preference sloSelectionPref = findPreference("slo_selection");
                        if (sloSelectionPref != null) {
                            updateSloSelectionSummary(sloSelectionPref);
                        }
                        
                        Toast.makeText(requireContext(), R.string.settings_saved, Toast.LENGTH_SHORT).show();
                    })
                    .show();
        }

        private void loadSlosForSelection() {
            // Show loading message
            Toast.makeText(requireContext(), R.string.loading_slos, Toast.LENGTH_SHORT).show();
            
            // Use lightweight method that only fetches SLO list without reports
            viewModel.loadSloListOnly();
            viewModel.getFilteredSlos().observe(this, slos -> {
                if (slos != null && !slos.isEmpty()) {
                    availableSlos = slos;
                    // Show dialog after data is loaded
                    showSloSelectionDialog();
                } else if (slos != null && slos.isEmpty()) {
                    Toast.makeText(requireContext(), R.string.slo_selection_error, Toast.LENGTH_SHORT).show();
                }
            });
        }

        private void updateSloSelectionSummary(Preference preference) {
            Set<String> selectedIds = preferencesManager.getSelectedSloIds();
            if (selectedIds.isEmpty()) {
                preference.setSummary(R.string.slo_selection_none);
            } else {
                preference.setSummary(getString(R.string.slo_selection_count, selectedIds.size()));
            }
        }

        private void validateApiConfiguration() {
            if (!preferencesManager.isConfigured()) {
                Toast.makeText(requireContext(), R.string.error_not_configured, Toast.LENGTH_SHORT).show();
                return;
            }

            // Show loading message
            Toast.makeText(requireContext(), R.string.validating_api, Toast.LENGTH_SHORT).show();

            // Reset API client to use current settings
            ApiClient.resetApiService();
            InstanaApiService apiService = ApiClient.getApiService(requireContext());

            // Call version endpoint
            Call<JsonObject> call = apiService.getVersion();
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        JsonObject versionInfo = response.body();
                        String versionText = formatVersionInfo(versionInfo);
                        
                        new MaterialAlertDialogBuilder(requireContext())
                                .setTitle(R.string.validate_api_title)
                                .setMessage(getString(R.string.validation_success, versionText))
                                .setPositiveButton(R.string.ok, null)
                                .show();
                    } else {
                        String errorMsg = "HTTP " + response.code();
                        if (response.message() != null && !response.message().isEmpty()) {
                            errorMsg += ": " + response.message();
                        }
                        showValidationError(errorMsg);
                    }
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    showValidationError(t.getMessage() != null ? t.getMessage() : "Unknown error");
                }
            });
        }

        private String formatVersionInfo(JsonObject versionInfo) {
            StringBuilder sb = new StringBuilder();
            
            // Try to extract version information from the response
            if (versionInfo.has("version")) {
                sb.append(versionInfo.get("version").getAsString());
            } else if (versionInfo.has("buildVersion")) {
                sb.append(versionInfo.get("buildVersion").getAsString());
            } else {
                // If no specific version field, show the entire response
                sb.append(versionInfo.toString());
            }
            
            return sb.toString();
        }

        private void showValidationError(String errorMessage) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.validate_api_title)
                    .setMessage(getString(R.string.validation_failed, errorMessage))
                    .setPositiveButton(R.string.ok, null)
                    .show();
        }
    }
}
