package com.alipay.sofa.rpc.codec.jackson.generic.Function;

import java.util.function.Function;

public class FunctionService<T,R> implements Function<T,R> {
    @Override
    public R apply(T t) {
        return null;
    }
}
