package me.zyee.hibatis.bytecode.impl;

import io.airlift.bytecode.BytecodeBlock;
import io.airlift.bytecode.ClassDefinition;
import io.airlift.bytecode.MethodDefinition;
import io.airlift.bytecode.ParameterizedType;
import io.airlift.bytecode.Scope;
import io.airlift.bytecode.Variable;
import io.airlift.bytecode.expression.BytecodeExpression;
import io.airlift.bytecode.expression.BytecodeExpressions;
import me.zyee.hibatis.dao.DaoInfo;
import me.zyee.hibatis.dao.DaoMethodInfo;
import org.apache.commons.lang3.ClassUtils;
import org.hibernate.query.Query;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.transform.Transformers;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

/**
 * @author yee
 * Created by yee on 2020/6/12
 **/
public class DefaultDaoMethodVisitor {
    private BytecodeBlock body;
    private Scope scope;
    private DaoInfo info;

    /**
     * 生成接口方法的实现
     *
     * @param classDefinition
     * @param method
     * @param daoInfo
     * @return
     * @throws NoSuchMethodException
     * @throws ClassNotFoundException
     */
    public MethodDefinition visit(ClassDefinition classDefinition, Method method, DaoMethodInfo daoInfo) throws NoSuchMethodException, ClassNotFoundException {
//        if (daoInfo.isNativeSql()) {
//            return new SQLMethodVisitor(classDefinition, method, daoInfo, info.getMaps()).visit();
//        } else {
//            return new HQLMethodVisitor(classDefinition, method, daoInfo, info.getMaps()).visit();
//        }
//        // 实现方法：4 处理返回值
//        if (daoInfo.getType() == MethodType.SELECT) {
//            visitSelect(daoInfo, query, method.getReturnType(), daoInfo.isNativeSql());
//        } else {
//            body.append(query.invoke("executeUpdate", int.class));
//            if (ClassUtils.isAssignable(void.class, method.getReturnType())) {
//                body.ret();
//            } else {
//                body.retInt();
//            }
//        }
        return null;
    }

    private void visitSelect(DaoMethodInfo info, Variable query, Class<?> returnType, boolean nativeSql) throws NoSuchMethodException, ClassNotFoundException {
        if (nativeSql) {
            visitNativeSelect(info, query, returnType);
        } else {
            if (ClassUtils.isAssignable(Collection.class, returnType) || ClassUtils.isAssignable(returnType, Collection.class)) {
                body.append(query.invoke("getResultList", List.class));
            } else if (returnType.isArray()) {
                final Method toArray = List.class.getDeclaredMethod("toArray", Object[].class);
                body.append(query.invoke("getResultList", List.class).invoke(toArray,
                        BytecodeExpressions.newArray(ParameterizedType.type(returnType.getComponentType()), 0)));
            } else {
                body.append(query.invoke("getSingleResult", Object.class).cast(returnType));
            }
            body.ret(returnType);
        }
    }

    private void visitNativeSelect(DaoMethodInfo info, Variable query, Class<?> returnType) throws NoSuchMethodException, ClassNotFoundException {
        Method transformer = Transformers.class.getDeclaredMethod("aliasToBean", Class.class);
        Method setTrans = Query.class.getDeclaredMethod("setResultTransformer", ResultTransformer.class);
        if (ClassUtils.isAssignable(Collection.class, returnType) || ClassUtils.isAssignable(returnType, Collection.class)) {
            final Class<?> returnItem = info.getResultType();
            if (null != returnItem) {
                body.append(query.invoke(setTrans,
                        BytecodeExpressions.invokeStatic(transformer,
                                BytecodeExpressions.constantClass(returnItem))));
            } else {
                body.append(query.invoke(setTrans,
                        BytecodeExpressions.invokeStatic(transformer,
                                scope.getThis().getField("entityClass", Class.class))));
            }
            body.append(query.invoke("getResultList", List.class));
            body.retObject();
        } else if (returnType.isArray()) {
            body.append(query.invoke(setTrans,
                    BytecodeExpressions.invokeStatic(transformer,
                            BytecodeExpressions.constantClass(returnType.getComponentType()))));
            final Method toArray = List.class.getDeclaredMethod("toArray", Object[].class);
            body.append(query.invoke("getResultList", List.class).invoke(toArray,
                    BytecodeExpressions.newArray(ParameterizedType.type(returnType.getComponentType()), 0)));
            body.retObject();
        } else {
            if (!ClassUtils.isPrimitiveOrWrapper(returnType)) {
                body.append(query.invoke(setTrans,
                        BytecodeExpressions.invokeStatic(transformer,
                                BytecodeExpressions.constantClass(returnType))));
            }

            body.append(visitSingleReturnType(query.invoke("getSingleResult", Object.class), returnType));
            body.ret(returnType);
        }
    }

    private BytecodeExpression visitSingleReturnType(BytecodeExpression expression, Class<?> returnType) {
        if (ClassUtils.isPrimitiveOrWrapper(returnType)) {
            if (ClassUtils.isAssignable(int.class, returnType) || ClassUtils.isAssignable(Integer.class, returnType)) {
                return expression.cast(Number.class).invoke("intValue", int.class);
            } else if (ClassUtils.isAssignable(long.class, returnType) || ClassUtils.isAssignable(Long.class, returnType)) {
                return expression.cast(Number.class).invoke("longValue", long.class);
            } else if (ClassUtils.isAssignable(float.class, returnType) || ClassUtils.isAssignable(Float.class, returnType)) {
                return expression.cast(Number.class).invoke("floatValue", float.class);
            } else if (ClassUtils.isAssignable(double.class, returnType) || ClassUtils.isAssignable(Double.class, returnType)) {
                return expression.cast(Number.class).invoke("doubleValue", double.class);
            } else if (ClassUtils.isAssignable(byte.class, returnType) || ClassUtils.isAssignable(Byte.class, returnType)) {
                return expression.cast(Number.class).invoke("byteValue", byte.class);
            } else if (ClassUtils.isAssignable(short.class, returnType) || ClassUtils.isAssignable(Short.class, returnType)) {
                return expression.cast(Number.class).invoke("shortValue", short.class);
            } else {
                return expression;
            }
        } else {
            return expression.cast(returnType);
        }
    }
}
