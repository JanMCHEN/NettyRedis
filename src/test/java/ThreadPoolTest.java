import org.junit.Test;

import java.util.*;
import java.util.concurrent.*;

public class ThreadPoolTest {

    @Test
    public void test1() throws IllegalAccessException, InstantiationException, InterruptedException, ClassNotFoundException {
        ArrayBlockingQueue<Runnable> runnable = new ArrayBlockingQueue<Runnable>(20);
        ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(5, 5, 0, TimeUnit.SECONDS, runnable, r -> new Thread(r, "pool"), new ThreadPoolExecutor.CallerRunsPolicy());
        CountDownLatch downLatch = new CountDownLatch(2);
        for(int i=0;i<100;++i) {
            poolExecutor.submit(()-> {
                System.out.println("我是worker");
                downLatch.countDown();
                System.out.println(Thread.currentThread().getName());
            });
        }

        downLatch.await();
        Class<Object> aClass = Object.class;
        System.out.println(aClass.newInstance());
    }

    @Test
    public void test2() {
        List<String> a = new ArrayList<>();
        a.add("oo");
        a.add("ook");
        a.sort(Comparator.naturalOrder());
        Comparator<String> cmp = new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return 0;
            }
        };
        Deque<Integer> q = new LinkedList<>();
    }
}
