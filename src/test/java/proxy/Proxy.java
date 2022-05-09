package proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;

public class Proxy implements InvocationHandler {
    private final Object obj;
    Proxy(Object obj) {
        this.obj = obj;
    }
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println(Arrays.deepToString(args));
        System.out.println("do something");
        return method.invoke(obj, args);
    }
}
