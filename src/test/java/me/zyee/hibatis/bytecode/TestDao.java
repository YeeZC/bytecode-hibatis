package me.zyee.hibatis.bytecode;

import me.zyee.hibatis.dao.annotation.Param;

/**
 * @author yee
 * @version 1.0
 * Created by yee on 2020/6/11
 */
public interface TestDao {
    TestBean[] findAll();

    int getAllCount();

    TestEntity findById(@Param("id") String id);

    int insert(@Param("id") String id, @Param("name") String name);
}
