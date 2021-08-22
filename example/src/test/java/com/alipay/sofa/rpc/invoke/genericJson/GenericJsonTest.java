package com.alipay.sofa.rpc.invoke.genericJson;

import com.alipay.sofa.rpc.api.GenericService;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import org.junit.Before;
import org.junit.Test;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class GenericJsonTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenericJsonTest.class);

    private GenericService testService;

    @Before
    public void init(){
        ApplicationConfig applicationServerConfig = new ApplicationConfig().setAppName("generic-server");
        ApplicationConfig applicationClientConfig = new ApplicationConfig().setAppName("generic-client");

        ServerConfig serverConfig = new ServerConfig()
                .setPort(22222)
                .setDaemon(false);

        ProviderConfig<IGenericService> providerConfig = new ProviderConfig<IGenericService>()
                .setApplication(applicationServerConfig)
                .setInterfaceId(IGenericService.class.getName())
                .setRef(new IGenericServiceImpl())
                .setServer(serverConfig);
        providerConfig.export();

        ConsumerConfig<GenericService> consumerConfig = new ConsumerConfig<GenericService>()
                .setApplication(applicationClientConfig)
                .setInterfaceId(IGenericService.class.getName())
                .setGeneric(true)
                .setTimeout(50000)
                .setDirectUrl("bolt://127.0.0.1:22222?appName=generic-server");
        testService = consumerConfig.refer();

        LOGGER.warn("started at pid {}", RpcRuntimeContext.PID);
    }

    //测试已知传入参数类型和返回类型
    @Test
    public void testInvoke(){
        try {
            String name = (String) testService.$invoke("echoStr", new String[] { "java.lang.String" },
                    new Object[] { "zhangsan" });
            LOGGER.warn("generic return name:{}", name);

            Integer age = (Integer) testService.$invoke("echoInt", new String[] { "java.lang.Integer" },
                    new Object[] { "23" });
            LOGGER.warn("generic return age:{}", age);

            Date date = new Date();

            Date birth = (Date) testService.$invoke("echoDate", new String[] { "java.util.Date" },
                    new Object[] { date });
            LOGGER.warn("generic return birth:{}", birth);
        }catch (Exception e){
            LOGGER.error(e.getMessage(), e);
        }
    }

    //测试传入类型和返回类型均未知
    @Test
    public void testGenericInvoke1(){
        try{
            Map<String, String> innerObject = new HashMap<>();
            innerObject.put("add", "xiafeilu89");

            Map<String, Object> genericObject = new HashMap<>();
            genericObject.put("name", "zhangsan");
            genericObject.put("age", 23);
            genericObject.put("birth", new Date());
            genericObject.put("inner", innerObject);


            Object o =  testService.$genericInvoke("echoEntity",
                    new String[] { "com.alipay.sofa.rpc.invoke.genericJson.TestEntity" },
                    new Object[] { genericObject });
            LOGGER.warn("generic return :{}", o);
        }catch (Exception e){
            LOGGER.error(e.getMessage(), e);
        }
    }

    //测试未知传入类型，已知返回类型
    @Test
    public void testGenericInvoke2(){
        try{
            Map<String, String> innerObject = new HashMap<>();
            innerObject.put("add", "xiafeilu89");

            Map<String, Object> genericObject = new HashMap<>();
            genericObject.put("name", "zhangsan");
            genericObject.put("age", 23);
            genericObject.put("birth", new Date());
            genericObject.put("inner", innerObject);

            TestEntity o =  testService.$genericInvoke("echoEntity",
                    new String[] { "com.alipay.sofa.rpc.invoke.genericJson.TestEntity" },
                    new Object[] { genericObject },
                    TestEntity.class);
            LOGGER.warn("generic return :{}", o);
        }catch (Exception e){
            LOGGER.error(e.getMessage(), e);
        }
    }

    //测试已知传入类型未知返回类型
    @Test
    public void testGenericInvoke3(){
        try{
            TestEntity.Inner inner = new TestEntity.Inner();
            inner.setAdd("xiafeilu89");

            TestEntity testEntity = new TestEntity();
            testEntity.setAge(23);
            testEntity.setName("zhangsan");
            testEntity.setBirth(new Date());
            testEntity.setInner(inner);

            Object o =  testService.$genericInvoke("echoEntity",
                    new String[] { "com.alipay.sofa.rpc.invoke.genericJson.TestEntity" },
                    new Object[] { testEntity });
            LOGGER.warn("generic return :{}", o);
        }catch (Exception e){
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Test
    public void testVoid(){
        Object result = testService.$invoke("testVoid", new String[]{}, new Object[] {});
        System.out.println(result);
    }
}
