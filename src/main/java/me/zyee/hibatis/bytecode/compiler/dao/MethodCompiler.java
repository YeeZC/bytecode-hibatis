package me.zyee.hibatis.bytecode.compiler.dao;

import io.airlift.bytecode.BytecodeBlock;
import io.airlift.bytecode.ClassDefinition;
import io.airlift.bytecode.MethodDefinition;
import io.airlift.bytecode.Parameter;
import io.airlift.bytecode.Scope;
import io.airlift.bytecode.Variable;
import io.airlift.bytecode.expression.BytecodeExpression;
import me.zyee.hibatis.bytecode.compiler.NoRetCompiler;
import me.zyee.hibatis.common.SupplierLazyGet;
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

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static io.airlift.bytecode.Access.PUBLIC;
import static io.airlift.bytecode.Access.a;
import static io.airlift.bytecode.ParameterizedType.type;
import static io.airlift.bytecode.expression.BytecodeExpressions.constantClass;
import static io.airlift.bytecode.expression.BytecodeExpressions.constantInt;
import static io.airlift.bytecode.expression.BytecodeExpressions.constantString;
import static io.airlift.bytecode.expression.BytecodeExpressions.newArray;
import static io.airlift.bytecode.expression.BytecodeExpressions.newInstance;
import static org.apache.commons.lang3.ClassUtils.isAssignable;
import static org.apache.commons.lang3.ClassUtils.isPrimitiveOrWrapper;
import static org.apache.commons.lang3.reflect.MethodUtils.getAccessibleMethod;

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
                a(PUBLIC),
                method.getName(),
                type(method.getReturnType()),
                parameters);
        final Scope scope = methodDefinition.getScope();
        final BytecodeBlock body = methodDefinition.getBody();
        final Variable params = scope.createTempVariable(Map.class);
        body.append(params.set(newInstance(HashMap.class,
                constantInt(parameters.length))));
        final ParameterCompiler compiler = new ParameterCompiler(params);
        for (Parameter parameter : parameters) {
            body.append(compiler.compile(parameter));
        }

        final String hql = methodInfo.getHql().trim();
        final BytecodeExpression sessionSupplier = scope.getThis().getField("session", SupplierLazyGet.class);
        final BytecodeExpression mapRegistry = scope.getThis().getField("mapRegistry", MapRegistry.class);
        final Variable builder = scope.declareVariable(MapperBuilder.class, "builder");
        body.append(builder.set(newInstance(SqlMapperBuilder.class, mapRegistry, sessionSupplier)));
        if (methodInfo.isNativeSql()) {
            body.append(builder.invoke("withSql", MapperBuilder.class));
        }
        if (methodInfo.getType() == MethodType.MODIFY) {
            body.append(builder.invoke("build", SqlMapper.class).invoke("executeUpdate",
                    Integer.TYPE,
                    constantString(hql),
                    params)).retInt();
        } else {
            Method toArray = getAccessibleMethod(List.class, "toArray", Object[].class);
            Optional.ofNullable(methodInfo.getResultMap()).ifPresent(mapId -> {
                body.append(builder.invoke("withResultMap",
                        MapperBuilder.class, constantString(mapId)));
            });

            Optional.ofNullable(methodInfo.getResultType()).ifPresent(type -> {
                body.append(builder.invoke("withResultType",
                        MapperBuilder.class, constantClass(type)));
            });

            final Class<?> returnType = method.getReturnType();
            if (isAssignable(returnType, Collection.class)
                    || isAssignable(Collection.class, returnType)) {
                final Variable mapper = scope.declareVariable(ListSelectMapper.class, "mapper");
                body.append(mapper.set(builder.invoke("build", SqlMapper.class).cast(ListSelectMapper.class)));
                body.append(mapper.invoke("select", List.class,
                        constantString(hql),
                        params)).retObject();
            } else if (returnType.isArray()) {
                body.append(builder.invoke("withResultType",
                        MapperBuilder.class, constantClass(returnType.getComponentType())));
                final Variable mapper = scope.declareVariable(ListSelectMapper.class, "mapper");
                body.append(mapper.set(builder.invoke("build", SqlMapper.class).cast(ListSelectMapper.class)));
                body.append(mapper.invoke("select", List.class,
                        constantString(hql),
                        params).invoke(toArray,
                        newArray(type(returnType), 0))
                        .cast(returnType)).retObject();
            } else {
                final Variable mapper = scope.declareVariable(OneSelectMapper.class, "mapper");
                final ValueCompiler valueCompiler = new ValueCompiler();
                if (!isPrimitiveOrWrapper(returnType) && !String.class.equals(returnType)) {
                    body.append(builder.invoke("withResultType",
                            MapperBuilder.class, constantClass(returnType)));
                }
                body.append(mapper.set(builder.invoke("build", SqlMapper.class).cast(OneSelectMapper.class)));
                final Variable result = scope.createTempVariable(Object.class);
                body.append(result.set(mapper.invoke("selectOne", Object.class,
                        constantString(hql),
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
