package me.zyee.hibatis.bytecode.compiler.bean;

import io.airlift.bytecode.AnnotationDefinition;
import io.airlift.bytecode.BytecodeBlock;
import io.airlift.bytecode.ClassDefinition;
import io.airlift.bytecode.FieldDefinition;
import io.airlift.bytecode.MethodDefinition;
import io.airlift.bytecode.Parameter;
import io.airlift.bytecode.Scope;
import io.airlift.bytecode.Variable;
import me.zyee.hibatis.bytecode.HibatisGenerator;
import me.zyee.hibatis.bytecode.annotation.Order;
import me.zyee.hibatis.bytecode.compiler.Compiler;
import me.zyee.hibatis.dao.DaoMapInfo;
import me.zyee.hibatis.dao.DaoProperty;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.airlift.bytecode.Access.FINAL;
import static io.airlift.bytecode.Access.PRIVATE;
import static io.airlift.bytecode.Access.PUBLIC;
import static io.airlift.bytecode.Access.a;
import static io.airlift.bytecode.Parameter.arg;
import static io.airlift.bytecode.ParameterizedType.type;
import static io.airlift.bytecode.expression.BytecodeExpressions.constantString;
import static io.airlift.bytecode.expression.BytecodeExpressions.constantTrue;
import static io.airlift.bytecode.expression.BytecodeExpressions.invokeStatic;
import static io.airlift.bytecode.expression.BytecodeExpressions.newInstance;
import static org.apache.commons.lang3.ClassUtils.isAssignable;
import static org.apache.commons.lang3.StringUtils.left;
import static org.apache.commons.lang3.StringUtils.right;
import static org.apache.commons.lang3.reflect.MethodUtils.getAccessibleMethod;

/**
 * @author yee
 * @version 1.0
 * Create by yee on 2020/6/17
 */
public class BeanCompiler implements Compiler<DaoMapInfo, ClassDefinition> {
    private final Method put;
    private final Method writeField;

    public BeanCompiler() {
        this.put = getAccessibleMethod(Map.class,
                "put", Object.class, Object.class);
        this.writeField = getAccessibleMethod(FieldUtils.class,
                "writeField", Object.class,
                String.class, Object.class, boolean.class);
    }

    @Override
    public ClassDefinition compile(DaoMapInfo mapInfo) {
        final String mapId = mapInfo.getMapId();
        final String className = firstCharUpper(mapId);
        ClassDefinition classDefinition = new ClassDefinition(a(PRIVATE, FINAL),
                HibatisGenerator.makeClassName("bean", className),
                type(Object.class),
                type(ObjectCast.class));
        classDefinition.declareDefaultConstructor(a(PUBLIC));
        final List<DaoProperty> properties = mapInfo.getProperties();

        final MethodDefinition cast = classDefinition.declareMethod(a(PUBLIC), "cast",
                type(Object.class));

        final BytecodeBlock body = cast.getBody();
        final Scope scope = cast.getScope();

        final Class<?> resultMap = Optional.<Class<?>>ofNullable(mapInfo.getClassName())
                .orElse(HashMap.class);
        final Variable result = scope.declareVariable(resultMap, "result");
        body.append(result.set(newInstance(resultMap)));

        for (int i = 0; i < properties.size(); i++) {
            final DaoProperty property = properties.get(i);
            final FieldDefinition fieldDefinition = classDefinition.declareField(a(PRIVATE),
                    property.getColumn(), property.getJavaType());
            final AnnotationDefinition annotation = fieldDefinition.declareAnnotation(Order.class);
            annotation.setValue("value", i);
            generateGetSet(classDefinition, fieldDefinition);
        }

        properties.forEach(property -> {
            if (isAssignable(Map.class, resultMap) || isAssignable(resultMap, Map.class)) {
                body.append(scope.getVariable("result")
                        .invoke(put, constantString(property.getProperty()),
                                scope.getThis().getField(property.getColumn(),
                                        property.getJavaType())));
            } else {
                body.append(invokeStatic(writeField,
                        scope.getVariable("result"),
                        constantString(property.getProperty()),
                        scope.getThis().getField(property.getColumn(),
                                property.getJavaType()),
                        constantTrue()));
            }
        });

        body.append(result).retObject();

        return classDefinition;
    }

    private String firstCharUpper(String mapId) {
        return left(mapId, 1).toUpperCase() +
                right(mapId, mapId.length() - 1).replace(".", "_");
    }

    private void generateGetSet(ClassDefinition classDefinition, FieldDefinition field) {
        // setter
        final String name = firstCharUpper(field.getName());
        final Parameter arg = arg(field.getName(), field.getType());
        final MethodDefinition setter = classDefinition.declareMethod(a(PUBLIC),
                "set" + name, type(void.class), arg);
        final Scope scope = setter.getScope();
        setter.getBody().append(scope.getThis().setField(field, arg)).ret();

    }

}
