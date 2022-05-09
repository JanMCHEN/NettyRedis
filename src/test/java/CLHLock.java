import java.util.concurrent.atomic.AtomicReference;

public class CLHLock {

    /*
    自旋锁
    每个线程自旋等待自己的变量，提高自旋效率
     */

    static class CLHNode {
        private volatile boolean locked;
        public void setLocked(boolean flag) {
            locked = flag;
        }
        public void setLocked() {
            setLocked(true);
        }
        public boolean isLocked() {
            return locked;
        }
    }

    private final AtomicReference<CLHNode> tail;

    private final ThreadLocal<CLHNode> curNode;
    private final ThreadLocal<CLHNode> preNode;

    private boolean debug=false;

    public CLHLock(boolean debug) {
        this();
        this.debug = debug;
    }

    public CLHLock() {
        tail = new AtomicReference<>(new CLHNode());
        curNode = ThreadLocal.withInitial(CLHNode::new);
        preNode = new ThreadLocal<CLHNode>() {
            @Override
            protected CLHNode initialValue() {
                return new CLHNode();
            }
        };
    }


    public void lock() {
        CLHNode cur = curNode.get();
        cur.setLocked();

        CLHNode pre = tail.getAndSet(cur);

        preNode.set(pre);

        while (pre.isLocked()) {
            if (debug)
                System.out.println("自旋等待解锁:"+Thread.currentThread().getName());
        }
        if (debug)
            System.out.println("成功获取锁:"+Thread.currentThread().getName());
    }

    public void unlock() {
        CLHNode cur = curNode.get();
        cur.setLocked(false);

        curNode.set(preNode.get());
    }

    static class CLHTest {
        static int cnt = 0;
        static CLHLock clhLock = new CLHLock(true);
        public static void test() throws InterruptedException {
            Runnable runnable = () -> {
                clhLock.lock();
                for (int i=0;i<1;++i) {
                    System.out.println(cnt++);
                }
                clhLock.unlock();
            };

            Thread[] threads = new Thread[10];

            for (int i=0;i<10;++i) {
                threads[i] = new Thread(runnable, "Thread "+i);
                threads[i].start();
            }
            for (int i=0;i<10;++i) {
                threads[i].join();
            }

            System.out.println("cnt="+cnt);
        }
    }


    public static void main(String[] args) throws InterruptedException {
        CLHTest.test();

    }
}
