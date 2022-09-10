package xyz.chenjm.redis.core.structure;


import java.util.HashMap;
import java.util.Map;

public class SortedSet implements RedisObject{
    private final Map<RedisObject, Integer> values;

    private final SkipList<RedisObject> scores;

    public SortedSet() {
        values = new HashMap<>();
        scores = new SkipList<>();
    }

    public boolean add(RedisObject key, int score) {
        Integer oo = values.get(key);
        if(oo == null) {
            scores.add(score, key);
            values.put(key, score);
            return true;
        }
        if(oo != score) {
            scores.remove(oo, key);
            scores.add(score, key);
            values.put(key, score);
        }
        return false;
    }

    public int size() {
        return values.size();
    }
}
