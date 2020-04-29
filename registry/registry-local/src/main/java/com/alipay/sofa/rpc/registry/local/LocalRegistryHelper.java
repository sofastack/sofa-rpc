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
package com.alipay.sofa.rpc.registry.local;

import com.alipay.sofa.rpc.client.ProviderGroup;
import com.alipay.sofa.rpc.client.ProviderHelper;
import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.client.ProviderInfoAttrs;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.SystemInfo;
import com.alipay.sofa.rpc.common.utils.CommonUtils;
import com.alipay.sofa.rpc.common.utils.FileUtils;
import com.alipay.sofa.rpc.common.utils.NetUtils;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.config.AbstractInterfaceConfig;
import com.alipay.sofa.rpc.config.ConfigUniqueNameGenerator;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import com.alipay.sofa.rpc.log.LogCodes;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;

import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Util method of local registry.
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class LocalRegistryHelper {

    /**
     * 日志
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(LocalRegistryHelper.class);

    /**
     * Check file's digest.
     *
     * @param address    the address
     * @param lastDigest the update digest
     * @return true被修改，false未被修改
     */
    public static boolean checkModified(String address, String lastDigest) {
        // 检查文件是否被修改了
        String newDigest = calMD5Checksum(address);
        return !StringUtils.equals(newDigest, lastDigest);
    }

    /**
     * 转为服务端提供者对象
     *
     * @param config 服务提供者配置
     * @param server 服务端
     * @return 本地服务提供者对象
     */
    public static ProviderInfo convertProviderToProviderInfo(ProviderConfig config, ServerConfig server) {
        ProviderInfo providerInfo = new ProviderInfo()
            .setPort(server.getPort())
            .setWeight(config.getWeight())
            .setSerializationType(config.getSerialization())
            .setProtocolType(server.getProtocol())
            .setPath(server.getContextPath())
            .setStaticAttrs(config.getParameters());
        String host = server.getHost();
        if (NetUtils.isLocalHost(host) || NetUtils.isAnyHost(host)) {
            host = SystemInfo.getLocalHost();
        }
        providerInfo.setHost(host);
        return providerInfo;
    }

    static Map<String, ProviderGroup> loadBackupFileToCache(String address) {

        Map<String, ProviderGroup> memoryCache = new ConcurrentHashMap<String, ProviderGroup>();
        // 从文件夹下读取指定文件
        File regFile = new File(address);
        if (!regFile.exists()) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Load backup file failure cause by can't found file: {}", regFile.getAbsolutePath());
            }
        } else {
            try {
                String content = FileUtils.file2String(regFile);
                Map<String, ProviderGroup> tmp = unMarshal(content);
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Load backup file from {}", regFile.getAbsolutePath());
                }
                // 加载到内存中
                if (tmp != null) {
                    memoryCache.putAll(tmp);
                }
            } catch (IOException e) {
                throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_READ_BACKUP_FILE,
                    regFile.getAbsolutePath()), e);
            }
        }
        return memoryCache;
    }

    static synchronized boolean backup(String address, Map<String, ProviderGroup> memoryCache) {
        // 先写一个lock文件，跨进程的锁
        File lockFile = new File(address + ".lock");
        while (lockFile.exists()) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Other process is writing, retry after 1s");
            }
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                // ignore
            }
            // 锁住超过10秒，删掉
            if (lockFile.exists()) {
                if ((RpcRuntimeContext.now() - lockFile.lastModified()) > 10000) {
                    boolean ret = lockFile.delete();
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn("Other process is locking over 60s, force release : {}", ret);
                    }
                } else {
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn("Other process is stilling writing, waiting 1s ...");
                    }
                }
            }
        }
        boolean created = false;
        try {
            // 写个锁文件
            lockFile.getParentFile().mkdirs();
            created = lockFile.createNewFile();
            if (!created) {
                // 写失败等待下次执行
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("Create lock file false, may be other process is writing. aborted");
                }
                return false;
            }

            // 得到备份的文件内容
            String content = StringUtils.defaultString(marshalCache(memoryCache));

            // 重命名文件，备份旧文件
            File regFile = new File(address);
            if (regFile.exists()) {
                if (!regFile.renameTo(new File(address + ".bak"))) {
                    regFile.delete();
                }
            }

            // 独占锁，写入文件
            if (regFile.createNewFile()) {
                RandomAccessFile randomAccessFile = null;
                FileLock lock = null;
                try {
                    randomAccessFile = new RandomAccessFile(regFile, "rw");
                    FileChannel fileChannel = randomAccessFile.getChannel();
                    // get an exclusive lock on this channel
                    lock = fileChannel.tryLock();
                    //FileLock lock = fileChannel.tryLock(0, Integer.MAX_VALUE, true);
                    fileChannel.write(RpcConstants.DEFAULT_CHARSET.encode(content));
                    fileChannel.force(false);
                } finally {
                    if (lock != null) {
                        lock.release();
                    }
                    if (randomAccessFile != null) {
                        randomAccessFile.close();
                    }
                }
                LOGGER.info("Write backup file to {}", regFile.getAbsolutePath());
                regFile.setLastModified(RpcRuntimeContext.now());
            }
        } catch (Exception e) {
            LOGGER.error("Backup registry file error !", e);
        } finally {
            if (created) {
                boolean deleted = lockFile.delete();
                if (!deleted) {
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn("Lock file create by this thread, but failed to delete it," +
                            " may be the elapsed time of this backup is too long");
                    }
                }
            }
        }
        return true;
    }

    private static String SEPARATORSTR = "\t";

    private static String marshalCache(Map<String, ProviderGroup> memoryCache) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, ProviderGroup> entry : memoryCache.entrySet()) {
            ProviderGroup group = entry.getValue();
            if (group != null) {
                List<ProviderInfo> ps = group.getProviderInfos();
                if (CommonUtils.isNotEmpty(ps)) {
                    sb.append(entry.getKey()).append(SEPARATORSTR);
                    for (ProviderInfo providerInfo : ps) {
                        sb.append(ProviderHelper.toUrl(providerInfo)).append(SEPARATORSTR);
                    }
                    sb.append(FileUtils.LINE_SEPARATOR);
                }
            }
        }
        return sb.toString();
    }

    private static Map<String, ProviderGroup> unMarshal(String context) {
        if (StringUtils.isBlank(context)) {
            return null;
        }
        Map<String, ProviderGroup> map = new HashMap<String, ProviderGroup>();
        String[] lines = StringUtils.split(context, FileUtils.LINE_SEPARATOR);
        for (String line : lines) {
            String[] fields = line.split(SEPARATORSTR);
            if (fields.length > 1) {
                String key = fields[0];
                Set<ProviderInfo> values = new HashSet<ProviderInfo>();
                for (int i = 1; i < fields.length; i++) {
                    String pstr = fields[i];
                    if (StringUtils.isNotEmpty(pstr)) {
                        ProviderInfo providerInfo = ProviderHelper.toProviderInfo(pstr);
                        providerInfo.setStaticAttr(ProviderInfoAttrs.ATTR_SOURCE, "local");
                        values.add(providerInfo);
                    }
                }
                map.put(key, new ProviderGroup(new ArrayList<ProviderInfo>(values)));
            }
        }
        return map;
    }

    /**
     * 服务注册中心的Key
     *
     * @param config   配置
     * @param protocol 协议
     * @return 返回值
     */
    static String buildListDataId(AbstractInterfaceConfig config, String protocol) {
        if (RpcConstants.PROTOCOL_TYPE_BOLT.equals(protocol)
            || RpcConstants.PROTOCOL_TYPE_TR.equals(protocol)) {
            return ConfigUniqueNameGenerator.getUniqueName(config) + "@DEFAULT";
        } else {
            return ConfigUniqueNameGenerator.getUniqueName(config) + "@" + protocol;
        }
    }

    private static byte[] createChecksum(String filename) {
        MessageDigest complete = null;
        try {
            complete = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            //ignore
        }
        String content = null;
        try {
            final File file = new File(filename);
            content = FileUtils.file2String(file);
        } catch (IOException e) {
            //ignore
        }

        if (content == null) {
            content = "";
        }
        byte[] digest = new byte[0];

        if (complete != null) {
            digest = complete.digest(content.getBytes());
        }

        return digest;
    }

    public static String calMD5Checksum(String filename) {
        byte[] b;
        b = createChecksum(filename);
        String digestInHex = DatatypeConverter.printHexBinary(b).toUpperCase();
        return digestInHex;
    }
}
