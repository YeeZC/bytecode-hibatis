package me.zyee.dynamic.hql.bytecode;

import me.zyee.dynamic.hql.config.annotation.Param;

import java.util.List;

/**
 * @author yee
 * @version 1.0
 * Created by yee on 2020/6/11
 */
public interface TestDao {
    List<TestEntity> findAll();

    List<TestEntity> findNativeAll();

    TestEntity findById(Test id);

    int insert(@Param("id") String id, @Param("name") String name);
}
