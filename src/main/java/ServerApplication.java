import annotation.CommandScan;
import core.BootstrapApplication;

@CommandScan("core.commands")
public class ServerApplication {
    public static void main(String[] args) {
        BootstrapApplication.run(ServerApplication.class, args);
    }
}
