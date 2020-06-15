package me.zyee.hibatis.dao;

import me.zyee.hibatis.dao.annotation.Attr;
import me.zyee.hibatis.dao.annotation.Children;

import java.util.List;
import java.util.Objects;

/**
 * @author yee
 * Created by yee on 2020/6/11
 **/
public class DaoMapInfo {
    @Attr(value = "id", require = true)
    private String mapId;
    @Attr("class")
    private Class<?> className;
    @Children
    private List<DaoProperty> properties;

    public Class<?> getClassName() {
        return className;
    }

    public void setClassName(Class<?> className) {
        this.className = className;
    }

    public List<DaoProperty> getProperties() {
        return properties;
    }

    public void setProperties(List<DaoProperty> properties) {
        this.properties = properties;
    }

    public String getMapId() {
        return mapId;
    }

    public void setMapId(String mapId) {
        this.mapId = mapId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DaoMapInfo that = (DaoMapInfo) o;
        return Objects.equals(mapId, that.mapId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mapId);
    }
}
