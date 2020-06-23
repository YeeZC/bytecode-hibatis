package me.zyee.hibatis.template.impl;

import me.zyee.hibatis.bytecode.HibatisGenerator;
import me.zyee.hibatis.common.LazyGet;
import me.zyee.hibatis.common.ReferenceItem;
import me.zyee.hibatis.common.SupplierLazyGet;
import me.zyee.hibatis.dao.registry.DaoRegistry;
import me.zyee.hibatis.datasource.DataSource;
import me.zyee.hibatis.exception.HibatisException;
import me.zyee.hibatis.query.page.Page;
import me.zyee.hibatis.query.page.PageHelper;
import me.zyee.hibatis.query.result.impl.PageListImpl;
import me.zyee.hibatis.template.HiBatisTemplate;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Id;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.LongAdder;

/**
 * @author yee
 * Created by yee on 2020/6/12
 **/
public class BaseHiBatisTemplate implements HiBatisTemplate {
    private final DaoRegistry registry;
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseHiBatisTemplate.class);
    private final SupplierLazyGet<Session> sessionSupplier;

    public BaseHiBatisTemplate(DataSource sessionFactory, DaoRegistry registry) {
        this.registry = registry;
        this.sessionSupplier = LazyGet.of(() -> {
            final Session session = sessionFactory.createSession();
            return (Session) Proxy.newProxyInstance(HibatisGenerator.getDefaultClassLoader(), new Class[]{Session.class,
                    ReferenceItem.class}, new InvocationHandler() {
                private final LongAdder reference = new LongAdder();

                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    final Class<?> declaringClass = method.getDeclaringClass();
                    final String name = method.getName();
                    if ("close".equals(name)) {
                        return MethodUtils.getAccessibleMethod(ReferenceItem.class, "decrement").invoke(proxy);
                    }
                    if (ClassUtils.isAssignable(Session.class, declaringClass) || ClassUtils.isAssignable(declaringClass, Session.class)) {
                        return method.invoke(session, args);
                    }
                    if (ClassUtils.isAssignable(ReferenceItem.class, declaringClass) || ClassUtils.isAssignable(declaringClass, ReferenceItem.class)) {
                        final Object invoke = MethodUtils.getAccessibleMethod(LongAdder.class, name).invoke(reference);
                        if ("decrement".equals(name)) {
                            if (reference.intValue() <= 0) {
                                session.close();
                            }
                        }
                        return invoke;
                    }
                    return null;
                }
            });
        })
                .withTest(session -> !session.isOpen());
    }

    @Override
    public <T> T runTx(Process<T> callable) {
        try (final Session session = sessionSupplier.get()) {
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
        try (final Session session = sessionSupplier.get()) {
            try {
                return callable.process(session);
            } catch (Exception e) {
                LOGGER.error("invoke error", e);
            }
        }
        return null;
    }

    @Override
    public <T> T createDao(Class<T> daoInf) throws HibatisException {
        try {
            return registry.getNewDao(daoInf, sessionSupplier);
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
    public void delete(Session session, Iterable<Object> entities) {
        for (Object entity : entities) {
            session.delete(entity);
        }
    }

    @Override
    public <T> Optional<T> get(Session session, Class<T> entity, Serializable id) {
        return Optional.ofNullable(session.get(entity, id));
    }

    @Override
    public <T> List<T> findByIds(Session session, Class<T> entity, Iterable<Serializable> ids) {
        final List<Field> idFields = FieldUtils.getFieldsListWithAnnotation(entity, Id.class);
        if (idFields.size() != 1) {
            List<T> result = new ArrayList<>();
            for (Serializable id : ids) {
                Optional.ofNullable(session.get(entity, id)).ifPresent(result::add);
            }
            return result;
        }
        final CriteriaBuilder builder = session.getCriteriaBuilder();
        final CriteriaQuery<T> criteriaQuery = builder.createQuery(entity);
        final Root<T> from = criteriaQuery.from(entity);
        final Path<Object> field = from.get(idFields.get(0).getName());
        List<Serializable> idsList = new ArrayList<>();
        for (Serializable id : ids) {
            idsList.add(id);
        }
        final Page page = PageHelper.getPage();
        final Predicate in = builder.in(field).getExpression().in(idsList);
        criteriaQuery.where(in);
        final Query<T> query = session.createQuery(criteriaQuery);
        if (null != page) {
            PageHelper.removeLocalPage();
            query.setFirstResult(page.getPage() * page.getSize());
            query.setMaxResults(page.getSize());
            final CriteriaQuery<Long> countQuery = builder.createQuery(Long.class);
            final Query<Long> count = session.createQuery(countQuery.where(in));
            final PageListImpl<T> ts = new PageListImpl<>(query::getResultList, count::getSingleResult);
            ts.setPageSize(page.getSize());
            ts.setCurrentPage(page.getPage());
            return ts;
        }

        return query.getResultList();
    }

    @Override
    public <T> List<T> findAll(Session session, Class<T> entity) {
        final CriteriaBuilder builder = session.getCriteriaBuilder();
        final CriteriaQuery<T> criteriaQuery = builder.createQuery(entity);
        final Page page = PageHelper.getPage();
        final Query<T> query = session.createQuery(criteriaQuery);
        if (null != page) {
            PageHelper.removeLocalPage();
            query.setFirstResult(page.getPage() * page.getSize());
            query.setMaxResults(page.getSize());
            final CriteriaQuery<Long> countQuery = builder.createQuery(Long.class);
            final Query<Long> count = session.createQuery(countQuery);
            final PageListImpl<T> ts = new PageListImpl<>(query::getResultList, count::getSingleResult);
            ts.setPageSize(page.getSize());
            ts.setCurrentPage(page.getPage());
            return ts;
        }
        return query.getResultList();
    }

    @Override
    public void update(Session session, Object entity) {
        session.merge(entity);
    }
}
