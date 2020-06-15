package me.zyee.hibatis.transformer;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.hibernate.HibernateException;
import org.hibernate.transform.AliasedTupleSubsetResultTransformer;
import org.hibernate.transform.ResultTransformer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
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
        if (ClassUtils.isAssignable(Map.class, targetClass) || ClassUtils.isAssignable(targetClass, Map.class)) {
            try {
                Object result = targetClass.newInstance();
                final Method put = MethodUtils.getAccessibleMethod(Map.class, "put", Object.class, Object.class);
                for (int i = 0; i < tuple.length; i++) {
                    String alias = aliases[i];
                    if (alias != null) {
                        put.invoke(result, alias2Property.get(alias), tuple[i]);
                    }
                }
                return result;
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new HibernateException("Could not instantiate resultclass: " + targetClass.getName());
            }

        } else {
            try {
                Object result = targetClass.newInstance();
                for (int i = 0; i < aliases.length; i++) {
                    if (null != aliases[i]) {
                        FieldUtils.writeField(result, alias2Property.get(aliases[i]), tuple[i], true);
                    }
                }
                return result;
            } catch (InstantiationException | IllegalAccessException e) {
                throw new HibernateException("Could not instantiate resultclass: " + targetClass.getName());
            }
        }
    }

    public static ResultTransformer of(Map<String, String> alias2Property, Class<?> targetClass) {
        return new HibatisResultTransformer(alias2Property, Optional.<Class<?>>ofNullable(targetClass).orElse(HashMap.class));
    }

}
