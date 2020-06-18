package me.zyee.hibatis.bytecode;

import io.airlift.bytecode.MethodDefinition;
import me.zyee.hibatis.exception.ByteCodeGenerateException;

/**
 * @author yee
 * @version 1.0
 * Create by yee on 2020/6/15
 */
@Deprecated
public interface MethodVisitor {
    /**
     * 生成方法
     *
     * @return
     * @throws ByteCodeGenerateException
     */
    MethodDefinition visit() throws ByteCodeGenerateException;
}
