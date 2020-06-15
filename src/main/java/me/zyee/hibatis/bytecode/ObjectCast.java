package me.zyee.hibatis.bytecode;

import org.apache.commons.lang3.reflect.MethodUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author yee
 * @version 1.0
 * Create by yee on 2020/6/15
 */
public class ObjectCast {
    public List<?> cast(List<?> source) {
        return source.stream().map(item -> {
            try {
                return MethodUtils.invokeMethod(item, true, "transfer");
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());
    }
}
