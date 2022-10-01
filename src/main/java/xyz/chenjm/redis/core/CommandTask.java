package xyz.chenjm.redis.core;

import io.netty.handler.codec.redis.ErrorRedisMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.chenjm.redis.command.RedisCommand;
import xyz.chenjm.redis.exception.RedisException;

import java.util.concurrent.Callable;

public class CommandTask implements Callable<Object>, Event {
    private static final Logger log = LoggerFactory.getLogger(CommandTask.class);

    private final RedisCommand cmd;
    private final String[] args;
    private final RedisClient client;

    Object res;
    private EventPublisher<CommandTask> publisher;

    public void setPublisher(EventPublisher<CommandTask> publisher) {
        this.publisher = publisher;
    }

    public CommandTask(RedisClient client, RedisCommand cmd, String[] args) {
        this.cmd = cmd;
        this.args = args;
        this.client = client;
    }

    @Override
    public Object call(){
        try{
            res = cmd.invoke(client, args);
        }catch (RedisException e) {
            res = new ErrorRedisMessage(e.getMessage());
        }catch (Exception ex) {
            log.error("command '{}'execute wrong", args[0], ex);
            res = RedisMessageFactory.ERR;
        }
        if (publisher != null) {
            publisher.onEvent(this);
        }
        if (!client.isMulti()) {
            client.writeAndFlush(res);
        }
        return res;
    }

    public RedisCommand getCmd() {
        return cmd;
    }

    public String[] getArgs() {
        return args;
    }

    public RedisClient getClient() {
        return client;
    }

    public Object getRes() {
        return res;
    }

    public EventPublisher<CommandTask> getPublisher() {
        return publisher;
    }
}
