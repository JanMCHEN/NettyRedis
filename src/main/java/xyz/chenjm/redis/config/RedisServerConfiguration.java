package xyz.chenjm.redis.config;

import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.DefaultEventLoop;
import io.netty.channel.EventLoop;
import xyz.chenjm.redis.command.CommandExecutor;
import xyz.chenjm.redis.command.CommandHolder;
import xyz.chenjm.redis.command.DefaultCommandExecutor;
import xyz.chenjm.redis.core.*;
import xyz.chenjm.redis.io.AofEventListener;
import xyz.chenjm.redis.io.AofWriter;
import xyz.chenjm.redis.io.ByteBufFileOutput;
import xyz.chenjm.redis.io.ByteBufInput;

import java.io.*;

public class RedisServerConfiguration implements RedisConfig{
    int dbNum = 16;
    String dir = "./";

    // append only file
    boolean appendOnly=true;
    String appendFileName = "appendonly.aof";
    String appendFsync="always";
    private EventLoop eventLoop;
    private CommandHolder commandHolder;

    private FileOutputStream aof;
    private FileOutputStream rdb;

    private final EventPublisher<CommandTask> commandPublisher = new EventPublisher<>();

    public void setEventLoop(EventLoop eventLoop) {
        this.eventLoop = eventLoop;
    }

    public void setCommandHolder(CommandHolder commandHolder) {
        this.commandHolder = commandHolder;
    }

    public RedisServer newServer() {
        RedisServer server = new RedisServer();
        RedisDB[] dbs = new RedisDB[dbNum];
        for (int i=0;i<dbNum;++i) {
            dbs[i] = new RedisDB();
        }
        server.setDbs(dbs);
        if (eventLoop == null) {
            eventLoop = new DefaultEventLoop();
        }
        server.setEventLoop(eventLoop);
        server.setCommandHolder(commandHolder);
        loadFromDisk(server);

        return server;
    }

    private void loadFromDisk(RedisServer server) {
        if (appendOnly) {
            loadAppendOnlyFile(server);
        }
    }

    private void loadAppendOnlyFile(RedisServer server) {
        InputStream stream = null;
        try {
            stream = new FileInputStream(appendFileName);
            stream = new ByteBufInput(stream);
            RedisClient client = new RedisClient(null, server);
            while (true) {
                String[] args = decodeArray(stream);
                if (args.length==0)
                    break;
                if ("MULTI".equalsIgnoreCase(args[0]) || "EXEC".equalsIgnoreCase(args[0])) {
                    continue;
                }
                commandHolder.getCommand(args).invoke(client, args);
            }

            aof = new FileOutputStream(appendFileName, true);

            AofWriter aofWriter = new AofWriter(aof);



            // aof listener
            if (commandExecutor instanceof DefaultCommandExecutor) {
                OutputStream out = new ByteBufFileOutput(appendFileName);
                ((DefaultCommandExecutor) commandExecutor).getPublisher().addListener(new AofEventListener(out));
            }

        } catch (IOException ignored) {

        }finally {
            try {
                if (stream != null)
                    stream.close();
            } catch (IOException ignored) {
            }
        }


    }

    private String[] decodeArray(InputStream in) throws IOException {
        int flag = in.read();
        if (flag==-1)
            return new String[0];

        if (flag != '*')
            throw new IllegalStateException();
        int n = decodeLength(in);
        String[] args = new String[n];
        for (int i=0;i<n;++i) {
            args[i] = decodeString(in);
        }
        return args;
    }

    private int decodeLength(InputStream in) throws IOException {
        int ans = 0;
        while (true) {
            int r = in.read();
            if (r < '0' || r > '9') {
                if (r == '\r')
                    r = in.read();
                if (r != '\n') {
                    throw new IllegalStateException();
                }
                break;
            }
            ans = ans * 10 + r - '0';
        }
        return ans;
    }

    private String decodeString(InputStream in) throws IOException {
        int flag = in.read();
        if (flag == '$') {
            int n = decodeLength(in);
            String ans = new String(in.readNBytes(n));
            if (ans.length() == n) {
                flag = in.read();
                if (flag == '\r')
                    flag = in.read();
                if (flag == '\n')
                    return ans;
            }
        }
        throw new IllegalStateException();
    }
}
