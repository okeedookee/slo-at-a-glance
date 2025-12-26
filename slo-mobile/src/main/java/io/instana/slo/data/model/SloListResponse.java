package io.instana.slo.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Response wrapper for the SLO list API endpoint
 */
public class SloListResponse {
    @SerializedName("items")
    private List<Slo> items;

    public SloListResponse() {
    }

    public SloListResponse(List<Slo> items) {
        this.items = items;
    }

    public List<Slo> getItems() {
        return items;
    }

    public void setItems(List<Slo> items) {
        this.items = items;
    }

    @Override
    public String toString() {
        return "SloListResponse{" +
                "items=" + (items != null ? items.size() : 0) + " items" +
                '}';
    }
}
