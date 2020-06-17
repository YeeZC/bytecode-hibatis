package me.zyee.hibatis.bytecode.compiler.impl;

import io.airlift.bytecode.Access;
import io.airlift.bytecode.BytecodeBlock;
import io.airlift.bytecode.ClassDefinition;
import io.airlift.bytecode.FieldDefinition;
import io.airlift.bytecode.MethodDefinition;
import io.airlift.bytecode.Parameter;
import io.airlift.bytecode.Scope;
import io.airlift.bytecode.expression.BytecodeExpressions;
import me.zyee.hibatis.bytecode.compiler.NoRetCompiler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * @author yee
 * @version 1.0
 * Create by yee on 2020/6/17
 */
public class ConstructorCompiler implements NoRetCompiler<ConstructorCompiler.Context> {
    private static final BiConsumer<ClassDefinition, Parameter[]> COMPILER = (definition, parameters) -> {

        final MethodDefinition newInstance = definition.declareMethod(Access.a(Access.PUBLIC, Access.STATIC),
                "newInstance", definition.getType(), parameters);
        newInstance.getBody()
                .append(BytecodeExpressions.newInstance(definition.getType(), parameters))
                .retObject();
    };

    @Override
    public void doCompile(Context context) {
        final Parameter[] parameters = context.getParameters();
        final ClassDefinition classDefinition = context.getClassDefinition();

        if (parameters.length == 0) {
            COMPILER.accept(classDefinition.declareDefaultConstructor(Access.a(Access.PRIVATE)), parameters);
        } else {
            final MethodDefinition constructor = classDefinition.declareConstructor(Access.a(Access.PRIVATE),
                    parameters);

            final BytecodeBlock block = constructor.getBody().append(constructor.getThis())
                    .invokeConstructor(Object.class);
            final Scope scope = constructor.getScope();
            final List<FieldDefinition> fields = classDefinition.getFields();
            for (FieldDefinition field : fields) {
                Optional.ofNullable(context.getFieldMapParam().apply(field))
                        .ifPresent(i -> block.append(scope.getThis().setField(field, parameters[i])));
            }
            block.ret();

            COMPILER.accept(classDefinition, parameters);
        }

    }

    public static class Context {
        private final ClassDefinition classDefinition;
        private final List<Parameter> parameters;
        private Function<FieldDefinition, Integer> fieldMapParam;

        private Context(ClassDefinition classDefinition) {
            this.classDefinition = classDefinition;
            this.parameters = new ArrayList<>();
        }

        public Context append(Parameter parameter) {
            this.parameters.add(parameter);
            return this;
        }

        public Context with(Function<FieldDefinition, Integer> field2ParamIdx) {
            this.fieldMapParam = field2ParamIdx;
            return this;
        }

        public static Context newInstance(ClassDefinition classDefinition) {
            return new Context(classDefinition);
        }

        public ClassDefinition getClassDefinition() {
            return classDefinition;
        }

        public Parameter[] getParameters() {
            return parameters.toArray(new Parameter[0]);
        }

        public Function<FieldDefinition, Integer> getFieldMapParam() {
            return Optional.ofNullable(fieldMapParam).orElse(field -> null);
        }
    }
}
