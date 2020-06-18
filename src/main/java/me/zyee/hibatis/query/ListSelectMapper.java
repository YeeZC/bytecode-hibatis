package me.zyee.hibatis.query;

import org.hibernate.Session;

import java.util.List;
import java.util.Map;

/**
 * @author yee
 * @version 1.0
 * Create by yee on 2020/6/18
 */
public interface ListSelectMapper<T> {
    List<T> select(Session session, String sql, Map param);
}
