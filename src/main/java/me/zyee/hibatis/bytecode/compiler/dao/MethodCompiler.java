package me.zyee.hibatis.bytecode.compiler.dao;

import io.airlift.bytecode.Access;
import io.airlift.bytecode.BytecodeBlock;
import io.airlift.bytecode.ClassDefinition;
import io.airlift.bytecode.MethodDefinition;
import io.airlift.bytecode.Parameter;
import io.airlift.bytecode.ParameterizedType;
import io.airlift.bytecode.Scope;
import io.airlift.bytecode.Variable;
import io.airlift.bytecode.expression.BytecodeExpression;
import io.airlift.bytecode.expression.BytecodeExpressions;
import me.zyee.hibatis.bytecode.compiler.NoRetCompiler;
import me.zyee.hibatis.dao.DaoMethodInfo;
import me.zyee.hibatis.dao.MethodType;
import me.zyee.hibatis.dao.annotation.Param;
import me.zyee.hibatis.dao.registry.MapRegistry;
import me.zyee.hibatis.exception.HibatisException;
import me.zyee.hibatis.query.ListSelectMapper;
import me.zyee.hibatis.query.MapperBuilder;
import me.zyee.hibatis.query.OneSelectMapper;
import me.zyee.hibatis.query.SqlMapper;
import me.zyee.hibatis.query.impl.SqlMapperBuilder;
import org.apache.commons.lang3.ClassUtils;
import org.hibernate.Session;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
        final BytecodeExpression mapRegistry = scope.getThis().getField("mapRegistry", MapRegistry.class);
        final Variable builder = scope.declareVariable(MapperBuilder.class, "builder");
        body.append(builder.set(BytecodeExpressions.newInstance(SqlMapperBuilder.class, mapRegistry)));
        if (methodInfo.isNativeSql()) {
            body.append(builder.invoke("withSql", MapperBuilder.class));
        }
        if (methodInfo.getType() == MethodType.MODIFY) {
            body.append(builder.invoke("build", SqlMapper.class).invoke("executeUpdate",
                    Integer.TYPE,
                    scope.getThis().getField("session", Session.class),
                    BytecodeExpressions.constantString(hql),
                    params)).retInt();
        } else {
            Optional.ofNullable(methodInfo.getResultMap()).ifPresent(mapId -> {
                body.append(builder.invoke("withResultMap",
                        MapperBuilder.class, BytecodeExpressions.constantString(mapId)));
            });

            Optional.ofNullable(methodInfo.getResultType()).ifPresent(type -> {
                body.append(builder.invoke("withResultType",
                        MapperBuilder.class, BytecodeExpressions.constantClass(type)));
            });

            final Class<?> returnType = method.getReturnType();
            if (ClassUtils.isAssignable(returnType, Collection.class)
                    || ClassUtils.isAssignable(Collection.class, returnType)) {
                final Variable mapper = scope.declareVariable(ListSelectMapper.class, "mapper");
                body.append(mapper.set(builder.invoke("build", SqlMapper.class).cast(ListSelectMapper.class)));
                body.append(mapper.invoke("select", List.class,
                        scope.getThis().getField("session", Session.class),
                        BytecodeExpressions.constantString(hql),
                        params)).retObject();
            } else if (returnType.isArray()) {
                body.append(builder.invoke("withResultType",
                        MapperBuilder.class, BytecodeExpressions.constantClass(returnType.getComponentType())));
                final Variable mapper = scope.declareVariable(ListSelectMapper.class, "mapper");
                body.append(mapper.set(builder.invoke("build", SqlMapper.class).cast(ListSelectMapper.class)));
                body.append(mapper.invoke("select", List.class,
                        scope.getThis().getField("session", Session.class),
                        BytecodeExpressions.constantString(hql),
                        params).invoke("toArray", Object[].class).cast(returnType)).retObject();
            } else {
                final Variable mapper = scope.declareVariable(OneSelectMapper.class, "mapper");
                final ValueCompiler valueCompiler = new ValueCompiler();
                if (!ClassUtils.isPrimitiveOrWrapper(returnType) && !String.class.equals(returnType)) {
                    body.append(builder.invoke("withResultType",
                            MapperBuilder.class, BytecodeExpressions.constantClass(returnType)));
                }
                body.append(mapper.set(builder.invoke("build", SqlMapper.class).cast(OneSelectMapper.class)));
                final Variable result = scope.createTempVariable(Object.class);
                body.append(result.set(mapper.invoke("selectOne", Object.class,
                        scope.getThis().getField("session", Session.class),
                        BytecodeExpressions.constantString(hql),
                        params)));
                body.append(valueCompiler.compile(new ValueCompiler.Context(returnType, result)))
                        .ret(returnType);
            }
        }
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
