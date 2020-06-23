package me.zyee.hibatis.common;

import org.apache.commons.lang3.ObjectUtils;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * @author yee
 * Created by yee on 2020/6/12
 **/
public abstract class LazyGet<T> implements AutoCloseable {
    protected volatile T element;
    protected Predicate<T> predicate = ObjectUtils::isEmpty;

    protected LazyGet() {
    }

    public <Get extends LazyGet<T>> Get withTest(Predicate<T> predicate) {
        this.predicate = element -> ObjectUtils.isEmpty(element) || predicate.test(element);
        return (Get) this;
    }

    @Override
    synchronized public void close() {
        if (element instanceof ReferenceItem) {
            ((ReferenceItem) element).decrement();
        }
        if (element instanceof AutoCloseable) {
            try {
                ((AutoCloseable) element).close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        element = null;
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

}
