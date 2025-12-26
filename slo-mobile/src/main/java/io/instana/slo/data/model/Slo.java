package io.instana.slo.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Represents an SLO (Service Level Objective) configuration item
 */
public class Slo {
    public enum LoadingState {
        NOT_LOADED,    // Initial state
        LOADING,       // Currently loading
        LOADED,        // Successfully loaded
        FAILED         // Loading failed
    }

    /**
     * Listener interface for SLO data loading events
     */
    public interface OnDataLoadedListener {
        /**
         * Called when SLO data loading is complete
         * @param slo The SLO that finished loading
         * @param success True if loading was successful, false otherwise
         */
        void onDataLoaded(Slo slo, boolean success);
    }

    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("entity")
    private SloEntity entity;

    // Transient fields for UI state (not from API)
    private transient TrafficLightStatus status;
    private transient SloReport report;
    private transient LoadingState loadingState = LoadingState.NOT_LOADED;
    private transient OnDataLoadedListener dataLoadedListener;

    public Slo() {
    }

    public Slo(String id, String name, SloEntity entity) {
        this.id = id;
        this.name = name;
        this.entity = entity;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SloEntity getEntity() {
        return entity;
    }

    public void setEntity(SloEntity entity) {
        this.entity = entity;
    }

    public TrafficLightStatus getStatus() {
        return status;
    }

    public void setStatus(TrafficLightStatus status) {
        this.status = status;
    }

    public SloReport getReport() {
        return report;
    }

    public void setReport(SloReport report) {
        this.report = report;
    }

    public LoadingState getLoadingState() {
        return loadingState;
    }

    public void setLoadingState(LoadingState loadingState) {
        LoadingState previousState = this.loadingState;
        this.loadingState = loadingState;
        
        // Trigger event when loading completes (transitions to LOADED or FAILED)
        if (previousState != loadingState &&
            (loadingState == LoadingState.LOADED || loadingState == LoadingState.FAILED)) {
            notifyDataLoaded(loadingState == LoadingState.LOADED);
        }
    }

    public OnDataLoadedListener getDataLoadedListener() {
        return dataLoadedListener;
    }

    public void setDataLoadedListener(OnDataLoadedListener listener) {
        this.dataLoadedListener = listener;
    }

    /**
     * Notify listener that data loading is complete
     */
    private void notifyDataLoaded(boolean success) {
        if (dataLoadedListener != null) {
            dataLoadedListener.onDataLoaded(this, success);
        }
    }

    @Override
    public String toString() {
        return "Slo{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", entity=" + entity +
                ", status=" + status +
                ", loadingState=" + loadingState +
                '}';
    }
}
