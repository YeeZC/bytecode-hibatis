package me.zyee.hibatis.query.impl;

import me.zyee.hibatis.common.SupplierLazyGet;
import me.zyee.hibatis.query.PageQuery;
import me.zyee.hibatis.query.SqlMapper;
import me.zyee.hibatis.query.result.impl.PageListImpl;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * @author yee
 * @version 1.0
 * Create by yee on 2020/6/18
 */
public class SqlMapperImpl<T> implements SqlMapper<T> {
    protected BiFunction<Session, String, Query<?>> createQuery;
    protected SupplierLazyGet<Session> sessionSupplier;

    public SqlMapperImpl(BiFunction<Session, String, Query<?>> createQuery, SupplierLazyGet<Session> sessionSupplier) {
        this.createQuery = createQuery;
        this.sessionSupplier = sessionSupplier;
    }

    @Override
    public List<T> select(String sql, Map param) {
        Session session = sessionSupplier.get();
        final Query<?> query = createQuery.apply(session, sql);
        query.setProperties(param);
        if (query instanceof PageQuery) {
            final PageListImpl<T> ts = new PageListImpl<>(
                    () -> {
                        try {
                            final List<?> resultList = query.getResultList();
                            return (List<T>) resultList;
                        } finally {
                            session.close();
                        }
                    },
                    () -> getCount(sql, param));
            ts.setCurrentPage(((PageQuery) query).getPage());
            ts.setPageSize(((PageQuery) query).getSize());
            return ts;
        }
        try {
            return (List<T>) query.getResultList();
        } finally {
            session.close();
        }
    }

    @Override
    public T selectOne(String sql, Map param) {
        try (Session session = sessionSupplier.get()) {
            final Query<?> query = createQuery.apply(session, sql);
            query.setProperties(param);
            return (T) query.getSingleResult();
        }
    }

    @Override
    public int executeUpdate(String sql, Map param) {
        try (Session session = sessionSupplier.get()) {
            final Query<?> query = createQuery.apply(session, sql);
            query.setProperties(param);
            return query.executeUpdate();
        }
    }


    @Override
    public long getCount(String sql, Map param) {
        try (Session session = sessionSupplier.get()) {
            String countSql = "select count(*) " + sql.substring(sql.toLowerCase().indexOf("from"));
            final Query<?> apply = createQuery.apply(session, countSql);
            apply.setProperties(param);
            return ((Number) apply.getSingleResult()).longValue();
        }
    }
}
