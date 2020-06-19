package me.zyee.hibatis.dao.registry;

import io.airlift.bytecode.ClassDefinition;
import me.zyee.hibatis.bytecode.HibatisGenerator;
import me.zyee.hibatis.bytecode.compiler.dao.DaoCompiler;
import me.zyee.hibatis.common.FunctionLazyGet;
import me.zyee.hibatis.common.LazyGet;
import me.zyee.hibatis.common.SupplierLazyGet;
import me.zyee.hibatis.dao.DaoInfo;
import me.zyee.hibatis.exception.ByteCodeGenerateException;
import me.zyee.hibatis.exception.HibatisException;
import me.zyee.hibatis.exception.HibatisNotFountException;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.hibernate.Session;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author yee
 * Created by yee on 2020/6/12
 **/
public class DaoRegistry {
    private final Map<Class<?>, SupplierLazyGet<MapRegistry>> mapContainer = new ConcurrentHashMap<>();
    private final Map<Class<?>,
            FunctionLazyGet<SupplierLazyGet<Session>, Object>> newContainer = new ConcurrentHashMap<>();

    public void addDao(DaoInfo dao) {
        final Class<?> inf = dao.getId();
        mapContainer.put(inf, LazyGet.of(() -> MapRegistry.of(dao)));
        newContainer.put(inf, LazyGet.of((lazyGet) -> {
            try {
                final ClassDefinition compile = new DaoCompiler().compile(dao);
                final Class<?> cls = HibatisGenerator.generate(compile, dao.getId(), null);
                return MethodUtils.invokeStaticMethod(cls,
                        "newInstance", lazyGet, mapContainer.get(inf).get());
            } catch (HibatisException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }));
    }

    public <T> T getNewDao(Class<T> daoClass, SupplierLazyGet<Session> session) throws ByteCodeGenerateException {
        if (newContainer.containsKey(daoClass)) {
            try {
                return daoClass.cast(newContainer.get(daoClass).get(session));
            } catch (Exception e) {
                throw new ByteCodeGenerateException(e);
            }
        }
        throw new ByteCodeGenerateException(new HibatisNotFountException("Instanse for Dao " + daoClass + " not found"));
    }
}
