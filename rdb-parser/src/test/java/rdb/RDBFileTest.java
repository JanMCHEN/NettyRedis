package rdb;

import org.junit.Test;
import util.CRC64InputStream;
import util.CRC64Utils;
import util.InputStreamUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class RDBFileTest {
    @Test
    public void testCRC() throws IOException {
        FileInputStream file = new FileInputStream("/home/cjm/dump.rdb");
        byte[] bytes = file.readAllBytes();
        System.out.println(bytes.length);
        long crc = CRC64Utils.check(0, bytes, bytes.length - 8);
        System.out.println(Long.toHexString(crc));

        for (int i=1;i<=8;++i) {
            System.out.println(Long.toHexString(bytes[bytes.length-i]&0xff));
        }

    }

    @Test
    public void testCRCInputStream() throws IOException {
        InputStream file = new FileInputStream("/home/cjm/dump.rdb");
        CRC64InputStream in = (CRC64InputStream) InputStreamUtils.CRC64InputStreamWrapper(file);

        byte[] bytes = in.readNBytes(170);
        System.out.println(Long.toHexString(in.getCrcSum()));

        in.setCrc_ok(false);
        long ex = InputStreamUtils.readLong(in);
        System.out.println(Long.toHexString(ex));
    }
}
