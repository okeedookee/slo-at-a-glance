package io.instana.slo.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Represents the entity associated with an SLO
 * Can be application, website, infrastructure, or synthetic
 */
public class SloEntity {
    @SerializedName("type")
    private String type;
    
    @SerializedName("infraType")
    private String infraType;

    @SerializedName("applicationId")
    private String applicationId;

    @SerializedName("websiteId")
    private String websiteId;

    @SerializedName("syntheticTestIds")
    private List<String> syntheticTestIds;

    public SloEntity() {
    }

    public SloEntity(String type) {
        this.type = type;
    }

    /**
     * Get the entity type, inferring it from the populated fields if not explicitly set
     */
    public String getEntityType() {
        // If type is explicitly set in the API response, use it
        if (type != null && !type.isEmpty() && !"null".equals(type)) {
            return type;
        }
        
        // Otherwise, infer the type from which ID field is populated
        if (applicationId != null && !applicationId.isEmpty()) {
            return "application";
        }
        if (websiteId != null && !websiteId.isEmpty()) {
            return "website";
        }
        if (syntheticTestIds != null && !syntheticTestIds.isEmpty()) {
            return "synthetic";
        }
        if (infraType != null && !infraType.isEmpty()) {
            return "infrastructure";
        }
        
        return null;
    }

    public void setEntityType(String entityType) {
        // Convert string "null" to actual null
        if ("null".equals(entityType)) {
            this.type = null;
        } else {
            this.type = entityType;
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
        String entityType = getEntityType();
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
                "type='" + type + '\'' +
                ", infraType='" + infraType + '\'' +
                ", applicationId='" + applicationId + '\'' +
                ", websiteId='" + websiteId + '\'' +
                ", syntheticTestIds=" + syntheticTestIds +
                ", inferredType='" + getEntityType() + '\'' +
                '}';
    }
}
