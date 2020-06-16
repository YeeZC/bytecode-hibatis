package me.zyee.hibatis.transformer;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.hibernate.HibernateException;
import org.hibernate.transform.AliasedTupleSubsetResultTransformer;
import org.hibernate.transform.ResultTransformer;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author yee
 * @version 1.0
 * Created by yee on 2020/6/15
 */
public class HibatisResultTransformer extends AliasedTupleSubsetResultTransformer {
    private final Map<String, String> alias2Property;
    private final Class<?> targetClass;

    public HibatisResultTransformer(Map<String, String> alias2Property, Class<?> targetClass) {
        this.alias2Property = alias2Property;
        this.targetClass = targetClass;
    }

    @Override
    public boolean isTransformedValueATupleElement(String[] aliases, int tupleLength) {
        return false;
    }

    @Override
    public Object transformTuple(Object[] tuple, String[] aliases) {
        if (tuple.length == alias2Property.size()) {
            Map<Integer, String> aliasesMap = makeIndex2Properties(aliases);
            if (aliasesMap.isEmpty()) {
                return tuple;
            }
            try {
                Object result = targetClass.newInstance();
                if (ClassUtils.isAssignable(Map.class, targetClass) || ClassUtils.isAssignable(targetClass, Map.class)) {
                    final Method put = MethodUtils.getAccessibleMethod(Map.class, "put", Object.class, Object.class);
                    for (int i = 0; i < tuple.length; i++) {
                        String alias = aliasesMap.get(i);
                        if (alias != null) {
                            put.invoke(result, alias, tuple[i]);
                        }
                    }
                } else {
                    for (int i = 0; i < tuple.length; i++) {
                        String alias = aliasesMap.get(i);
                        if (alias != null) {
                            FieldUtils.writeField(result, alias, tuple[i], true);
                        }
                    }
                }
                return result;
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new HibernateException("Could not instantiate resultclass: " + targetClass.getName());
            }
        } else if (tuple.length == 1) {
            if (null == tuple[0]) {
                return null;
            }
            final Class<?> item = tuple[0].getClass();
            if (ClassUtils.isPrimitiveOrWrapper(item) || ClassUtils.isAssignable(String.class, item)) {
                return item;
            }
            try {
                Object result = targetClass.newInstance();
                final List<Field> fields = FieldUtils.getAllFieldsList(item);
                if (ClassUtils.isAssignable(Map.class, targetClass) || ClassUtils.isAssignable(targetClass, Map.class)) {
                    final Method put = MethodUtils.getAccessibleMethod(Map.class, "put", Object.class, Object.class);
                    for (Field field : fields) {
                        final Object o = FieldUtils.readField(field, tuple[0], true);
                        if (null != o) {
                            put.invoke(result, alias2Property.getOrDefault(field.getName(), field.getName()), o);
                        }
                    }
                } else {
                    for (Field field : fields) {
                        final Object o = FieldUtils.readField(field, tuple[0], true);
                        if (null != o) {
                            FieldUtils.writeField(result, alias2Property.getOrDefault(field.getName(), field.getName()), o, true);
                        }
                    }
                }
                return result;
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new HibernateException("Could not instantiate resultclass: " + targetClass.getName());
            }
        } else {
            return tuple;
        }
    }

    private Map<Integer, String> makeIndex2Properties(String[] aliases) {
        Map<Integer, String> aliasesMap = new HashMap<>(alias2Property.size());

        for (Map.Entry<String, String> entry : alias2Property.entrySet()) {
            // 解析hql查询的
            try {
                final int i = Integer.parseInt(StringUtils.substringBefore(entry.getKey(), "_hql_"));
                aliasesMap.put(i, entry.getValue());
            } catch (Throwable ignore) {
                break;
            }
        }
        if (aliasesMap.isEmpty()) {
            if (null != aliases) {
                for (int i = 0; i < aliases.length; i++) {
                    final String alias = aliases[i];
                    if (null != alias) {
                        aliasesMap.put(i, alias2Property.get(alias));
                    }
                }
            }
        }
        return aliasesMap;
    }

    public static ResultTransformer of(Map<String, String> alias2Property, Class<?> targetClass) {
        return new HibatisResultTransformer(alias2Property, Optional.<Class<?>>ofNullable(targetClass).orElse(HashMap.class));
    }

}
