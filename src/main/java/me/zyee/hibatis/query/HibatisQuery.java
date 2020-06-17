package me.zyee.hibatis.query;

import me.zyee.hibatis.exception.ByteCodeGenerateException;
import org.hibernate.Session;

import java.util.List;
import java.util.Map;

/**
 * @author yee
 * @version 1.0
 * Create by yee on 2020/6/17
 */
public interface HibatisQuery {
    List<Object> select(Session session, QueryParam param) throws ByteCodeGenerateException;

    Object selectOne(Session session, QueryParam param) throws ByteCodeGenerateException;

    int getCount(Session session, QueryParam param);

    int executeUpdate(Session session, QueryParam param);

    class QueryParam {
        private final String hql;
        private final boolean nativeSql;
        private String mapId;
        private Map<String, Object> params;
        private Class<?> returnType;

        private QueryParam(String hql, boolean nativeSql) {
            this.hql = hql;
            this.nativeSql = nativeSql;
        }

        public String getHql() {
            return hql;
        }

        public String getMapId() {
            return mapId;
        }

        public QueryParam setMapId(String mapId) {
            this.mapId = mapId;
            return this;
        }

        public Map<String, Object> getParams() {
            return params;
        }

        public QueryParam setParams(Map<String, Object> params) {
            this.params = params;
            return this;
        }

        public boolean isNativeSql() {
            return nativeSql;
        }

        public Class<?> getReturnType() {
            return returnType;
        }

        public QueryParam setReturnType(Class<?> returnType) {
            this.returnType = returnType;
            return this;
        }

        public static QueryParam create(String hql, boolean nativeSql) {
            return new QueryParam(hql, nativeSql);
        }


    }
}
