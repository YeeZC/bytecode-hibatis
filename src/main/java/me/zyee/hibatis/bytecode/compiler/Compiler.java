package me.zyee.hibatis.bytecode.compiler;

import me.zyee.hibatis.exception.HibatisException;

/**
 * @author yee
 * @version 1.0
 * Create by yee on 2020/6/17
 */
public interface Compiler<Node, Ret> {
    /**
     * 编译方法
     *
     * @param node
     * @return
     * @throws HibatisException
     */
    Ret compile(Node node) throws HibatisException;
}
