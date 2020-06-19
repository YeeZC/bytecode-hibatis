package me.zyee.hibatis.common;

import java.util.function.Function;

/**
 * @author yee
 * @version 1.0
 * Create by yee on 2020/6/19
 */
public class FunctionLazyGet<P, T> extends LazyGet<T> {
    private final Function<P, T> fn;

    FunctionLazyGet(Function<P, T> fn) {
        this.fn = fn;
    }


    public T get(P p) {
        synchronized (this) {
            if (predicate.test(element)) {
                element = fn.apply(p);
            }
        }
        return element;
    }
}
