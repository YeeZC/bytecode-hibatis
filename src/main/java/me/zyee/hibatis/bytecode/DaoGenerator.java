package me.zyee.hibatis.bytecode;

import io.airlift.bytecode.BytecodeUtils;
import io.airlift.bytecode.ClassGenerator;
import io.airlift.bytecode.DynamicClassLoader;
import io.airlift.bytecode.ParameterizedType;
import me.zyee.hibatis.bytecode.impl.DefaultDaoVisitor;
import me.zyee.hibatis.dao.DaoInfo;
import org.apache.commons.lang3.ClassUtils;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;

/**
 * @author yee
 * Created by yee on 2020/6/11
 **/
public class DaoGenerator {
    public static Class<?> generate(DaoInfo info, Path out) throws ClassNotFoundException, NoSuchMethodException {
        final Class<?> inf = ClassUtils.getClass(info.getClassName());

        final DynamicClassLoader dynamicClassLoader = new DynamicClassLoader(DaoGenerator.class.getClassLoader()
                , Collections.emptyMap());

        return ClassGenerator.classGenerator(dynamicClassLoader).dumpClassFilesTo(Optional.ofNullable(out)).defineClass(new DefaultDaoVisitor().visit(info), inf);
    }

    public  static Class<?> generate(DaoInfo info) throws ClassNotFoundException, NoSuchMethodException {
        return generate(info, null);
    }

    public static ParameterizedType makeClassName(Package pk, String className) {
        final String name = pk.getName();
        final String string = BytecodeUtils.toJavaIdentifierString(className + "$" + System.currentTimeMillis());
        return ParameterizedType.typeFromJavaClassName(name + ".$gen.impl." + string);
    }
}
