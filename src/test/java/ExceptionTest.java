import org.junit.Test;

class MyException extends RuntimeException {
    public static MyException e1 = new MyException();
    public static MyException e2 = new MyException();
}
public class ExceptionTest {
    @Test
    public void throwEx() throws InterruptedException {
        MyException [] es = new MyException[2];
        Thread t1 = new Thread(()->{
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try{
                throw MyException.e1;
            }catch (MyException e){
                es[0] = e;
                e.printStackTrace();
            }
        });
        Thread t2 = new Thread(()->{
            try{
                throw MyException.e1;
            }catch (MyException e){
                es[1] = e;
                e.printStackTrace();
            }
        });
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        System.out.println(es[0]==es[1]);
    }
}
