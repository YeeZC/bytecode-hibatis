package me.zyee.hibatis.query.result.impl;

import me.zyee.hibatis.dao.registry.LazyGet;
import me.zyee.hibatis.query.result.PageList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author yee
 * @version 1.0
 * Create by yee on 2020/6/19
 */
public class PageListImpl<T> extends ArrayList<T> implements PageList<T> {

    private int currentPage;
    private int pageSize;

    private final LazyGet.SupplierLazyGet<List<T>> content;
    private final LazyGet.SupplierLazyGet<Long> count;

    public PageListImpl(Supplier<List<T>> content, Supplier<Long> count) {
        this.content = LazyGet.of(content);
        this.count = LazyGet.of(count);
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    @Override
    public long getCount() {
        return count.get();
    }

    @Override
    public List<T> getContent() {
        return content.get();
    }

    @Override
    public int getCurrentPage() {
        return currentPage;
    }

    @Override
    public int getPageSize() {
        return pageSize;
    }

    @Override
    public Iterator<T> iterator() {
        return content.get().iterator();
    }

    @Override
    public int size() {
        return content.get().size();
    }

    @Override
    public boolean add(T t) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        throw new UnsupportedOperationException();
    }
}
