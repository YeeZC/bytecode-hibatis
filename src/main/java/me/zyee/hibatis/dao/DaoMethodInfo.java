package me.zyee.hibatis.dao;

import me.zyee.hibatis.dao.annotation.Attr;
import me.zyee.hibatis.dao.annotation.Content;

/**
 * @author yee
 * Created by yee on 2020/6/11
 **/
public class DaoMethodInfo {
    @Attr(value = "id", require = true)
    private String id;
    @Attr("resultType")
    private String resultType;
    @Attr("native")
    private Boolean nativeSql;
    @Content
    private String hql;
    private MethodType type;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getResultType() {
        return resultType;
    }

    public void setResultType(String resultType) {
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
}