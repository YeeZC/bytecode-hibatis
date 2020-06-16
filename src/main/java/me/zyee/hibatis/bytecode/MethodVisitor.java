package me.zyee.hibatis.bytecode;

import io.airlift.bytecode.MethodDefinition;
import me.zyee.hibatis.exception.ByteCodeGenerateException;

/**
 * @author yee
 * @version 1.0
 * Create by yee on 2020/6/15
 */
public interface MethodVisitor {
    MethodDefinition visit() throws ByteCodeGenerateException;
}
