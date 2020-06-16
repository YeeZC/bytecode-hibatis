package me.zyee.hibatis.bytecode.impl;

import io.airlift.bytecode.ClassDefinition;
import io.airlift.bytecode.Variable;
import io.airlift.bytecode.expression.BytecodeExpressions;
import me.zyee.hibatis.bytecode.BeanGenerator;
import me.zyee.hibatis.dao.DaoMethodInfo;
import me.zyee.hibatis.dao.registry.MapRegistry;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.lang.reflect.Method;
import java.util.List;

/**
 * @author yee
 * @version 1.0
 * Create by yee on 2020/6/15
 */
public class HQLMethodVisitor extends BaseMethodVisitor {

    public HQLMethodVisitor(ClassDefinition classDefinition, Method method, DaoMethodInfo methodInfo, MapRegistry maps) {
        super(classDefinition, method, methodInfo, maps, Query.class);
    }

    @Override
    protected Variable generateList(DaoMethodInfo methodInfo, Method transformer, Class<?> componentClass, Method setTrans) {
        final String resultMap = methodInfo.getResultMap();
        final Class<?> resultType = methodInfo.getResultType();
        if (StringUtils.isNotEmpty(resultMap)) {
            body.append(registry.getMapBlock(resultMap, scope, true));
            final Variable tmp = BeanGenerator.createVariable(scope, List.class, "result");
            body.append(tmp.set(query.invoke("getResultList", List.class)));
            return tmp;
        } else if (null != resultType) {
            final Variable result = BeanGenerator.createVariable(scope, List.class, "result");
            body.append(query.invoke(setTrans,
                    BytecodeExpressions.invokeStatic(transformer,
                            BytecodeExpressions.constantClass(resultType))));
            body.append(result.set(query.invoke("getResultList", List.class)));
            return result;
        } else if (componentClass != null) {
            final Variable result = BeanGenerator.createVariable(scope, List.class, "result");
            body.append(query.invoke(setTrans,
                    BytecodeExpressions.invokeStatic(transformer,
                            BytecodeExpressions.constantClass(componentClass))));
            body.append(result.set(query.invoke("getResultList", List.class)));
            return result;
        } else {
            final Variable result = BeanGenerator.createVariable(scope, List.class, "result");
            body.append(result.set(query.invoke("getResultList", List.class)));
            return result;
        }
    }

    @Override
    protected void generateSingle(DaoMethodInfo methodInfo, Class<?> methodReturnType, Method transformer, Method setTrans) {
        final String resultMap = methodInfo.getResultMap();
        final Class<?> resultType = methodInfo.getResultType();
        if (StringUtils.isNotEmpty(resultMap)) {
            body.append(registry.getMapBlock(resultMap, scope, true));
            body.append(query.invoke("getSingleResult", Object.class)
                    .cast(registry.getMapClass(resultMap)));
            body.retObject();
        } else if (null != resultType) {
            body.append(query.invoke(setTrans,
                    BytecodeExpressions.invokeStatic(transformer,
                            BytecodeExpressions.constantClass(resultType))));
            body.append(query.invoke("getSingleResult", Object.class));
            body.retObject();
        } else {
            body.append(visitSingleReturnType(query.invoke("getSingleResult", Object.class), methodReturnType));
            body.ret(methodReturnType);
        }
    }

    @Override
    protected Method createQueryMethod() {
        return MethodUtils.getAccessibleMethod(Session.class, "createQuery", String.class);
    }
}
