package com.playops.api.dto;

public class CreateTemplateGenerateJobRequest {
    private String targetSpecPath;
    private String instruction;

    public String getTargetSpecPath() { return targetSpecPath; }
    public void setTargetSpecPath(String targetSpecPath) { this.targetSpecPath = targetSpecPath; }

    public String getInstruction() { return instruction; }
    public void setInstruction(String instruction) { this.instruction = instruction; }
}
