package me.zyee.dynamic.hql.config;

import me.zyee.dynamic.hql.config.annotation.Attr;

/**
 * @author yeezc
 * Created by yeezc on 2020/6/11
 **/
public class DaoProperty {
    @Attr(value = "column", require = true)
    private String column;
    @Attr(value = "property", require = true)
    private String property;

    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }
}
