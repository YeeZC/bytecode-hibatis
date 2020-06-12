package me.zyee.hibatis.dao.registry;

import java.util.function.Supplier;

/**
 * @author yee
 * Created by yee on 2020/6/12
 **/
public class LazyGet<T> {
    private volatile T element;
    private final Supplier<T> supplier;

    private LazyGet(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    public T get() {
        synchronized (this) {
            if (null == element) {
                element = supplier.get();
            }
        }
        return element;
    }

    public static <T> LazyGet<T> of(Supplier<T> supplier) {
        return new LazyGet<>(supplier);
    }
}
