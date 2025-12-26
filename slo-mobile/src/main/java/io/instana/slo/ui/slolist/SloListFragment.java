package io.instana.slo.ui.slolist;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import io.instana.slo.R;
import io.instana.slo.data.model.Slo;
import io.instana.slo.data.model.TrafficLightStatus;
import io.instana.slo.ui.slodetail.SloDetailActivity;
import io.instana.slo.util.PreferencesManager;

import java.util.ArrayList;

/**
 * Fragment displaying the list of SLOs with traffic light visualization
 */
public class SloListFragment extends Fragment implements SloAdapter.OnSloClickListener {
    private static final String TAG = "SloListFragment";
    private SloListViewModel viewModel;
    private SloAdapter adapter;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private TextView emptyView;
    private TextView errorView;
    private TextView loadingIndicator;
    private Spinner statusFilterSpinner;
    private Spinner entityTypeFilterSpinner;
    private PreferencesManager preferencesManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_slo_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        preferencesManager = new PreferencesManager(requireContext());
        
        // Initialize views
        recyclerView = view.findViewById(R.id.recycler_view);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
        progressBar = view.findViewById(R.id.progress_bar);
        emptyView = view.findViewById(R.id.empty_view);
        errorView = view.findViewById(R.id.error_view);
        loadingIndicator = view.findViewById(R.id.loading_indicator);
        statusFilterSpinner = view.findViewById(R.id.status_filter_spinner);
        entityTypeFilterSpinner = view.findViewById(R.id.entity_type_filter_spinner);

        // Set up RecyclerView with 2 columns
        adapter = new SloAdapter(this);
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        recyclerView.setAdapter(adapter);

        // Set up ViewModel
        viewModel = new ViewModelProvider(this).get(SloListViewModel.class);

        // Observe filtered SLOs
        viewModel.getFilteredSlos().observe(getViewLifecycleOwner(), slos -> {
            Log.d(TAG, "========================================");
            Log.d(TAG, "SLO List Data Received in Fragment");
            Log.d(TAG, "  Total SLOs: " + (slos != null ? slos.size() : 0));
            if (slos != null) {
                for (Slo slo : slos) {
                    Log.d(TAG, "  - " + slo.getName() + " (Status: " + slo.getStatus() + ", Loading: " + slo.getLoadingState() + ")");
                }
            }
            Log.d(TAG, "  Submitting list to adapter...");
            // Submit the list directly - the ViewModel already creates new list instances
            adapter.submitList(slos);
            updateEmptyView(slos == null || slos.isEmpty());
            Log.d(TAG, "========================================");
        });

        // Observe loading/error states
        viewModel.getSloListResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null) {
                Log.d(TAG, "SLO List Result Status: " + result.status);
                switch (result.status) {
                    case LOADING:
                        Log.d(TAG, "  -> Showing loading indicator");
                        showLoading(true);
                        hideError();
                        break;
                    case SUCCESS:
                        Log.d(TAG, "  -> Data loaded successfully");
                        showLoading(false);
                        hideError();
                        break;
                    case ERROR:
                        Log.e(TAG, "  -> Error loading data: " + result.message);
                        showLoading(false);
                        showError(result.message);
                        break;
                }
            }
        });

        // Observe loading data state
        viewModel.getIsLoadingData().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading != null) {
                Log.d(TAG, "Loading data state changed: " + isLoading);
                Log.d(TAG, "Loading data state changed: " + isLoading);
                loadingIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
        });

        // Set up swipe refresh
        swipeRefreshLayout.setOnRefreshListener(() -> {
            refresh();
            swipeRefreshLayout.setRefreshing(false);
        });

        // Set up filters
        setupFilters();

        // Load data only if configured AND SLOs are selected
        if (preferencesManager.isConfigured()) {
            if (preferencesManager.hasSelectedSlos()) {
                viewModel.loadSlos();
            } else {
                // Show empty state with message to select SLOs
                updateEmptyView(true);
            }
        } else {
            showError(getString(R.string.error_not_configured));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh when returning from settings in case selection changed
        if (preferencesManager.isConfigured()) {
            if (preferencesManager.hasSelectedSlos()) {
                // Reload data to reflect any changes in selection
                viewModel.loadSlos();
            } else {
                // No SLOs selected - clear the list and show empty view
                adapter.submitList(new ArrayList<>());
                updateEmptyView(true);
            }
        }
    }

    /**
     * Set up filter spinners
     */
    private void setupFilters() {
        // Set up status filter spinner
        String[] statusOptions = {
            getString(R.string.filter_all),
            getString(R.string.filter_green),
            getString(R.string.filter_yellow),
            getString(R.string.filter_red)
        };
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            statusOptions
        );
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusFilterSpinner.setAdapter(statusAdapter);
        statusFilterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                TrafficLightStatus status = null;
                switch (position) {
                    case 1: status = TrafficLightStatus.GREEN; break;
                    case 2: status = TrafficLightStatus.YELLOW; break;
                    case 3: status = TrafficLightStatus.RED; break;
                }
                viewModel.setStatusFilter(status);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        // Set up entity type filter spinner
        String[] entityTypeOptions = {
            getString(R.string.filter_all),
            getString(R.string.entity_application),
            getString(R.string.entity_website),
            getString(R.string.entity_synthetic),
            getString(R.string.entity_infrastructure)
        };
        ArrayAdapter<String> entityTypeAdapter = new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            entityTypeOptions
        );
        entityTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        entityTypeFilterSpinner.setAdapter(entityTypeAdapter);
        entityTypeFilterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String entityType = null;
                switch (position) {
                    case 1: entityType = "application"; break;
                    case 2: entityType = "website"; break;
                    case 3: entityType = "synthetic"; break;
                    case 4: entityType = "infrastructure"; break;
                }
                viewModel.setEntityTypeFilter(entityType);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    /**
     * Show/hide loading indicator
     */
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    /**
     * Show error message
     */
    private void showError(String message) {
        errorView.setText(message);
        errorView.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }

    /**
     * Hide error message
     */
    private void hideError() {
        errorView.setVisibility(View.GONE);
    }

    /**
     * Update empty view visibility
     */
    private void updateEmptyView(boolean isEmpty) {
        emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    /**
     * Refresh the SLO list
     */
    public void refresh() {
        if (preferencesManager.isConfigured()) {
            if (preferencesManager.hasSelectedSlos()) {
                viewModel.refresh();
            } else {
                Toast.makeText(requireContext(), "Please select SLOs in Settings first", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(requireContext(), R.string.error_not_configured, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSloClick(Slo slo) {
        // Open detail activity
        Intent intent = new Intent(requireContext(), SloDetailActivity.class);
        intent.putExtra(SloDetailActivity.EXTRA_SLO_ID, slo.getId());
        intent.putExtra(SloDetailActivity.EXTRA_SLO_NAME, slo.getName());
        startActivity(intent);
    }
}
