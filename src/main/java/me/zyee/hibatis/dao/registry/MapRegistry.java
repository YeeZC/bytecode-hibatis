package me.zyee.hibatis.dao.registry;

import io.airlift.bytecode.BytecodeBlock;
import io.airlift.bytecode.Scope;
import io.airlift.bytecode.Variable;
import io.airlift.bytecode.expression.BytecodeExpressions;
import me.zyee.hibatis.bytecode.BeanGenerator;
import me.zyee.hibatis.dao.DaoInfo;
import me.zyee.hibatis.dao.DaoMapInfo;
import me.zyee.hibatis.dao.DaoProperty;
import me.zyee.hibatis.transformer.HibatisResultTransformer;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.hibernate.query.Query;
import org.hibernate.transform.ResultTransformer;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
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
    private final ConcurrentMap<String, Supplier<Class<?>>> classSuppliers = new ConcurrentHashMap<>();

    public void addMap(DaoMapInfo mapInfo) {
        container.put(mapInfo.getMapId(), LazyGet.of((scope, isHql) -> {
            final BytecodeBlock body = new BytecodeBlock();
            final Variable query = scope.getVariable("query");
            final Method put = MethodUtils.getAccessibleMethod(Map.class, "put", Object.class, Object.class);
            final Method setTrans = MethodUtils.getAccessibleMethod(Query.class, "setResultTransformer", ResultTransformer.class);
            final Variable alias2Properties = BeanGenerator.createVariable(scope, HashMap.class, "alias2Properties");
            final List<DaoProperty> properties = mapInfo.getProperties();
            body.append(alias2Properties.set(BytecodeExpressions.newInstance(HashMap.class,
                    BytecodeExpressions.constantInt(properties.size()))));
            if (isHql) {
                for (int i = 0; i < properties.size(); i++) {
                    final DaoProperty property = properties.get(i);
                    body.append(alias2Properties.invoke(put, BytecodeExpressions.constantString(i + "_hql_" + property.getColumn()),
                            BytecodeExpressions.constantString(property.getProperty())));
                }
            } else {
                properties.forEach(property -> {
                    body.append(alias2Properties.invoke(put, BytecodeExpressions.constantString(property.getColumn()),
                            BytecodeExpressions.constantString(property.getProperty())));
                });
            }
            final Method of = MethodUtils.getAccessibleMethod(HibatisResultTransformer.class, "of", Map.class, Class.class);
            body.append(query.invoke(setTrans,
                    BytecodeExpressions.invokeStatic(of, alias2Properties,
                            BytecodeExpressions.constantClass(Optional.<Class<?>>ofNullable(mapInfo.getClassName())
                                    .orElse(HashMap.class)))));
            return body;
        }));
        classSuppliers.put(mapInfo.getMapId(), () -> Optional.<Class<?>>ofNullable(mapInfo.getClassName()).orElse(HashMap.class));
    }

    public BytecodeBlock getMapBlock(String mapId, Scope scope, boolean isHql) {
        if (container.containsKey(mapId)) {
            return container.get(mapId).get(scope, isHql);
        }
        throw new RuntimeException("Not found");
    }

    public Class<?> getMapClass(String mapId) {
        return classSuppliers.get(mapId).get();
    }

    public static MapRegistry of(DaoInfo dao) {
        final MapRegistry mapRegistry = new MapRegistry();
        dao.getMaps().forEach(mapRegistry::addMap);
        return mapRegistry;
    }
}
