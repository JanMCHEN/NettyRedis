package xyz.chenjm.redis.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CompositePropertySource implements PropertySource{
    private final List<PropertySource> sources = new ArrayList<>();
    private final Map<String, String> properties = new HashMap<>();

    public void addResource(PropertySource source) {
        sources.add(source);
    }

    @Override
    public List<String> getProperties(String key) {
        if (properties.containsKey(key)) return List.of(properties.get(key));
        for (PropertySource source : sources) {
            List<String> ans = source.getProperties(key);
            if (ans != null) return ans;
        }
        return null;
    }

    @Override
    public void setProperty(String key, String value) {
        properties.put(key, value);
    }
}
