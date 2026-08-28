package com.playops.api.dto;

import java.util.List;

public record RunTestRequest(String grep, String specPath, List<String> specPaths, String caseTitle, Boolean sequential) {}
