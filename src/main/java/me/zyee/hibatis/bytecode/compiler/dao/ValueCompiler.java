package me.zyee.hibatis.bytecode.compiler.dao;

import io.airlift.bytecode.BytecodeNode;
import io.airlift.bytecode.Variable;
import me.zyee.hibatis.bytecode.compiler.impl.ByteCodeNodeCompiler;
import me.zyee.hibatis.exception.HibatisException;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author yee
 * @version 1.0
 * Create by yee on 2020/6/18
 */
public class ValueCompiler implements ByteCodeNodeCompiler<ValueCompiler.Context> {
    private static final Map<Class<?>, Method> toMethod = new ConcurrentHashMap<>();

    static {
        toMethod.put(Integer.class, MethodUtils.getAccessibleMethod(Number.class, "intValue"));
        toMethod.put(Byte.class, MethodUtils.getAccessibleMethod(Number.class, "byteValue"));
        toMethod.put(Short.class, MethodUtils.getAccessibleMethod(Number.class, "shortValue"));
        toMethod.put(Long.class, MethodUtils.getAccessibleMethod(Number.class, "longValue"));
        toMethod.put(Float.class, MethodUtils.getAccessibleMethod(Number.class, "floatValue"));
        toMethod.put(Double.class, MethodUtils.getAccessibleMethod(Number.class, "doubleValue"));
    }

    @Override
    public BytecodeNode compile(Context context) throws HibatisException {
        final Class<?> returnType = context.returnType;
        final Method method = toMethod.get(ClassUtils.primitiveToWrapper(returnType));
        if (null != method) {
            return context.variable.cast(Number.class).invoke(method);
        }

        if (returnType.equals(String.class)) {
            return context.variable.invoke("toString", String.class);
        }

        return context.variable.cast(returnType);
    }

    public static class Context {
        private final Class<?> returnType;
        private final Variable variable;

        public Context(Class<?> returnType, Variable variable) {
            this.returnType = returnType;
            this.variable = variable;
        }
    }
}
