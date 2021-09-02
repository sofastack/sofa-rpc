package com.alipay.sofa.rpc.invoke.genericJson;

import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;

import java.util.Date;

public class IGenericServiceImpl implements IGenericService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IGenericServiceImpl.class);

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
    public String testVoidParam() {
        LOGGER.info("receive void param");
        return "testVoid success!";
    }

    @Override
    public void testVoidResp(){
        LOGGER.info("server test void resp success!!!");
    }
}
