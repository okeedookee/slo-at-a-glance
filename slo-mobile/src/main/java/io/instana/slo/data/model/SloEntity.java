package io.instana.slo.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Represents the entity associated with an SLO
 * Can be application, website, infrastructure, or synthetic
 */
public class SloEntity {
    @SerializedName("entityType")
    private String entityType;

    @SerializedName("applicationId")
    private String applicationId;

    @SerializedName("websiteId")
    private String websiteId;

    @SerializedName("syntheticTestIds")
    private List<String> syntheticTestIds;

    public SloEntity() {
    }

    public SloEntity(String entityType) {
        this.entityType = entityType;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        // Convert string "null" to actual null
        if ("null".equals(entityType)) {
            this.entityType = null;
        } else {
            this.entityType = entityType;
        }
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        // Convert string "null" to actual null
        this.applicationId = "null".equals(applicationId) ? null : applicationId;
    }

    public String getWebsiteId() {
        return websiteId;
    }

    public void setWebsiteId(String websiteId) {
        // Convert string "null" to actual null
        this.websiteId = "null".equals(websiteId) ? null : websiteId;
    }

    public List<String> getSyntheticTestIds() {
        return syntheticTestIds;
    }

    public void setSyntheticTestIds(List<String> syntheticTestIds) {
        this.syntheticTestIds = syntheticTestIds;
    }

    /**
     * Get the entity ID based on entity type
     */
    public String getEntityId() {
        if (entityType == null) {
            return null;
        }
        
        switch (entityType.toLowerCase()) {
            case "application":
                return applicationId;
            case "website":
                return websiteId;
            case "synthetic":
                return syntheticTestIds != null && !syntheticTestIds.isEmpty() 
                    ? syntheticTestIds.get(0) : null;
            default:
                return null;
        }
    }

    @Override
    public String toString() {
        return "SloEntity{" +
                "entityType='" + entityType + '\'' +
                ", applicationId='" + applicationId + '\'' +
                ", websiteId='" + websiteId + '\'' +
                ", syntheticTestIds=" + syntheticTestIds +
                '}';
    }
}
