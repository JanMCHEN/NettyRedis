package io;

import java.io.IOException;
import java.io.InputStream;

public class BytesInputStream extends InputStream {
    private byte[] content;
    private int readIndex;
    public BytesInputStream(final byte[] bytes) {
        content = bytes;
        readIndex = 0;
    }
    @Override
    public int read() throws IOException {
        if (readIndex < content.length){
            return content[readIndex++] & 0xff;
        }
        return -1;
    }

    @Override
    public void close() throws IOException {
        content = new byte[0];
        readIndex = 0;
    }
}
