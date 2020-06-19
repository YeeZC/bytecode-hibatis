package me.zyee.hibatis.query.page;

/**
 * @author yee
 * @version 1.0
 * Create by yee on 2020/6/19
 */
public class PageHelper {
    private static final ThreadLocal<Page> LOCAL_PAGE = new ThreadLocal<>();

    public static void startPage(int page, int size) {
        if (page >= 0 && size > 0) {
            final Page p = new Page();
            p.setPage(page);
            p.setSize(size);
            LOCAL_PAGE.set(p);
        }
    }


    public static Page getPage() {
        return LOCAL_PAGE.get();
    }

    public static void removeLocalPage() {
        LOCAL_PAGE.remove();
    }
}
