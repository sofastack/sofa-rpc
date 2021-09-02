package com.alipay.sofa.rpc.tripleGeneric;

import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Date;

public class TripleGenericInterfaceImpl implements TripleGenericInterface{

    private static final Logger LOGGER = LoggerFactory.getLogger(TripleGenericInterfaceImpl.class);

    @Override
    public TestEntity echoEntity(TestEntity testEntity) {
        LOGGER.info("receive entity: {}", testEntity);
        return testEntity;
    }

    @Override
    public String echoStr(String name) {
        LOGGER.info("receive name: {}", name);
        return name;
    }

    @Override
    public Integer echoInt(Integer age) {
        LOGGER.info("receive age: {}", age);
        return age;
    }

    @Override
    public Date echoDate(Date birth) {
        LOGGER.info("receive birth: {}", birth);
        return birth;
    }

    @Override
    public ArrayList<Integer> testVoidParam() {
        return Lists.newArrayList(1,2,3);
    }

    @Override
    public void testVoidResp() {
        LOGGER.info("TEST SUCESS!!!!！");
    }
}
