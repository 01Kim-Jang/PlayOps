package com.playops.api.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.playops.api.exception.ApiException;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ProjectEnvVariables {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private ProjectEnvVariables() {}

    public static Map<String, String> parse(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            Map<String, Object> raw = MAPPER.readValue(json, MAP_TYPE);
            Map<String, String> result = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : raw.entrySet()) {
                if (entry.getKey() == null || entry.getKey().isBlank()) {
                    continue;
                }
                Object value = entry.getValue();
                result.put(entry.getKey().trim(), value != null ? String.valueOf(value) : "");
            }
            return result;
        } catch (Exception e) {
            throw new ApiException(400, "envVariables must be a JSON object of string keys and values");
        }
    }

    public static void validate(String json) {
        parse(json);
    }
}
