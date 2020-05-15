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
package com.alipay.sofa.rpc.ext;

import com.alipay.sofa.rpc.context.RpcRunningState;
import com.alipay.sofa.rpc.filter.Filter;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.protocol.Protocol;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class ExtensionLoaderTest {

    /** Logger for ExtensionLoaderTest **/
    private static final Logger LOGGER = LoggerFactory.getLogger(ExtensionLoaderTest.class);

    @Before
    public void before() throws NoSuchFieldException, IllegalAccessException {
        Field loaderMap = ExtensionLoaderFactory.class.getDeclaredField("LOADER_MAP");
        loaderMap.setAccessible(true);
        ConcurrentMap<Class, ExtensionLoader> map = ((ConcurrentMap<Class, ExtensionLoader>) loaderMap.get(null));
        map.clear();

    }

    @Test
    public void testInit() throws Exception {
        boolean error = false;
        try {
            ExtensionLoader loader = new ExtensionLoader(null, false, null);
        } catch (Exception e) {
            error = true;
        }
        Assert.assertTrue(error);

        Field field = RpcRunningState.class.getDeclaredField("shuttingDown");
        field.setAccessible(true);
        boolean origin = (Boolean) field.get(null);
        try {
            field.set(null, true);
            ExtensionLoader loader = new ExtensionLoader(Filter.class, null);
            Assert.assertNull(loader.getAllExtensions());
        } finally {
            field.set(null, origin);
        }

        error = false;
        try {
            ExtensionLoader loader = new ExtensionLoader(NotExtensible.class, false, null);
        } catch (Exception e) {
            error = true;
        }
        Assert.assertTrue(error);

    }

    @Test
    public void testLoadFromFile() throws Exception {
        ExtensionLoader loader = new ExtensionLoader<Filter>(Filter.class, false, null);
        loader.loadFromFile("META-INF/ext1/");
        Assert.assertTrue(loader.getAllExtensions().isEmpty());

        // test right extension
        loader = new ExtensionLoader<Filter>(Filter.class, false, null);
        loader.loadFromFile("META-INF/ext2/");
        Assert.assertFalse(loader.getAllExtensions().isEmpty());
        ExtensionClass extensionClass = loader.getExtensionClass("rightxx0");
        Assert.assertNotNull(extensionClass);
        Assert.assertTrue(extensionClass.getOrder() == 123);

        loader = new ExtensionLoader<Filter>(Filter.class, false, new TestErrorLoadListener());
        loader.loadFromFile("META-INF/ext3/");
        Assert.assertFalse(loader.getAllExtensions().isEmpty());
        extensionClass = loader.getExtensionClass("rightxx0");
        Assert.assertNotNull(extensionClass);
        extensionClass = loader.getExtensionClass("rightxx1");
        Assert.assertNotNull(extensionClass);
        Assert.assertTrue(extensionClass.getOrder() == 128);

        loader = new ExtensionLoader<Filter>(Filter.class, false, new TestLoadListener());
        loader.loadFromFile("META-INF/ext4/");
        Assert.assertFalse(loader.getAllExtensions().isEmpty());
        extensionClass = loader.getExtensionClass("rightxx0");
        Assert.assertNotNull(extensionClass);
        Assert.assertTrue(extensionClass.getOrder() == 123);
        extensionClass = loader.getExtensionClass("rightxx1");
        Assert.assertNotNull(extensionClass);
        Assert.assertTrue(extensionClass.getOrder() == 128);

    }

    @Test
    public void testOverride() throws Exception {
        // test for extension override
        ExtensionLoader loader = new ExtensionLoader<Filter>(Filter.class, false, null);
        URL url = Filter.class.getResource("/META-INF/sofa-rpc/" + Filter.class.getName());
        boolean error = false;
        try {
            loader.readLine(url, "com.alipay.sofa.rpc.ext.OverrideFilter0");
            loader.readLine(url, "com.alipay.sofa.rpc.ext.OverrideFilter0");
        } catch (Throwable t) {
            error = true;
            LOGGER.error(t.getMessage());
        }
        Assert.assertTrue(error);
        Assert.assertFalse(loader.all.isEmpty());
        Assert.assertTrue(loader.all.size() == 1);

        error = false;
        loader.all.clear();
        try {
            loader.readLine(url, "com.alipay.sofa.rpc.ext.OverrideFilter0");
            loader.readLine(url, "com.alipay.sofa.rpc.ext.OverrideFilter3");
        } catch (Throwable t) {
            error = true;
            LOGGER.error(t.getMessage());
        }
        Assert.assertTrue(error);
        Assert.assertFalse(loader.all.isEmpty());
        Assert.assertTrue(loader.all.size() == 1);

        error = false;
        loader.all.clear();
        try {
            loader.readLine(url, "com.alipay.sofa.rpc.ext.OverrideFilter0");
            loader.readLine(url, "com.alipay.sofa.rpc.ext.OverrideFilter1");
            loader.readLine(url, "com.alipay.sofa.rpc.ext.OverrideFilter2");
        } catch (Throwable t) {
            error = true;
            LOGGER.error(t.getMessage());
        }
        Assert.assertFalse(error);
        Assert.assertFalse(loader.all.isEmpty());
        Assert.assertTrue(loader.all.size() == 1);
        ExtensionClass extensionClass = loader.getExtensionClass("ooo");
        Assert.assertTrue(extensionClass.getClazz() == OverrideFilter1.class);
        Assert.assertTrue(extensionClass.getOrder() == 2);

        error = false;
        loader.all.clear();
        try {
            loader.readLine(url, "com.alipay.sofa.rpc.ext.OverrideFilter1");
            loader.readLine(url, "com.alipay.sofa.rpc.ext.OverrideFilter0");
        } catch (Throwable t) {
            error = true;
            LOGGER.error(t.getMessage());
        }
        Assert.assertFalse(error);
        Assert.assertFalse(loader.all.isEmpty());
        Assert.assertTrue(loader.all.size() == 1);
        extensionClass = loader.getExtensionClass("ooo");
        Assert.assertTrue(extensionClass.getClazz() == OverrideFilter1.class);
        Assert.assertTrue(extensionClass.getOrder() == 2);

        error = false;
        loader.all.clear();
        try {
            loader.readLine(url, "com.alipay.sofa.rpc.ext.OverrideFilter1");
            loader.readLine(url, "com.alipay.sofa.rpc.ext.OverrideFilter0");
            loader.readLine(url, "com.alipay.sofa.rpc.ext.OverrideFilter2");
        } catch (Throwable t) {
            error = true;
            LOGGER.error(t.getMessage());
        }
        Assert.assertFalse(error);
        Assert.assertFalse(loader.all.isEmpty());
        Assert.assertTrue(loader.all.size() == 1);
        extensionClass = loader.getExtensionClass("ooo");
        Assert.assertTrue(extensionClass.getClazz() == OverrideFilter1.class);
        Assert.assertTrue(extensionClass.getOrder() == 2);
    }

    @Test
    public void testRejection() throws Exception {
        // test for rejection
        ExtensionLoader loader = new ExtensionLoader<Filter>(Filter.class, false, null);
        URL url = Filter.class.getResource("/META-INF/sofa-rpc/" + Filter.class.getName());
        try {
            loader.readLine(url, "com.alipay.sofa.rpc.ext.RejectionFilter"); // 0
            loader.readLine(url, "com.alipay.sofa.rpc.ext.RejectionFilter0"); // 0
            loader.readLine(url, "com.alipay.sofa.rpc.ext.RejectionFilter1"); // 0, 1
            loader.readLine(url, "com.alipay.sofa.rpc.ext.RejectionFilter2"); // 1, 2, (-3)
            loader.readLine(url, "com.alipay.sofa.rpc.ext.RejectionFilter3"); // 1, 2
            loader.readLine(url, "com.alipay.sofa.rpc.ext.RejectionFilter4"); // 2, 4
        } catch (Throwable t) {
            LOGGER.error(t.getMessage());
        }
        Assert.assertFalse(loader.all.isEmpty());
        Assert.assertTrue(loader.all.size() == 2);
        Assert.assertTrue(loader.getExtensionClass("rrr2") != null);
        Assert.assertTrue(loader.getExtensionClass("rrr4") != null);

        loader.all.clear();
        try {
            loader.readLine(url, "com.alipay.sofa.rpc.ext.RejectionFilter5"); // 5 (-6)
            loader.readLine(url, "com.alipay.sofa.rpc.ext.RejectionFilter6"); // 6
        } catch (Throwable t) {
            LOGGER.error(t.getMessage());
        }
        Assert.assertFalse(loader.all.isEmpty());
        Assert.assertTrue(loader.all.size() == 1);
        Assert.assertTrue(loader.getExtensionClass("rrr6") != null);
    }

    @Test
    public void testReadLine() throws Exception {
        ExtensionLoader loader = new ExtensionLoader<Filter>(Filter.class, false, null);
        URL url = Filter.class.getResource("/META-INF/sofa-rpc/" + Filter.class.getName());
        try {
            loader.readLine(url, "com.alipay.sofa.rpc.ext.NotFilter");
        } catch (Throwable t) {
            LOGGER.error(t.getMessage());
        }
        Assert.assertTrue(loader.all.isEmpty());

        try {
            loader.readLine(url, null);
        } catch (Throwable t) {
            LOGGER.error(t.getMessage());
        }
        Assert.assertTrue(loader.all.isEmpty());

        try {
            loader.readLine(url, "    ");
        } catch (Throwable t) {
            LOGGER.error(t.getMessage());
        }
        Assert.assertTrue(loader.all.isEmpty());

        loader.all.clear();
        try {
            loader.readLine(url, "com.alipay.sofa.rpc.ext.WrongFilter0");
        } catch (Throwable t) {
            LOGGER.error(t.getMessage());
        }
        try {
            loader.readLine(url, "com.alipay.sofa.rpc.ext.WrongFilter1");
        } catch (Throwable t) {
            LOGGER.error(t.getMessage());
        }
        try {
            loader.readLine(url, "com.alipay.sofa.rpc.ext.WrongFilter2");
        } catch (Throwable t) {
            LOGGER.error(t.getMessage());
        }
        try {
            loader.readLine(url, "com.alipay.sofa.rpc.ext.WrongFilter3");
        } catch (Throwable t) {
            LOGGER.error(t.getMessage());
        }
        try {
            loader.readLine(url, "w3=com.alipay.sofa.rpc.ext.WrongFilter4");
        } catch (Throwable t) {
            LOGGER.error(t.getMessage());
        }
        try {
            loader.readLine(url, "echo1=com.alipay.sofa.rpc.ext.ExtensionFilter");
        } catch (Throwable t) {
            LOGGER.error(t.getMessage());
        }
        Assert.assertTrue(loader.all.isEmpty());

        try {
            loader.readLine(url, "com.alipay.sofa.rpc.ext.RightFilter0");
        } catch (Throwable t) {
            LOGGER.error(t.getMessage());
        }
        Assert.assertFalse(loader.all.isEmpty());

        loader.all.clear();
        try {
            loader.readLine(url, "rightxx0=com.alipay.sofa.rpc.ext.RightFilter0");
        } catch (Throwable t) {
            LOGGER.error(t.getMessage());
        }
        Assert.assertFalse(loader.all.isEmpty());

        // 重复加载
        boolean isOk = true;
        try {
            loader.readLine(url, "com.alipay.sofa.rpc.ext.RightFilter0");
        } catch (Throwable t) {
            LOGGER.error(t.getMessage());
            isOk = false;
        }
        Assert.assertFalse(isOk);
        Assert.assertFalse(loader.all.isEmpty());
        Assert.assertTrue(loader.all.size() == 1);

        ExtensionLoader loader2 = new ExtensionLoader<Protocol>(Protocol.class, false, null);
        URL url2 = Filter.class.getResource("/META-INF/sofa-rpc/" + Protocol.class.getName());
        try {
            loader2.readLine(url2, "com.alipay.sofa.rpc.ext.WrongProtocol");
        } catch (Throwable t) {
            LOGGER.error(t.getMessage());
        }
        Assert.assertTrue(loader2.all.isEmpty());
    }

    @Test
    public void testParseAliasAndClassName() throws Exception {
        ExtensionLoader loader = new ExtensionLoader<Filter>(Filter.class);

        Assert.assertNull(loader.parseAliasAndClassName(null));
        Assert.assertNull(loader.parseAliasAndClassName(""));
        Assert.assertNull(loader.parseAliasAndClassName("    "));
        Assert.assertNull(loader.parseAliasAndClassName("\t"));
        Assert.assertNull(loader.parseAliasAndClassName("\r\n"));

        Assert.assertNull(loader.parseAliasAndClassName("    # xxxx"));
        Assert.assertNull(loader.parseAliasAndClassName("# xxxx"));
        Assert.assertNull(loader.parseAliasAndClassName("xxx="));

        Assert.assertArrayEquals(loader.parseAliasAndClassName("1111"), new String[] { null, "1111" });
        Assert.assertArrayEquals(loader.parseAliasAndClassName("  1111"), new String[] { null, "1111" });
        Assert.assertArrayEquals(loader.parseAliasAndClassName("  1111   "), new String[] { null, "1111" });
        Assert.assertArrayEquals(loader.parseAliasAndClassName("1111   "), new String[] { null, "1111" });
        Assert.assertArrayEquals(loader.parseAliasAndClassName("1111#aa"), new String[] { null, "1111" });
        Assert.assertArrayEquals(loader.parseAliasAndClassName("  1111#aa"), new String[] { null, "1111" });
        Assert.assertArrayEquals(loader.parseAliasAndClassName("1111#aa   "), new String[] { null, "1111" });
        Assert.assertArrayEquals(loader.parseAliasAndClassName("  1111#aa  "), new String[] { null, "1111" });
        Assert.assertArrayEquals(loader.parseAliasAndClassName("1111 #aa"), new String[] { null, "1111" });
        Assert.assertArrayEquals(loader.parseAliasAndClassName("  1111 #aa"), new String[] { null, "1111" });
        Assert.assertArrayEquals(loader.parseAliasAndClassName("1111 #aa   "), new String[] { null, "1111" });
        Assert.assertArrayEquals(loader.parseAliasAndClassName("  1111 #aa  "), new String[] { null, "1111" });

        Assert.assertArrayEquals(loader.parseAliasAndClassName("aa=1111"), new String[] { "aa", "1111" });
        Assert.assertArrayEquals(loader.parseAliasAndClassName("  aa=1111"), new String[] { "aa", "1111" });
        Assert.assertArrayEquals(loader.parseAliasAndClassName("aa=1111  "), new String[] { "aa", "1111" });
        Assert.assertArrayEquals(loader.parseAliasAndClassName("  aa=1111  "), new String[] { "aa", "1111" });
        Assert.assertArrayEquals(loader.parseAliasAndClassName("aa=1111#aa"), new String[] { "aa", "1111" });
        Assert.assertArrayEquals(loader.parseAliasAndClassName("  aa=1111#aa"), new String[] { "aa", "1111" });
        Assert.assertArrayEquals(loader.parseAliasAndClassName("aa=1111#aa  "), new String[] { "aa", "1111" });
        Assert.assertArrayEquals(loader.parseAliasAndClassName("  aa=1111#aa  "), new String[] { "aa", "1111" });
        Assert.assertArrayEquals(loader.parseAliasAndClassName("aa=1111  #aa"), new String[] { "aa", "1111" });
        Assert.assertArrayEquals(loader.parseAliasAndClassName("  aa=1111  #aa"), new String[] { "aa", "1111" });
        Assert.assertArrayEquals(loader.parseAliasAndClassName("aa=1111  #aa  "), new String[] { "aa", "1111" });
        Assert.assertArrayEquals(loader.parseAliasAndClassName("  aa=1111  #aa  "), new String[] { "aa", "1111" });
    }

    @Test
    public void testDynamicLoadExtension() {
        ExtensionLoader<Filter> extensionLoader = ExtensionLoaderFactory.getExtensionLoader(Filter.class);
        extensionLoader.loadExtension(DynamicFilter.class);
        Filter dynamic0 = extensionLoader.getExtension("dynamic0");
        Assert.assertTrue(dynamic0 instanceof DynamicFilter);
    }

    @Test
    public void testAddListener(){
        ExtensionLoader<Filter> extensionLoader = ExtensionLoaderFactory.getExtensionLoader(Filter.class);
        extensionLoader.loadExtension(DynamicFilter.class);
        ConcurrentMap<String, ExtensionClass<Filter>> all = extensionLoader.all;
        String alias = "dynamic0";
        Assert.assertTrue(all.containsKey(alias));


        List<String> filters = new ArrayList<>();
        extensionLoader = ExtensionLoaderFactory.getExtensionLoader(Filter.class);
        extensionLoader.addListener( new  ExtensionLoaderListener<Filter>() {
            @Override
            public void onLoad(ExtensionClass<Filter> extensionClass) {
                filters.add(extensionClass.getAlias());
            }
        });

        Assert.assertTrue(filters.contains(alias));

    }
}