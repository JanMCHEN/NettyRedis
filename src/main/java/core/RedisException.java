package core;

public class RedisException extends RuntimeException {
    public final static RedisException ERROR_TYPE = new RedisException();
    public RedisException(){}
    public RedisException(String message) {
        super(message);
    }

    public static void main(String[] args) {
        try {
            throw ERROR_TYPE;
        }catch (RedisException e){
            e.printStackTrace();
            throw ERROR_TYPE;
        }

    }
}
