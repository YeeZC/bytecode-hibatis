package me.zyee.hibatis.transformer;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.ObjectUtils;
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
        if (ObjectUtils.allNotNull(aliases)) {
            try {
                Object result = targetClass.newInstance();
                if (ClassUtils.isAssignable(Map.class, targetClass) || ClassUtils.isAssignable(targetClass, Map.class)) {
                    final Method put = MethodUtils.getAccessibleMethod(Map.class, "put", Object.class, Object.class);
                    for (int i = 0; i < tuple.length; i++) {
                        String alias = aliases[i];
                        if (alias != null) {
                            put.invoke(result, alias2Property.getOrDefault(alias, alias), tuple[i]);
                        }
                    }
                } else {
                    for (int i = 0; i < aliases.length; i++) {
                        if (null != aliases[i]) {
                            FieldUtils.writeField(result, alias2Property.getOrDefault(aliases[i], aliases[i]), tuple[i], true);
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

    public static ResultTransformer of(Map<String, String> alias2Property, Class<?> targetClass) {
        return new HibatisResultTransformer(alias2Property, Optional.<Class<?>>ofNullable(targetClass).orElse(HashMap.class));
    }

}
