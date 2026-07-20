package com.webjob.application.dto.Request.Redis;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import org.springframework.http.server.PathContainer;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class PermissionSet {
    private final Map<String, List<String>> permissions = new HashMap<>();

    //    private transient AntPathMatcher matcher = new AntPathMatcher();
    @JsonIgnore
    private final PathPatternParser parser = new PathPatternParser();

    public void add(String method, String path) {
        permissions
                .computeIfAbsent(method, k -> new ArrayList<>())
                .add(path);
    }


    public boolean match(String method, String requestPath) {

        List<String> paths = permissions.get(method.toUpperCase());

        if (paths == null || paths.isEmpty()) {
            return false;
        }

        PathContainer path = PathContainer.parsePath(requestPath);

        for (String pattern : paths) {
            if (parser.parse(pattern).matches(path)) {
                return true;
            }
        }

        return false;
    }
}
