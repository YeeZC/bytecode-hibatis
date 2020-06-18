package me.zyee.hibatis.transformer;

import me.zyee.hibatis.bytecode.annotation.Order;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.hibernate.HibernateException;
import org.hibernate.property.access.internal.PropertyAccessStrategyBasicImpl;
import org.hibernate.property.access.internal.PropertyAccessStrategyChainedImpl;
import org.hibernate.property.access.internal.PropertyAccessStrategyFieldImpl;
import org.hibernate.property.access.internal.PropertyAccessStrategyMapImpl;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.transform.AliasedTupleSubsetResultTransformer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * @author yee
 * @version 1.0
 * Create by yee on 2020/6/16
 */
public class HibatisReturnClassTransformer extends AliasedTupleSubsetResultTransformer {
    private final Class<?> resultClass;
    private boolean isInitialized;
    private String[] aliases;
    private Setter[] setters;

    public HibatisReturnClassTransformer(Class<?> resultClass) {
        if (resultClass == null) {
            throw new IllegalArgumentException("resultClass cannot be null");
        }
        isInitialized = false;
        this.resultClass = resultClass;
    }

    @Override
    public boolean isTransformedValueATupleElement(String[] aliases, int tupleLength) {
        return false;
    }

    @Override
    public Object transformTuple(Object[] tuple, String[] aliases) {
        Object result = tuple;
        if (tuple.length == 1) {
            if (null == tuple[0]) {
                return null;
            }
            if (ClassUtils.isAssignable(tuple[0].getClass(), resultClass)
                    || ClassUtils.isAssignable(resultClass, tuple[0].getClass())) {
                return tuple[0];
            }
        }
        if (!isInitialized) {
            initialize(aliases);
        } else {
            check(aliases);
        }
        if (ObjectUtils.allNotNull(setters)) {
            try {
                result = resultClass.newInstance();
            } catch (InstantiationException e) {
                throw new HibernateException("Could not instantiate resultclass: " + resultClass.getName());
            } catch (IllegalAccessException e) {
                try {
                    final Constructor<?> constructor = resultClass.getDeclaredConstructor();
                    constructor.setAccessible(true);
                    result = constructor.newInstance();
                    constructor.setAccessible(false);


                } catch (NoSuchMethodException |
                        IllegalAccessException |
                        InstantiationException |
                        InvocationTargetException e1) {
                    throw new HibernateException("Could not instantiate resultclass: " + resultClass.getName());
                }
            }
            for (int i = 0; i < aliases.length; i++) {
                if (setters[i] != null) {
                    setters[i].set(result, tuple[i], null);
                }
            }
        } else {
            result = generateForMap(tuple, result);
        }

        return result;
    }

    private Object generateForMap(Object[] tuple, Object result) {
        final List<Field> fields = FieldUtils.getFieldsListWithAnnotation(resultClass, Order.class);
        if (fields.size() == tuple.length) {
            try {
                result = resultClass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                final Constructor<?> constructor;
                try {
                    constructor = resultClass.getDeclaredConstructor();
                    constructor.setAccessible(true);
                    result = constructor.newInstance();
                    constructor.setAccessible(false);
                } catch (NoSuchMethodException | IllegalAccessException
                        | InstantiationException | InvocationTargetException exp) {
                    throw new HibernateException("Could not instantiate resultclass: "
                            + resultClass.getName());
                }
            }
            fields.sort(Comparator.comparingInt(o -> o.getAnnotation(Order.class).value()));
            for (int i = 0; i < tuple.length; i++) {
                if (null != tuple[i] && fields.get(i).getType().isInstance(tuple[i])) {
                    try {
                        FieldUtils.writeField(fields.get(i), result, tuple[i], true);
                    } catch (IllegalAccessException e) {
                        throw new HibernateException("Could not set field field name: " + fields.get(i).getName());
                    }
                }
            }
        }
        return result;
    }

    private void initialize(String[] aliases) {
        PropertyAccessStrategyChainedImpl propertyAccessStrategy = new PropertyAccessStrategyChainedImpl(
                PropertyAccessStrategyBasicImpl.INSTANCE,
                PropertyAccessStrategyFieldImpl.INSTANCE,
                PropertyAccessStrategyMapImpl.INSTANCE
        );
        this.aliases = new String[aliases.length];
        setters = new Setter[aliases.length];
        for (int i = 0; i < aliases.length; i++) {
            String alias = aliases[i];
            if (alias != null) {
                this.aliases[i] = alias;
                setters[i] = propertyAccessStrategy.buildPropertyAccess(resultClass, alias).getSetter();
            }
        }
        isInitialized = true;
    }

    private void check(String[] aliases) {
        if (!Arrays.equals(aliases, this.aliases)) {
            throw new IllegalStateException(
                    "aliases are different from what is cached; aliases=" + Arrays.asList(aliases) +
                            " cached=" + Arrays.asList(this.aliases));
        }
    }

}
