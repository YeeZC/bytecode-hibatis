package me.zyee.hibatis.query.impl;

import me.zyee.hibatis.dao.registry.MapRegistry;
import me.zyee.hibatis.query.HibatisQuery;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.List;
import java.util.Map;

/**
 * @author yee
 * @version 1.0
 * Create by yee on 2020/6/17
 */
public class HibatisQueryImpl<T> implements HibatisQuery<T> {
    private final MapRegistry mapRegistry;

    public HibatisQueryImpl(MapRegistry mapRegistry) {
        this.mapRegistry = mapRegistry;
    }

    @Override
    public List<T> select(Session session, String hql, String mapId, Map<String, Object> params, boolean nativeSql) {
        final Query query = createQuery(session, hql, nativeSql);
        query.setProperties(params);
        return query.getResultList();
    }

    @Override
    public T selectOne(Session session, String hql, String mapId, Map<String, Object> params, boolean nativeSql) {
        final Query query = createQuery(session, hql, nativeSql);
        query.setProperties(params);

        return (T) query.getSingleResult();
    }

    @Override
    public int getCount(Session session, String hql, Map<String, Object> params, boolean nativeSql) {
        final String lowerCase = hql.toLowerCase();
        final int from = StringUtils.indexOf(lowerCase, "from");
        if (from >= 0) {
            final Query query = createQuery(session, "select count(*) " + hql.substring(from), nativeSql);
            query.setProperties(params);
            final Object singleResult = query.getSingleResult();
            if (singleResult instanceof Number) {
                return ((Number) singleResult).intValue();
            }
        }
        return 0;
    }

    @Override
    public int executeUpdate(Session session, String hql, Map<String, Object> params, boolean nativeSql) {
        final Query query = createQuery(session, hql, nativeSql);
        query.setProperties(params);
        return query.executeUpdate();
    }

    private Query createQuery(Session session, String hql, boolean nativeSql) {
        if (nativeSql) {
            return session.createSQLQuery(hql);
        }
        return session.createQuery(hql);
    }
}
