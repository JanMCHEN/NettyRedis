package core;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class RedisAofAround implements RedisCommandAround {
    public final static byte[] CRLF = "\r\n".getBytes(StandardCharsets.UTF_8);
    FileOutputStream aof;

    public RedisAofAround(String aofName) throws FileNotFoundException {
        aof = new ByteBufFileOutput(aofName);
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


    private void write(String[] args) {
        try {
            aof.write('*');
            aof.write(Long.toString(args.length).getBytes(StandardCharsets.UTF_8));
            aof.write(CRLF);
            for (String arg : args) {
                write(arg);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void write(String arg) throws IOException {
        byte[] bytes = arg.getBytes(StandardCharsets.UTF_8);
        aof.write('$');
        aof.write(Long.toString(bytes.length).getBytes(StandardCharsets.UTF_8));
        aof.write(CRLF);
        aof.write(bytes);
        aof.write(CRLF);
    }

}
