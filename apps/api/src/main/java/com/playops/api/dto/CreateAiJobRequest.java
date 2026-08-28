package com.playops.api.dto;

public class CreateAiJobRequest {
    private Long failedExecutionId;
    private String targetSpecPath;
    private String instruction;

    public Long getFailedExecutionId() { return failedExecutionId; }
    public void setFailedExecutionId(Long failedExecutionId) { this.failedExecutionId = failedExecutionId; }

    public String getTargetSpecPath() { return targetSpecPath; }
    public void setTargetSpecPath(String targetSpecPath) { this.targetSpecPath = targetSpecPath; }

    public String getInstruction() { return instruction; }
    public void setInstruction(String instruction) { this.instruction = instruction; }
}
