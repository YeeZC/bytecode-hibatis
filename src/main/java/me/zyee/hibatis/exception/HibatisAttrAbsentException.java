package me.zyee.hibatis.exception;

/**
 * @author yee
 * @version 1.0
 * Create by yee on 2020/6/16
 */
public class HibatisAttrAbsentException extends RuntimeException {
    public HibatisAttrAbsentException(String attr) {
        super("Attribute " + attr + " is absent");
    }
}
