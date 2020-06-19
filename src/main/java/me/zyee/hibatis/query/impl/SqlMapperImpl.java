package me.zyee.hibatis.query.impl;

import me.zyee.hibatis.query.SqlMapper;
import me.zyee.hibatis.query.page.Page;
import me.zyee.hibatis.query.page.PageHelper;
import me.zyee.hibatis.query.result.impl.PageListImpl;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * @author yee
 * @version 1.0
 * Create by yee on 2020/6/18
 */
public class SqlMapperImpl<T> implements SqlMapper<T> {
    protected BiFunction<Session, String, Query> createQuery;
    protected static final String PAGE_PARAM = "_HIBATIS_PAGE_";

    public SqlMapperImpl(BiFunction<Session, String, Query> createQuery) {
        this.createQuery = createQuery;
    }

    @Override
    public List<T> select(Session session, String sql, Map param) {
        final Query<?> query = createQuery.apply(session, sql);
        final Page page = (Page) param.remove(PAGE_PARAM);
        query.setProperties(param);
        if (null != page) {
            query.setFirstResult(page.getPage() * page.getSize())
                    .setMaxResults(page.getSize());
        }
        return (List<T>) query.getResultList();
    }

    @Override
    public T selectOne(Session session, String sql, Map param) {
        final Query<?> query = createQuery.apply(session, sql);
        query.setProperties(param);
        return (T) query.getSingleResult();
    }

    @Override
    public int executeUpdate(Session session, String sql, Map param) {
        final Query<?> query = createQuery.apply(session, sql);
        query.setProperties(param);
        return query.executeUpdate();
    }

    @Override
    public List<T> selectPage(Session session, String sql, Map param) {
        final Page page = PageHelper.get();
        if (null == page) {
            return select(session, sql, param);
        }
        try {
            Map pageParam = new HashMap(param);
            pageParam.put(PAGE_PARAM, page);
            final PageListImpl<T> ts = new PageListImpl<>(() -> select(session, sql, pageParam),
                    () -> getCount(session, sql, param));
            ts.setCurrentPage(page.getPage());
            ts.setPageSize(page.getSize());
            return ts;
        } finally {
            PageHelper.clear();
        }
    }

    @Override
    public long getCount(Session session, String sql, Map param) {
        String countSql = "select count(*) " + sql.substring(sql.toLowerCase().indexOf("from"));
        final Query apply = createQuery.apply(session, countSql);
        apply.setProperties(param);
        return ((Number) apply.getSingleResult()).longValue();
    }
}
