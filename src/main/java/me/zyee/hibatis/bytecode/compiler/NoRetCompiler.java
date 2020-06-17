package me.zyee.hibatis.bytecode.compiler;

import me.zyee.hibatis.exception.HibatisException;

/**
 * @author yee
 * @version 1.0
 * Create by yee on 2020/6/17
 */
public interface NoRetCompiler<Node> extends Compiler<Node, Void> {
    void doCompile(Node node) throws HibatisException;

    @Override
    default Void compile(Node node) throws HibatisException {
        doCompile(node);
        return null;
    }
}
