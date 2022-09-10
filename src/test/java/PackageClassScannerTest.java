import org.junit.Test;
import xyz.chenjm.redis.utils.PackageClassScanner;

public class PackageClassScannerTest {

    @Test
    public void test() {

        PackageClassScanner scanner = new PackageClassScanner();

        scanner.scan("core.commands", "org.junit");
    }
}
