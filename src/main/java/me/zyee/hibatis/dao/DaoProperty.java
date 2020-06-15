package me.zyee.hibatis.dao;

import me.zyee.hibatis.dao.annotation.Attr;

import java.util.Objects;

/**
 * @author yee
 * Created by yee on 2020/6/11
 **/
public class DaoProperty {
    @Attr(value = "column", require = true)
    private String column;
    @Attr(value = "field", require = true)
    private String property;
    @Attr(value = "javaType", require = true)
    private Class<?> javaType;

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

    public Class<?> getJavaType() {
        return javaType;
    }

    public void setJavaType(Class<?> javaType) {
        this.javaType = javaType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DaoProperty that = (DaoProperty) o;
        return Objects.equals(column, that.column) &&
                Objects.equals(property, that.property) &&
                Objects.equals(javaType, that.javaType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(column, property, javaType);
    }
}
