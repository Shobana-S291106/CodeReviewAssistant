package com.codereviewassistant.backend.dto;


public class CodeReviewResponse {

    private boolean success;
    private String message;
    private AnalysisResult analysisResult;

    public CodeReviewResponse() {
    }

    public CodeReviewResponse(boolean success, String message, AnalysisResult analysisResult) {
        this.success = success;
        this.message = message;
        this.analysisResult = analysisResult;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public AnalysisResult getAnalysisResult() {
        return analysisResult;
    }

    public void setAnalysisResult(AnalysisResult analysisResult) {
        this.analysisResult = analysisResult;
    }
}