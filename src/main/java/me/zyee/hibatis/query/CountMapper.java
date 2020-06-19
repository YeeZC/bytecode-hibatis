package me.zyee.hibatis.query;

import org.hibernate.Session;

import java.util.Map;

/**
 * @author yee
 * @version 1.0
 * Create by yee on 2020/6/19
 */
public interface CountMapper {
    long getCount(Session session, String sql, Map param);
}
