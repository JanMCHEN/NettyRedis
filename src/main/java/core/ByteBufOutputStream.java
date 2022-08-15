package core;

import java.io.BufferedOutputStream;
import java.io.OutputStream;

public class ByteBufOutputStream extends BufferedOutputStream {
    public ByteBufOutputStream(OutputStream out) {
        super(out);
    }
}
