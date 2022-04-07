package com.alipay.sofa.rpc.codec.jackson.generic;

/**
 * @author gujin
 * Created on 2022/4/7 8:57 下午
 */
public interface GenericService<T, R> {

    public R hello(T t);

}
