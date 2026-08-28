package com.playops.api.dto;

public class AiAnalysisResponse {
    private String summary;
    private String detailedExplanation;
    private String recommendedCodeFix;
    private String preventionTips;
    private String userLevel;
    private String rawAiResponse;

    public AiAnalysisResponse() {}

    public AiAnalysisResponse(String summary, String detailedExplanation, String recommendedCodeFix, String preventionTips, String userLevel, String rawAiResponse) {
        this.summary = summary;
        this.detailedExplanation = detailedExplanation;
        this.recommendedCodeFix = recommendedCodeFix;
        this.preventionTips = preventionTips;
        this.userLevel = userLevel;
        this.rawAiResponse = rawAiResponse;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDetailedExplanation() {
        return detailedExplanation;
    }

    public void setDetailedExplanation(String detailedExplanation) {
        this.detailedExplanation = detailedExplanation;
    }

    public String getRecommendedCodeFix() {
        return recommendedCodeFix;
    }

    public void setRecommendedCodeFix(String recommendedCodeFix) {
        this.recommendedCodeFix = recommendedCodeFix;
    }

    public String getPreventionTips() {
        return preventionTips;
    }

    public void setPreventionTips(String preventionTips) {
        this.preventionTips = preventionTips;
    }

    public String getUserLevel() {
        return userLevel;
    }

    public void setUserLevel(String userLevel) {
        this.userLevel = userLevel;
    }

    public String getRawAiResponse() {
        return rawAiResponse;
    }

    public void setRawAiResponse(String rawAiResponse) {
        this.rawAiResponse = rawAiResponse;
    }
}
