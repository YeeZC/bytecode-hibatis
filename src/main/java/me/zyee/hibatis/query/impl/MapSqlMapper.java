package me.zyee.hibatis.query.impl;

import me.zyee.hibatis.bytecode.compiler.bean.ObjectCast;
import me.zyee.hibatis.common.SupplierLazyGet;
import me.zyee.hibatis.dao.registry.MapRegistry;
import me.zyee.hibatis.exception.ByteCodeGenerateException;
import me.zyee.hibatis.query.PageQuery;
import me.zyee.hibatis.query.result.impl.PageListImpl;
import me.zyee.hibatis.transformer.HibatisReturnClassTransformer;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * @author yee
 * @version 1.0
 * Create by yee on 2020/6/18
 */
public class MapSqlMapper extends SqlMapperImpl {
    private final MapRegistry mapRegistry;
    private final String mapId;

    public MapSqlMapper(MapRegistry mapRegistry, String mapId,
                        BiFunction<Session, String, Query<?>> createQuery,
                        SupplierLazyGet<Session> sessionSupplier) {
        super(createQuery, sessionSupplier);
        this.mapRegistry = mapRegistry;
        this.mapId = mapId;
    }

    @Override
    public List select(String sql, Map param) {
        Session session = (Session) sessionSupplier.get();
        final Query<?> query = getQuery(session, sql, param);
        if (query instanceof PageQuery) {
            final PageListImpl<?> ts = new PageListImpl<>(() ->
            {
                try {
                    return query.getResultStream()
                            .map(o -> ((ObjectCast<?>) o).cast())
                            .collect(Collectors.toList());
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
            return query.getResultStream().map(o -> ((ObjectCast<?>) o).cast())
                    .collect(Collectors.toList());
        } finally {
            session.close();
        }
    }

    private Query<?> getQuery(Session session, String sql, Map param) {
        final Query<?> query = (Query<?>) createQuery.apply(session, sql);
        query.setProperties(param);
        try {
            final ClassLoader classLoader = session.getClass().getClassLoader();
            final Class<?> mapClass = mapRegistry.getMapClass(mapId, classLoader);
            query.setResultTransformer(
                    new HibatisReturnClassTransformer(
                            mapClass));
        } catch (ByteCodeGenerateException e) {
            throw new RuntimeException(e);
        }
        return query;
    }

    @Override
    public Object selectOne(String sql, Map param) {
        try (Session session = (Session) sessionSupplier.get()) {
            return ((ObjectCast<?>) getQuery(session, sql, param).getSingleResult()).cast();
        }
    }
}
