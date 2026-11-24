package com.walmart.cms.coreapi.service;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Deep-merges maps in scope precedence order; values in later scopes override earlier scopes. */
@Component
public class ConfigurationMergeResolver {
    public Map<String,Object> merge(Map<String,Object>... scopes) {
        Map<String,Object> result = new LinkedHashMap<>();
        for (Map<String,Object> scope : scopes) {
            if (scope != null) deepMerge(result, scope);
        }
        return result;
    }
    @SuppressWarnings("unchecked")
    private void deepMerge(Map<String,Object> target, Map<String,Object> source) {
        source.forEach((key, value) -> {
            Object current = target.get(key);
            if (current instanceof Map<?,?> currentMap && value instanceof Map<?,?> valueMap) {
                Map<String,Object> nested = new LinkedHashMap<>((Map<String,Object>) currentMap);
                deepMerge(nested, (Map<String,Object>) valueMap);
                target.put(key, nested);
            } else {
                target.put(key, value);
            }
        });
    }
}
