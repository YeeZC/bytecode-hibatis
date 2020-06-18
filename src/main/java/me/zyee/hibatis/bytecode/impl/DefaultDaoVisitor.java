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
import me.zyee.hibatis.dao.registry.MapRegistry;
import me.zyee.hibatis.exception.ByteCodeGenerateException;
import me.zyee.hibatis.exception.HibatisNotFountException;
import org.hibernate.Session;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author yee
 * Created by yee on 2020/6/12
 **/
@Deprecated
public class DefaultDaoVisitor {

    /**
     * 将DaoInfo转成ClassDefinition 结果用于生成实体Dao类
     *
     * @param info
     * @return
     * @throws ByteCodeGenerateException
     */
    public ClassDefinition visit(DaoInfo info) throws ByteCodeGenerateException {
        // dao 接口
        final Class<?> inf = info.getId();
        // entity类型
        final Class<?> entity = info.getEntity();
        // public final class TestDaoImpl
        ClassDefinition classDefinition = new ClassDefinition(Access.a(Access.PUBLIC, Access.FINAL),
                DaoGenerator.makeClassName("dao", inf.getSimpleName()),
                ParameterizedType.type(Object.class),
                ParameterizedType.type(inf));
        // private Session session;
        final FieldDefinition session = classDefinition
                .declareField(Access.a(Access.PRIVATE), "session"
                        , Session.class);
        // private final Class entityClass;
        final FieldDefinition entityClass = classDefinition
                .declareField(Access.a(Access.PRIVATE, Access.FINAL), "entityClass"
                        , Class.class);
        // private TestDaoImpl(Session session) {
        //      this.session = session;
        //      this.entityClass = TestEntity.class;
        // }
        createConstructor(entity, classDefinition, session, entityClass);

        final List<DaoMethodInfo> methods = info.getMethodInfos();

        // 将解析的方法放到map中，便于判断方法是否重复，是否有实现
        Map<String, DaoMethodInfo> infoMap = new HashMap<>(methods.size());
        for (DaoMethodInfo method : methods) {
            final DaoMethodInfo ret = infoMap.putIfAbsent(method.getId(), method);
            // 如果ret不为空则表示map中已存在，表示存在重复的实现方法
            if (ret != null) {
                throw new RuntimeException("重复");
            }
        }

        for (Method method : inf.getDeclaredMethods()) {
            // 接口中待实现的方法没有实现
            if (!infoMap.containsKey(method.getName())) {
                throw new ByteCodeGenerateException(new HibatisNotFountException("Method " + method.getName() + " not found"));
            }
            // 生成接口中的方法实现
            DaoMethodInfo methodInfo = infoMap.get(method.getName());
            if (methodInfo.isNativeSql()) {
                new SQLMethodVisitor(classDefinition, method, methodInfo, MapRegistry.of(info)).visit();
            } else {
                new HQLMethodVisitor(classDefinition, method, methodInfo, MapRegistry.of(info)).visit();
            }
        }

        // public static TestDaoImpl newInstance(Session session);
        createNewInstance(classDefinition);
        return classDefinition;
    }

    private void createConstructor(Class<?> entity, ClassDefinition classDefinition, FieldDefinition session, FieldDefinition entityClass) {
        final Parameter paramSession = Parameter.arg("session", Session.class);
        final MethodDefinition constructor = classDefinition.declareConstructor(Access.a(Access.PRIVATE), paramSession);
        final BytecodeBlock body = constructor.getBody();
        final Scope scope = constructor.getScope();
        body.setDescription("Constructor");
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
