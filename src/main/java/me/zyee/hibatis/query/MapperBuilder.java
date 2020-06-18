package me.zyee.hibatis.query;

/**
 * @author yee
 * @version 1.0
 * Create by yee on 2020/6/18
 */
public interface MapperBuilder {
    MapperBuilder withResultMap(String mapId);

    MapperBuilder withResultType(Class<?> resultType);

    MapperBuilder withSql();

    SqlMapper build();
}
