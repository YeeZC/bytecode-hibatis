package me.zyee.hibatis.dao.registry;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author yee
 * Created by yee on 2020/6/12
 **/
public abstract class LazyGet<T> {
    protected volatile T element;

    private LazyGet() {
    }

    public static <T> SupplierLazyGet<T> of(Supplier<T> supplier) {
        return new SupplierLazyGet<>(supplier);
    }

    public static <P, T> FunctionLazyGet<P, T> of(Function<P, T> fn) {
        return new FunctionLazyGet<>(fn);
    }

    public static <P, P2, T> BiFunctionLazyGet<P, P2, T> of(BiFunction<P, P2, T> fn) {
        return new BiFunctionLazyGet<>(fn);
    }

    public static class SupplierLazyGet<T> extends LazyGet<T> {
        private final Supplier<T> supplier;

        private SupplierLazyGet(Supplier<T> supplier) {
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
    }

    public static class FunctionLazyGet<P, T> extends LazyGet<T> {
        private final Function<P, T> fn;

        private FunctionLazyGet(Function<P, T> fn) {
            this.fn = fn;
        }

        public T get(P p) {
            synchronized (this) {
                if (null == element) {
                    element = fn.apply(p);
                }
            }
            return element;
        }
    }

    public static class BiFunctionLazyGet<P, P2, T> extends LazyGet<T> {
        private final BiFunction<P, P2, T> fn;

        private BiFunctionLazyGet(BiFunction<P, P2, T> fn) {
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
}
