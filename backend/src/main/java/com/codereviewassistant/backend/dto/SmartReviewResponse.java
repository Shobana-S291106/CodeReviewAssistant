package com.codereviewassistant.backend.dto;

public class SmartReviewResponse {

    private String fileName;
    private String localAnalysis;
    private String aiReview;

    public SmartReviewResponse() {
    }

    public SmartReviewResponse(
            String fileName,
            String localAnalysis,
            String aiReview
    ) {
        this.fileName = fileName;
        this.localAnalysis = localAnalysis;
        this.aiReview = aiReview;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getLocalAnalysis() {
        return localAnalysis;
    }

    public void setLocalAnalysis(String localAnalysis) {
        this.localAnalysis = localAnalysis;
    }

    public String getAiReview() {
        return aiReview;
    }

    public void setAiReview(String aiReview) {
        this.aiReview = aiReview;
    }
}
