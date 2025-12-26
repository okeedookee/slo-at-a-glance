package io.instana.slo.ui.slolist;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import io.instana.slo.data.model.Slo;
import io.instana.slo.data.model.TrafficLightStatus;
import io.instana.slo.data.repository.SloRepository;
import io.instana.slo.util.PreferencesManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ViewModel for the SLO list screen
 */
public class SloListViewModel extends AndroidViewModel {
    private final SloRepository repository;
    private final PreferencesManager preferencesManager;
    private final MediatorLiveData<List<Slo>> filteredSlos;
    private final MutableLiveData<TrafficLightStatus> statusFilter;
    private final MutableLiveData<String> entityTypeFilter;
    private final MutableLiveData<SloRepository.Result<List<Slo>>> sloListResult;
    private final MutableLiveData<Boolean> isLoadingData;
    private LiveData<SloRepository.Result<List<Slo>>> allSlosLiveData;
    private List<Slo> allSlos;
    private boolean filtersInitialized = false;

    public SloListViewModel(@NonNull Application application) {
        super(application);
        repository = SloRepository.getInstance(application);
        preferencesManager = new PreferencesManager(application);
        filteredSlos = new MediatorLiveData<>();
        statusFilter = new MutableLiveData<>(null); // null means "all"
        entityTypeFilter = new MutableLiveData<>(null); // null means "all"
        sloListResult = new MutableLiveData<>();
        isLoadingData = new MutableLiveData<>(false);
        allSlos = new ArrayList<>();
        
        // Set up filter observers once
        filteredSlos.addSource(statusFilter, status -> applyFilters());
        filteredSlos.addSource(entityTypeFilter, entityType -> applyFilters());
        filtersInitialized = true;
    }

    /**
     * Get the filtered list of SLOs
     */
    public LiveData<List<Slo>> getFilteredSlos() {
        return filteredSlos;
    }

    /**
     * Load only SLO list (without reports) for selection purposes
     * Used in settings to avoid heavy API calls
     */
    public void loadSloListOnly() {
        if (allSlosLiveData != null) {
            filteredSlos.removeSource(allSlosLiveData);
        }

        allSlosLiveData = repository.getSloListOnly();
        
        filteredSlos.addSource(allSlosLiveData, result -> {
            // Forward the result to sloListResult for loading/error state observation
            sloListResult.setValue(result);
            
            if (result.status == SloRepository.Result.Status.SUCCESS && result.data != null) {
                allSlos = result.data;
                // Don't apply filters for list-only mode, just set all SLOs
                filteredSlos.setValue(allSlos);
            }
        });
    }

    /**
     * Load SLOs from the repository (list only, without reports initially)
     * Reports will be loaded only for filtered/visible SLOs
     */
    public void loadSlos() {
        if (allSlosLiveData != null) {
            filteredSlos.removeSource(allSlosLiveData);
        }

        // Use getSloListOnly() to fetch just the list without reports
        allSlosLiveData = repository.getSloListOnly();
        
        filteredSlos.addSource(allSlosLiveData, result -> {
            // Forward the result to sloListResult for loading/error state observation
            sloListResult.setValue(result);
            
            if (result.status == SloRepository.Result.Status.SUCCESS && result.data != null) {
                allSlos = result.data;
                applyFilters();
                // Load reports only for filtered SLOs
                loadReportsForFilteredSlos();
            }
        });
    }
    
    /**
     * Load reports only for SLOs that pass the filters
     */
    private void loadReportsForFilteredSlos() {
        List<Slo> filtered = filteredSlos.getValue();
        if (filtered != null && !filtered.isEmpty()) {
            android.util.Log.d("SloListViewModel", "Loading reports for " + filtered.size() + " filtered SLOs");
            
            // Set up listeners for each SLO to trigger UI updates when data loads
            for (Slo slo : filtered) {
                slo.setDataLoadedListener((loadedSlo, success) -> {
                    android.util.Log.d("SloListViewModel", "SLO data loaded: " + loadedSlo.getName() +
                        " (success: " + success + ") - triggering UI update");
                    // Trigger LiveData update by re-applying filters
                    // This will cause the Fragment to receive the updated list
                    applyFilters();
                });
            }
            
            repository.loadReportsForSlos(filtered, allSlos);
        }
    }

    /**
     * Get the raw repository result (for loading/error states)
     */
    public LiveData<SloRepository.Result<List<Slo>>> getSloListResult() {
        return sloListResult;
    }

    /**
     * Get loading data state (true if any SLOs are still loading)
     */
    public LiveData<Boolean> getIsLoadingData() {
        return isLoadingData;
    }

    /**
     * Set the status filter
     */
    public void setStatusFilter(TrafficLightStatus status) {
        statusFilter.setValue(status);
    }

    /**
     * Set the entity type filter
     */
    public void setEntityTypeFilter(String entityType) {
        entityTypeFilter.setValue(entityType);
    }

    /**
     * Get current status filter
     */
    public LiveData<TrafficLightStatus> getStatusFilter() {
        return statusFilter;
    }

    /**
     * Get current entity type filter
     */
    public LiveData<String> getEntityTypeFilter() {
        return entityTypeFilter;
    }

    /**
     * Clear all filters
     */
    public void clearFilters() {
        statusFilter.setValue(null);
        entityTypeFilter.setValue(null);
    }

    /**
     * Apply current filters to the SLO list
     */
    private void applyFilters() {
        if (allSlos == null || allSlos.isEmpty()) {
            filteredSlos.setValue(new ArrayList<>());
            isLoadingData.setValue(false);
            return;
        }

        // Create a new list with the same SLO references
        // Note: We don't clone Slo objects because DiffUtil needs to detect
        // changes in the same objects (status, loadingState, report fields)
        List<Slo> filtered = new ArrayList<>(allSlos);

        // Apply SLO selection filter (from settings)
        // If no SLOs are selected, show nothing
        Set<String> selectedSloIds = preferencesManager.getSelectedSloIds();
        if (selectedSloIds.isEmpty()) {
            filteredSlos.setValue(new ArrayList<>());
            isLoadingData.setValue(false);
            return;
        }
        
        filtered = filtered.stream()
                .filter(slo -> selectedSloIds.contains(slo.getId()))
                .collect(Collectors.toList());

        // Apply status filter
        TrafficLightStatus currentStatusFilter = statusFilter.getValue();
        if (currentStatusFilter != null) {
            filtered = filtered.stream()
                    .filter(slo -> slo.getStatus() == currentStatusFilter)
                    .collect(Collectors.toList());
        }

        // Apply entity type filter
        String currentEntityTypeFilter = entityTypeFilter.getValue();
        if (currentEntityTypeFilter != null && !currentEntityTypeFilter.isEmpty()) {
            android.util.Log.d("SloListViewModel", "Filtering by entity type: " + currentEntityTypeFilter);
            android.util.Log.d("SloListViewModel", "Total SLOs before entity filter: " + filtered.size());
            
            // Count SLOs with null entities for debugging
            long nullEntityCount = filtered.stream()
                    .filter(slo -> slo.getEntity() == null || slo.getEntity().getEntityType() == null)
                    .count();
            android.util.Log.d("SloListViewModel", "SLOs with null entity/entityType: " + nullEntityCount);
            
            filtered = filtered.stream()
                    .filter(slo -> {
                        if (slo.getEntity() == null) {
                            android.util.Log.d("SloListViewModel", "SLO '" + slo.getName() + "' has null entity");
                            return false;
                        }
                        if (slo.getEntity().getEntityType() == null) {
                            android.util.Log.d("SloListViewModel", "SLO '" + slo.getName() + "' has null entityType");
                            return false;
                        }
                        String entityType = slo.getEntity().getEntityType().trim();
                        boolean matches = currentEntityTypeFilter.equalsIgnoreCase(entityType);
                        android.util.Log.d("SloListViewModel", "SLO '" + slo.getName() + "' entityType: '" + entityType + "' matches: " + matches);
                        return matches;
                    })
                    .collect(Collectors.toList());
            android.util.Log.d("SloListViewModel", "After entity type filter: " + filtered.size() + " SLOs");
        }
        
        // Check if any FILTERED SLOs are still loading (not all SLOs)
        // This ensures the loading indicator reflects only the visible SLOs
        boolean anyLoading = filtered.stream()
                .anyMatch(slo -> slo.getLoadingState() == Slo.LoadingState.LOADING ||
                                slo.getLoadingState() == Slo.LoadingState.NOT_LOADED);
        isLoadingData.setValue(anyLoading);

        // Always set a new list instance to trigger LiveData observers
        filteredSlos.setValue(new ArrayList<>(filtered));
    }

    /**
     * Refresh the SLO list
     */
    public void refresh() {
        loadSlos();
    }

    /**
     * Get count by status
     */
    public int getCountByStatus(TrafficLightStatus status) {
        if (allSlos == null) return 0;
        return (int) allSlos.stream()
                .filter(slo -> slo.getStatus() == status)
                .count();
    }

    /**
     * Get count by entity type
     */
    public int getCountByEntityType(String entityType) {
        if (allSlos == null) return 0;
        return (int) allSlos.stream()
                .filter(slo -> slo.getEntity() != null && 
                        entityType.equalsIgnoreCase(slo.getEntity().getEntityType()))
                .count();
    }
}
