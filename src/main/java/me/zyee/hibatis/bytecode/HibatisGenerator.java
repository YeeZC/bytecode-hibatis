package me.zyee.hibatis.bytecode;

import io.airlift.bytecode.BytecodeUtils;
import io.airlift.bytecode.ClassDefinition;
import io.airlift.bytecode.ClassGenerator;
import io.airlift.bytecode.DynamicClassLoader;
import io.airlift.bytecode.ParameterizedType;
import io.airlift.bytecode.Scope;
import io.airlift.bytecode.Variable;
import org.apache.commons.lang3.StringUtils;
import org.n3r.idworker.Sid;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;

/**
 * 用于生成Dao实现类，动态字节码强无敌
 *
 * @author yee
 * Created by yee on 2020/6/11
 **/
public class HibatisGenerator {

    public static Class<?> generate(ClassDefinition definition, Class<?> inf, ClassLoader classLoader, Path out) {
        final ClassLoader loader = Optional.ofNullable(classLoader)
                .orElse(getDefaultClassLoader());
        final DynamicClassLoader dynamicClassLoader = new DynamicClassLoader(loader, Collections.emptyMap());
        return ClassGenerator.classGenerator(dynamicClassLoader)
                .dumpClassFilesTo(Optional.ofNullable(out))
                .defineClass(definition, inf);
    }

    public static Class<?> generate(ClassDefinition definition, Class<?> inf, ClassLoader classLoader) {
        return generate(definition, inf, classLoader, null);
    }

    /**
     * 构造Dao实体类的类型
     *
     * @param prefix
     * @param className
     * @return
     */
    public static ParameterizedType makeClassName(String prefix, String className) {
        final String string = BytecodeUtils.toJavaIdentifierString(className + "$_" + Sid.nextShort());
        return ParameterizedType.typeFromJavaClassName("me.zyee.hibatis.binding.gen." + prefix + "." + string);
    }

    public static Variable createVariable(Scope scope, Class<?> clazz, String name) {
        if (StringUtils.isEmpty(name)) {
            return scope.createTempVariable(clazz);
        }
        return scope.declareVariable(clazz, name);
    }

    public static ClassLoader getDefaultClassLoader() {
        ClassLoader cl = null;

        try {
            cl = Thread.currentThread().getContextClassLoader();
        } catch (Throwable ignore) {
        }

        if (cl == null) {
            cl = HibatisGenerator.class.getClassLoader();
            if (cl == null) {
                try {
                    cl = ClassLoader.getSystemClassLoader();
                } catch (Throwable ignore) {
                }
            }
        }

        return cl;
    }
}
