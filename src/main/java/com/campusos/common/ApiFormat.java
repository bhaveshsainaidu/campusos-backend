package com.campusos.common;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/** Consistent API response envelope and error format. */
public final class ApiFormat {
    private ApiFormat() {}

    public static Map<String, Object> error(String code, String message, Map<String, String> fieldErrors,
                                            Instant timestamp, String path) {
        Map<String, Object> err = new LinkedHashMap<>();
        err.put("code", code);
        err.put("message", message);
        if (fieldErrors != null && !fieldErrors.isEmpty()) {
            err.put("fieldErrors", fieldErrors);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("error", err);
        body.put("timestamp", timestamp.toString());
        body.put("path", path);
        return body;
    }
}
