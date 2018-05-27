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
package com.alipay.sofa.rpc.codec.sofahessian;

import com.alipay.hessian.ClassNameResolver;
import com.alipay.hessian.NameBlackListFilter;
import com.alipay.sofa.rpc.common.SofaOptions;
import com.alipay.sofa.rpc.common.struct.UnsafeByteArrayInputStream;
import com.alipay.sofa.rpc.common.struct.UnsafeByteArrayOutputStream;
import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.caucho.hessian.io.SerializerFactory;
import com.sun.test.TestBean;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class BlackListFileLoaderTest {
    @Test
    public void testAll() throws Exception {

        List<String> blacks = BlackListFileLoader.SOFA_SERIALIZE_BLACK_LIST;
        Assert.assertNotNull(blacks);

        String s = System.getProperty(SofaOptions.CONFIG_SERIALIZE_BLACKLIST_OVERRIDE);
        try {
            System.setProperty(SofaOptions.CONFIG_SERIALIZE_BLACKLIST_OVERRIDE, "-java.net.Socket");
            blacks = BlackListFileLoader.loadFile("/sofa-rpc/serialize_blacklist.txt");
        } finally {
            if (s != null) {
                System.setProperty(SofaOptions.CONFIG_SERIALIZE_BLACKLIST_OVERRIDE, s);
            }
        }

        NameBlackListFilter filter = new NameBlackListFilter(blacks);
        boolean pass = true;
        String className = null;
        try {
            className = filter.resolve("com.alipay.xx");
        } catch (Exception e) {
            pass = false;
        }
        Assert.assertNotNull(className);
        Assert.assertTrue(pass);

        pass = true;
        try {
            className = filter.resolve("java.lang.Thread");
        } catch (Exception e) {
            pass = false;
        }
        Assert.assertFalse(pass);

        pass = true;
        try {
            className = filter.resolve("java.net.Socket");
        } catch (Exception e) {
            pass = false;
        }
        Assert.assertTrue(pass);
    }

    @Test
    public void overrideBlackList() {
        List<String> origin = buildOriginList();
        BlackListFileLoader.overrideBlackList(origin, "-*");
        Assert.assertTrue(origin.size() == 0);
        origin = buildOriginList();
        BlackListFileLoader.overrideBlackList(origin, "!*");
        Assert.assertTrue(origin.size() == 0);
        origin = buildOriginList();
        BlackListFileLoader.overrideBlackList(origin, "-default");
        Assert.assertTrue(origin.size() == 0);
        origin = buildOriginList();
        BlackListFileLoader.overrideBlackList(origin, "!default");
        Assert.assertTrue(origin.size() == 0);

        origin = buildOriginList();
        BlackListFileLoader.overrideBlackList(origin, "-*,-com.xxx");
        Assert.assertTrue(origin.size() == 0);

        origin = buildOriginList();
        BlackListFileLoader.overrideBlackList(origin, "aaa,-*,-com.xxx");
        Assert.assertTrue(origin.size() == 1);
        origin = buildOriginList();
        BlackListFileLoader.overrideBlackList(origin, "-*,aaa");
        Assert.assertTrue(origin.size() == 1);

        origin = buildOriginList();
        BlackListFileLoader.overrideBlackList(origin, "-com.xxx");
        Assert.assertTrue(origin.size() == 2);

        origin = buildOriginList();
        BlackListFileLoader.overrideBlackList(origin, "-com.xxx,-com.yyy");
        Assert.assertTrue(origin.size() == 1);

        origin = buildOriginList();
        BlackListFileLoader.overrideBlackList(origin, "com.xxx,-com.yyy");
        Assert.assertTrue(origin.size() == 2);
        origin = buildOriginList();
        BlackListFileLoader.overrideBlackList(origin, "com.aaa,-com.yyy");
        Assert.assertTrue(origin.size() == 3);
        origin = buildOriginList();
        BlackListFileLoader.overrideBlackList(origin, "com.aaa");
        Assert.assertTrue(origin.size() == 4);
        origin = buildOriginList();
        BlackListFileLoader.overrideBlackList(origin, "com.xxx;com.yyy;com.zzz");
        Assert.assertTrue(origin.size() == 3);
        origin = buildOriginList();
        BlackListFileLoader.overrideBlackList(origin, "com.aaa,com.bbb,com.ccc");
        Assert.assertTrue(origin.size() == 6);
    }

    private List<String> buildOriginList() {
        List<String> origin = new ArrayList<String>();
        origin.add("com.xxx");
        origin.add("com.yyy");
        origin.add("com.zzz");
        return origin;
    }

    @Test
    public void testInBlack() {
        SerializerFactory serializerFactory = new SerializerFactory();
        ClassNameResolver resolver = new ClassNameResolver();
        resolver.addFilter(new NameBlackListFilter(
            BlackListFileLoader.SOFA_SERIALIZE_BLACK_LIST, 8192));
        serializerFactory.setClassNameResolver(resolver);

        UnsafeByteArrayOutputStream stream = new UnsafeByteArrayOutputStream();
        Hessian2Output output = new Hessian2Output(stream);
        output.setSerializerFactory(serializerFactory);
        boolean pass = true;
        try {
            output.writeObject(new TestBean());
            output.close();
        } catch (IOException e) {
            pass = false;
            Assert.assertTrue(e.getMessage().contains("black"));
        }
        Assert.assertFalse(pass);

        byte[] bs = new byte[] { 79, -91, 99, 111, 109, 46, 115, 117, 110, 46, 116, 101, 115, 116, 46, 84, 101, 115,
                116, 66, 101, 97, 110, -111, 4, 110, 97, 109, 101, 111, -112, 78 };
        UnsafeByteArrayInputStream inputStream = new UnsafeByteArrayInputStream(bs);
        Hessian2Input input = new Hessian2Input(inputStream);
        input.setSerializerFactory(serializerFactory);
        pass = true;
        try {
            TestBean bean = (TestBean) input.readObject(TestBean.class);
        } catch (IOException e) {
            pass = false;
            Assert.assertTrue(e.getMessage().contains("black"));
        }
        Assert.assertFalse(pass);

        inputStream = new UnsafeByteArrayInputStream(bs);
        input = new Hessian2Input(inputStream);
        input.setSerializerFactory(serializerFactory);
        pass = true;
        try {
            TestBean bean = (TestBean) input.readObject();
        } catch (IOException e) {
            pass = false;
            Assert.assertTrue(e.getMessage().contains("black"));
        }
        Assert.assertFalse(pass);
    }

}