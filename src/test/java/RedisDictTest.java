import core.RedisDict;
import org.junit.Test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

interface A<K, V>{

}

interface C<K, V> extends A<K, V> {}

class D<K, V> implements C {}

class B<K, V>{
    A<K, V> get() {
        return new D<>();

    }
}

public class RedisDictTest {

    public static void main(String[] args) {
        RedisDict<String, String> dict = new RedisDict<>();

        for (int i=0;i<1000;++i) {
            dict.put("hello"+i, "word"+i);
        }

        for (int i=0;i<100000;++i) {
            dict.get("hello"+i);
        }

        System.out.println(dict.size());
    }

    void costTime(Map<String, String> map) {
        long st = System.currentTimeMillis();
        for (int i=0;i<1000000;++i) {
            map.put("hello"+i, "word"+i);
        }

        for (int i=0;i<1000000;++i) {
            map.get("hello"+i);
        }

        for (int i=0;i<100000;++i) {
            map.remove("hello"+i);
        }

        long sp = System.currentTimeMillis();

        System.out.println(sp - st);
        System.out.println("size="+map.size());
    }

    @Test
    public void testHashMap() {
        for (int i=0;i<10;++i) {
            Map<String, String> map = new HashMap<>();
            costTime(map);
//            System.out.println(map);
//            Iterator<Map.Entry<String, String>> iterator = map.entrySet().iterator();
//            while (iterator.hasNext()) {
//                iterator.next();
//                iterator.remove();
//            }

        }
    }

    @Test
    public void testRedisMap() {
        for (int i=0;i<10;++i) {
            Map<String, String> map = new RedisDict<>();
            costTime(map);
//            Iterator<Map.Entry<String, String>> iterator = map.entrySet().iterator();
//            System.out.println(map);
//            int j=0;
//            while (iterator.hasNext()) {
////                System.out.println(iterator.next());
//                j++;
//                iterator.next();
//                iterator.remove();
//            }
//            System.out.println(map.size());
//            System.out.println(j);

        }


    }
}
