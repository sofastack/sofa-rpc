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

import com.alipay.sofa.rpc.common.SofaConfigs;
import com.alipay.sofa.rpc.common.SofaOptions;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static com.alipay.sofa.rpc.common.utils.IOUtils.closeQuietly;

/**
 * Load blacklist from file.
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class BlackListFileLoader {

    private static final Logger      LOGGER                    = LoggerFactory.getLogger(BlackListFileLoader.class);

    public static final List<String> SOFA_SERIALIZE_BLACK_LIST = loadFile("/sofa-rpc/serialize_blacklist.txt");

    static List<String> loadFile(String path) {
        List<String> blackPrefixList = new ArrayList<String>();
        InputStream input = null;
        try {
            input = BlackListFileLoader.class.getResourceAsStream(path);
            if (input != null) {
                readToList(input, "UTF-8", blackPrefixList);
            }
            String overStr = SofaConfigs.getStringValue(SofaOptions.CONFIG_SERIALIZE_BLACKLIST_OVERRIDE, "");
            if (StringUtils.isNotBlank(overStr)) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Serialize blacklist will override with configuration: {}", overStr);
                }
                overrideBlackList(blackPrefixList, overStr);
            }
        } catch (Exception e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error(e.getMessage(), e);
            }
        } finally {
            closeQuietly(input);
        }
        return blackPrefixList;
    }

    /**
     * 读文件，将结果丢入List
     *
     * @param input           输入流程
     * @param encoding        编码
     * @param blackPrefixList 保持黑名单前缀的List
     */
    private static void readToList(InputStream input, String encoding, List<String> blackPrefixList) {
        InputStreamReader reader = null;
        BufferedReader bufferedReader = null;
        try {
            reader = new InputStreamReader(input, encoding);
            bufferedReader = new BufferedReader(reader);
            String lineText;
            while ((lineText = bufferedReader.readLine()) != null) {
                String pkg = lineText.trim();
                if (pkg.length() > 0) {
                    blackPrefixList.add(pkg);
                }
            }
        } catch (IOException e) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn(e.getMessage(), e);
            }
        } finally {
            closeQuietly(bufferedReader);
            closeQuietly(reader);
        }
    }

    /**
     * Override blacklist with override string.
     * 
     * @param originList Origin black list
     * @param overrideStr The override string
     */
    static void overrideBlackList(List<String> originList, String overrideStr) {
        List<String> adds = new ArrayList<String>();
        String[] overrideItems = StringUtils.splitWithCommaOrSemicolon(overrideStr);
        for (String overrideItem : overrideItems) {
            if (StringUtils.isNotBlank(overrideItem)) {
                if (overrideItem.startsWith("!") || overrideItem.startsWith("-")) {
                    overrideItem = overrideItem.substring(1);
                    if ("*".equals(overrideItem) || "default".equals(overrideItem)) {
                        originList.clear();
                    } else {
                        originList.remove(overrideItem);
                    }
                } else {
                    if (!originList.contains(overrideItem)) {
                        adds.add(overrideItem);
                    }
                }
            }
        }
        if (adds.size() > 0) {
            originList.addAll(adds);
        }
    }
}
