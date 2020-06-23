package me.zyee.hibatis.query.impl;

import me.zyee.hibatis.common.SupplierLazyGet;
import me.zyee.hibatis.query.PageQuery;
import me.zyee.hibatis.query.result.impl.PageListImpl;
import me.zyee.hibatis.transformer.HibatisReturnClassTransformer;
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
public class TypeSqlMapper extends SqlMapperImpl<Object> {
    private final Class<?> returnType;

    public TypeSqlMapper(Class<?> returnType, BiFunction<Session, String, Query<?>> createQuery,
                         SupplierLazyGet<Session> sessionSupplier) {
        super(createQuery, sessionSupplier);
        this.returnType = returnType;
    }

    @Override
    public List select(String sql, Map param) {
        final Session session = sessionSupplier.get();
        final Query<?> query = getQuery(session, sql, param);
        if (query instanceof PageQuery) {
            final PageListImpl<?> ts = new PageListImpl<>(() -> {
                try {
                    return query.getResultList();
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
            return query.getResultList();
        } finally {
            session.close();
        }
    }

    private Query<?> getQuery(Session session, String sql, Map param) {
        final Query<?> query = createQuery.apply(session, sql);
        query.setProperties(param);
        query.setResultTransformer(
                new HibatisReturnClassTransformer(
                        returnType));
        return query;
    }

    @Override
    public Object selectOne(String sql, Map param) {
        try (Session session = sessionSupplier.get()) {
            return getQuery(session, sql, param).getSingleResult();
        }
    }
}
