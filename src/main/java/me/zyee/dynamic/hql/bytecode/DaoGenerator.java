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
import io.airlift.bytecode.expression.BytecodeExpression;
import io.airlift.bytecode.expression.BytecodeExpressions;
import me.zyee.dynamic.hql.config.DaoInfo;
import me.zyee.dynamic.hql.config.DaoMapInfo;
import me.zyee.dynamic.hql.config.DaoMethodInfo;
import me.zyee.dynamic.hql.config.MethodType;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

import java.lang.reflect.Method;
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
    public static Class<?> generate(DaoInfo info) throws ClassNotFoundException {
        final Class<?> inf = ClassUtils.getClass(info.getClassName());
        final ClassDefinition classDefinition = new ClassDefinition(Access.a(Access.PUBLIC, Access.FINAL),
                makeClassName(inf.getPackage(), inf.getName() + "Impl"),
                ParameterizedType.type(Object.class),
                ParameterizedType.type(inf));
        final FieldDefinition session = classDefinition
                .declareField(Access.a(Access.PRIVATE), "session"
                        , Session.class);
        defineConstructor(classDefinition, session);
        defineMethods(classDefinition, inf, info.getMethodInfos());
        final DynamicClassLoader dynamicClassLoader = new DynamicClassLoader(DaoGenerator.class.getClassLoader()
                , Collections.emptyMap());
        return ClassGenerator.classGenerator(dynamicClassLoader).defineClass(classDefinition, inf);
    }

    private static void defineConstructor(ClassDefinition classDefinition, FieldDefinition session) {
        final Parameter paramSession = Parameter.arg("session", Session.class);
        final MethodDefinition constructor = classDefinition.declareConstructor(Access.a(Access.PUBLIC), paramSession);
        final BytecodeBlock body = constructor.getBody();
        final Scope scope = constructor.getScope();
        final BytecodeExpression bytecodeExpression = scope.getThis().setField(session, paramSession);
        body.append(bytecodeExpression);
        body.ret();
    }

    private static void defineMethods(ClassDefinition classDefinition, Class<?> inf, List<DaoMethodInfo> methods) {
        final Method[] javaMethods = inf.getDeclaredMethods();
        Map<String, DaoMethodInfo> infoMap = new HashMap<>(methods.size());
        for (DaoMethodInfo method : methods) {
            final DaoMethodInfo info = infoMap.putIfAbsent(method.getId(), method);
            if (info != null) {
                throw new RuntimeException("重复");
            }
        }

        for (Method javaMethod : javaMethods) {
            defineMethod(classDefinition, infoMap, javaMethod);

        }
    }

    private static void defineMethod(ClassDefinition classDefinition, Map<String, DaoMethodInfo> infoMap, Method javaMethod) throws NoSuchMethodException {
        final String methodName = javaMethod.getName();
        if (!infoMap.containsKey(methodName)) {
            throw new RuntimeException("没有找到对应的方法");
        }
        final DaoMethodInfo daoMethodInfo = infoMap.get(methodName);

        final Parameter[] parameters = Stream.of(javaMethod.getParameterTypes()).map(clazz ->
                Parameter.arg(clazz.getSimpleName(), clazz)).toArray(Parameter[]::new);
        final MethodDefinition methodDefinition = classDefinition.declareMethod(Access.a(Access.PUBLIC),
                methodName,
                ParameterizedType.type(javaMethod.getReturnType()),
                parameters);
        final BytecodeBlock body = methodDefinition.getBody();
        final Scope scope = methodDefinition.getScope();
        final String hql = daoMethodInfo.getHql();
        final DaoMapInfo paramMap = daoMethodInfo.getParamMap();
        final String paramType = daoMethodInfo.getParamType();
        final DaoMapInfo resultMap = daoMethodInfo.getResultMap();
        final String resultType = daoMethodInfo.getResultType();
        final Variable query = scope.declareVariable(Query.class, "query");
        // Query query = this.session.createQuery(hql);
        body.append(query.set(scope.getThis().getField("session", Session.class).invoke("createQuery", Query.class,
                BytecodeExpressions.constantString(hql))));

        if (daoMethodInfo.getType() == MethodType.SELECT) {
            if (null == paramMap && StringUtils.isEmpty(paramType)) {
                final Variable result = scope.declareVariable(List.class, "result");
                body.append(result.set(query.invoke("getResultList", List.class)));
//                body.retObject().append(result);
            } else if (null != paramMap) {

            }

        }


//        TryCatch tryCatch = new TryCatch()
        SessionFactory factory;
        final Session session1 = factory.openSession();
        final Query query1 = session1.createQuery(hql);
        query1.getResultList();
        session1.close();
    }

    private static ParameterizedType makeClassName(Package pk, String className) {
        final String name = pk.getName();
        final String string = BytecodeUtils.toJavaIdentifierString(className + "$" + System.currentTimeMillis());
        return ParameterizedType.typeFromJavaClassName(name + ".$gen.impl." + string);
    }
}
