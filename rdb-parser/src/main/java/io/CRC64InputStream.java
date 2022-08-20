package io;

import util.CRC64Utils;

import java.io.IOException;
import java.io.InputStream;

public class CRC64InputStream extends InputStream {
    long crcSum;
    InputStream delegate;

    boolean crc_ok = true;

    public CRC64InputStream(InputStream in) {
        delegate = in;
    }

    @Override
    public int read() throws IOException {
        int read = delegate.read();
        if (read > -1 && crc_ok) crcSum = CRC64Utils.check(crcSum, read);
        return read;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        boolean check = crc_ok;
        crc_ok = false;
        int read = super.read(b, off, len);
        if(check) {
            crcSum = CRC64Utils.check(crcSum, b, read);
            crc_ok = true;
        }
        return read;
    }

    @Override
    public byte[] readAllBytes() throws IOException {
        boolean check = crc_ok;
        crc_ok = false;
        byte[] bytes = super.readAllBytes();
        if(check) {
            crcSum = CRC64Utils.check(crcSum, bytes);
            crc_ok = true;
        }
        return bytes;
    }

    @Override
    public byte[] readNBytes(int len) throws IOException {
        boolean check = crc_ok;
        crc_ok = false;
        byte[] bytes = super.readNBytes(len);
        if(check) {
            crcSum = CRC64Utils.check(crcSum, bytes);
            crc_ok = true;
        }
        return bytes;
    }

    @Override
    public int readNBytes(byte[] b, int off, int len) throws IOException {
        boolean check = crc_ok;
        crc_ok = false;
        int read = super.readNBytes(b, off, len);
        if(check) {
            crcSum = CRC64Utils.check(crcSum, b, read);
            crc_ok = true;
        }
        return read;
    }

    @Override
    public long skip(long n) throws IOException {
        boolean check = crc_ok;
        crc_ok = false;
        long skip = super.skip(n);
        crc_ok = check;
        return skip;
    }

    public long getCrcSum() {
        return crcSum;
    }

    public void setCrc_ok(boolean ok) {
        crc_ok = ok;
    }

    public void setDelegate(InputStream in) {
        delegate = in;
    }

    public InputStream getDelegate() {
        return delegate;
    }
}
