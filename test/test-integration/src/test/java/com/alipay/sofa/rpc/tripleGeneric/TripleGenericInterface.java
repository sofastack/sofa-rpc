package com.alipay.sofa.rpc.tripleGeneric;

import java.util.ArrayList;
import java.util.Date;

public interface TripleGenericInterface {

    TestEntity echoEntity(TestEntity testEntity);

    String echoStr(String name);

    Integer echoInt(Integer age);

    Date echoDate(Date birth);

    ArrayList<Integer> testVoidParam();

    void  testVoidResp();
}
