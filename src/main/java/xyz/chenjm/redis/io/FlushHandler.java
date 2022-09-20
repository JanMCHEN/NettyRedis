package xyz.chenjm.redis.io;

import io.netty.channel.EventLoop;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

public interface FlushHandler {

    static FlushHandler getInstance(String type) {
        switch (type) {
            case "always":
                return new AlwaysFlush();
            case "sec":
            case "second":
                return new EverySecondFlush();
            case "no":
                return new NoFlush();
            default:
                throw new IllegalArgumentException("type should be once of 'always'/'sec'/'no'");
        }
    }

    void tryFlush(OutputStream stream) throws IOException;
    default void flush(OutputStream stream) throws IOException {
        stream.flush();
        if (stream instanceof FileOutputStream) {
            ((FileOutputStream) stream).getFD().sync();
        }
    }

    class AlwaysFlush implements FlushHandler {
        @Override
        public void tryFlush(OutputStream stream) throws IOException {
            flush(stream);
        }
    }

    class EverySecondFlush implements FlushHandler {
        volatile boolean needFlush;
        EventLoop loop;
        public EverySecondFlush() {
        }

        public synchronized void flush0(OutputStream stream) {
            needFlush = true;
            try {
                flush(stream);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void tryFlush(OutputStream stream) throws IOException {
            if (needFlush) {
                needFlush = false;
                loop.schedule(()-> flush0(stream), 1, TimeUnit.SECONDS);
            }
            else {
                stream.flush();
            }
        }
    }

    class NoFlush implements FlushHandler {
        @Override
        public void tryFlush(OutputStream stream) throws IOException {
            stream.flush();
        }
    }
}
