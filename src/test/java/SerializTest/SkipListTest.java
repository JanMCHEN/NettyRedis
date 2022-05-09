package SerializTest;

import core.SkipList;
import org.junit.Test;

import java.util.Random;
import java.util.concurrent.ConcurrentSkipListMap;

public class SkipListTest {
    static final Random random = new Random();
    static final int MAX_LENGTH = 10000000;
    static Integer[] num = new Integer[MAX_LENGTH];

    static {
        for (int i = 0; i < MAX_LENGTH; ++i) {
            num[i] = random.nextInt();
        }
    }
    @Test
    public void testMySkip() {
        SkipList<Integer> skipList = new SkipList<>();

        long tic = System.currentTimeMillis();
        for(int i=0;i<MAX_LENGTH;++i) {
            skipList.add(num[i], num[i]);
        }
        long toc = System.currentTimeMillis();
        System.out.println("My cost:"+(toc-tic));
        skipList.usageLevel();

    }

    @Test
    public void testConSkip() {
        ConcurrentSkipListMap<Integer, Integer> skipList = new ConcurrentSkipListMap<>();
        long tic = System.currentTimeMillis();
        for(int i=0;i<MAX_LENGTH;++i) {
            skipList.put(num[i], num[i]);
        }
        long toc = System.currentTimeMillis();
        System.out.println("cost:"+(toc-tic));
        System.out.println(skipList.size());
    }

    @Test
    public void testRemove() {
        SkipList<Integer> skipList = new SkipList<>();
        for(int i=10000;i>=0;--i) {
            skipList.add(i, i);
        }
        for(int i=-10;i<=10;++i) {
            System.out.println(skipList.remove(i, i));
        }
        skipList.usageLevel();
    }
}
