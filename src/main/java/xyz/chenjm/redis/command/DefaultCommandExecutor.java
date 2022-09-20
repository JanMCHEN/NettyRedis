package xyz.chenjm.redis.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.chenjm.redis.core.EventPublisher;
import xyz.chenjm.redis.core.RedisClient;
import xyz.chenjm.redis.exception.NoSuchCommandErr;

import java.util.HashMap;
import java.util.Map;

public class DefaultCommandExecutor implements CommandExecutor {
    private static final Logger log = LoggerFactory.getLogger(DefaultCommandExecutor.class);

    Map<String, Command> commandMap = new HashMap<>();

    EventPublisher<CommandEvent> publisher = new EventPublisher<>();

    public EventPublisher<CommandEvent> getPublisher() {
        return publisher;
    }

    @Override
    public void addCommand(Command cmd) {
        String key = cmd.getName();
        Command put = commandMap.put(key, cmd);
        if (put != null) {
            log.warn("repeat command '{}', old={}, new={}", key, put, cmd);
        }
    }

    @Override
    public void addCommand(CommandRunner runner) {
//        Class<? extends Command> cls = cmd.getClass();
//        Command ann = cls.getAnnotation(Command.class);
//        String key;
//        if (ann == null) {
//            key = cls.getSimpleName().toUpperCase();
//            if (key.startsWith("COMMAND"))
//                key = key.substring("COMMAND".length());
//        }else {
//            key = ann.value().toUpperCase();
//        }
//        commandMetaMap.put(key, new CommandWrapper(ann, cmd));
    }

    @Override
    public Command getCommand(String ... args) {
        Command cmd = commandMap.get(args[0]);
        if (cmd == null)
            throw new NoSuchCommandErr(args[0]);
        cmd.checkArgs(args);
        return cmd;
    }

    public Object call(RedisClient client, Command cmd, String... args) {
        Object res = cmd.invoke(client, args);
        publisher.onEvent(new CommandEvent(client, cmd, res, args));
        return res;
    }
}
