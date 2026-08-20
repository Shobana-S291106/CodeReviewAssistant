package com.codereviewassistant.backend.dto;

public class ReviewIssue {

    private String severity;
    private String category;
    private String message;
    private int line;
    private String suggestion;

    public ReviewIssue(
            String severity,
            String category,
            String message,
            int line,
            String suggestion
    ) {
        this.severity = severity;
        this.category = category;
        this.message = message;
        this.line = line;
        this.suggestion = suggestion;
    }

    public String getSeverity() {
        return severity;
    }

    public String getCategory() {
        return category;
    }

    public String getMessage() {
        return message;
    }

    public int getLine() {
        return line;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }
}