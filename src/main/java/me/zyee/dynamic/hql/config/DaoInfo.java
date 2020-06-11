package me.zyee.dynamic.hql.config;

import me.zyee.dynamic.hql.config.annotation.Attr;

import java.util.List;

/**
 * @author yeezc
 * Created by yeezc on 2020/6/11
 **/
public class DaoInfo {
    @Attr(value = "to", require = true)
    private String className;
    private List<DaoMethodInfo> methodInfos;

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public List<DaoMethodInfo> getMethodInfos() {
        return methodInfos;
    }

    public void setMethodInfos(List<DaoMethodInfo> methodInfos) {
        this.methodInfos = methodInfos;
    }
}
