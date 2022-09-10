package xyz.chenjm.redis;

import xyz.chenjm.redis.annotation.CommandScan;
import xyz.chenjm.redis.core.BootstrapApplication;

@CommandScan("xyz.chenjm.redis.command")
public class ServerApplication {
    public static void main(String[] args) {
        BootstrapApplication.run(ServerApplication.class, args);
    }
}
