package xyz.chenjm.redis.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.chenjm.redis.annotation.Command;
import xyz.chenjm.redis.exception.NoSuchCommandErr;

import java.util.HashMap;
import java.util.Map;

public class DefaultCommandHolder implements CommandHolder{
    private static final Logger log = LoggerFactory.getLogger(DefaultCommandHolder.class);
    Map<String, RedisCommand> commandMap = new HashMap<>();
    @Override
    public void addCommand(RedisCommand cmd) {
        String key = cmd.getName().toUpperCase();
        RedisCommand put = commandMap.put(key, cmd);
        if (put != null) {
            log.warn("repeat command '{}', old={}, new={}", key, put, cmd);
        }
    }

    @Override
    public void addCommand(CommandRunner runner) {
        Class<? extends CommandRunner> cls = runner.getClass();
        Command ann = cls.getAnnotation(Command.class);
        String key;
        if (ann == null) {
            key = cls.getSimpleName().toUpperCase();
            if (key.startsWith("COMMAND"))
                key = key.substring("COMMAND".length());
        }else {
            key = ann.value();
        }
        RedisCommand cmd = new RedisCommand();
        cmd.setName(key);
        cmd.setRunner(runner);
        if (ann != null) {
            cmd.readonly(ann.readonly());
            cmd.multi(ann.multi());
            cmd.setArgNums(ann.args());
        }
        addCommand(cmd);
    }

    @Override
    public RedisCommand getCommand(String ... args) {
        RedisCommand cmd = commandMap.get(args[0].toUpperCase());
        if (cmd == null)
            throw new NoSuchCommandErr(args[0]);
        cmd.checkArgs(args);
        return cmd;
    }
}
