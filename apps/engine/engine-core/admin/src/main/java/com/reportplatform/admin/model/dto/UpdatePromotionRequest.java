package com.reportplatform.admin.model.dto;

/**
 * Request DTO for admin modifications to a promotion candidate.
 * Allows admins to adjust the proposed DDL and indexes before approval.
 */
public class UpdatePromotionRequest {

    private String finalDdl;
    private String proposedIndexes;

    public String getFinalDdl() {
        return finalDdl;
    }

    public void setFinalDdl(String finalDdl) {
        this.finalDdl = finalDdl;
    }

    public String getProposedIndexes() {
        return proposedIndexes;
    }

    public void setProposedIndexes(String proposedIndexes) {
        this.proposedIndexes = proposedIndexes;
    }
}
