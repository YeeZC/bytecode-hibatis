package me.zyee.dynamic.hql.template.impl;

import me.zyee.dynamic.hql.template.HQLTemplate;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.io.Serializable;
import java.util.List;

/**
 * @author yeezc
 * Created by yeezc on 2020/6/11
 **/
public abstract class BaseHQLTemplate<Entity, ID extends Serializable> implements HQLTemplate<Entity, ID> {

    private final Session session;
    private final Class<Entity> entity;

    public BaseHQLTemplate(Session session, Class<Entity> entity) {
        this.session = session;
        this.entity = entity;
    }

    @Override
    public Entity select(ID id) {
        return session.get(entity, id);
    }

    @Override
    public List<Entity> findAll() {
        String hql = "from " + entity.getSimpleName();
        Query<Entity> query = session.createQuery(hql, entity);
        return query.getResultList();
    }

    @Override
    public Entity merge(Entity entity) {
        return this.entity.cast(session.merge(entity));
    }

    @Override
    public boolean delete(ID id) {
        this.session.delete(select(id));
        return true;
    }

    @Override
    public boolean deleteAll() {
        List<Entity> all = findAll();
        for (Entity entity : all) {
            session.delete(entity);
        }
        return true;
    }
}
