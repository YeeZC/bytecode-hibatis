package me.zyee.hibatis.bytecode;

import me.zyee.hibatis.dao.annotation.Param;

/**
 * @author yee
 * @version 1.0
 * Created by yee on 2020/6/12
 */
public class Test {
    @Param("id")
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
