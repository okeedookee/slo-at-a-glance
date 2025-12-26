package io.instana.slo.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import io.instana.slo.data.api.ApiClient;
import io.instana.slo.data.api.InstanaApiService;
import io.instana.slo.data.model.Slo;
import io.instana.slo.data.model.SloListResponse;
import io.instana.slo.data.model.SloReport;
import io.instana.slo.util.TrafficLightCalculator;
import io.instana.slo.util.PreferencesManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository for managing SLO data
 * Handles API calls and data transformation
 */
public class SloRepository {
    private static final String TAG = "SloRepository";
    private static SloRepository instance;
    
    private final InstanaApiService apiService;
    private final PreferencesManager preferencesManager;
    private final Context context;

    private SloRepository(Context context) {
        this.context = context.getApplicationContext();
        this.apiService = ApiClient.getApiService(context);
        this.preferencesManager = new PreferencesManager(context);
    }

    /**
     * Get singleton instance of the repository
     */
    public static synchronized SloRepository getInstance(Context context) {
        if (instance == null) {
            instance = new SloRepository(context);
        }
        return instance;
    }

    /**
     * Reset the repository (useful when API settings change)
     */
    public static void resetInstance() {
        instance = null;
        ApiClient.resetApiService();
    }

    /**
     * Fetch only the list of SLOs from the API (without reports)
     * Used for SLO selection in settings
     *
     * @return LiveData containing the list of SLOs without status
     */
    public LiveData<Result<List<Slo>>> getSloListOnly() {
        MutableLiveData<Result<List<Slo>>> result = new MutableLiveData<>();
        result.setValue(Result.loading());

        apiService.getSloList().enqueue(new Callback<SloListResponse>() {
            @Override
            public void onResponse(Call<SloListResponse> call, Response<SloListResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Log raw response for debugging
                    try {
                        String rawJson = new com.google.gson.Gson().toJson(response.body());
                        Log.d(TAG, "Raw API Response: " + rawJson);
                    } catch (Exception e) {
                        Log.e(TAG, "Could not serialize response", e);
                    }
                    
                    List<Slo> slos = response.body().getItems();
                    if (slos != null) {
                        Log.d(TAG, "========================================");
                        Log.d(TAG, "Fetched " + slos.size() + " SLOs from API");
                        Log.d(TAG, "========================================");
                        // Log detailed information for each SLO
                        for (Slo slo : slos) {
                            Log.d(TAG, "SLO: '" + slo.getName() + "' (id: " + slo.getId() + ")");
                            if (slo.getEntity() == null) {
                                Log.d(TAG, "  -> Entity: NULL");
                            } else {
                                Log.d(TAG, "  -> Entity: " + slo.getEntity().toString());
                                if (slo.getEntity().getEntityType() == null) {
                                    Log.d(TAG, "  -> EntityType: NULL");
                                } else {
                                    Log.d(TAG, "  -> EntityType: '" + slo.getEntity().getEntityType() + "'");
                                }
                            }
                        }
                        Log.d(TAG, "========================================");
                        result.setValue(Result.success(slos));
                    } else {
                        Log.w(TAG, "API returned null items list");
                        result.setValue(Result.success(new ArrayList<>()));
                    }
                } else {
                    String errorMsg = "Failed to fetch SLOs: " + response.code();
                    Log.e(TAG, errorMsg);
                    if (response.errorBody() != null) {
                        try {
                            Log.e(TAG, "Error body: " + response.errorBody().string());
                        } catch (IOException e) {
                            Log.e(TAG, "Could not read error body", e);
                        }
                    }
                    result.setValue(Result.error(errorMsg));
                }
            }

            @Override
            public void onFailure(Call<SloListResponse> call, Throwable t) {
                String errorMsg = "Network error: " + t.getMessage();
                Log.e(TAG, errorMsg, t);
                result.setValue(Result.error(errorMsg));
            }
        });

        return result;
    }

    /**
     * Fetch the list of SLOs from the API with their reports and status
     * Each SLO is loaded independently in its own thread
     *
     * @return LiveData containing the list of SLOs with their status
     */
    public LiveData<Result<List<Slo>>> getSloList() {
        MutableLiveData<Result<List<Slo>>> result = new MutableLiveData<>();
        result.setValue(Result.loading());

        apiService.getSloList().enqueue(new Callback<SloListResponse>() {
            @Override
            public void onResponse(Call<SloListResponse> call, Response<SloListResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Slo> slos = response.body().getItems();
                    if (slos != null) {
                        // Set all SLOs to NOT_LOADED state initially
                        for (Slo slo : slos) {
                            slo.setLoadingState(Slo.LoadingState.NOT_LOADED);
                        }
                        // Return the list immediately so UI can display SLOs
                        result.setValue(Result.success(slos));
                        // Then fetch reports for each SLO independently
                        fetchReportsForSlosIndependently(slos, result);
                    } else {
                        result.setValue(Result.success(new ArrayList<>()));
                    }
                } else {
                    String errorMsg = "Failed to fetch SLOs: " + response.code();
                    Log.e(TAG, errorMsg);
                    result.setValue(Result.error(errorMsg));
                }
            }

            @Override
            public void onFailure(Call<SloListResponse> call, Throwable t) {
                String errorMsg = "Network error: " + t.getMessage();
                Log.e(TAG, errorMsg, t);
                result.setValue(Result.error(errorMsg));
            }
        });

        return result;
    }

    /**
     * Load reports only for specific SLOs (lazy loading)
     * Used to load reports only for filtered/visible SLOs
     *
     * @param slosToLoad List of SLOs to load reports for
     * @param allSlos Complete list of all SLOs (for LiveData updates)
     */
    public void loadReportsForSlos(List<Slo> slosToLoad, List<Slo> allSlos) {
        if (slosToLoad == null || slosToLoad.isEmpty()) {
            return;
        }
        
        Log.d(TAG, "Loading reports for " + slosToLoad.size() + " SLOs (out of " + allSlos.size() + " total)");
        
        // Create a result LiveData for updates
        MutableLiveData<Result<List<Slo>>> result = new MutableLiveData<>();
        
        // Fetch reports only for the specified SLOs
        fetchReportsForSlosIndependently(slosToLoad, result);
    }

    /**
     * Fetch reports for all SLOs independently
     * Each SLO is loaded in its own thread and updates are pushed individually
     */
    private void fetchReportsForSlosIndependently(List<Slo> slos, MutableLiveData<Result<List<Slo>>> result) {
        final double yellowThreshold = preferencesManager.getYellowThreshold();

        for (Slo slo : slos) {
            // Don't set up listener here - the ViewModel will set it up
            // Just log when loading completes
            
            // Set loading state
            slo.setLoadingState(Slo.LoadingState.LOADING);
            // Create a new list to trigger LiveData change detection
            result.setValue(Result.success(new ArrayList<>(slos)));

            // Fetch report directly using callback instead of observeForever
            apiService.getSloReport(slo.getId()).enqueue(new Callback<SloReport>() {
                @Override
                public void onResponse(Call<SloReport> call, Response<SloReport> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        SloReport report = response.body();
                        slo.setReport(report);
                        
                        // Calculate traffic light status
                        slo.setStatus(TrafficLightCalculator.calculate(
                                report.getSli(),
                                report.getSloTarget(),
                                report.getErrorBudgetRemaining(),
                                report.getTotalErrorBudget(),
                                yellowThreshold
                        ));
                        Log.d(TAG, "SLO '" + slo.getName() + "' loaded successfully. Status: " + slo.getStatus());
                        // Setting loading state will trigger the ViewModel's listener
                        slo.setLoadingState(Slo.LoadingState.LOADED);
                    } else {
                        // Loading failed
                        String errorMsg = "Failed to fetch SLO report: " + response.code();
                        Log.e(TAG, errorMsg);
                        slo.setStatus(null); // Will show as "Unknown"
                        // Setting loading state will trigger the ViewModel's listener
                        slo.setLoadingState(Slo.LoadingState.FAILED);
                    }
                }

                @Override
                public void onFailure(Call<SloReport> call, Throwable t) {
                    String errorMsg = "Network error: " + t.getMessage();
                    Log.e(TAG, errorMsg, t);
                    slo.setStatus(null); // Will show as "Unknown"
                    // Setting loading state will trigger the ViewModel's listener
                    slo.setLoadingState(Slo.LoadingState.FAILED);
                }
            });
        }
    }

    /**
     * Fetch detailed report for a specific SLO
     * 
     * @param sloId The ID of the SLO
     * @return LiveData containing the SLO report
     */
    public LiveData<Result<SloReport>> getSloReport(String sloId) {
        MutableLiveData<Result<SloReport>> result = new MutableLiveData<>();
        result.setValue(Result.loading());

        apiService.getSloReport(sloId).enqueue(new Callback<SloReport>() {
            @Override
            public void onResponse(Call<SloReport> call, Response<SloReport> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.setValue(Result.success(response.body()));
                } else {
                    String errorMsg = "Failed to fetch SLO report: " + response.code();
                    Log.e(TAG, errorMsg);
                    result.setValue(Result.error(errorMsg));
                }
            }

            @Override
            public void onFailure(Call<SloReport> call, Throwable t) {
                String errorMsg = "Network error: " + t.getMessage();
                Log.e(TAG, errorMsg, t);
                result.setValue(Result.error(errorMsg));
            }
        });

        return result;
    }

    /**
     * Result wrapper class for API responses
     */
    public static class Result<T> {
        public enum Status {
            SUCCESS, ERROR, LOADING
        }

        public final Status status;
        public final T data;
        public final String message;

        private Result(Status status, T data, String message) {
            this.status = status;
            this.data = data;
            this.message = message;
        }

        public static <T> Result<T> success(T data) {
            return new Result<>(Status.SUCCESS, data, null);
        }

        public static <T> Result<T> error(String message) {
            return new Result<>(Status.ERROR, null, message);
        }

        public static <T> Result<T> loading() {
            return new Result<>(Status.LOADING, null, null);
        }
    }
}
