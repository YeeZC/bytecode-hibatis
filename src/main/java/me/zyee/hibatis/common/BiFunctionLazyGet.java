package me.zyee.hibatis.common;

import java.util.function.BiFunction;

/**
 * @author yee
 * @version 1.0
 * Create by yee on 2020/6/19
 */
public class BiFunctionLazyGet<P, P2, T> extends LazyGet<T> {
    private final BiFunction<P, P2, T> fn;

    BiFunctionLazyGet(BiFunction<P, P2, T> fn) {
        this.fn = fn;
    }

    public T get(P p, P2 p2) {
        synchronized (this) {
            if (null == element) {
                element = fn.apply(p, p2);
            }
        }
        return element;
    }
}
