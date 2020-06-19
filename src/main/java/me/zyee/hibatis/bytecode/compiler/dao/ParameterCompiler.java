package me.zyee.hibatis.bytecode.compiler.dao;

import io.airlift.bytecode.BytecodeBlock;
import io.airlift.bytecode.Parameter;
import io.airlift.bytecode.Variable;
import me.zyee.hibatis.bytecode.compiler.impl.ByteCodeNodeCompiler;
import me.zyee.hibatis.dao.annotation.Param;
import me.zyee.hibatis.exception.HibatisException;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static io.airlift.bytecode.expression.BytecodeExpressions.constantString;
import static io.airlift.bytecode.expression.BytecodeExpressions.constantTrue;
import static io.airlift.bytecode.expression.BytecodeExpressions.invokeStatic;

/**
 * @author yee
 * @version 1.0
 * Create by yee on 2020/6/17
 */
public class ParameterCompiler implements ByteCodeNodeCompiler<Parameter> {
    public final static String VAR_PREFIX = "var_";

    private final Variable map;
    private final Method put;
    private final Method readField;

    public ParameterCompiler(Variable map) {
        this.map = map;
        this.put = MethodUtils.getAccessibleMethod(Map.class, "put", Object.class, Object.class);
        this.readField = MethodUtils.getAccessibleMethod(FieldUtils.class,
                "readField", Object.class, String.class, boolean.class);
    }

    @Override
    public BytecodeBlock compile(Parameter parameter) throws HibatisException {

        try {
            final BytecodeBlock block = new BytecodeBlock();
            final String name = parameter.getName();
            if (name.startsWith(VAR_PREFIX)) {
                final String javaClassName = parameter.getType().getJavaClassName();
                final Class<?> aClass = ClassUtils.getClass(javaClassName);
                if (!ClassUtils.isPrimitiveOrWrapper(aClass) && !ClassUtils.isAssignable(String.class, aClass)) {
                    final List<Field> fields = FieldUtils.getFieldsListWithAnnotation(aClass, Param.class);
                    for (Field field : fields) {
                        final Param param = field.getAnnotation(Param.class);
                        block.append(map.invoke(put,
                                constantString(param.value()),
                                invokeStatic(readField, parameter,
                                        constantString(field.getName()),
                                        constantTrue())));
                    }
                } else {
                    block.append(map.invoke(put,
                            constantString(name.replace(VAR_PREFIX, "")),
                            parameter));
                }

            } else {
                block.append(map.invoke(put,
                        constantString(name),
                        parameter));
            }
            return block;
        } catch (ClassNotFoundException e) {
            throw new HibatisException(e);
        }
    }
}
