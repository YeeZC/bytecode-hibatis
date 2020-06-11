package me.zyee.dynamic.hql.template;

import java.util.List;

/**
 * @author yeezc
 * Created by yeezc on 2020/6/11
 **/
public interface HQLTemplate<Entity, ID> {
    Entity select(ID id);

    List<Entity> findAll();

    Entity merge(Entity entity);

    boolean delete(ID id);

    boolean deleteAll();
}
