import org.junit.Test;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

public class FIFOMutex {
    private final AtomicBoolean lock = new AtomicBoolean(false);
    private final Queue<Thread> waiters = new ConcurrentLinkedQueue<>();

    public void lock() {
        Thread current = Thread.currentThread();
        boolean wasInterrupted = false;

        waiters.offer(current);

        while(waiters.peek() != current || !lock.compareAndSet(false, true)) {
            LockSupport.park(this);
            if(Thread.interrupted()) {
                wasInterrupted = true;
            }
        }

        if(wasInterrupted) {
            current.interrupt();
        }
    }

    public void unlock() {
        if(Thread.currentThread() != waiters.peek()) {
            throw new IllegalMonitorStateException();
        }
        lock.set(false);
        waiters.poll();
        System.out.println(waiters.size()+" waiters");
        LockSupport.unpark(waiters.peek());
    }

    @Test
    public void lockTest() throws InterruptedException {
        Runnable getLock = () -> {
            try{
                lock();
                Thread.sleep(1000);
                System.out.println(Thread.currentThread().getName()+" lock");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }finally {
                unlock();
                System.out.println(Thread.currentThread().getName()+" unlock");
            }
        };
        for(int i=0;i<10;++i) {
            new Thread(getLock, ""+i).start();
        }
        Thread.sleep(10000);
    }

    @Test
    public void reeTest() {
        ReentrantLock lock = new ReentrantLock();
        lock.unlock();
    }
}
