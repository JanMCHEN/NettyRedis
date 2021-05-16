package core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public interface RedisSet extends Serializable {
    public long add(RedisObject ...values);
    public long remove(RedisObject ...values);
    public List<RedisObject> members();
    public boolean contains(RedisObject value);
    public int size();
    public boolean isEmpty();

    public static class Hash implements RedisSet {

        private final HashSet<RedisObject> contents;
        public Hash() {
            contents = new HashSet<>();
        }

        @Override
        public long add(RedisObject... values) {
            int ans = 0;
            for (var value:values) {
                if(contents.add(value)) ans++;
            }
            return ans;
        }

        @Override
        public long remove(RedisObject... values) {
            int ans = 0;
            for (var value:values) {
                if(contents.remove(value)) ans++;
            }
            return ans;
        }

        @Override
        public List<RedisObject> members() {
            Object[] array = contents.toArray();
            List<RedisObject> res = new ArrayList<>(array.length);
            for(Object a:array) {
                res.add((RedisObject)a);
            }
            return res;
        }

        @Override
        public boolean contains(RedisObject value) {
            return contents.contains(value);
        }

        @Override
        public int size() {
            return contents.size();
        }

        @Override
        public boolean isEmpty() {
            return contents.isEmpty();
        }
    }

}
