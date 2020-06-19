package me.zyee.hibatis.bytecode.compiler.dao;

import io.airlift.bytecode.Access;
import io.airlift.bytecode.ClassDefinition;
import io.airlift.bytecode.FieldDefinition;
import io.airlift.bytecode.Parameter;
import io.airlift.bytecode.ParameterizedType;
import me.zyee.hibatis.bytecode.HibatisGenerator;
import me.zyee.hibatis.bytecode.compiler.Compiler;
import me.zyee.hibatis.bytecode.compiler.impl.ConstructorCompiler;
import me.zyee.hibatis.common.SupplierLazyGet;
import me.zyee.hibatis.dao.DaoInfo;
import me.zyee.hibatis.dao.DaoMethodInfo;
import me.zyee.hibatis.dao.registry.MapRegistry;
import me.zyee.hibatis.exception.ByteCodeGenerateException;
import me.zyee.hibatis.exception.HibatisException;
import me.zyee.hibatis.exception.HibatisNotFountException;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author yee
 * @version 1.0
 * Create by yee on 2020/6/17
 */
public class DaoCompiler implements Compiler<DaoInfo, ClassDefinition> {
    private final ConstructorCompiler compiler = new ConstructorCompiler();

    @Override
    public ClassDefinition compile(DaoInfo daoInfo) throws HibatisException {
        final Class<?> inf = daoInfo.getId();
        ClassDefinition classDefinition = new ClassDefinition(Access.a(Access.PUBLIC, Access.FINAL),
                HibatisGenerator.makeClassName("dao", inf.getSimpleName()),
                ParameterizedType.type(Object.class),
                ParameterizedType.type(inf));
        final FieldDefinition session = classDefinition.declareField(Access.a(Access.PRIVATE, Access.FINAL),
                "session", SupplierLazyGet.class);
        final FieldDefinition mapRegistry = classDefinition.declareField(Access.a(Access.PRIVATE, Access.FINAL),
                "mapRegistry", MapRegistry.class);

        compiler.compile(ConstructorCompiler.Context.newInstance(classDefinition)
                .append(Parameter.arg("session", SupplierLazyGet.class))
                .append(Parameter.arg("mapRegistry", MapRegistry.class))
                .with(field -> {
                    if (field.equals(session)) {
                        return 0;
                    } else if (field.equals(mapRegistry)) {
                        return 1;
                    }
                    return null;
                }));


        final List<DaoMethodInfo> methods = daoInfo.getMethodInfos();

        // 将解析的方法放到map中，便于判断方法是否重复，是否有实现
        Map<String, DaoMethodInfo> infoMap = new HashMap<>(methods.size());
        for (DaoMethodInfo method : methods) {
            final DaoMethodInfo ret = infoMap.putIfAbsent(method.getId(), method);
            // 如果ret不为空则表示map中已存在，表示存在重复的实现方法
            if (ret != null) {
                throw new RuntimeException("重复");
            }
        }

        MethodCompiler methodCompiler = new MethodCompiler(classDefinition);

        for (Method method : inf.getDeclaredMethods()) {
            // 接口中待实现的方法没有实现
            if (!infoMap.containsKey(method.getName())) {
                throw new ByteCodeGenerateException(new HibatisNotFountException("Method " + method.getName() + " not found"));
            }
            // 生成接口中的方法实现
            DaoMethodInfo methodInfo = infoMap.get(method.getName());
            methodCompiler.compile(MethodCompiler.Context.of(methodInfo, method));
        }
        return classDefinition;
    }
}
