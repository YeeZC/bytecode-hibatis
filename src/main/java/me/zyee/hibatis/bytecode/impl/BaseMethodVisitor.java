package me.zyee.hibatis.bytecode.impl;

import io.airlift.bytecode.Access;
import io.airlift.bytecode.BytecodeBlock;
import io.airlift.bytecode.ClassDefinition;
import io.airlift.bytecode.MethodDefinition;
import io.airlift.bytecode.Parameter;
import io.airlift.bytecode.ParameterizedType;
import io.airlift.bytecode.Scope;
import io.airlift.bytecode.Variable;
import io.airlift.bytecode.expression.BytecodeExpression;
import io.airlift.bytecode.expression.BytecodeExpressions;
import me.zyee.hibatis.bytecode.BeanGenerator;
import me.zyee.hibatis.bytecode.MethodVisitor;
import me.zyee.hibatis.dao.DaoMethodInfo;
import me.zyee.hibatis.dao.MethodType;
import me.zyee.hibatis.dao.annotation.Param;
import me.zyee.hibatis.dao.registry.MapRegistry;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.transform.Transformers;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author yee
 * @version 1.0
 * Create by yee on 2020/6/15
 */
public abstract class BaseMethodVisitor implements MethodVisitor {
    private final ClassDefinition classDefinition;
    private final Method method;
    private final DaoMethodInfo methodInfo;
    private final Class<?> queryClass;
    protected MapRegistry registry;
    protected MethodDefinition methodDefinition;
    protected Scope scope;
    protected BytecodeBlock body;
    protected Variable query;


    public BaseMethodVisitor(ClassDefinition classDefinition,
                             Method method,
                             DaoMethodInfo methodInfo,
                             MapRegistry registry,
                             Class<?> queryClass) {
        this.classDefinition = classDefinition;
        this.method = method;
        this.methodInfo = methodInfo;
        this.queryClass = queryClass;
        this.registry = registry;
    }

    protected void setMapProperties(Parameter[] parameters) throws NoSuchMethodException, ClassNotFoundException {
        if (parameters.length > 0) {
            final Variable properties = scope.createTempVariable(Map.class);
            body.append(properties.set(BytecodeExpressions.newInstance(ParameterizedType.type(HashMap.class),
                    BytecodeExpressions.constantInt(parameters.length))));
            final Method put = Map.class.getDeclaredMethod("put", java.lang.Object.class, java.lang.Object.class);
            for (Parameter parameter : parameters) {
                final String name = parameter.getName();
                if (name.startsWith("var_")) {
                    final String javaClassName = parameter.getType().getJavaClassName();
                    final Class<?> aClass = ClassUtils.getClass(javaClassName);
                    if (!ClassUtils.isPrimitiveOrWrapper(aClass) && !ClassUtils.isAssignable(String.class, aClass)) {
                        final List<Field> fields = FieldUtils.getFieldsListWithAnnotation(aClass, Param.class);
                        final Method readField = FieldUtils.class.getDeclaredMethod("readField", java.lang.Object.class, String.class, boolean.class);
                        for (Field field : fields) {
                            final Param param = field.getAnnotation(Param.class);
                            body.append(properties.invoke(put,
                                    BytecodeExpressions.constantString(param.value()),
                                    BytecodeExpressions.invokeStatic(readField, parameter,
                                            BytecodeExpressions.constantString(field.getName()),
                                            BytecodeExpressions.constantTrue())));
                        }
                    } else {
                        body.append(properties.invoke(put,
                                BytecodeExpressions.constantString(name.replace("var_", "")),
                                parameter));
                    }

                } else {
                    body.append(properties.invoke(put,
                            BytecodeExpressions.constantString(name),
                            parameter));
                }
            }
            body.append(query.invoke("setProperties", Query.class, properties));
        }
    }

    @Override
    public MethodDefinition visit() throws NoSuchMethodException, ClassNotFoundException {
        final Parameter[] parameters = Stream.of(method.getParameters()).map(parameter -> {
            final Class<?> type = parameter.getType();
            final Param param = parameter.getAnnotation(Param.class);
            // 参数打了Param注解的是直接注入sql/hql中的
            // 参数没打Param直接表示是实体类参数，目前的处理方法是将每个属性put到map中作为sql/hql的参数
            if (null != param) {
                return Parameter.arg(param.value(), type);
            }
            return Parameter.arg("var_" + parameter.getName(), type);
        }).toArray(Parameter[]::new);
        methodDefinition = classDefinition.declareMethod(Access.a(Access.PUBLIC), method.getName(),
                ParameterizedType.type(method.getReturnType()), parameters);
        this.scope = methodDefinition.getScope();
        this.body = methodDefinition.getBody();
        this.query = BeanGenerator.createVariable(scope, queryClass, "query");
        String hql = methodInfo.getHql().trim();
        body.append(query.set(scope.getThis().getField("session", Session.class).invoke(createQueryMethod(),
                BytecodeExpressions.constantString(hql))));
        setMapProperties(parameters);
        if (methodInfo.getType() == MethodType.SELECT) {
            visitSelect(methodInfo, method.getReturnType());
        } else {
            body.append(query.invoke("executeUpdate", int.class));
            if (ClassUtils.isAssignable(void.class, method.getReturnType())) {
                body.ret();
            } else {
                body.retInt();
            }
        }
        return methodDefinition;
    }

    protected BytecodeExpression visitSingleReturnType(BytecodeExpression expression, Class<?> returnType) {
        if (ClassUtils.isPrimitiveOrWrapper(returnType)) {
            if (ClassUtils.isAssignable(int.class, returnType) || ClassUtils.isAssignable(Integer.class, returnType)) {
                return expression.cast(Number.class).invoke("intValue", int.class);
            } else if (ClassUtils.isAssignable(long.class, returnType) || ClassUtils.isAssignable(Long.class, returnType)) {
                return expression.cast(Number.class).invoke("longValue", long.class);
            } else if (ClassUtils.isAssignable(float.class, returnType) || ClassUtils.isAssignable(Float.class, returnType)) {
                return expression.cast(Number.class).invoke("floatValue", float.class);
            } else if (ClassUtils.isAssignable(double.class, returnType) || ClassUtils.isAssignable(Double.class, returnType)) {
                return expression.cast(Number.class).invoke("doubleValue", double.class);
            } else if (ClassUtils.isAssignable(byte.class, returnType) || ClassUtils.isAssignable(Byte.class, returnType)) {
                return expression.cast(Number.class).invoke("byteValue", byte.class);
            } else if (ClassUtils.isAssignable(short.class, returnType) || ClassUtils.isAssignable(Short.class, returnType)) {
                return expression.cast(Number.class).invoke("shortValue", short.class);
            } else {
                return expression;
            }
        } else {
            return expression.cast(returnType);
        }
    }

    protected void visitSelect(DaoMethodInfo methodInfo, Class<?> methodReturnType) throws NoSuchMethodException {
        Method transformer = Transformers.class.getDeclaredMethod("aliasToBean", Class.class);
        Method setTrans = Query.class.getDeclaredMethod("setResultTransformer", ResultTransformer.class);
        final Method toArray = List.class.getDeclaredMethod("toArray", java.lang.Object[].class);
        if (ClassUtils.isAssignable(Collection.class, methodReturnType) || ClassUtils.isAssignable(methodReturnType, Collection.class)) {
            final Variable variable = generateList(methodInfo, transformer, null, setTrans);
            body.append(variable).retObject();
        } else if (methodReturnType.isArray()) {
            final Variable variable = generateList(methodInfo, transformer, methodReturnType.getComponentType(), setTrans);
            body.append(variable.invoke(toArray, BytecodeExpressions.newArray(ParameterizedType.type(methodReturnType.getComponentType()), 0)));
            body.retObject();
        } else {
            generateSingle(methodInfo, methodReturnType, transformer, setTrans);
        }
    }

    /**
     * 生成单返回值
     *
     * @param methodInfo
     * @param methodReturnType
     * @param transformer
     * @param setTrans
     */
    abstract protected void generateSingle(DaoMethodInfo methodInfo, Class<?> methodReturnType, Method transformer, Method setTrans);

    /**
     * 生成List返回值
     *
     * @param methodInfo
     * @param transformer
     * @param componentClass
     * @param setTrans
     * @return
     */
    abstract protected Variable generateList(DaoMethodInfo methodInfo, Method transformer, Class<?> componentClass,
                                             Method setTrans);

    /**
     * 创建查询Method
     *
     * @return
     */
    protected abstract Method createQueryMethod();
}
