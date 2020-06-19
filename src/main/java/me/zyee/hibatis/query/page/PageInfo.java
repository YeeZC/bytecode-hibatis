package me.zyee.hibatis.query.page;

import me.zyee.hibatis.query.result.PageList;

import java.util.List;

/**
 * @author yee
 * @version 1.0
 * Create by yee on 2020/6/19
 */
public class PageInfo {
    private List<?> data;
    private int page;
    private int size;
    private long total;

    public List<?> getData() {
        return data;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getTotal() {
        return total;
    }

    public static PageInfo of(List<?> result) {
        final PageInfo pageInfo = new PageInfo();
        if (result instanceof PageList) {
            PageList<?> list = (PageList<?>) result;
            pageInfo.data = list.getContent();
            pageInfo.total = list.getCount();
            pageInfo.page = list.getCurrentPage();
            pageInfo.size = list.getPageSize();
        } else {
            pageInfo.data = result;
            pageInfo.total = result.size();
            pageInfo.page = 0;
            pageInfo.size = result.size();
        }
        return pageInfo;
    }

    @Override
    public String toString() {
        return "PageInfo{" +
                "data=" + data +
                ", page=" + page +
                ", size=" + size +
                ", total=" + total +
                '}';
    }
}
