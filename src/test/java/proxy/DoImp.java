package proxy;

public class DoImp implements Do{
    @Override
    public void select() {
        System.out.println("select");
        System.out.println(this);
    }

    @Override
    public void update() {
        System.out.println(("update"));


    }

    @Override
    public void hello() {
        System.out.println("hello world");
    }
}
