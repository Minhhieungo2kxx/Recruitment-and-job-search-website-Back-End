package com.webjob.application.Dto.Request.Redis;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import org.springframework.util.AntPathMatcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class PermissionSet {
    private final Map<String, List<String>> permissions = new HashMap<>();
    @JsonIgnore
    private transient AntPathMatcher matcher = new AntPathMatcher();

    public void add(String method, String path) {
        permissions
                .computeIfAbsent(method, k -> new ArrayList<>())
                .add(path);
    }

    public boolean match(String method, String requestPath) {
        if (matcher == null) {
            matcher = new AntPathMatcher(); // recreate sau khi deserialize
        }

        List<String> paths = permissions.get(method);

        if (paths == null) {
            return false;
        }

        for (String pattern : paths) {
            if (matcher.match(pattern, requestPath)) {
                return true;
            }
        }

        return false;
    }
}
