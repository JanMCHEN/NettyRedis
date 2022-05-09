package SerializTest;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.channels.Channel;
import java.util.HashMap;

public class TestMulti {
    public static void main(String[] args) throws IOException, InterruptedException {
        HashMap<String, String> map = new HashMap<>();

        Runnable task = () -> {
            for(int i=0;i<1000000;++i) {
                map.put("hello"+i, "world"+i);
            }
            System.out.println(map.size());
        };

        Runnable writeTask = () -> {
            try {
                new ObjectOutputStream(OutputStream.nullOutputStream()).writeObject(map);
                System.out.println(map.size());
            } catch (IOException e) {
                e.printStackTrace();
            }
        };

        Thread thread = new Thread(task);
        thread.start();
        Thread.sleep(1);

        for(int i=0;i<50;++i) {
            new Thread(writeTask).start();
        }

//        thread.join();

//        System.out.println(map.size());
    }
}

