package rdb;

import entry.type.ZipMapEntry;
import entry.type.ZsetEntry;
import org.junit.Test;
import rdb.io.ToyInputStream;

import java.io.IOException;

public class EntryTest {
    private ToyInputStream in = new ToyInputStream();

    @Test
    public void testZset() throws IOException {
        in.fromHexString("04\n" +
                "01 63 12 34 2E 30 31 39 39 39 39 39 39 39 39 39 39 39 39 39 36\n" +
                "01 64 FE\n" +
                "01 61 12 33 2E 31 38 39 39 39 39 39 39 39 39 39 39 39 39 39 39\n" +
                "01 65 FF");

        ZsetEntry entry = new ZsetEntry();
        entry.parse(in);

        System.out.println(entry);
    }

    public void testHash() throws IOException{
        in.fromHexString("");
    }

    @Test
    public void testZipMap() throws IOException{
        in.fromHexString("18 02 06 4D 4B 44 31 47 36 01 00 32 05 59 4E 4E 58 4b 04 00 46 37 54 49 FF");
        ZipMapEntry zipMapEntry = new ZipMapEntry();
        zipMapEntry.parse(in);
        System.out.println(zipMapEntry);
    }
}
