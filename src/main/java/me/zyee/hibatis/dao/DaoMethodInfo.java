package me.zyee.hibatis.dao;

import me.zyee.hibatis.dao.annotation.Attr;
import me.zyee.hibatis.dao.annotation.Content;

import java.util.Objects;

/**
 * @author yee
 * Created by yee on 2020/6/11
 **/
public class DaoMethodInfo {
    @Attr(value = "id", require = true)
    private String id;
    @Attr("resultType")
    private Class<?> resultType;
    @Attr("native")
    private Boolean nativeSql;
    @Attr("resultMap")
    private String resultMap;
    @Content
    private String hql;
    private MethodType type;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Class<?> getResultType() {
        return resultType;
    }

    public void setResultType(Class<?> resultType) {
        this.resultType = resultType;
    }

    public MethodType getType() {
        return type;
    }

    public void setType(MethodType type) {
        this.type = type;
    }

    public String getHql() {
        return hql;
    }

    public void setHql(String hql) {
        this.hql = hql;
    }

    public Boolean isNativeSql() {
        return nativeSql;
    }

    public void setNativeSql(Boolean nativeSql) {
        this.nativeSql = nativeSql;
    }

    public String getResultMap() {
        return resultMap;
    }

    public void setResultMap(String resultMap) {
        this.resultMap = resultMap;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DaoMethodInfo that = (DaoMethodInfo) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
