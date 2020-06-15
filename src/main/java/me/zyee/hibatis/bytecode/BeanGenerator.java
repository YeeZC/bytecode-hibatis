package me.zyee.hibatis.bytecode;

import io.airlift.bytecode.Access;
import io.airlift.bytecode.BytecodeBlock;
import io.airlift.bytecode.ClassDefinition;
import io.airlift.bytecode.ClassGenerator;
import io.airlift.bytecode.DynamicClassLoader;
import io.airlift.bytecode.MethodDefinition;
import io.airlift.bytecode.ParameterizedType;
import io.airlift.bytecode.Scope;
import io.airlift.bytecode.Variable;
import io.airlift.bytecode.expression.BytecodeExpressions;
import me.zyee.hibatis.dao.DaoMapInfo;
import me.zyee.hibatis.dao.DaoProperty;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author yee
 * @version 1.0
 * Create by yee on 2020/6/15
 */
public class BeanGenerator {
    public static Class<?> generate(DaoMapInfo mapInfo, Path out) throws NoSuchMethodException {
        // 动态字节码生成的classLoader
        final DynamicClassLoader dynamicClassLoader = new DynamicClassLoader(BeanGenerator.class.getClassLoader()
                , Collections.emptyMap());
        final String packageName = BeanGenerator.class.getPackage().getName();
        final ClassDefinition classDefinition = new ClassDefinition(Access.a(Access.PUBLIC, Access.FINAL),
                DaoGenerator.makeClassName(packageName + ".bean", mapInfo.getMapId()),
                ParameterizedType.type(Object.class),
                ParameterizedType.type(Serializable.class));
        for (DaoProperty property : mapInfo.getProperties()) {
            classDefinition.declareField(Access.a(Access.PUBLIC), property.getColumn(), property.getJavaType());
        }
        final Class<?> className = mapInfo.getClassName();
        MethodDefinition transfer = classDefinition.declareMethod(Access.a(Access.PUBLIC), "transfer",
                ParameterizedType.type(Object.class));
        if (null != className) {
            makeTransferMethod(transfer, mapInfo.getClassName(), mapInfo.getProperties());
        } else {
            makeTransferMethod(transfer, HashMap.class, mapInfo.getProperties());
        }

        return ClassGenerator.classGenerator(dynamicClassLoader)
                .dumpClassFilesTo(Optional.ofNullable(out)).defineClass(classDefinition, Serializable.class);
    }

    private static void makeTransferMethod(MethodDefinition methodDefinition,
                                           Class<?> returnType, List<DaoProperty> properties) throws NoSuchMethodException {
        final BytecodeBlock body = methodDefinition.getBody();
        final Scope scope = methodDefinition.getScope();
        final Variable result = createVariable(scope, returnType, "result");
        body.append(result.set(BytecodeExpressions.newInstance(returnType)));
        final Method put = Map.class.getMethod("put", Object.class, Object.class);
        final Method writeField = FieldUtils.class.getMethod("writeField", Object.class, String.class, Object.class, boolean.class);
        if (ClassUtils.isAssignable(Map.class, returnType) || ClassUtils.isAssignable(returnType, Map.class)) {
            for (DaoProperty property : properties) {
                body.append(result.invoke(put, BytecodeExpressions.constantString(property.getProperty()),
                        scope.getThis().getField(property.getColumn(), property.getJavaType())));
            }
        } else {
            for (DaoProperty property : properties) {
                body.append(BytecodeExpressions.invokeStatic(writeField, result,
                        BytecodeExpressions.constantString(property.getProperty()),
                        scope.getThis().getField(property.getColumn(), property.getJavaType()),
                        BytecodeExpressions.constantTrue()));
            }
        }
        body.append(result);
        body.ret(returnType);
    }

    public static Variable createVariable(Scope scope, Class<?> clazz, String name) {
        if (StringUtils.isEmpty(name)) {
            return scope.createTempVariable(clazz);
        }
        return scope.declareVariable(clazz, name);
    }
}
