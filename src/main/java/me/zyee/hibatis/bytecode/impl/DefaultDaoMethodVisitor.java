package me.zyee.hibatis.bytecode.impl;

import io.airlift.bytecode.Access;
import io.airlift.bytecode.BytecodeBlock;
import io.airlift.bytecode.ClassDefinition;
import io.airlift.bytecode.MethodDefinition;
import io.airlift.bytecode.Parameter;
import io.airlift.bytecode.ParameterizedType;
import io.airlift.bytecode.Scope;
import io.airlift.bytecode.Variable;
import io.airlift.bytecode.expression.BytecodeExpressions;
import me.zyee.hibatis.dao.DaoMethodInfo;
import me.zyee.hibatis.dao.MethodType;
import me.zyee.hibatis.dao.annotation.Param;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
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
 * Created by yee on 2020/6/12
 **/
public class DefaultDaoMethodVisitor {
    private BytecodeBlock body;
    private Scope scope;

    /**
     * 生成接口方法的实现
     *
     * @param classDefinition
     * @param method
     * @param daoInfo
     * @return
     * @throws NoSuchMethodException
     * @throws ClassNotFoundException
     */
    public MethodDefinition visit(ClassDefinition classDefinition, Method method, DaoMethodInfo daoInfo) throws NoSuchMethodException, ClassNotFoundException {
        final String methodName = method.getName();
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
        // 实现方法：1 方法签名 public returnType methodName(args...);
        final ParameterizedType returnType = ParameterizedType.type(method.getReturnType());
        final MethodDefinition methodDefinition = classDefinition.declareMethod(Access.a(Access.PUBLIC), methodName, returnType, parameters);
        body = methodDefinition.getBody();
        scope = methodDefinition.getScope();
        Variable query = null;
        final String hql = daoInfo.getHql().trim();
        // 实现方法：2 构造Query对象 NativeQuery query = this.session.createSQLQuery(hql); // Query query = this.session.createQuery(hql);
        if (daoInfo.isNativeSql()) {
            query = createVariable(NativeQuery.class, "query");
            body.append(query.set(scope.getThis().getField("session", Session.class).invoke("createSQLQuery", NativeQuery.class,
                    BytecodeExpressions.constantString(hql))));
        } else {
            query = createVariable(Query.class, "query");
            body.append(query.set(scope.getThis().getField("session", Session.class).invoke("createQuery", Query.class,
                    BytecodeExpressions.constantString(hql))));
        }
        // 实现方法：3 传参数 query.setProperties(Map);
        setMapProperties(query, parameters);
        // 实现方法：4 处理返回值
        if (daoInfo.getType() == MethodType.SELECT) {
            visitSelect(daoInfo, query, method.getReturnType(), daoInfo.isNativeSql());
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

    private void visitSelect(DaoMethodInfo info, Variable query, Class<?> returnType, boolean nativeSql) throws NoSuchMethodException, ClassNotFoundException {
        if (nativeSql) {
            visitNativeSelect(info, query, returnType);
        } else {
            if (ClassUtils.isAssignable(Collection.class, returnType) || ClassUtils.isAssignable(returnType, Collection.class)) {
                body.append(query.invoke("getResultList", List.class));
            } else if (returnType.isArray()) {
                final Method toArray = List.class.getDeclaredMethod("toArray", Object[].class);
                body.append(query.invoke("getResultList", List.class).invoke(toArray,
                        BytecodeExpressions.newArray(ParameterizedType.type(returnType.getComponentType()), 0)));
            } else {
                body.append(query.invoke("getSingleResult", Object.class).cast(returnType));
            }
            body.ret(returnType);
        }
    }

    private void visitNativeSelect(DaoMethodInfo info, Variable query, Class<?> returnType) throws NoSuchMethodException, ClassNotFoundException {
        Method transformer = Transformers.class.getDeclaredMethod("aliasToBean", Class.class);
        Method setTrans = Query.class.getDeclaredMethod("setResultTransformer", ResultTransformer.class);
        if (ClassUtils.isAssignable(Collection.class, returnType) || ClassUtils.isAssignable(returnType, Collection.class)) {
            final String resultType = info.getResultType();
            if (StringUtils.isNotEmpty(resultType)) {
                final Class<?> returnItem = ClassUtils.getClass(resultType);
                body.append(query.invoke(setTrans,
                        BytecodeExpressions.invokeStatic(transformer,
                                BytecodeExpressions.constantClass(returnItem))));
            } else {
                body.append(query.invoke(setTrans,
                        BytecodeExpressions.invokeStatic(transformer,
                                scope.getThis().getField("entityClass", Class.class))));
            }
            body.append(query.invoke("getResultList", List.class));
            body.retObject();
        } else if (returnType.isArray()) {
            body.append(query.invoke(setTrans,
                    BytecodeExpressions.invokeStatic(transformer,
                            BytecodeExpressions.constantClass(returnType.getComponentType()))));
            final Method toArray = List.class.getDeclaredMethod("toArray", Object[].class);
            body.append(query.invoke("getResultList", List.class).invoke(toArray,
                    BytecodeExpressions.newArray(ParameterizedType.type(returnType.getComponentType()), 0)));
            body.retObject();
        } else {
            body.append(query.invoke(setTrans,
                    BytecodeExpressions.invokeStatic(transformer,
                            BytecodeExpressions.constantClass(returnType))));
            body.append(query.invoke("getSingleResult", Object.class).cast(returnType));
            body.retObject();
        }
    }

    private Variable createVariable(Class<?> clazz, String name) {
        if (StringUtils.isEmpty(name)) {
            return scope.createTempVariable(clazz);
        }
        return scope.declareVariable(clazz, name);
    }


    private void setMapProperties(Variable query, Parameter[] parameters) throws NoSuchMethodException, ClassNotFoundException {
        if (parameters.length > 0) {
            final Variable properties = scope.createTempVariable(Map.class);
            body.append(properties.set(BytecodeExpressions.newInstance(ParameterizedType.type(HashMap.class),
                    BytecodeExpressions.constantInt(parameters.length))));
            final Method put = Map.class.getDeclaredMethod("put", Object.class, Object.class);
            for (Parameter parameter : parameters) {
                final String name = parameter.getName();
                if (name.startsWith("var_")) {
                    final String javaClassName = parameter.getType().getJavaClassName();
                    final Class<?> aClass = ClassUtils.getClass(javaClassName);
                    if (!ClassUtils.isPrimitiveOrWrapper(aClass) && !ClassUtils.isAssignable(String.class, aClass)) {
                        final List<Field> fields = FieldUtils.getFieldsListWithAnnotation(aClass, Param.class);
                        final Method readField = FieldUtils.class.getDeclaredMethod("readField", Object.class, String.class, boolean.class);
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
}
