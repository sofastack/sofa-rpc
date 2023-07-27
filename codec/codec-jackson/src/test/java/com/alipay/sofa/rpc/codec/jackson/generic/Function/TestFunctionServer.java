package com.alipay.sofa.rpc.codec.jackson.generic.Function;

public abstract class TestFunctionServer<T,R> extends FunctionService<T,R>{

    @Override
    public R apply(T t) {
        return this.process(t);
    }

    public abstract R process(T t);
}
