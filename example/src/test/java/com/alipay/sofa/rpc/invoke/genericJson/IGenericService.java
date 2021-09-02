package com.alipay.sofa.rpc.invoke.genericJson;

import java.util.Date;

public interface IGenericService {

    TestEntity echoEntity(TestEntity testEntity);

    String echoStr(String name);

    Integer echoInt(Integer age);

    Date echoDate(Date birth);

    String testVoidParam();

    void testVoidResp();
}
