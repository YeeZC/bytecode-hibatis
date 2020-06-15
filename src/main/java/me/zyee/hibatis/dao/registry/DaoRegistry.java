package me.zyee.hibatis.dao.registry;

import me.zyee.hibatis.bytecode.DaoGenerator;
import me.zyee.hibatis.dao.DaoInfo;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author yee
 * Created by yee on 2020/6/12
 **/
public class DaoRegistry {
    private final Map<Class<?>, LazyGet<Class<?>>> container = new ConcurrentHashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(DaoRegistry.class);

    public void addDao(DaoInfo dao) {
        final Class<?> inf = dao.getId();
        container.put(inf, LazyGet.of(() -> {
            try {
                return DaoGenerator.generate(dao);
            } catch (Exception e) {
                LOGGER.error("generate error", e);
                throw new RuntimeException(e);
            }
        }));
    }

    public <T> T getDao(Class<T> daoClass, Session session) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (container.containsKey(daoClass)) {
            final Class<?> cls = container.get(daoClass).get();
            return daoClass.cast(MethodUtils.invokeStaticMethod(cls, "newInstance", session));
        }
        throw new RuntimeException("Not found");
    }

}
