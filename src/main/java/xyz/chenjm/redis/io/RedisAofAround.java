package xyz.chenjm.redis.io;

import xyz.chenjm.redis.core.RedisClient;
import xyz.chenjm.redis.command.RedisCommandAround;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class RedisAofAround implements RedisCommandAround {
    public final static byte[] CRLF = "\r\n".getBytes(StandardCharsets.US_ASCII);
    FileOutputStream aof;
    int dbIndex = -1;

    public RedisAofAround(String aofName) throws FileNotFoundException {
        aof = new ByteBufFileOutput(aofName);
    }

    /**
     * 切换数据库时需要追加select 命令
     * @param client
     * @param args
     */
    @Override
    public void before(RedisClient client, String... args) {
        int i = client.getDbIndex();
        if (i != dbIndex) {
            write("select", String.valueOf(i));
            dbIndex = i;
        }
    }

    public RedisAofAround(FileOutputStream fos){
        aof = fos;
    }
    @Override
    public Object after(Object returnValue, RedisClient client, String... args) {

        write(args);
        try {
            aof.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return returnValue;
    }


    private void write(String... args) {
        try {
            aof.write('*');
            aof.write(Long.toString(args.length).getBytes(StandardCharsets.US_ASCII));
            aof.write(CRLF);
            for (String arg : args) {
                write(arg);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void write(String arg) throws IOException {
        byte[] bytes = arg.getBytes(StandardCharsets.US_ASCII);
        aof.write('$');
        aof.write(Long.toString(bytes.length).getBytes(StandardCharsets.US_ASCII));
        aof.write(CRLF);
        aof.write(bytes);
        aof.write(CRLF);
    }

}
