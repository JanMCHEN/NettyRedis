package xyz.chenjm.redis.io;

import xyz.chenjm.redis.command.CommandEvent;
import xyz.chenjm.redis.core.EventListener;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class AofEventListener implements EventListener<CommandEvent> {
    public final static byte[] CRLF = "\r\n".getBytes(StandardCharsets.US_ASCII);
    private final OutputStream out;
    int selectDb = -1;

    FlushHandler flushHandler = FlushHandler.getInstance("always");


    public AofEventListener(OutputStream outputStream) {
        out = outputStream;
    }

    @Override
    public void onEvent(CommandEvent e) {
        if (e.getCmd().readonly()) {
            return;
        }
        if(e.getClient().selectDb() != selectDb) {
            selectDb = e.getClient().selectDb();
            write(new String[] {"select", String.valueOf(selectDb)});
        }

        write(e.getArgs());
        try {
            flushHandler.tryFlush(out);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    void write(String[] args) {
        try {
            out.write('*');
            out.write(Long.toString(args.length).getBytes(StandardCharsets.US_ASCII));
            out.write(CRLF);
            for (String arg : args) {
                write(arg);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void write(String arg) throws IOException {
        byte[] bytes = arg.getBytes(StandardCharsets.US_ASCII);
        out.write('$');
        out.write(Long.toString(bytes.length).getBytes(StandardCharsets.US_ASCII));
        out.write(CRLF);
        out.write(bytes);
        out.write(CRLF);
    }


}
