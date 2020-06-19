package me.zyee.hibatis.common;

import java.util.function.Supplier;

/**
 * @author yee
 * @version 1.0
 * Create by yee on 2020/6/19
 */
public class SupplierLazyGet<T> extends LazyGet<T> {
    private final Supplier<T> supplier;


    SupplierLazyGet(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    public T get() {
        synchronized (this) {
            if (!predicate.test(element)) {
                element = supplier.get();
            }
        }
        return element;
    }
}
