package proxy;

public class ProxyDo implements Do {
    Do iml;
    public ProxyDo(Do d) {
        iml = d;
    }
    @Override
    public void select() {
        iml.select();
    }

    @Override
    public void update() {
        iml.update();
    }

    @Override
    public void hello() {
        iml.hello();
    }
}
