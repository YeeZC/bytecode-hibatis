package me.zyee.hibatis.exception;

/**
 * @author yee
 * @version 1.0
 * Create by yee on 2020/6/16
 */
public class HibatisException extends Exception {
    public HibatisException(Throwable cause) {
        super(cause);
    }

    public HibatisException(String message) {
        super(message);
    }
}
