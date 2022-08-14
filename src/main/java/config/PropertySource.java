package config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.util.List;

public interface PropertySource {
    Logger log = LoggerFactory.getLogger(PropertySource.class);

    static PropertySource getDefaultPropertySource(String propertyFile, String... args) {
        CompositePropertySource source = new CompositePropertySource();
        source.addResource(new CommandLinePropertySource(args));
        if (propertyFile!=null && !propertyFile.isEmpty()) {
            try {
                source.addResource(new FilePropertySource(propertyFile));
            } catch (FileNotFoundException e) {
                log.warn("'{}' not a file", propertyFile);
            }
        }

        return source;
    }

    default String getProperty(String key) {
        List<String> properties = getProperties(key);
        if (properties!=null && !properties.isEmpty()) {
            return properties.get(0);
        }
        return null;
    }
    List<String> getProperties(String key);

    default String getPropertyOrDefault(String key, String defaultValue) {
        String res = getProperty(key);
        if (res==null || res.isEmpty()) return defaultValue;
        return res;
    }

    void setProperty(String key, String value);
}
