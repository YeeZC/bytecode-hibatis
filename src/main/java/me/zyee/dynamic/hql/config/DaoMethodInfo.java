package me.zyee.dynamic.hql.config;

import me.zyee.dynamic.hql.config.annotation.Attr;
import me.zyee.dynamic.hql.config.annotation.Content;

/**
 * @author yeezc
 * Created by yeezc on 2020/6/11
 **/
public class DaoMethodInfo {
    @Attr(value = "id", require = true)
    private String id;
    @Attr("resultMap")
    private DaoMapInfo resultMap;
    @Attr("resultType")
    private String resultType;
    @Attr("paramMap")
    private DaoMapInfo paramMap;
    @Attr("paramType")
    private String paramType;
    private MethodType type;
    @Content
    private String hql;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public DaoMapInfo getResultMap() {
        return resultMap;
    }

    public void setResultMap(DaoMapInfo resultMap) {
        this.resultMap = resultMap;
    }

    public String getResultType() {
        return resultType;
    }

    public void setResultType(String resultType) {
        this.resultType = resultType;
    }

    public DaoMapInfo getParamMap() {
        return paramMap;
    }

    public void setParamMap(DaoMapInfo paramMap) {
        this.paramMap = paramMap;
    }

    public String getParamType() {
        return paramType;
    }

    public void setParamType(String paramType) {
        this.paramType = paramType;
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
}
