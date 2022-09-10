package xyz.chenjm.redis.command.string;

import xyz.chenjm.redis.command.TypeAdapter;
import xyz.chenjm.redis.core.RedisDB;
import xyz.chenjm.redis.exception.ErrorTypeException;

public interface StringAdapter extends TypeAdapter<CharSequence> {

    @Override
    default CharSequence get(RedisDB db, String key) {
        Object obj = db.getAndDelete(key);
        if (obj == null || obj instanceof CharSequence) {
            return (CharSequence) obj;
        }
        throw new ErrorTypeException();
    }
}
