package me.zyee.hibatis.query.impl;

import me.zyee.hibatis.dao.registry.MapRegistry;
import me.zyee.hibatis.exception.ByteCodeGenerateException;
import me.zyee.hibatis.query.HibatisQuery;
import me.zyee.hibatis.transformer.HibatisReturnClassTransformer;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author yee
 * @version 1.0
 * Create by yee on 2020/6/17
 */
public class HibatisQueryImpl implements HibatisQuery {
    private final MapRegistry mapRegistry;

    public HibatisQueryImpl(MapRegistry mapRegistry) {
        this.mapRegistry = mapRegistry;
    }

    @Override
    public List<Object> select(Session session, QueryParam param) throws ByteCodeGenerateException {
        final Query query = createQuery(session, param);
        query.setProperties(Optional.ofNullable(param.getParams()).orElse(Collections.emptyMap()));
        final String mapId = param.getMapId();
        if (StringUtils.isNotEmpty(mapId)) {
            query.setResultTransformer(
                    new HibatisReturnClassTransformer(mapRegistry.getMapClass(mapId, session.getClass().getClassLoader())));
        }
        return query.getResultList();
    }

    @Override
    public Object selectOne(Session session, QueryParam param) throws ByteCodeGenerateException {
        final Query query = createQuery(session, param);
        final String mapId = param.getMapId();
        if (StringUtils.isNotEmpty(mapId)) {
            query.setResultTransformer(
                    new HibatisReturnClassTransformer(mapRegistry.getMapClass(mapId, session.getClass().getClassLoader())));
        }
        query.setProperties(Optional.ofNullable(param.getParams()).orElse(Collections.emptyMap()));
        return query.getSingleResult();
    }

    @Override
    public int getCount(Session session, QueryParam param) {
        final String lowerCase = param.getHql().toLowerCase();
        final int from = StringUtils.indexOf(lowerCase, "from");
        if (from >= 0) {
            final QueryParam queryParam = QueryParam.create("select count(*) " + param.getHql().substring(from), param.isNativeSql());
            final Query query = createQuery(session, queryParam);
            query.setProperties(Optional.ofNullable(param.getParams()).orElse(Collections.emptyMap()));
            final Object singleResult = query.getSingleResult();
            if (singleResult instanceof Number) {
                return ((Number) singleResult).intValue();
            }
        }
        return 0;
    }

    @Override
    public int executeUpdate(Session session, QueryParam param) {
        final Query query = createQuery(session, param);
        query.setProperties(Optional.ofNullable(param.getParams()).orElse(Collections.emptyMap()));
        return query.executeUpdate();
    }

    private Query createQuery(Session session, QueryParam param) {
        if (param.isNativeSql()) {
            return session.createSQLQuery(param.getHql());
        }
        return session.createQuery(param.getHql());
    }
}
