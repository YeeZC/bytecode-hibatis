package me.zyee.hibatis.query;

import java.util.Map;

/**
 * @author yee
 * @version 1.0
 * Create by yee on 2020/6/18
 */
public interface UpdateMapper {
    int executeUpdate(String sql, Map param);
}
