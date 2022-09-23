package xyz.chenjm.redis.core;

import xyz.chenjm.redis.command.RedisCommand;
import xyz.chenjm.redis.io.CommandWriter;

public class AofListener implements EventListener<CommandTask>{
    private CommandWriter writer;
    int selectDb = -1;

    public CommandWriter getWriter() {
        return writer;
    }

    public void setWriter(CommandWriter writer) {
        this.writer = writer;
    }

    @Override
    public void onEvent(CommandTask e) {
        RedisCommand cmd = e.getCmd();
        if (cmd.readonly()) {
            return;
        }
        int db = e.getClient().selectDb();
        if (db != selectDb) {
            writer.write("select", db+"");
            selectDb = db;
        }
        String[] args = e.getArgs();
        writer.write(args);
    }
}
