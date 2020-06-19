package me.zyee.hibatis.dao.registry;

import io.airlift.bytecode.ClassDefinition;
import me.zyee.hibatis.bytecode.HibatisGenerator;
import me.zyee.hibatis.bytecode.compiler.dao.DaoCompiler;
import me.zyee.hibatis.dao.DaoInfo;
import me.zyee.hibatis.exception.ByteCodeGenerateException;
import me.zyee.hibatis.exception.HibatisException;
import me.zyee.hibatis.exception.HibatisNotFountException;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author yee
 * Created by yee on 2020/6/12
 **/
public class DaoRegistry {
    private final Map<Class<?>, LazyGet.SupplierLazyGet<Class<?>>> container = new ConcurrentHashMap<>();
    private final Map<Class<?>, LazyGet.SupplierLazyGet<MapRegistry>> mapContainer = new ConcurrentHashMap<>();
    private final Map<Class<?>, LazyGet.SupplierLazyGet<Class<?>>> newContainer = new ConcurrentHashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(DaoRegistry.class);

    public void addDao(DaoInfo dao) {
        final Class<?> inf = dao.getId();
        container.put(inf, LazyGet.of(() -> {
            try {
                return HibatisGenerator.generate(dao);
            } catch (Exception e) {
                LOGGER.error("generate error", e);
                throw new RuntimeException(e);
            }
        }));
        mapContainer.put(inf, LazyGet.of(() -> MapRegistry.of(dao)));
        newContainer.put(inf, LazyGet.of(() -> {
            try {
                final ClassDefinition compile = new DaoCompiler().compile(dao);
                return HibatisGenerator.generate(compile, dao.getId(), null, Paths.get("/Users/yee/work/tmp1"));
            } catch (HibatisException e) {
                throw new RuntimeException(e);
            }
        }));
    }

    @Deprecated
    public <T> T getDao(Class<T> daoClass, Session session) throws ByteCodeGenerateException {
        if (container.containsKey(daoClass)) {
            try {
                final Class<?> cls = container.get(daoClass).get();
                return daoClass.cast(MethodUtils.invokeStaticMethod(cls,
                        "newInstance", session));
            } catch (Exception e) {
                throw new ByteCodeGenerateException(e);
            }
        }
        throw new ByteCodeGenerateException(new HibatisNotFountException("Instanse for Dao " + daoClass + " not found"));
    }

    public <T> T getNewDao(Class<T> daoClass, Session session) throws ByteCodeGenerateException {
        if (newContainer.containsKey(daoClass)) {
            try {
                final Class<?> cls = newContainer.get(daoClass).get();
                return daoClass.cast(MethodUtils.invokeStaticMethod(cls,
                        "newInstance", session, mapContainer.get(daoClass).get()));
            } catch (Exception e) {
                throw new ByteCodeGenerateException(e);
            }
        }
        throw new ByteCodeGenerateException(new HibatisNotFountException("Instanse for Dao " + daoClass + " not found"));
    }
}
