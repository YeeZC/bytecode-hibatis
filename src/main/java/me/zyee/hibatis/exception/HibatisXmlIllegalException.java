package me.zyee.hibatis.exception;

/**
 * @author yee
 * @version 1.0
 * Create by yee on 2020/6/16
 */
public class HibatisXmlIllegalException extends RuntimeException {

    public HibatisXmlIllegalException() {
    }

    public HibatisXmlIllegalException(Throwable cause) {
        super(cause);
    }

    public HibatisXmlIllegalException(String message) {
        super(message);
    }

    public HibatisXmlIllegalException(String message, Throwable cause) {
        super(message, cause);
    }
}
