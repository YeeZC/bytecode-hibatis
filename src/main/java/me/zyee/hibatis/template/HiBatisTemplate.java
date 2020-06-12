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
    /**
     * 有事务的执行
     * @param callable
     * @param <T>
     * @return
     */
    <T> T runTx(Process<T> callable);

    /**
     * 无事务的执行
     * @param callable
     * @param <T>
     * @return
     */
    <T> T runNonTx(Process<T> callable);

    /**
     * 创建Dao对象
     * @param daoInf
     * @param session
     * @param <T>
     * @return
     */
    <T> T createDao(Class<T> daoInf, Session session);

    /**
     * 插入方法
     *
     * @param session
     * @param entity
     */
    void insert(Session session, Object entity);

    /**
     * 批量插入方法
     * @param session
     * @param entities
     */
    void insert(Session session, Iterable<Object> entities);

    /**
     * 删除方法
     *
     * @param session
     * @param entity
     */
    void delete(Session session, Object entity);

    /**
     * 批量删除方法
     *
     * @param session
     * @param entities
     */
    void delete(Session session, Iterable<Object> entities);

    /**
     * 根据ID获取Entity
     *
     * @param session
     * @param entity
     * @param id
     * @param <T>
     * @return
     */
    <T> Optional<T> get(Session session, Class<T> entity, Serializable id);

    /**
     * 批量ID获取
     *
     * @param session
     * @param entity
     * @param ids
     * @param <T>
     * @return
     */
    <T> List<T> findByIds(Session session, Class<T> entity, Iterable<Serializable> ids);

    /**
     * 查找所有Entity
     *
     * @param session
     * @param entity
     * @param <T>
     * @return
     */
    <T> List<T> findAll(Session session, Class<T> entity);

    /**
     * 更新方法
     * @param session
     * @param entity
     */
    void update(Session session, Object entity);

    /**
     * 自定义dao执行 有事务
     * @param daoClass
     * @param callable
     * @param <Dao>
     * @param <T>
     * @return
     */
    default <Dao, T> T runTx(Class<Dao> daoClass, BiProcess<Dao, T> callable) {
        return runTx(session -> {
            final Dao dao = createDao(daoClass, session);
            return callable.process(session, dao);
        });
    }

    /**
     * 自定义dao执行  无事务
     * @param daoClass
     * @param callable
     * @param <Dao>
     * @param <T>
     * @return
     */
    default <Dao, T> T runNonTx(Class<Dao> daoClass, BiProcess<Dao, T> callable) {
        return runNonTx(session -> {
            final Dao dao = createDao(daoClass, session);
            return callable.process(session, dao);
        });
    }

    /**
     * 有事务 插入单值
     * @param entity
     */
    default void insert(Object entity) {
        runTx(session -> {
            insert(session, entity);
            return null;
        });
    }

    /**
     * 有事务 批量插入
     *
     * @param entities
     */
    default void insert(Iterable<Object> entities) {
        runTx(session -> {
            insert(session, entities);
            return null;
        });
    }

    /**
     * 有事务 删除单值
     *
     * @param entity
     */
    default void delete(Object entity) {
        runTx(session -> {
            delete(session, entity);
            return null;
        });
    }

    /**
     * 有事务 批量删除
     * @param entities
     */
    default void delete(Iterable<Object> entities) {
        runTx(session -> {
            delete(session, entities);
            return null;
        });
    }

    /**
     * 获取单值
     *
     * @param entity
     * @param id
     * @param <T>
     * @return
     */
    default <T> Optional<T> get(Class<T> entity, Serializable id) {
        return runNonTx(session -> get(session, entity, id));
    }

    /**
     * 批量获取
     *
     * @param entity
     * @param ids
     * @param <T>
     * @return
     */
    default <T> List<T> findByIds(Class<T> entity, Iterable<Serializable> ids) {
        return runNonTx(session ->
                findByIds(session, entity, ids)
        );
    }

    /**
     * 更新
     *
     * @param entity
     */
    default void update(Object entity)  {
        runTx(session -> session.merge(entity));
    }

    /**
     * 查找所有
     * @param entity
     * @param <T>
     * @return
     */
    default <T> List<T> findAll(Class<T> entity) {
        return runNonTx(session -> findAll(session, entity));
    }

    /**
     * 单参数process
     * @param <T>
     */
    interface Process<T> {
        /**
         * 执行方法
         * @param session
         * @return
         * @throws Exception
         */
        T process(Session session) throws Exception;
    }

    /**
     * 双参数process
     * @param <Dao> Dao接口
     * @param <Ret> 返回值
     */
    interface BiProcess<Dao, Ret> {
        /**
         * 执行方法
         * @param session hibernate session
         * @param dao 自定义的dao
         * @return
         * @throws Exception
         */
        Ret process(Session session, Dao dao) throws Exception;
    }
}
