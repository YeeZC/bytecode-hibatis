package me.zyee.hibatis.dao;

import me.zyee.hibatis.dao.annotation.Attr;

/**
 * @author yee
 * Created by yee on 2020/6/11
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
