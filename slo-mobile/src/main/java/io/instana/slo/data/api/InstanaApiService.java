package io.instana.slo.data.api;

import com.google.gson.JsonObject;
import io.instana.slo.data.model.SloListResponse;
import io.instana.slo.data.model.SloReport;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * Retrofit API service interface for Instana API endpoints
 */
public interface InstanaApiService {
    
    /**
     * Fetch the list of SLOs from the Instana API
     * Endpoint: GET /api/settings/slo
     *
     * @return Call object containing SloListResponse with items array
     */
    @GET("/api/settings/slo")
    Call<SloListResponse> getSloList();

    /**
     * Fetch detailed SLO report for a specific SLO
     * Endpoint: GET /api/slo/report/{sloId}
     *
     * @param sloId The ID of the SLO to fetch the report for
     * @return Call object containing SloReport with metrics
     */
    @GET("/api/slo/report/{sloId}")
    Call<SloReport> getSloReport(@Path("sloId") String sloId);

    /**
     * Fetch Instana version information for API validation
     * Endpoint: GET /api/instana/version
     *
     * @return Call object containing version information as JsonObject
     */
    @GET("/api/instana/version")
    Call<JsonObject> getVersion();
}
