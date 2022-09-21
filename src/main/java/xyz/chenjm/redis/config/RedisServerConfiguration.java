package xyz.chenjm.redis.config;

import io.netty.channel.DefaultEventLoop;
import io.netty.channel.EventLoop;
import xyz.chenjm.redis.command.CommandExecutor;
import xyz.chenjm.redis.command.DefaultCommandExecutor;
import xyz.chenjm.redis.core.RedisClient;
import xyz.chenjm.redis.core.RedisDB;
import xyz.chenjm.redis.core.RedisServer;
import xyz.chenjm.redis.io.AofEventListener;
import xyz.chenjm.redis.io.ByteBufFileOutput;
import xyz.chenjm.redis.io.ByteBufInput;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class RedisServerConfiguration implements RedisConfig{
    int dbNum = 16;
    String dir = "./";

    // append only file
    boolean appendOnly=true;
    String appendFileName;
    String appendFsync="always";
    private EventLoop eventLoop;
    private CommandExecutor commandExecutor;

    public void setEventLoop(EventLoop eventLoop) {
        this.eventLoop = eventLoop;
    }

    public void setCommandExecutor(CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
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
        server.setCmdExecutor(commandExecutor);
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
                commandExecutor.getCommand(args).invoke(client, args);
            }

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
            int r = in.read() - '0';
            if (r < 0 || r > 9) {
                if (in.read() != '\n') {
                    throw new IllegalStateException();
                }
                break;
            }
            ans = ans * 10 + r;
        }
        return ans;
    }

    private String decodeString(InputStream in) throws IOException {
        int flag = in.read();
        if (flag == '$') {
            int n = decodeLength(in);
            String ans = new String(in.readNBytes(n));
            if (ans.length() == n) {
                in.readNBytes(2);
                return ans;
            }
        }
        throw new IllegalStateException();
    }
}
