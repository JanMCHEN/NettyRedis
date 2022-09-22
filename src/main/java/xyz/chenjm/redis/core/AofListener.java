package xyz.chenjm.redis.core;

import xyz.chenjm.redis.command.RedisCommand;
import xyz.chenjm.redis.io.AofWriter;

public class AofListener implements EventListener<CommandTask>{
    private AofWriter writer;

    public AofWriter getWriter() {
        return writer;
    }

    public void setWriter(AofWriter writer) {
        this.writer = writer;
    }

    @Override
    public void onEvent(CommandTask e) {
        String[] args = e.getArgs();
        RedisCommand cmd = e.getCmd();

        if (cmd.readonly()) {
            return;
        }
        writer.write(args);
    }
}
