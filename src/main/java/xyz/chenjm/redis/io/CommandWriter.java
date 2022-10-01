package xyz.chenjm.redis.io;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class CommandWriter {
    private final OutputStream out;
    int selectDb = -1;

    public CommandWriter(OutputStream outputStream) {
        out = outputStream;
    }

    public void write(String... args) {
        try {
            out.write('*');
            out.write(Long.toString(args.length).getBytes(StandardCharsets.US_ASCII));
            writeCRLF();

            for (String arg : args) {
                write(arg);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            out.flush();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

    }

    void writeCRLF() throws IOException {
        out.write('\r');
        out.write('\n');
    }

    void write(String arg) throws IOException {
        byte[] bytes = arg.getBytes(StandardCharsets.US_ASCII);
        out.write('$');
        out.write(Long.toString(bytes.length).getBytes(StandardCharsets.US_ASCII));
        writeCRLF();
        out.write(bytes);
        writeCRLF();
    }
}