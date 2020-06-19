package me.zyee.hibatis.query.result;

import java.util.List;

/**
 * @author yee
 * @version 1.0
 * Create by yee on 2020/6/19
 */
public interface PageList<T> {
    long getCount();

    List<T> getContent();

    int getCurrentPage();

    int getPageSize();
}
