package me.zyee.hibatis.bytecode.impl;

import io.airlift.bytecode.Access;
import io.airlift.bytecode.BytecodeBlock;
import io.airlift.bytecode.ClassDefinition;
import io.airlift.bytecode.FieldDefinition;
import io.airlift.bytecode.MethodDefinition;
import io.airlift.bytecode.Parameter;
import io.airlift.bytecode.ParameterizedType;
import io.airlift.bytecode.Scope;
import io.airlift.bytecode.expression.BytecodeExpressions;
import me.zyee.hibatis.bytecode.DaoGenerator;
import me.zyee.hibatis.dao.DaoInfo;
import me.zyee.hibatis.dao.DaoMethodInfo;
import org.apache.commons.lang3.ClassUtils;
import org.hibernate.Session;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author yee
 * Created by yee on 2020/6/12
 **/
public class DefaultDaoVisitor {
    private final DefaultDaoMethodVisitor methodVisitor;

    public DefaultDaoVisitor() {
        this.methodVisitor = new DefaultDaoMethodVisitor();
    }

    public ClassDefinition visit(DaoInfo info) throws ClassNotFoundException, NoSuchMethodException {
        final Class<?> inf = ClassUtils.getClass(info.getClassName());
        final Class<?> entity = ClassUtils.getClass(info.getEntity());
        ClassDefinition classDefinition = new ClassDefinition(Access.a(Access.PUBLIC, Access.FINAL),
                DaoGenerator.makeClassName(inf.getPackage(), inf.getSimpleName() + "Impl"),
                ParameterizedType.type(Object.class),
                ParameterizedType.type(inf));
        final FieldDefinition session = classDefinition
                .declareField(Access.a(Access.PRIVATE), "session"
                        , Session.class);
        final FieldDefinition entityClass = classDefinition
                .declareField(Access.a(Access.PRIVATE, Access.FINAL), "entityClass"
                        , Class.class);
        createConstructor(entity, classDefinition, session, entityClass);

        final List<DaoMethodInfo> methods = info.getMethodInfos();

        Map<String, DaoMethodInfo> infoMap = new HashMap<>(methods.size());
        for (DaoMethodInfo method : methods) {
            final DaoMethodInfo ret = infoMap.putIfAbsent(method.getId(), method);
            if (ret != null) {
                throw new RuntimeException("重复");
            }
        }

        for (Method method : inf.getDeclaredMethods()) {
            if (!infoMap.containsKey(method.getName())) {
                throw new RuntimeException("Not found");
            }
            methodVisitor.visit(classDefinition, method, infoMap.get(method.getName()));
        }

        createNewInstance(classDefinition);
        return classDefinition;
    }

    private void createConstructor(Class<?> entity, ClassDefinition classDefinition, FieldDefinition session, FieldDefinition entityClass) {
        final Parameter paramSession = Parameter.arg("session", Session.class);
        final MethodDefinition constructor = classDefinition.declareConstructor(Access.a(Access.PRIVATE), paramSession);
        final BytecodeBlock body = constructor.getBody();
        final Scope scope = constructor.getScope();
        body.comment("Inital");
        body.append(constructor.getThis())
                .invokeConstructor(Object.class);
        body.append(scope.getThis().setField(session, paramSession))
                .append(scope.getThis().setField(entityClass, BytecodeExpressions.constantClass(entity)));
        body.ret();
    }

    private void createNewInstance(ClassDefinition classDefinition) {
        final Parameter session = Parameter.arg("session", Session.class);
        final MethodDefinition newInstance = classDefinition.declareMethod(Access.a(Access.PUBLIC, Access.STATIC), "newInstance", classDefinition.getType(), session);
        final BytecodeBlock body = newInstance.getBody();
        body.append(BytecodeExpressions.newInstance(classDefinition.getType(), session));
        body.retObject();
    }
}
