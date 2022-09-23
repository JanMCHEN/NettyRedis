package xyz.chenjm.redis.io;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class FlushFileOutputStreamWrapper extends OutputStream {
    private FileOutputStream fos;
    FlushHandler flushHandler;

    public FlushFileOutputStreamWrapper(FileOutputStream out) {
        this(out, 0);
    }

    public FlushFileOutputStreamWrapper(FileOutputStream out, int flush) {
        fos = out;
        if (flush == 0) {
            flushHandler = new AlwaysFlush();
        } else if (flush < 0) {
            flushHandler = new NoFlush();
        }else {
            flushHandler = new DelayFlush(flush);
        }
    }

    public void setFos(FileOutputStream fos) {
        this.fos = fos;
    }

    @Override
    public void write(int b) throws IOException {
        fos.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        fos.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        fos.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        flushHandler.tryFlush(fos);
    }

    @Override
    public void close() throws IOException {
        fos.close();
    }

    private interface FlushHandler {
        void tryFlush(FileOutputStream stream) throws IOException;
    }

    static private class DelayFlush implements FlushHandler {

        volatile boolean needFlush = true;
        ScheduledExecutorService executorService;

        int delay;

        public DelayFlush(int delay) {
            this.delay = delay;
            executorService = new ScheduledThreadPoolExecutor(1);
        }
        private synchronized void flush0(FileOutputStream stream) {
            needFlush = true;
            try {
                stream.getFD().sync();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void tryFlush(FileOutputStream stream) throws IOException {
            if (needFlush) {
                needFlush = false;
                executorService.schedule(()-> flush0(stream), delay, TimeUnit.MILLISECONDS);
            }
        }

    }

    static private class AlwaysFlush implements FlushHandler {
        @Override
        public void tryFlush(FileOutputStream stream) throws IOException {
            stream.getFD().sync();
        }
    }

    static private class NoFlush implements FlushHandler {
        @Override
        public void tryFlush(FileOutputStream stream) throws IOException {
        }
    }

}
