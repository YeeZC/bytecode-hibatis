package me.zyee.hibatis.query.page;

import me.zyee.hibatis.query.PageQuery;

/**
 * @author yee
 * @version 1.0
 * Create by yee on 2020/6/19
 */
public class Page implements PageQuery {
    private int page;
    private int size;

    @Override
    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    @Override
    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
