package me.zyee.hibatis.query.impl;

import me.zyee.hibatis.dao.registry.MapRegistry;
import me.zyee.hibatis.query.MapperBuilder;
import me.zyee.hibatis.query.SqlMapper;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.query.Query;

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

        BiFunction<Session, String, Query> createQuery = ((session, s) ->
                sql ? session.createSQLQuery(s) : session.createQuery(s));

        if (StringUtils.isNotEmpty(mapId)) {
            return new MapSqlMapper(mapRegistry, mapId, createQuery);
        }
        if (null != resultType) {
            return new TypeSqlMapper(resultType, createQuery);
        }
        return new SqlMapperImpl<>(createQuery);
    }


}
