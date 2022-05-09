package proxy;

import org.junit.Test;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Proxy;

public class ProxyTest {
    public static void main(String[] args) {
        DoImp doImp = new DoImp();
        proxy.Proxy proxy = new proxy.Proxy(doImp);
        Do proxyInstance = (Do) Proxy.newProxyInstance(doImp.getClass().getClassLoader(), doImp.getClass().getInterfaces(), proxy);
        proxyInstance.select();
        System.out.println();
    }

    @Test
    public void test1() {
        Do doImp = new DoImp();
        Do aDo = new ProxyDo(doImp);
    }

    @Test
    public void test2() {
        Do doImp = new DoImp();
    }

    public Object invoke(MethodHandle method, Object ...args) throws Throwable {
        method.invoke(args);
        return null;
    }
}
