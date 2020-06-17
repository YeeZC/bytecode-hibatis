package me.zyee.hibatis.bytecode.compiler.dao;

import io.airlift.bytecode.BytecodeBlock;
import io.airlift.bytecode.Scope;
import io.airlift.bytecode.Variable;
import io.airlift.bytecode.expression.BytecodeExpressions;
import me.zyee.hibatis.bytecode.DaoGenerator;
import me.zyee.hibatis.bytecode.compiler.impl.ByteCodeNodeCompiler;
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

/**
 * @author yee
 * @version 1.0
 * Create by yee on 2020/6/17
 */
public class MapCompiler implements ByteCodeNodeCompiler<MapCompiler.Context> {
    private final Method put;
    private final Method setTrans;

    public MapCompiler() {
        this.put = MethodUtils.getAccessibleMethod(Map.class,
                "put", Object.class, Object.class);
        this.setTrans = MethodUtils.getAccessibleMethod(Query.class,
                "setResultTransformer", ResultTransformer.class);
    }

    @Override
    public BytecodeBlock compile(MapCompiler.Context context) {
        final Scope scope = context.getScope();
        final DaoMapInfo mapInfo = context.getMapInfo();
        final BytecodeBlock body = new BytecodeBlock();
        final Variable query = scope.getVariable("query");

        final Variable alias2Properties = DaoGenerator.createVariable(scope, HashMap.class, "alias2Properties");
        final List<DaoProperty> properties = mapInfo.getProperties();
        body.append(alias2Properties.set(BytecodeExpressions.newInstance(HashMap.class,
                BytecodeExpressions.constantInt(properties.size()))));
        if (context.isHql()) {
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
    }

    public static class Context {
        private final Scope scope;
        private final boolean hql;
        private final DaoMapInfo mapInfo;

        public Context(Scope scope, boolean hql, DaoMapInfo mapInfo) {
            this.scope = scope;
            this.hql = hql;
            this.mapInfo = mapInfo;
        }

        public Scope getScope() {
            return scope;
        }

        public boolean isHql() {
            return hql;
        }

        public DaoMapInfo getMapInfo() {
            return mapInfo;
        }
    }
}
