package rdb;

import org.junit.Test;
import util.CRC64Utils;

public class CRC64Test {
    @Test
    public void test1() {
        String str = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed " +
        "do eiusmod tempor incididunt ut labore et dolore magna " +
        "aliqua. Ut enim ad minim veniam, quis nostrud exercitation " +
        "ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis " +
        "aute irure dolor in reprehenderit in voluptate velit esse " +
        "cillum dolore eu fugiat nulla pariatur. Excepteur sint " +
        "occaecat cupidatat non proident, sunt in culpa qui officia " +
        "deserunt mollit anim id est laborum.\0";

        char[] chars = str.toCharArray();
        long check = CRC64Utils.check(str.getBytes());
        System.out.println(Long.toHexString(check));
    }
}
