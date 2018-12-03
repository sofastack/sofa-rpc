/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alipay.sofa.rpc.test.generic;

import com.alipay.hessian.generic.model.GenericArray;
import com.alipay.hessian.generic.model.GenericCollection;
import com.alipay.hessian.generic.model.GenericMap;
import com.alipay.hessian.generic.model.GenericObject;
import com.alipay.hessian.generic.util.GenericUtils;
import com.alipay.sofa.rpc.api.GenericContext;
import com.alipay.sofa.rpc.api.GenericService;
import com.alipay.sofa.rpc.api.future.SofaResponseFuture;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.MethodConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.test.ActivelyDestroyTest;
import com.alipay.sofa.rpc.test.generic.bean.BasicBean;
import com.alipay.sofa.rpc.test.generic.bean.ComplexBean;
import com.alipay.sofa.rpc.test.generic.bean.Job;
import com.alipay.sofa.rpc.test.generic.bean.MyList;
import com.alipay.sofa.rpc.test.generic.bean.MyMap;
import com.alipay.sofa.rpc.test.generic.bean.People;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author xuanbei
 * @since 2016/07/28
 */
public class GenericTest extends ActivelyDestroyTest {
    @Test
    public void testAll() throws SofaRpcException, InterruptedException {
        // 发布服务

        ServerConfig serverConfig2 = new ServerConfig()
            .setPort(22222)
            .setDaemon(false);

        List<MethodConfig> methodConfigs = new ArrayList<MethodConfig>();
        MethodConfig config1 = new MethodConfig().setName("helloFuture").setInvokeType("future");
        methodConfigs.add(config1);
        MethodConfig config2 = new MethodConfig().setName("helloCallback")
            .setInvokeType("callback").setOnReturn(new TestCallback());
        methodConfigs.add(config2);
        MethodConfig config21 = new MethodConfig().setName("helloCallbackException")
            .setInvokeType("callback").setOnReturn(new TestCallback());
        methodConfigs.add(config21);
        MethodConfig config3 = new MethodConfig().setName("helloOneway")
            .setInvokeType("oneway");
        methodConfigs.add(config3);

        // C服务的服务端
        ProviderConfig<TestInterface> CProvider = new ProviderConfig<TestInterface>()
            .setInterfaceId(TestInterface.class.getName())
            .setRef(new TestClass())
            .setServer(serverConfig2);
        CProvider.export();

        // 引用服务
        ConsumerConfig<GenericService> BConsumer = new ConsumerConfig<GenericService>()
            .setInterfaceId(TestInterface.class.getName())
            .setGeneric(true)
            .setMethods(methodConfigs)
            .setDirectUrl("bolt://127.0.0.1:22222")
            .setTimeout(3000);
        GenericService proxy = BConsumer.refer();

        GenericObject genericObject = new GenericObject(
            "com.alipay.sofa.rpc.test.generic.bean.People");
        genericObject.putField("name", "Lilei");
        genericObject.putField("job", new Job("coder"));
        People people = new People();
        people.setName("Lilei");
        people.setJob(new Job("coder"));

        // sync 调用
        assertEquals(proxy.$invoke("hello",
            new String[] { "com.alipay.sofa.rpc.test.generic.bean.People" },
            new Object[] { people }), new TestClass().hello(people));

        People peopleResult = proxy.$genericInvoke("hello",
            new String[] { "com.alipay.sofa.rpc.test.generic.bean.People" },
            new Object[] { genericObject }, People.class);

        assertEquals(peopleResult, new TestClass().hello(people));

        GenericObject result = (GenericObject) proxy.$genericInvoke("hello",
            new String[] { "com.alipay.sofa.rpc.test.generic.bean.People" },
            new Object[] { genericObject });
        isCorrect(result);

        // future 调用
        proxy.$invoke("helloFuture",
            new String[] { "com.alipay.sofa.rpc.test.generic.bean.People" },
            new Object[] { people });
        assertEquals(SofaResponseFuture.getResponse(1000, true),
            new TestClass().helloFuture(people));

        proxy.$genericInvoke("helloFuture",
            new String[] { "com.alipay.sofa.rpc.test.generic.bean.People" },
            new Object[] { genericObject }, People.class);
        assertEquals(SofaResponseFuture.getResponse(1000, true),
            new TestClass().helloFuture(people));

        proxy.$genericInvoke("helloFuture",
            new String[] { "com.alipay.sofa.rpc.test.generic.bean.People" },
            new Object[] { genericObject });
        result = (GenericObject) SofaResponseFuture.getResponse(1000, true);
        isCorrect(result);

        // callback调用
        proxy.$invoke("helloCallback",
            new String[] { "com.alipay.sofa.rpc.test.generic.bean.People" },
            new Object[] { people });
        TestCallback.startLatach();
        assertEquals(TestCallback.result, new TestClass().helloCallback(people));

        proxy.$genericInvoke("helloCallback",
            new String[] { "com.alipay.sofa.rpc.test.generic.bean.People" },
            new Object[] { genericObject }, People.class);
        TestCallback.startLatach();
        assertEquals(TestCallback.result, new TestClass().helloCallback(people));

        proxy.$genericInvoke("helloCallback",
            new String[] { "com.alipay.sofa.rpc.test.generic.bean.People" },
            new Object[] { genericObject });
        TestCallback.startLatach();
        isCorrect((GenericObject) TestCallback.result);
        TestCallback.result = null;

        // oneway调用
        proxy.$invoke("helloOneway", new String[] { "com.alipay.sofa.rpc.test.generic.bean.People" },
            new Object[] { people });

        proxy.$genericInvoke("helloOneway",
            new String[] { "com.alipay.sofa.rpc.test.generic.bean.People" },
            new Object[] { genericObject }, People.class);

        proxy.$genericInvoke("helloOneway",
            new String[] { "com.alipay.sofa.rpc.test.generic.bean.People" },
            new Object[] { genericObject });

        // callback出现异常
        proxy.$invoke("helloCallbackException",
            new String[] { "com.alipay.sofa.rpc.test.generic.bean.People" },
            new Object[] { people });
        TestCallback.startLatach();
        Assert.assertEquals(((Throwable) TestCallback.result).getMessage(), "Hello~");

        proxy.$genericInvoke("helloCallbackException",
            new String[] { "com.alipay.sofa.rpc.test.generic.bean.People" },
            new Object[] { genericObject }, People.class);
        TestCallback.startLatach();
        Assert.assertEquals(((Throwable) TestCallback.result).getMessage(), "Hello~");

        proxy.$genericInvoke("helloCallbackException",
            new String[] { "com.alipay.sofa.rpc.test.generic.bean.People" },
            new Object[] { genericObject });
        TestCallback.startLatach();
        Assert.assertEquals(((Throwable) TestCallback.result).getMessage(), "Hello~");

        testTimeout(proxy, genericObject, people);
        testComplexBean(proxy);
        testBasicBean(proxy);
    }

    private void testTimeout(final GenericService proxy, GenericObject genericObject, People people) {

        // 1. 构造GenericContext 对象
        GenericContext genericContext = new GenericContext();
        genericContext.setClientTimeout(5000);

        // 2. 未指定参数,发生超时
        boolean isSuccess = true;
        try {
            proxy.$genericInvoke("helloTimeout",
                new String[] { "com.alipay.sofa.rpc.test.generic.bean.People" },
                new Object[] { people });
        } catch (Exception e) {
            isSuccess = false;
        }
        Assert.assertFalse(isSuccess);

        // 3. 指定超时,结果序列化为People 类
        People peopleResult = proxy.$genericInvoke("helloTimeout",
            new String[] { "com.alipay.sofa.rpc.test.generic.bean.People" },
            new Object[] { genericObject }, People.class, genericContext);
        assertEquals(peopleResult, people);

        // 4. 指定超时,结果序列化为GenericObject
        GenericObject result = (GenericObject) proxy.$genericInvoke("helloTimeout",
            new String[] { "com.alipay.sofa.rpc.test.generic.bean.People" },
            new Object[] { genericObject }, genericContext);
        isCorrect(result);

    }

    private void testComplexBean(GenericService proxy) {
        ComplexBean complexBean = createJdkComplexBean();
        Object obj = proxy.$genericInvoke("helloComplexBean",
            new String[] { ComplexBean.class.getName() }, new Object[] { complexBean });
        assertJdkGenericResult(obj);
        obj = GenericUtils.convertToObject(obj);
        assertJdkResult(obj);

        obj = proxy.$genericInvoke("helloComplexBean",
            new String[] { ComplexBean.class.getName() }, new Object[] { complexBean },
            ComplexBean.class);
        assertJdkResult(obj);
        obj = GenericUtils.convertToGenericObject(obj);
        assertJdkGenericResult(obj);

        complexBean = createMyComplexBean();
        obj = proxy.$genericInvoke("helloComplexBean",
            new String[] { ComplexBean.class.getName() }, new Object[] { complexBean });
        assertMyGenericResult(obj);
        obj = GenericUtils.convertToObject(obj);
        assertMyComplexBeanResult(obj);

        obj = proxy.$genericInvoke("helloComplexBean",
            new String[] { ComplexBean.class.getName() }, new Object[] { complexBean },
            ComplexBean.class);
        assertMyComplexBeanResult(obj);
        obj = GenericUtils.convertToGenericObject(obj);
        assertMyGenericResult(obj);
    }

    private void testBasicBean(GenericService proxy) {
        BasicBean basicBean = new BasicBean((short) 12, new Short((short) 32), 21, new Integer(43),
            (byte) 12, new Byte((byte) 13), 1274646l, 873763l, (float) 1456.9877,
            (float) 1456.9877, 82837.93883, 82837.88, true, false);
        Object obj = proxy.$genericInvoke("helloBasicBean",
            new String[] { BasicBean.class.getName() }, new Object[] { basicBean });
        obj = GenericUtils.convertToObject(obj);
        assertEquals(obj, basicBean);

        obj = proxy.$genericInvoke("helloBasicBean", new String[] { BasicBean.class.getName() },
            new Object[] { basicBean }, BasicBean.class);
        assertEquals(obj, basicBean);
        obj = GenericUtils.convertToGenericObject(obj);
        assertGenericBasicBean(obj);
    }

    private void assertGenericBasicBean(Object go) {
        assertEquals(go.getClass(), GenericObject.class);
        GenericObject genericObject = (GenericObject) go;
        assertEquals(BasicBean.class.getName(), genericObject.getType());
        BasicBean bb = new BasicBean((short) 12, new Short((short) 32), 21, new Integer(43),
            (byte) 12, new Byte((byte) 13), 1274646l, 873763l, (float) 1456.9877,
            (float) 1456.9877, 82837.93883, 82837.88, true, false);
        assertEquals(bb.getB(), genericObject.getField("b"));
        assertEquals(bb.getBb(), genericObject.getField("bb"));
        assertEquals(bb.getS(), genericObject.getField("s"));
        assertEquals(bb.getSs(), genericObject.getField("ss"));
        assertEquals(bb.getF(), genericObject.getField("f"));
        assertEquals(bb.getFf(), genericObject.getField("ff"));
        assertEquals(bb.getD(), genericObject.getField("d"));
        assertEquals(bb.getDd(), genericObject.getField("dd"));
        assertEquals(bb.getL(), genericObject.getField("l"));
        assertEquals(bb.getLl(), genericObject.getField("ll"));
        assertEquals(bb.getI(), genericObject.getField("i"));
        assertEquals(bb.getIi(), genericObject.getField("ii"));
        assertEquals(bb.isBo(), true);
        assertEquals(bb.getBbo(), false);
    }

    /**
     * 构建jdk对象构成bean
     */
    private ComplexBean createJdkComplexBean() {

        ComplexBean complexBean = new ComplexBean();

        complexBean.setStrs(new String[] { "123", "321", null });
        complexBean.setClazz(ArrayList.class);
        complexBean.setJobs(new Job[] { null, new Job("coder"), null });
        ArrayList list = new ArrayList();
        list.add(null);
        list.add(1);
        list.add(null);
        complexBean.setList(list);

        return complexBean;
    }

    /**
     * 构建自定义扩展对象构成bean
     */
    private ComplexBean createMyComplexBean() {

        ComplexBean complexBean = new ComplexBean();

        complexBean.setStrs(new String[] { null, "123", null });
        complexBean.setClazz(MyList.class);
        complexBean.setJobs(new Job[] { null, new Job("coder"), null });
        ArrayList list = new MyList();
        list.add(null);
        list.add(1);
        list.add(null);
        complexBean.setList(list);
        MyMap map = new MyMap();
        map.put("1", new Job("coder"));
        map.put(1, new People("wang", null));
        complexBean.setMap(map);

        return complexBean;
    }

    private void assertJdkGenericResult(Object obj) {

        ComplexBean complexBean = createJdkComplexBean();

        Assert.assertEquals(GenericObject.class, obj.getClass());
        GenericObject gb = (GenericObject) obj;
        Assert.assertEquals(gb.getType(), ComplexBean.class.getName());
        assertArrayEquals(gb.getField("strs"), complexBean.getStrs());
        Assert.assertEquals(gb.getField("jobs").getClass(), GenericArray.class);
        assertArrayEquals(GenericUtils.convertToObject(gb.getField("jobs")), complexBean.getJobs());
        Assert.assertEquals(gb.getField("list").getClass(), ArrayList.class);
        assertEquals(gb.getField("list"), complexBean.getList());
        assertEquals(gb.getField("map"), complexBean.getMap());
    }

    private void assertJdkResult(Object obj) {
        Assert.assertEquals(ComplexBean.class, obj.getClass());
        ComplexBean cx1 = createJdkComplexBean();
        ComplexBean cx2 = (ComplexBean) obj;
        assertArrayEquals(cx1.getStrs(), cx2.getStrs());
        assertArrayEquals(cx1.getJobs(), cx2.getJobs());
        assertEquals(cx1.getList(), cx2.getList());
        assertEquals(cx1.getMap(), cx2.getMap());
        assertEquals(cx1.getList().getClass(), cx2.getList().getClass());
        assertEquals(cx1.getClass(), cx2.getClass());
    }

    private void assertMyGenericResult(Object obj) {
        ComplexBean complexBean = createMyComplexBean();

        Assert.assertEquals(GenericObject.class, obj.getClass());
        GenericObject gb = (GenericObject) obj;
        Assert.assertEquals(gb.getType(), ComplexBean.class.getName());
        assertArrayEquals(gb.getField("strs"), complexBean.getStrs());
        Assert.assertEquals(gb.getField("jobs").getClass(), GenericArray.class);
        assertArrayEquals(GenericUtils.convertToObject(gb.getField("jobs")), complexBean.getJobs());
        Assert.assertEquals(gb.getField("list").getClass(), GenericCollection.class);
        Assert.assertEquals(GenericUtils.convertToObject(gb.getField("list")).getClass(),
            MyList.class);
        assertEquals(GenericUtils.convertToObject(gb.getField("list")),
            complexBean.getList());
        Assert.assertEquals(gb.getField("map").getClass(), GenericMap.class);
        Assert.assertEquals(GenericUtils.convertToObject(gb.getField("map")).getClass(),
            MyMap.class);
        assertEquals(GenericUtils.convertToObject(gb.getField("map")), complexBean.getMap());
    }

    private void assertMyComplexBeanResult(Object obj) {
        Assert.assertEquals(ComplexBean.class, obj.getClass());
        ComplexBean cx1 = createMyComplexBean();
        ComplexBean cx2 = (ComplexBean) obj;
        assertArrayEquals(cx1.getStrs(), cx2.getStrs());
        assertArrayEquals(cx1.getJobs(), cx2.getJobs());
        assertEquals(cx1.getList(), cx2.getList());
        assertEquals(cx1.getMap(), cx2.getMap());
        assertEquals(cx1.getList().getClass(), cx2.getList().getClass());
        assertEquals(cx1.getMap().getClass(), cx2.getMap().getClass());
        assertEquals(cx1.getClass(), cx2.getClass());
    }

    private void assertArrayEquals(Object obj1, Object obj2) {
        Object[] left = (Object[]) obj1;
        Object[] right = (Object[]) obj2;
        Assert.assertEquals(left.length, right.length);
        Assert.assertEquals(obj1.getClass(), obj2.getClass());
        for (int i = 0; i < left.length; i++) {
            Assert.assertEquals(left[i], right[i]);
        }
    }

    private void isCorrect(GenericObject result) {
        Assert.assertEquals(result.getType(), "com.alipay.sofa.rpc.test.generic.bean.People");
        Assert.assertEquals(result.getField("name"), "Lilei");
        GenericObject genericObject = (GenericObject) result.getField("job");
        Assert.assertEquals(genericObject.getType(), "com.alipay.sofa.rpc.test.generic.bean.Job");
        Assert.assertEquals(genericObject.getField("name"), "coder");
    }
}
