package me.zyee.hibatis.bytecode.compiler.bean;

import io.airlift.bytecode.Access;
import io.airlift.bytecode.AnnotationDefinition;
import io.airlift.bytecode.BytecodeBlock;
import io.airlift.bytecode.ClassDefinition;
import io.airlift.bytecode.FieldDefinition;
import io.airlift.bytecode.MethodDefinition;
import io.airlift.bytecode.Parameter;
import io.airlift.bytecode.ParameterizedType;
import io.airlift.bytecode.Scope;
import io.airlift.bytecode.Variable;
import io.airlift.bytecode.expression.BytecodeExpressions;
import me.zyee.hibatis.bytecode.HibatisGenerator;
import me.zyee.hibatis.bytecode.annotation.Order;
import me.zyee.hibatis.bytecode.compiler.Compiler;
import me.zyee.hibatis.dao.DaoMapInfo;
import me.zyee.hibatis.dao.DaoProperty;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

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
public class BeanCompiler implements Compiler<DaoMapInfo, ClassDefinition> {
    private final Method put;
    private final Method writeField;

    public BeanCompiler() {
        this.put = MethodUtils.getAccessibleMethod(Map.class,
                "put", Object.class, Object.class);
        this.writeField = MethodUtils.getAccessibleMethod(FieldUtils.class,
                "writeField", Object.class,
                String.class, Object.class, boolean.class);
    }

    @Override
    public ClassDefinition compile(DaoMapInfo mapInfo) {
        final String mapId = mapInfo.getMapId();
        final String className = firstCharUpper(mapId);
        ClassDefinition classDefinition = new ClassDefinition(Access.a(Access.PRIVATE, Access.FINAL),
                HibatisGenerator.makeClassName("bean", className),
                ParameterizedType.type(Object.class),
                ParameterizedType.type(ObjectCast.class));
        classDefinition.declareDefaultConstructor(Access.a(Access.PUBLIC));
        final List<DaoProperty> properties = mapInfo.getProperties();

        final MethodDefinition cast = classDefinition.declareMethod(Access.a(Access.PUBLIC), "cast",
                ParameterizedType.type(Object.class));

        final BytecodeBlock body = cast.getBody();
        final Scope scope = cast.getScope();

        final Class<?> resultMap = Optional.<Class<?>>ofNullable(mapInfo.getClassName())
                .orElse(HashMap.class);
        final Variable result = scope.declareVariable(resultMap, "result");
        body.append(result.set(BytecodeExpressions.newInstance(resultMap)));

        for (int i = 0; i < properties.size(); i++) {
            final DaoProperty property = properties.get(i);
            final FieldDefinition fieldDefinition = classDefinition.declareField(Access.a(Access.PRIVATE),
                    property.getColumn(), property.getJavaType());
            final AnnotationDefinition annotation = fieldDefinition.declareAnnotation(Order.class);
            annotation.setValue("value", i);
            generateGetSet(classDefinition, fieldDefinition);
        }

        properties.forEach(property -> {
            if (ClassUtils.isAssignable(Map.class, resultMap) || ClassUtils.isAssignable(resultMap, Map.class)) {
                body.append(scope.getVariable("result")
                        .invoke(put, BytecodeExpressions.constantString(property.getProperty()),
                                scope.getThis().getField(property.getColumn(),
                                        property.getJavaType())));
            } else {
                body.append(BytecodeExpressions.invokeStatic(writeField,
                        scope.getVariable("result"),
                        BytecodeExpressions.constantString(property.getProperty()),
                        scope.getThis().getField(property.getColumn(),
                                property.getJavaType()),
                        BytecodeExpressions.constantTrue()));
            }
        });

        body.append(result).retObject();

        return classDefinition;
    }

    private String firstCharUpper(String mapId) {
        return StringUtils.left(mapId, 1).toUpperCase() +
                StringUtils.right(mapId, mapId.length() - 1).replace(".", "_");
    }

    private void generateGetSet(ClassDefinition classDefinition, FieldDefinition field) {
        // setter
        final String name = firstCharUpper(field.getName());
        final Parameter arg = Parameter.arg(field.getName(), field.getType());
        final MethodDefinition setter = classDefinition.declareMethod(Access.a(Access.PUBLIC),
                "set" + name, ParameterizedType.type(void.class), arg);
        final Scope scope = setter.getScope();
        setter.getBody().append(scope.getThis().setField(field, arg)).ret();

    }

}
