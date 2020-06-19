package me.zyee.hibatis.dao.registry;

import me.zyee.hibatis.bytecode.HibatisGenerator;
import me.zyee.hibatis.bytecode.compiler.bean.BeanCompiler;
import me.zyee.hibatis.bytecode.compiler.bean.ObjectCast;
import me.zyee.hibatis.common.FunctionLazyGet;
import me.zyee.hibatis.common.LazyGet;
import me.zyee.hibatis.dao.DaoInfo;
import me.zyee.hibatis.dao.DaoMapInfo;
import me.zyee.hibatis.exception.ByteCodeGenerateException;
import me.zyee.hibatis.exception.HibatisNotFountException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author yee
 * @version 1.0
 * Create by yee on 2020/6/15
 */
public class MapRegistry {
    private final Map<String, FunctionLazyGet<ClassLoader, Class<?>>> beanContainer = new ConcurrentHashMap<>();

    public void addMap(DaoMapInfo mapInfo) {
        beanContainer.put(mapInfo.getMapId(), LazyGet.of(loader -> {
            final BeanCompiler beanCompiler = new BeanCompiler();
            return HibatisGenerator.generate(beanCompiler.compile(mapInfo), ObjectCast.class, loader);
        }));
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

    public static MapRegistry of(DaoInfo dao) {
        final MapRegistry mapRegistry = new MapRegistry();
        dao.getMaps().forEach(mapRegistry::addMap);
        return mapRegistry;
    }
}
