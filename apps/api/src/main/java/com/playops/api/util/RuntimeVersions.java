package com.playops.api.util;

public final class RuntimeVersions {

    public static final String NODE_VERSION = "22";
    public static final String RESOLVED_NODE_VERSION = "22.16.0";
    public static final String PLAYWRIGHT_VERSION = "1.53.0";

    private RuntimeVersions() {
    }

    public static boolean isAllowedNodeVersion(String version) {
        return NODE_VERSION.equals(trim(version));
    }

    public static boolean isAllowedPlaywrightVersion(String version) {
        return PLAYWRIGHT_VERSION.equals(trim(version));
    }

    private static String trim(String value) {
        return value != null ? value.trim() : "";
    }
}
