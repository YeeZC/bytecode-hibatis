package me.zyee.hibatis.dao.registry;

import io.airlift.bytecode.BytecodeBlock;
import io.airlift.bytecode.Scope;
import me.zyee.hibatis.bytecode.HibatisGenerator;
import me.zyee.hibatis.bytecode.compiler.bean.BeanCompiler;
import me.zyee.hibatis.bytecode.compiler.bean.ObjectCast;
import me.zyee.hibatis.bytecode.compiler.dao.MapCompiler;
import me.zyee.hibatis.dao.DaoInfo;
import me.zyee.hibatis.dao.DaoMapInfo;
import me.zyee.hibatis.exception.ByteCodeGenerateException;
import me.zyee.hibatis.exception.HibatisNotFountException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * @author yee
 * @version 1.0
 * Create by yee on 2020/6/15
 */
public class MapRegistry {
    private final ConcurrentMap<String, LazyGet.BiFunctionLazyGet<Scope, Boolean, BytecodeBlock>> container = new ConcurrentHashMap<>();
    private final Map<String, LazyGet.FunctionLazyGet<ClassLoader, Class<?>>> beanContainer = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Supplier<Class<?>>> classSuppliers = new ConcurrentHashMap<>();
    private final MapCompiler compiler = new MapCompiler();

    public void addMap(DaoMapInfo mapInfo) {
        container.put(mapInfo.getMapId(), LazyGet.of((scope, isHql) ->
                compiler.compile(new MapCompiler.Context(scope, isHql, mapInfo))));
        classSuppliers.put(mapInfo.getMapId(),
                () -> Optional.<Class<?>>ofNullable(mapInfo.getClassName()).orElse(HashMap.class));
        beanContainer.put(mapInfo.getMapId(), LazyGet.of(loader -> {
            final BeanCompiler beanCompiler = new BeanCompiler();
            return HibatisGenerator.generate(beanCompiler.compile(mapInfo), ObjectCast.class, loader);
        }));
    }

    @Deprecated
    public BytecodeBlock getMapBlock(String mapId, Scope scope, boolean isHql) throws ByteCodeGenerateException {
        if (container.containsKey(mapId)) {
            try {
                return container.get(mapId).get(scope, isHql);
            } catch (Exception e) {
                throw new ByteCodeGenerateException(e);
            }
        }
        throw new ByteCodeGenerateException(new HibatisNotFountException("Map " + mapId + " not found"));
    }

    public Class<?> getMapClass(String mapId, ClassLoader loader) throws ByteCodeGenerateException {
        if (beanContainer.containsKey(mapId)) {
            try {
                return beanContainer.get(mapId).get(loader);
            } catch (Exception e) {
                throw new ByteCodeGenerateException(e);
            }
        }
        throw new ByteCodeGenerateException(new HibatisNotFountException("Map " + mapId + " not found"));
    }

    @Deprecated
    public Class<?> getMapClass(String mapId) {
        return classSuppliers.get(mapId).get();
    }

    public static MapRegistry of(DaoInfo dao) {
        final MapRegistry mapRegistry = new MapRegistry();
        dao.getMaps().forEach(mapRegistry::addMap);
        return mapRegistry;
    }
}
