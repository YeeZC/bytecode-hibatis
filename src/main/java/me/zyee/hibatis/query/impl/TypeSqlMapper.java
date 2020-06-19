package me.zyee.hibatis.query.impl;

import me.zyee.hibatis.query.page.Page;
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

    public TypeSqlMapper(Class<?> returnType, BiFunction<Session, String, Query> createQuery) {
        super(createQuery);
        this.returnType = returnType;
    }

    @Override
    public List select(Session session, String sql, Map param) {
        final Query query = getQuery(session, sql, param);
        return query.getResultList();
    }

    private Query getQuery(Session session, String sql, Map param) {
        final Query<?> query = (Query<?>) createQuery.apply(session, sql);
        final Page page = (Page) param.remove(PAGE_PARAM);
        query.setProperties(param);
        if (null != page) {
            query.setFirstResult(page.getPage() * page.getSize())
                    .setMaxResults(page.getSize());
        }
        query.setProperties(param);
        query.setResultTransformer(
                new HibatisReturnClassTransformer(
                        returnType));
        return query;
    }

    @Override
    public Object selectOne(Session session, String sql, Map param) {
        return getQuery(session, sql, param).getSingleResult();
    }
}
