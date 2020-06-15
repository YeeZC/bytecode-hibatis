package me.zyee.hibatis.dao;

import me.zyee.hibatis.dao.annotation.Attr;

import java.util.List;
import java.util.Objects;

/**
 * @author yee
 * Created by yee on 2020/6/11
 **/
public class DaoInfo {
    @Attr(value = "id", require = true)
    private Class<?> id;
    @Attr(value = "entity", require = true)
    private Class<?> entity;
    private List<DaoMethodInfo> methodInfos;
    private List<DaoMapInfo> maps;


    public List<DaoMethodInfo> getMethodInfos() {
        return methodInfos;
    }

    public void setMethodInfos(List<DaoMethodInfo> methodInfos) {
        this.methodInfos = methodInfos;
    }

    public Class<?> getId() {
        return id;
    }

    public void setId(Class<?> id) {
        this.id = id;
    }

    public Class<?> getEntity() {
        return entity;
    }

    public void setEntity(Class<?> entity) {
        this.entity = entity;
    }

    public List<DaoMapInfo> getMaps() {
        return maps;
    }

    public void setMaps(List<DaoMapInfo> maps) {
        this.maps = maps;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DaoInfo daoInfo = (DaoInfo) o;
        return Objects.equals(id, daoInfo.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
