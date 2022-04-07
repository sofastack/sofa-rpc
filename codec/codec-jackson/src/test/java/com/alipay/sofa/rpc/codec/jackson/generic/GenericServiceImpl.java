package com.alipay.sofa.rpc.codec.jackson.generic;

/**
 * @author gujin
 * Created on 2022/4/7 8:57 下午
 */
public class GenericServiceImpl implements GenericService<MyReq, MyResp> {

    @Override
    public MyResp hello(MyReq myReq) {
        return null;
    }
}
