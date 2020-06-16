package me.zyee.hibatis.template.impl;

import me.zyee.hibatis.dao.registry.DaoRegistry;
import me.zyee.hibatis.exception.HibatisException;
import me.zyee.hibatis.template.HiBatisTemplate;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Id;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author yee
 * Created by yee on 2020/6/12
 **/
public class BaseHiBatisTemplate implements HiBatisTemplate {
    private final SessionFactory sessionFactory;
    private final DaoRegistry registry;
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseHiBatisTemplate.class);

    public BaseHiBatisTemplate(SessionFactory sessionFactory, DaoRegistry registry) {
        this.sessionFactory = sessionFactory;
        this.registry = registry;
    }

    @Override
    public <T> T runTx(Process<T> callable) {
        try (final Session session = sessionFactory.openSession()) {
            final Transaction tx = session.beginTransaction();
            try {
                final T process = callable.process(session);
                tx.commit();
                return process;
            } catch (Exception e) {
                LOGGER.error("invoke error rollback", e);
                tx.rollback();
            }
        }
        return null;
    }

    @Override
    public <T> T runNonTx(Process<T> callable) {
        try (final Session session = sessionFactory.openSession()) {
            try {
                return callable.process(session);
            } catch (Exception e) {
                LOGGER.error("invoke error", e);
            }
        }
        return null;
    }

    @Override
    public <T> T createDao(Class<T> daoInf, Session session) throws HibatisException {
        try {
            return registry.getDao(daoInf, session);
        } catch (Exception e) {
            LOGGER.error("get Dao error", e);
            throw new HibatisException(e);
        }
    }


     @Override
     public void insert(Session session, Object entity) {
        session.persist(entity);
    }

     @Override
     public void insert(Session session, Iterable<Object> entities) {
        for (Object entity : entities) {
            session.persist(entity);
        }
    }

     @Override
     public void delete(Session session, Object entity) {
        session.delete(entity);
    }

     @Override
     public  void delete(Session session, Iterable<Object> entities) {
        for (Object entity : entities) {
            session.delete(entity);
        }
    }

     @Override
     public  <T>Optional<T> get(Session session, Class<T> entity, Serializable id) {
        return Optional.ofNullable(session.get(entity, id));
    }

     @Override
     public <T>List<T> findByIds(Session session, Class<T> entity, Iterable<Serializable> ids) {
        final List<Field> idFields = FieldUtils.getFieldsListWithAnnotation(entity, Id.class);
        if (idFields.size() != 1) {
            List<T> result = new ArrayList<>();
            for (Serializable id : ids) {
                Optional.ofNullable(session.get(entity, id)).ifPresent(result::add);
            }
            return result;
        }
        final CriteriaBuilder builder = session.getCriteriaBuilder();
        final CriteriaQuery<T> query = builder.createQuery(entity);
        final Root<T> from = query.from(entity);
        final Path<Object> field = from.get(idFields.get(0).getName());
        List<Serializable> idsList = new ArrayList<>();
        for (Serializable id : ids) {
            idsList.add(id);
        }
        query.where(builder.in(field).getExpression().in(idsList));
        return session.createQuery(query).getResultList();
    }

    @Override
    public <T> List<T> findAll(Session session, Class<T> entity) {
        final CriteriaQuery<T> query = session.getCriteriaBuilder().createQuery(entity);
        return session.createQuery(query).getResultList();
    }

    @Override
     public void update(Session session, Object entity) {
        session.merge(entity);
    }
}
