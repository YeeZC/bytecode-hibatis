package me.zyee.hibatis.query.impl;

import me.zyee.hibatis.query.SqlMapper;
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
    protected BiFunction<Session, String, Query> createQuery;

    public SqlMapperImpl(BiFunction<Session, String, Query> createQuery) {
        this.createQuery = createQuery;
    }

    @Override
    public List<T> select(Session session, String sql, Map param) {
        final Query<?> query = createQuery.apply(session, sql);
        query.setProperties(param);
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

}
