package me.zyee.dynamic.hql.bytecode;

import io.airlift.bytecode.Access;
import io.airlift.bytecode.BytecodeBlock;
import io.airlift.bytecode.BytecodeUtils;
import io.airlift.bytecode.ClassDefinition;
import io.airlift.bytecode.ClassGenerator;
import io.airlift.bytecode.DynamicClassLoader;
import io.airlift.bytecode.FieldDefinition;
import io.airlift.bytecode.MethodDefinition;
import io.airlift.bytecode.Parameter;
import io.airlift.bytecode.ParameterizedType;
import io.airlift.bytecode.Scope;
import io.airlift.bytecode.Variable;
import io.airlift.bytecode.expression.BytecodeExpressions;
import me.zyee.dynamic.hql.config.DaoInfo;
import me.zyee.dynamic.hql.config.DaoMethodInfo;
import me.zyee.dynamic.hql.config.annotation.Param;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.transform.Transformers;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author yeezc
 * Created by yeezc on 2020/6/11
 **/
public class DaoGenerator {
    public static Class<?> generate(DaoInfo info) throws ClassNotFoundException, NoSuchMethodException {
        final Class<?> inf = ClassUtils.getClass(info.getClassName());
        final ClassDefinition classDefinition = new ClassDefinition(Access.a(Access.PUBLIC, Access.FINAL),
                makeClassName(inf.getPackage(), inf.getSimpleName() + "Impl"),
                ParameterizedType.type(Object.class),
                ParameterizedType.type(inf));
        final FieldDefinition session = classDefinition
                .declareField(Access.a(Access.PRIVATE), "session"
                        , Session.class);
        final FieldDefinition entity = classDefinition
                .declareField(Access.a(Access.PRIVATE, Access.FINAL), "entity"
                        , Class.class);
        defineConstructor(classDefinition, session, entity, ClassUtils.getClass(info.getEntity()));
        defineMethods(classDefinition, session, entity, inf, info.getMethodInfos());
        final DynamicClassLoader dynamicClassLoader = new DynamicClassLoader(DaoGenerator.class.getClassLoader()
                , Collections.emptyMap());
        return ClassGenerator.classGenerator(dynamicClassLoader).dumpClassFilesTo(Paths.get("/Users/yee/work")).defineClass(classDefinition, inf);
    }

    private static void defineConstructor(ClassDefinition classDefinition, FieldDefinition session, FieldDefinition entity, Class entityClass) {
        final Parameter paramSession = Parameter.arg("session", Session.class);
        final MethodDefinition constructor = classDefinition.declareConstructor(Access.a(Access.PUBLIC), paramSession);
        final BytecodeBlock body = constructor.getBody();
        final Scope scope = constructor.getScope();
        body.comment("Inital");
        body.append(constructor.getThis())
                .invokeConstructor(Object.class);
        body.append(scope.getThis().setField(session, paramSession))
                .append(scope.getThis().setField(entity, BytecodeExpressions.constantClass(entityClass)));
        body.ret();
    }

    private static void defineMethods(ClassDefinition classDefinition, FieldDefinition session, FieldDefinition entity,
                                      Class<?> inf, List<DaoMethodInfo> methods) throws ClassNotFoundException, NoSuchMethodException {
        final Method[] javaMethods = inf.getDeclaredMethods();
        Map<String, DaoMethodInfo> infoMap = new HashMap<>(methods.size());
        for (DaoMethodInfo method : methods) {
            final DaoMethodInfo info = infoMap.putIfAbsent(method.getId(), method);
            if (info != null) {
                throw new RuntimeException("重复");
            }
        }

        for (Method javaMethod : javaMethods) {
            defineMethod(classDefinition, session, entity, infoMap, javaMethod);
        }
    }

    private static void defineMethod(ClassDefinition classDefinition,
                                     FieldDefinition session,
                                     FieldDefinition entity,
                                     Map<String, DaoMethodInfo> infoMap,
                                     Method javaMethod) throws ClassNotFoundException, NoSuchMethodException {
        final String methodName = javaMethod.getName();
        if (!infoMap.containsKey(methodName)) {
            throw new RuntimeException("没有找到对应的方法");
        }
        final DaoMethodInfo daoMethodInfo = infoMap.get(methodName);

        final Parameter[] parameters = Stream.of(javaMethod.getParameters()).map(parameter ->
        {
            final Class<?> type = parameter.getType();
            final Param param = parameter.getAnnotation(Param.class);
            if (null != param) {
                return Parameter.arg(param.value(), type);
            }
            return Parameter.arg("var_" + parameter.getName(), type);
        }).toArray(Parameter[]::new);
        final MethodDefinition methodDefinition = classDefinition.declareMethod(Access.a(Access.PUBLIC),
                methodName,
                ParameterizedType.type(javaMethod.getReturnType()),
                parameters);
        final BytecodeBlock body = methodDefinition.getBody();
        final Scope scope = methodDefinition.getScope();


        final String hql = daoMethodInfo.getHql().trim();

        Variable query = null;
        if (daoMethodInfo.isNativeSql()) {
            query = scope.declareVariable(NativeQuery.class, "query");
            body.append(query.set(scope.getThis().getField(session).invoke("createSQLQuery", NativeQuery.class,
                    BytecodeExpressions.constantString(hql))));
        } else {
            query = scope.declareVariable(Query.class, "query");
            body.append(query.set(scope.getThis().getField(session).invoke("createQuery", Query.class,
                    BytecodeExpressions.constantString(hql))));
        }
        // Query query = this.session.createQuery(hql);

        setMapProperties(parameters, body, scope, query);
        switch (daoMethodInfo.getType()) {
            case SELECT: {
                Method transformer = Transformers.class.getDeclaredMethod("aliasToBean", Class.class);
                Method setTrans = Query.class.getDeclaredMethod("setResultTransformer", ResultTransformer.class);
                if (ClassUtils.isAssignable(void.class, javaMethod.getReturnType())) {
                    body.append(query.invoke("getResultList", List.class));
                    body.ret();
                } else if (ClassUtils.isAssignable(List.class, javaMethod.getReturnType())) {
                    if (daoMethodInfo.isNativeSql()) {
                        body.append(query.invoke(setTrans,
                                BytecodeExpressions.invokeStatic(transformer,
                                        scope.getThis().getField(entity))));

                    }
                    body.append(query.invoke("getResultList", List.class));
                    body.retObject();
                } else {
                    if (daoMethodInfo.isNativeSql()) {
                        body.append(query.invoke(setTrans,
                                BytecodeExpressions.invokeStatic(transformer,
                                        BytecodeExpressions.constantClass(javaMethod.getReturnType()))));
                    }
                    body.append(query.invoke("getSingleResult", Object.class).cast(javaMethod.getReturnType()));
                    body.ret(javaMethod.getReturnType());
                }
                break;
            }
            case MODIFY: {
                body.append(query.invoke("executeUpdate", int.class));
                if (ClassUtils.isAssignable(void.class, javaMethod.getReturnType())) {
                    body.ret();
                } else {
                    body.retInt();
                }
                break;
            }
            default:
        }
    }

    private static void setMapProperties(Parameter[] parameters, BytecodeBlock body, Scope scope, Variable query) throws NoSuchMethodException, ClassNotFoundException {
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
                        final List<Field> fields = FieldUtils.getAllFieldsList(aClass);
                        final Method readField = FieldUtils.class.getDeclaredMethod("readField", Object.class, String.class, boolean.class);
                        for (Field field : fields) {
                            body.append(properties.invoke(put,
                                    BytecodeExpressions.constantString(field.getName()),
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

    private static ParameterizedType makeClassName(Package pk, String className) {
        final String name = pk.getName();
        final String string = BytecodeUtils.toJavaIdentifierString(className + "$" + System.currentTimeMillis());
        return ParameterizedType.typeFromJavaClassName(name + ".$gen.impl." + string);
    }
}
