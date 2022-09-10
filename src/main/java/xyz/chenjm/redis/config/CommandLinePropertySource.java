package xyz.chenjm.redis.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandLinePropertySource implements PropertySource{
    private List<String> args;
    private Map<String, List<String>> optionArgs;
    private List<String> nonOptionArgs;

    public List<String> getNonOptionArgs() {
        return List.copyOf(nonOptionArgs);
    }

    public CommandLinePropertySource(String... s) {
        args = new ArrayList<>();
        optionArgs = new HashMap<>();
        nonOptionArgs = new ArrayList<>();
        addArgs(s);
    }

    public void addArg(String arg) {
        args.add(arg);
        if (arg.startsWith("--") && arg.contains("=")) {
            int i = arg.indexOf('=');
            String key = arg.substring(2, i);
            String value = i==arg.length()-1? "": arg.substring(i+1);
            setProperty(key, value);
        }
        else {
            nonOptionArgs.add(arg);
        }
    }

    public void addArgs(String ...args) {
        for (String arg : args) {
            addArg(arg);
        }
    }

    @Override
    public List<String> getProperties(String key) {
        return optionArgs.get(key);
    }

    @Override
    public void setProperty(String key, String value) {
        if (!optionArgs.containsKey(key)) {
            optionArgs.put(key, new ArrayList<>());
        }
        optionArgs.get(key).add(value);
    }
}
