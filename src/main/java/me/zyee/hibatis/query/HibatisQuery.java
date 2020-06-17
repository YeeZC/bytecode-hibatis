package me.zyee.hibatis.query;

import org.hibernate.Session;

import java.util.List;
import java.util.Map;

/**
 * @author yee
 * @version 1.0
 * Create by yee on 2020/6/17
 */
public interface HibatisQuery<T> {
    List<T> select(Session session, String hql, String mapId, Map<String, Object> params, boolean nativeSql);

    T selectOne(Session session, String hql, String mapId, Map<String, Object> params, boolean nativeSql);

    int getCount(Session session, String hql, Map<String, Object> params, boolean nativeSql);

    int executeUpdate(Session session, String hql, Map<String, Object> params, boolean nativeSql);
}
