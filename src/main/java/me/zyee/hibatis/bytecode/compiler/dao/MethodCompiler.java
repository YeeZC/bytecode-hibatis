package me.zyee.hibatis.bytecode.compiler.dao;

import io.airlift.bytecode.Access;
import io.airlift.bytecode.BytecodeBlock;
import io.airlift.bytecode.ClassDefinition;
import io.airlift.bytecode.MethodDefinition;
import io.airlift.bytecode.Parameter;
import io.airlift.bytecode.ParameterizedType;
import io.airlift.bytecode.Scope;
import io.airlift.bytecode.Variable;
import io.airlift.bytecode.expression.BytecodeExpressions;
import me.zyee.hibatis.bytecode.compiler.NoRetCompiler;
import me.zyee.hibatis.dao.DaoMethodInfo;
import me.zyee.hibatis.dao.annotation.Param;
import me.zyee.hibatis.exception.HibatisException;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author yee
 * @version 1.0
 * Create by yee on 2020/6/17
 */
public class MethodCompiler implements NoRetCompiler<MethodCompiler.Context> {
    private final ClassDefinition classDefinition;

    public MethodCompiler(ClassDefinition classDefinition) {
        this.classDefinition = classDefinition;
    }

    @Override
    public void doCompile(Context context) throws HibatisException {
        final Method method = context.getMethod();
        final DaoMethodInfo methodInfo = context.getMethodInfo();
        final Parameter[] parameters = transferParams(method);
        final MethodDefinition methodDefinition = classDefinition.declareMethod(
                Access.a(Access.PUBLIC),
                method.getName(),
                ParameterizedType.type(method.getReturnType()),
                parameters);
        final Scope scope = methodDefinition.getScope();
        final BytecodeBlock body = methodDefinition.getBody();
        final Variable params = scope.createTempVariable(Map.class);
        body.append(params.set(BytecodeExpressions.newInstance(HashMap.class,
                BytecodeExpressions.constantInt(parameters.length))));
        final ParameterCompiler compiler = new ParameterCompiler(params);
        for (Parameter parameter : parameters) {
            body.append(compiler.compile(parameter));
        }

        final String hql = methodInfo.getHql().trim();
        // TODO 方法体生成

    }

    private Parameter[] transferParams(Method method) {
        return Stream.of(method.getParameters()).map(parameter -> {
            final Class<?> type = parameter.getType();
            final Param param = parameter.getAnnotation(Param.class);
            // 参数打了Param注解的是直接注入sql/hql中的
            // 参数没打Param直接表示是实体类参数，目前的处理方法是将每个属性put到map中作为sql/hql的参数
            if (null != param) {
                return Parameter.arg(param.value(), type);
            }
            return Parameter.arg("var_" + parameter.getName(), type);
        }).toArray(Parameter[]::new);
    }

    public static class Context {
        private final DaoMethodInfo methodInfo;
        private final Method method;

        public Context(DaoMethodInfo methodInfo, Method method) {
            this.methodInfo = methodInfo;
            this.method = method;
        }

        public DaoMethodInfo getMethodInfo() {
            return methodInfo;
        }

        public Method getMethod() {
            return method;
        }

        public static Context of(DaoMethodInfo methodInfo, Method method) {
            return new Context(methodInfo, method);
        }
    }
}
