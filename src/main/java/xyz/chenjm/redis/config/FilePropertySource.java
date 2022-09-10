package xyz.chenjm.redis.config;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

public class FilePropertySource implements PropertySource{
    Properties properties = new Properties();

    public FilePropertySource(FileInputStream inputStream) {
        try {
            properties.load(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public FilePropertySource(String fileName) throws FileNotFoundException {
        this(new FileInputStream(fileName));
    }

    @Override
    public List<String> getProperties(String key) {
        return List.of(properties.getProperty(key));
    }

    @Override
    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }
}
