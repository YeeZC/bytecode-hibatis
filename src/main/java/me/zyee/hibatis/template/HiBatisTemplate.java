package me.zyee.hibatis.template;

import org.hibernate.Session;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

/**
 * @author yee
 * Created by yee on 2020/6/12
 **/
public interface HiBatisTemplate {
    <T> T runTx(Process<T> callable);

    <T> T runNonTx(Process<T> callable);

    <T> T createDao(Class<T> daoInf, Session session);

    void insert(Session session, Object entity) ;

    void insert(Session session, Iterable<Object> entities);

    void delete(Session session, Object entity) ;

    void delete(Session session, Iterable<Object> entities) ;

    <T>Optional<T> get(Session session, Class<T> entity, Serializable id) ;

    <T>List<T> findByIds(Session session, Class<T> entity, Iterable<Serializable> ids) ;
    <T>List<T> findAll(Session session, Class<T> entity);

    void update(Session session, Object entity);

    default <Dao, T> T runTx(Class<Dao> daoClass, BiProcess<Dao, T> callable) {
        return runTx(session -> {
            final Dao dao = createDao(daoClass, session);
            return callable.process(session, dao);
        });
    }

    default <Dao, T> T runNonTx(Class<Dao> daoClass, BiProcess<Dao, T> callable) {
        return runNonTx(session -> {
            final Dao dao = createDao(daoClass, session);
            return callable.process(session, dao);
        });
    }

    default void insert(Object entity) {
        runTx(session -> {
            insert(session, entity);
            return null;
        });
    }

    default void insert(Iterable<Object> entities){
        runTx(session -> {
            insert(session, entities);
            return null;
        });
    }

    default void delete(Object entity)  {
        runTx(session -> {
            delete(session, entity);
            return null;
        });
    }

    default void delete(Iterable<Object> entities) {
        runTx(session -> {
            delete(session, entities);
            return null;
        });
    }

    default <T>Optional<T> get(Class<T> entity, Serializable id) {
        return runNonTx(session -> get(session, entity, id));
    }

    default <T>List<T> findByIds(Class<T> entity, Iterable<Serializable> ids){
        return runNonTx(session ->
                findByIds(session, entity, ids)
        );
    }

    default void update(Object entity)  {
        runTx(session -> session.merge(entity));
    }

    default <T> List<T> findAll(Class<T> entity) {
        return runNonTx(session -> findAll(session, entity));
    }

    interface Process<T> {
        T process(Session session) throws Exception;
    }

    interface BiProcess<Dao, Ret> {
        Ret process(Session session, Dao dao) throws Exception;
    }
}
