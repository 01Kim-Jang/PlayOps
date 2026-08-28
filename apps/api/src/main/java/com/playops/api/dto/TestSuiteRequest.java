package com.playops.api.dto;

import java.util.List;

public record TestSuiteRequest(String name, List<String> specPaths, String grep, Integer caseCount, Boolean sequential) {}
