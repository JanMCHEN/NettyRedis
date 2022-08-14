import org.junit.Test;
import utils.PackageClassScanner;

public class PackageClassScannerTest {

    @Test
    public void test() {
        PackageClassScanner scanner = new PackageClassScanner();

        scanner.scan("core.commands", "org.junit");
    }
}
