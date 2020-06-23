package me.zyee.hibatis.query;

import java.util.Map;

/**
 * @author yee
 * @version 1.0
 * Create by yee on 2020/6/19
 */
public interface CountMapper {
    long getCount(String sql, Map param);
}
