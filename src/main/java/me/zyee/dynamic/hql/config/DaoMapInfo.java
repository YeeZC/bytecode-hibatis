package me.zyee.dynamic.hql.config;

import me.zyee.dynamic.hql.config.annotation.Attr;
import me.zyee.dynamic.hql.config.annotation.Children;

import java.util.List;

/**
 * @author yeezc
 * Created by yeezc on 2020/6/11
 **/
public class DaoMapInfo {
    @Attr(value = "id", require = true)
    private String mapId;
    @Attr("to")
    private String className;
    @Children
    private List<DaoProperty> properties;

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
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
}
