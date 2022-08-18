package exception;

public class RDBFileException extends RuntimeException{
    public RDBFileException(String msg) {
        super(msg);
    }

    public RDBFileException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
