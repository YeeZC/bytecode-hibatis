package me.zyee.hibatis.query.impl;

import me.zyee.hibatis.bytecode.HibatisGenerator;
import me.zyee.hibatis.dao.registry.MapRegistry;
import me.zyee.hibatis.query.MapperBuilder;
import me.zyee.hibatis.query.PageQuery;
import me.zyee.hibatis.query.SqlMapper;
import me.zyee.hibatis.query.page.Page;
import me.zyee.hibatis.query.page.PageHelper;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * @author yee
 * @version 1.0
 * Create by yee on 2020/6/18
 */
public class SqlMapperBuilder implements MapperBuilder {
    private boolean sql;
    private String mapId;
    private Class<?> resultType;
    private final MapRegistry mapRegistry;


    public SqlMapperBuilder(MapRegistry mapRegistry) {
        this.mapRegistry = mapRegistry;
    }

    @Override
    public MapperBuilder withResultMap(String mapId) {
        this.mapId = mapId;
        return this;
    }

    @Override
    public MapperBuilder withResultType(Class<?> resultType) {
        this.resultType = resultType;
        return this;
    }

    @Override
    public MapperBuilder withSql() {
        sql = true;
        return this;
    }

    @Override
    public SqlMapper build() {

        BiFunction<Session, String, Query<?>> createQuery = ((session, s) -> {
            final Query<?> query = sql ? session.createSQLQuery(s) : session.createQuery(s);
            final Optional<Page> opt = Optional.ofNullable(PageHelper.getPage());
            if (opt.isPresent()) {
                final Page page = opt.get();
                try {
                    query.setFirstResult(page.getPage() * page.getSize())
                            .setMaxResults(page.getSize());
                    return (Query<?>) Proxy.newProxyInstance(HibatisGenerator.getDefaultClassLoader(),
                            new Class[]{PageQuery.class, Query.class},
                            (proxy, method, args) -> {
                                final Class<?> declaringClass = method.getDeclaringClass();
                                if (ClassUtils.isAssignable(declaringClass, Query.class)
                                        || ClassUtils.isAssignable(Query.class, declaringClass)) {
                                    return method.invoke(query, args);
                                }
                                if (ClassUtils.isAssignable(declaringClass, PageQuery.class)
                                        || ClassUtils.isAssignable(PageQuery.class, declaringClass)) {
                                    return method.invoke(page, args);
                                }
                                return null;
                            });
                } finally {
                    PageHelper.removeLocalPage();
                }
            }
            return query;
        });

        if (StringUtils.isNotEmpty(mapId)) {
            return new MapSqlMapper(mapRegistry, mapId, createQuery);
        }
        if (null != resultType) {
            return new TypeSqlMapper(resultType, createQuery);
        }
        return new SqlMapperImpl<>(createQuery);
    }


}
