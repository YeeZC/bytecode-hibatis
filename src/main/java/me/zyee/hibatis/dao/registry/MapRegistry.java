package me.zyee.hibatis.dao.registry;

import me.zyee.hibatis.bytecode.BeanGenerator;
import me.zyee.hibatis.dao.DaoInfo;
import me.zyee.hibatis.dao.DaoMapInfo;

import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author yee
 * @version 1.0
 * Create by yee on 2020/6/15
 */
public class MapRegistry {
    private final ConcurrentMap<String, LazyGet<Class<?>>> container = new ConcurrentHashMap<>();
    private final Path out;

    public MapRegistry(Path path) {
        this.out = path;
    }

    public void addMap(DaoMapInfo mapInfo) {
        container.put(mapInfo.getMapId(), LazyGet.of(() -> {
            try {
                return BeanGenerator.generate(mapInfo, out);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("生成错误");
            }
        }));
    }

    public Class<?> getMapClass(String mapId) {
        if (container.containsKey(mapId)) {
            return container.get(mapId).get();
        }
        throw new RuntimeException("Not found");
    }

    public static MapRegistry of(Path path, DaoInfo dao) {
        final MapRegistry mapRegistry = new MapRegistry(path);
        dao.getMaps().forEach(mapRegistry::addMap);
        return mapRegistry;
    }
}
